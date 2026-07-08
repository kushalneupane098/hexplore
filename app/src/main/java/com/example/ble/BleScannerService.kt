package com.example.ble

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.Intent
import android.content.BroadcastReceiver
import android.content.IntentFilter
import android.os.Build
import android.os.IBinder
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import java.util.concurrent.ConcurrentHashMap
import java.util.ArrayList

class BleScannerService : Service() {

    private val TAG = "BleScannerService"
    private val NOTIFICATION_ID = 8472
    private val CHANNEL_ID = "hexplore_ble_scanning_channel"

    private var bluetoothAdapter: BluetoothAdapter? = null
    private var bluetoothLeScanner: BluetoothLeScanner? = null
    private var isScanning = false

    // Thread-safe maps for debouncing and selecting the strongest beacon based on rolling average RSSI
    private val beaconRssiHistory = ConcurrentHashMap<String, ArrayList<Int>>()
    private val beaconLastSeen = ConcurrentHashMap<String, Long>()

    private val handler = Handler(Looper.getMainLooper())
    private val updateRunnable = object : Runnable {
        override fun run() {
            evaluateTrackedBeacons()
            handler.postDelayed(this, 1500) // check every 1.5 seconds
        }
    }

    private val restartScanRunnable = object : Runnable {
        override fun run() {
            if (isScanning) {
                Log.d(TAG, "Periodic scan restart: Stopping scan to refresh Bluetooth controller...")
                stopScanning()
                handler.postDelayed({
                    Log.d(TAG, "Periodic scan restart: Re-starting scan...")
                    startScanning()
                }, 1000)
            }
            handler.postDelayed(this, 90000) // restart scan every 90 seconds
        }
    }

