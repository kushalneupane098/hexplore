package com.example.ble

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object BleSignalTracker {
    private val _detectedBeacon = MutableStateFlow<BeaconSignal?>(null)
    val detectedBeacon: StateFlow<BeaconSignal?> = _detectedBeacon.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    fun updateBeacon(uid: String, rssi: Int) {
        _detectedBeacon.value = BeaconSignal(uid, rssi, System.currentTimeMillis())
    }

    fun clearBeacon() {
        _detectedBeacon.value = null
    }

    fun setScanningState(scanning: Boolean) {
        _isScanning.value = scanning
    }
}

data class BeaconSignal(
    val uid: String,
    val rssi: Int,
    val timestamp: Long
)