    private val bluetoothStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val action = intent?.action
            if (action == BluetoothAdapter.ACTION_STATE_CHANGED) {
                val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                if (state == BluetoothAdapter.STATE_ON) {
                    Log.d(TAG, "Bluetooth turned ON. Starting scanner.")
                    initBluetooth()
                    startScanning()
                } else if (state == BluetoothAdapter.STATE_OFF) {
                    Log.d(TAG, "Bluetooth turned OFF. Stopping scanner.")
                    stopScanning()
                }
            }
        }
    }

    private val scanCallback = object : ScanCallback() {
        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            super.onScanResult(callbackType, result)
            result?.let {
                processScanResult(it)
            }
        }

        @SuppressLint("MissingPermission")
        override fun onBatchScanResults(results: MutableList<ScanResult>?) {
            super.onBatchScanResults(results)
            results?.forEach { result ->
                processScanResult(result)
            }
        }

        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
            Log.e(TAG, "BLE Scan Failed with error code: $errorCode")
        }
    }

    @Synchronized
    private fun trackBeaconRssi(uid: String, rssi: Int) {
        val currentTime = System.currentTimeMillis()
        beaconLastSeen[uid] = currentTime
        val readings = beaconRssiHistory.getOrPut(uid) { ArrayList() }
        readings.add(rssi)
        if (readings.size > 5) {
            readings.removeAt(0)
        }
    }

    @Synchronized
    private fun evaluateTrackedBeacons() {
        val currentTime = System.currentTimeMillis()
        
        // Prune beacons not seen in the last 6 seconds
        val iterator = beaconLastSeen.entries.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (currentTime - entry.value > 12000) {
                beaconRssiHistory.remove(entry.key)
                iterator.remove()
            }
        }
        
        // Calculate average RSSI for all active tracked beacons
        val averages = beaconRssiHistory.mapValues { entry ->
            entry.value.average()
        }
        
        // Find the beacon with the absolute strongest average RSSI
        val strongestBeacon = averages.maxByOrNull { it.value }
        
        if (strongestBeacon != null) {
            val strongestUid = strongestBeacon.key
            val avgRssi = strongestBeacon.value.toInt()
            Log.d(TAG, "Strongest beacon selected: $strongestUid, average RSSI=$avgRssi (active: ${averages.size})")
            BleSignalTracker.updateBeacon(strongestUid, avgRssi)
        } else {
            // No active beacons are in range (all decayed/pruned or none detected)
            if (BleSignalTracker.detectedBeacon.value != null) {
                Log.d(TAG, "No beacons in range. Clearing tracker.")
                BleSignalTracker.clearBeacon()
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun processScanResult(result: ScanResult) {
        val rssi = result.rssi
        
        // 1. Check by Device Name
        val deviceName = (result.device.name ?: result.scanRecord?.deviceName)?.trim()?.uppercase()
        if (deviceName != null && deviceName.contains("HEX_BEACON")) {
            // Robustly extract the beacon number
            val match = Regex("""HEX_BEACON_?(\d+)""").find(deviceName)
            val num = match?.groupValues?.get(1)?.toIntOrNull()
            if (num != null) {
                val uid = "HEX_BEACON_" + num.toString().padStart(2, '0')
                Log.d(TAG, "Detected beacon by name: $uid (original: $deviceName), RSSI=$rssi")
                trackBeaconRssi(uid, rssi)
                return
            }
        }

        // 2. Fallback to standard iBeacon decoding (Major=1, Minor=1..6)
        val iBeacon = parseIBeacon(result)
        if (iBeacon != null) {
            val uid = "HEX_BEACON_" + iBeacon.minor.toString().padStart(2, '0')
            Log.d(TAG, "Detected iBeacon packet: Major=${iBeacon.major}, Minor=${iBeacon.minor} -> $uid, RSSI=$rssi")
            trackBeaconRssi(uid, rssi)
            return
        }

        // 3. Fallback to custom manufacturer data (loops through all manufacturer keys)
        val mfgDataArray = result.scanRecord?.manufacturerSpecificData
        if (mfgDataArray != null) {
            for (i in 0 until mfgDataArray.size()) {
                val key = mfgDataArray.keyAt(i)
                val value = mfgDataArray.valueAt(i)
                if (value != null) {
                    val valueStr = String(value, Charsets.UTF_8).trim()
                    if (valueStr.contains("HEX_BEACON")) {
                        val match = Regex("""HEX_BEACON_?(\d+)""").find(valueStr)
                        val num = match?.groupValues?.get(1)?.toIntOrNull()
                        if (num != null) {
                            val uid = "HEX_BEACON_" + num.toString().padStart(2, '0')
                            Log.d(TAG, "Detected beacon by manufacturer data (ID=$key): $uid, RSSI=$rssi")
                            trackBeaconRssi(uid, rssi)
                            return
                        }
                    }
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "BleScannerService Created")
        createNotificationChannel()
        initBluetooth()
        handler.post(updateRunnable)
        handler.postDelayed(restartScanRunnable, 90000) // schedule first scan restart in 90 seconds
        
        // Register Bluetooth state changes listener
        val filter = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        registerReceiver(bluetoothStateReceiver, filter)
    }

    @SuppressLint("ForegroundServiceType")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "BleScannerService StartCommand received")
        
        // Start as Foreground Service
        val notification = createNotification("Initializing HEX Spatial Radar...")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        startScanning()
        return START_STICKY
    }

    override fun onDestroy() {
        Log.d(TAG, "BleScannerService Destroyed")
        try {
            unregisterReceiver(bluetoothStateReceiver)
        } catch (e: Exception) {
            Log.e(TAG, "Error unregistering receiver", e)
        }
        handler.removeCallbacks(updateRunnable)
        handler.removeCallbacks(restartScanRunnable)
        stopScanning()
        BleSignalTracker.setScanningState(false)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null // We don't bind, we just start/stop
    }

    private fun initBluetooth() {
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        if (bluetoothManager == null) {
            Log.e(TAG, "Bluetooth Service not available")
            return
        }
        bluetoothAdapter = bluetoothManager.adapter
        if (bluetoothAdapter == null) {
            Log.e(TAG, "Bluetooth Adapter not available")
            return
        }
        bluetoothLeScanner = bluetoothAdapter?.bluetoothLeScanner
        if (bluetoothLeScanner == null) {
            Log.w(TAG, "BluetoothLeScanner is null. Bluetooth might be turned off.")
        }
    }

    @SuppressLint("MissingPermission")
    private fun startScanning() {
        if (isScanning) return
        
        // Try to get scanner again in case bluetooth was turned on after service start
        if (bluetoothLeScanner == null) {
            bluetoothLeScanner = bluetoothAdapter?.bluetoothLeScanner
        }

        val scanner = bluetoothLeScanner
        if (scanner == null) {
            Log.e(TAG, "Cannot start scan: BluetoothLeScanner is null")
            updateNotification("Error: Please enable Bluetooth")
            return
        }

        try {
            val settings = ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build()

            scanner.startScan(null, settings, scanCallback)
            isScanning = true
            BleSignalTracker.setScanningState(true)
            Log.d(TAG, "BLE Hardware Scan Started Successfully")
            updateNotification("HEX Spatial Radar Active — Scanning for Beacons...")
        } catch (e: SecurityException) {
            Log.e(TAG, "Missing bluetooth scan permissions", e)
            updateNotification("Permission Required for Scanning")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start BLE Scan", e)
            updateNotification("Failed to start spatial scanning")
        }
    }

    @SuppressLint("MissingPermission")
    private fun stopScanning() {
        if (!isScanning) return
        try {
            bluetoothLeScanner?.stopScan(scanCallback)
            Log.d(TAG, "BLE Scan Stopped")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping BLE scan", e)
        } finally {
            isScanning = false
            BleSignalTracker.setScanningState(false)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "HEXplore Indoor Spatial Scanner",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Monitors Esp32 iBeacons to trigger automatic campus navigation"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(serviceChannel)
        }
    }

    private fun createNotification(contentText: String): Notification {
        val notificationIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        
        val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent, pendingIntentFlags
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("HEXplore Spatial Radar")
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.stat_sys_data_bluetooth) // standard system icon fallback
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
    }

    private fun updateNotification(text: String) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        notificationManager?.notify(NOTIFICATION_ID, createNotification(text))
    }

    /**
     * Ultra-robust iBeacon packet decoder.
     * Uses OS-parsed manufacturer data first, falling back to raw advertisement byte scanning if needed.
     */
    private fun parseIBeacon(result: ScanResult): IBeaconData? {
        try {
            val manufacturerData = result.scanRecord?.getManufacturerSpecificData(0x004C)
            if (manufacturerData != null && manufacturerData.size >= 23) {
                // Byte 0: 0x02 (iBeacon Type)
                // Byte 1: 0x15 (iBeacon Length)
                if (manufacturerData[0] == 0x02.toByte() && manufacturerData[1] == 0x15.toByte()) {
                    val major = ((manufacturerData[18].toInt() and 0xFF) shl 8) or (manufacturerData[19].toInt() and 0xFF)
                    val minor = ((manufacturerData[20].toInt() and 0xFF) shl 8) or (manufacturerData[21].toInt() and 0xFF)
                    return IBeaconData(major, minor)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing manufacturer specific data", e)
        }

        val scanRecord = result.scanRecord?.bytes ?: return null
        if (scanRecord.size < 30) return null
        
        var startIdx = 0
        while (startIdx <= scanRecord.size - 25) {
            // Check for Apple Company code (0x4C, 0x00 - Little Endian) and iBeacon prefix (0x0215)
            if (scanRecord[startIdx] == 0x4C.toByte() && 
                scanRecord[startIdx + 1] == 0x00.toByte() && 
                scanRecord[startIdx + 2] == 0x02.toByte() && 
                scanRecord[startIdx + 3] == 0x15.toByte()) {
                
                // Major is at startIdx + 20 and startIdx + 21
                val major = ((scanRecord[startIdx + 20].toInt() and 0xFF) shl 8) or 
                            (scanRecord[startIdx + 21].toInt() and 0xFF)
                
                // Minor is at startIdx + 22 and startIdx + 23
                val minor = ((scanRecord[startIdx + 22].toInt() and 0xFF) shl 8) or 
                            (scanRecord[startIdx + 23].toInt() and 0xFF)
                            
                return IBeaconData(major, minor)
            }
            startIdx++
        }
        return null
    }

    private data class IBeaconData(val major: Int, val minor: Int)
}
