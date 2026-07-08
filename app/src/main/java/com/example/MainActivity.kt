package com.example

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.VideogameAsset
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ble.BleViewModel
import com.example.ble.BleViewModelFactory
import com.example.data.PlaceWithDetails
import com.example.ui.components.GlassmorphicCard
import com.example.ui.theme.DarkBg
import com.example.ui.theme.DeepNavy
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.NeonBlue
import com.example.ui.theme.TranslucentSurface
import com.example.ui.theme.WhiteTranslucent
import com.example.ui.RevolutionizedRadar
import com.example.ui.LocationImage
import com.example.ui.getIconForLocation
import com.example.ui.CoilImageWithFallback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.example.data.JsonPathway
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.flow.MutableStateFlow

class MainActivity : ComponentActivity(), SensorEventListener {
    private lateinit var sensorManager: SensorManager
    private var rotationSensor: Sensor? = null
    private var accelerometer: Sensor? = null
    private var magnetometer: Sensor? = null

    private val _compassHeading = MutableStateFlow(0f)

    private var gravity = FloatArray(3)
    private var geomagnetic = FloatArray(3)
    private var hasGravity = false
    private var hasGeomagnetic = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                HexAppMainContainer(compassHeadingFlow = _compassHeading)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        rotationSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        } ?: run {
            accelerometer?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI) }
            magnetometer?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI) }
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_ROTATION_VECTOR) {
            val rotationMatrix = FloatArray(9)
            SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
            val orientation = FloatArray(3)
            SensorManager.getOrientation(rotationMatrix, orientation)
            val azimuthInRadians = orientation[0]
            var azimuthInDegrees = Math.toDegrees(azimuthInRadians.toDouble()).toFloat()
            if (azimuthInDegrees < 0) {
                azimuthInDegrees += 360f
            }
            _compassHeading.value = azimuthInDegrees
        } else if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            gravity = event.values.clone()
            hasGravity = true
        } else if (event.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
            geomagnetic = event.values.clone()
            hasGeomagnetic = true
        }

        if (hasGravity && hasGeomagnetic) {
            val r = FloatArray(9)
            val i = FloatArray(9)
            if (SensorManager.getRotationMatrix(r, i, gravity, geomagnetic)) {
                val orientation = FloatArray(3)
                SensorManager.getOrientation(r, orientation)
                val azimuthInRadians = orientation[0]
                var azimuthInDegrees = Math.toDegrees(azimuthInRadians.toDouble()).toFloat()
                if (azimuthInDegrees < 0) {
                    azimuthInDegrees += 360f
                }
                
                val current = _compassHeading.value
                var diff = azimuthInDegrees - current
                while (diff < -180) diff += 360
                while (diff > 180) diff -= 360
                _compassHeading.value = (current + diff * 0.18f + 360f) % 360f
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}

@Composable
fun HexAppMainContainer(compassHeadingFlow: MutableStateFlow<Float>) {
    val context = LocalContext.current
    val viewModel: BleViewModel = viewModel(factory = BleViewModelFactory(context))

    val currentPlace by viewModel.currentPlace.collectAsState()
    val allPlaces by viewModel.allPlaces.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()
    val completedQuests by viewModel.completedQuests.collectAsState()
    val compassHeading by compassHeadingFlow.collectAsState()

    var selectedTab by remember { mutableStateOf(0) }
    var hasBlePermissions by remember { mutableStateOf(false) }

    val requiredPermissions = remember {
        val list = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            list.add(Manifest.permission.BLUETOOTH_SCAN)
            list.add(Manifest.permission.BLUETOOTH_CONNECT)
        }
        list.add(Manifest.permission.ACCESS_FINE_LOCATION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            list.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        list
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        hasBlePermissions = results.values.all { it }
        if (hasBlePermissions) {
            viewModel.startScannerService(context)
        } else {
            Toast.makeText(context, "Bluetooth & Location Permissions are required.", Toast.LENGTH_LONG).show()
        }
    }

    LaunchedEffect(Unit) {
        val granted = requiredPermissions.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
        hasBlePermissions = granted
        if (granted) {
            viewModel.startScannerService(context)
        } else {
            launcher.launch(requiredPermissions.toTypedArray())
        }
    }

    val detectedBeacon by com.example.ble.BleSignalTracker.detectedBeacon.collectAsState()
    val activeRadarPlace = allPlaces.find { it.place.uid == detectedBeacon?.uid }
        ?: currentPlace
        ?: allPlaces.find { it.place.uid == "HEX_BEACON_05" }
        ?: allPlaces.firstOrNull()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.statusBars,
        bottomBar = {
            GlassmorphicCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .navigationBarsPadding(),
                shape = RoundedCornerShape(24.dp),
                borderColor = ElectricCyan.copy(alpha = 0.25f),
                backgroundColor = DeepNavy.copy(alpha = 0.85f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BottomNavItem(
                        icon = Icons.Default.Radar,
                        label = "Explore",
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 }
                    )
                    BottomNavItem(
                        icon = Icons.Default.LocationOn,
                        label = "Zones",
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 }
                    )
                    BottomNavItem(
                        icon = Icons.Default.Star,
                        label = "Me",
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 }
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF1B263B),
                            Color(0xFF0D1B2A),
                            Color(0xFF040A12)
                        )
                    )
                )
                .padding(paddingValues)
        ) {
            if (allPlaces.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF040A12)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Image(
                            painter = painterResource(id = com.example.R.drawable.logo),
                            contentDescription = "App Logo",
                            modifier = Modifier
                                .size(96.dp)
                                .clip(RoundedCornerShape(24.dp))
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = "Welcome to HEX 2083",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Initializing database...",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = ElectricCyan,
                            letterSpacing = 1.sp
                        )
                    }
                }
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    if (selectedTab == 0) {
                        HeaderSection(isScanning = isScanning)
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        when (selectedTab) {
                            0 -> ExploreScreen(
                                activePlace = activeRadarPlace,
                                allPlaces = allPlaces,
                                isScanning = isScanning,
                                onPlaceClick = { viewModel.selectPlaceManually(it) },
                                detectedBeacon = detectedBeacon,
                                compassHeading = compassHeading
                            )
                            1 -> ZonesScreen(
                                allPlaces = allPlaces,
                                detectedBeacon = detectedBeacon,
                                onPlaceClick = { viewModel.selectPlaceManually(it) }
                            )
                            2 -> MeScreen(
                                viewModel = viewModel,
                                allPlaces = allPlaces
                            )
                        }
                    }
                }
            }
                    androidx.compose.animation.AnimatedVisibility(
                        visible = currentPlace != null,
                        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        currentPlace?.let { place ->
                            var isLevel3Expanded by remember { mutableStateOf(false) }

                            LaunchedEffect(place.place.uid) {
                                isLevel3Expanded = false
                            }

                            val isWithinProximity = (detectedBeacon != null && detectedBeacon?.uid == place.place.uid)

                            if (isLevel3Expanded) {
                                Level3DeepDivePanel(
                                    place = place,
                                    isWithinProximity = isWithinProximity,
                                    completedQuests = completedQuests,
                                    onToggleQuest = { viewModel.toggleQuestCompletion(it) },
                                    onBack = { isLevel3Expanded = false },
                                    onDismiss = { viewModel.clearCurrentPlace() }
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.Black.copy(alpha = 0.5f))
                                        .clickable { viewModel.clearCurrentPlace() },
                                    contentAlignment = Alignment.BottomCenter
                                ) {
                                    Level2ProximityDiscovery(
                                        place = place,
                                        isWithinProximity = isWithinProximity,
                                        onExploreClick = { isLevel3Expanded = true },
                                        onDismiss = { viewModel.clearCurrentPlace() },
                                        modifier = Modifier.clickable(enabled = false) { /* Prevent click through */ }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

@Composable
fun BottomNavItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .testTag("nav_item_${label.lowercase()}"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (selected) ElectricCyan else Color.White.copy(alpha = 0.4f),
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Bold,
            color = if (selected) ElectricCyan else Color.White.copy(alpha = 0.4f)
        )
    }
}

@Composable
fun ExploreScreen(
    activePlace: PlaceWithDetails?,
    allPlaces: List<PlaceWithDetails>,
    isScanning: Boolean,
    onPlaceClick: (PlaceWithDetails) -> Unit,
    detectedBeacon: com.example.ble.BeaconSignal?,
    compassHeading: Float = 0f
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val centerPlace = if (isScanning && detectedBeacon != null) activePlace else null
                
                RevolutionizedRadar(
                    centerPlace = centerPlace,
                    onBubbleClick = onPlaceClick,
                    allPlaces = allPlaces,
                    isScanning = isScanning,
                    heading = compassHeading
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = if (isScanning) {
                        if (detectedBeacon != null) "Department Zone Unlocked" else "Scanning HCOE Campus..."
                    } else "Radar Offline",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(6.dp))
                
                Text(
                    text = if (isScanning) {
                        if (detectedBeacon != null) "You are currently near ${activePlace?.place?.locationName}. Explore directions or dive deep below." 
                        else "Approach a department zone or project stall to automatically unlock exhibits!"
                    } else "Enable the scanning radar to explore the Himalaya Exhibition.",
                    color = WhiteTranslucent,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
            }
        }

        if (isScanning && detectedBeacon != null && activePlace != null) {
            item {
                GlassmorphicCard(
                    onClick = { onPlaceClick(activePlace) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    borderColor = ElectricCyan.copy(alpha = 0.3f),
                    backgroundColor = TranslucentSurface.copy(alpha = 0.5f)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LocationImage(
                            imageName = activePlace.place.imageAsset,
                            contentDescription = activePlace.place.locationName,
                            modifier = Modifier
                                .size(72.dp)
                                .clip(RoundedCornerShape(16.dp))
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "ACTIVE ZONE",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = ElectricCyan,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = activePlace.place.locationName,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Tap to Explore showcases & games",
                                fontSize = 12.sp,
                                color = WhiteTranslucent
                            )
                        }
                    }
                }
            }
        }
        
        item {
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
fun ZonesScreen(
    allPlaces: List<PlaceWithDetails>,
    detectedBeacon: com.example.ble.BeaconSignal?,
    onPlaceClick: (PlaceWithDetails) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // 1. STATS OVERVIEW HEADER CARD
        val activeCount = if (detectedBeacon != null) 1 else 0
        val totalProjects = allPlaces.sumOf { it.showcases.size }
        val totalGames = allPlaces.sumOf { it.games.size }

        GlassmorphicCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            shape = RoundedCornerShape(20.dp),
            borderColor = ElectricCyan.copy(alpha = 0.15f),
            backgroundColor = DeepNavy.copy(alpha = 0.45f)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "TOTAL ZONES",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = WhiteTranslucent,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "${allPlaces.size}",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = ElectricCyan
                    )
                }
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(24.dp)
                        .background(WhiteTranslucent.copy(alpha = 0.2f))
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "PROJECTS",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = WhiteTranslucent,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "$totalProjects",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                }
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(24.dp)
                        .background(WhiteTranslucent.copy(alpha = 0.2f))
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "ACTIVE",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = WhiteTranslucent,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = if (activeCount > 0) "1 NEARBY" else "SCANNING",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = if (activeCount > 0) Color(0xFF2DCE89) else Color(0xFFFFD700)
                    )
                }
            }
        }

        Text(
            text = "HCOE EXHIBITION DIRECTORY",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = ElectricCyan,
            letterSpacing = 1.5.sp,
            modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
        )
        Text(
            text = "Click any department zone to preview its info. Accessing student project showcases and live challenges requires in-person proximity.",
            fontSize = 11.sp,
            color = WhiteTranslucent,
            modifier = Modifier.padding(start = 4.dp, bottom = 16.dp),
            lineHeight = 16.sp
        )

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(allPlaces) { place ->
                val isNearby = (detectedBeacon != null && detectedBeacon.uid == place.place.uid)

                GlassmorphicCard(
                    onClick = { onPlaceClick(place) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("zone_card_${place.place.uid}"),
                    shape = RoundedCornerShape(20.dp),
                    borderColor = if (isNearby) Color(0xFF2DCE89).copy(alpha = 0.45f) else Color.White.copy(alpha = 0.08f),
                    backgroundColor = if (isNearby) Color(0xFF0C1D18).copy(alpha = 0.6f) else TranslucentSurface.copy(alpha = 0.4f)
                ) {
                    val context = LocalContext.current
                    val localPhotoResId = remember(place.place.imageAsset) {
                        val res = context.resources.getIdentifier(place.place.imageAsset, "drawable", context.packageName)
                        if (res != 0) res else com.example.R.drawable.img_zone_default
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Custom Coil Thumbnail loading
                        CoilImageWithFallback(
                            imageUrl = place.place.imageUrl,
                            fallbackIcon = getIconForLocation(place.place.imageAsset),
                            contentDescription = place.place.locationName,
                            modifier = Modifier
                                .size(80.dp)
                                .clip(RoundedCornerShape(14.dp)),
                            localFallbackResId = localPhotoResId
                        )

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f, fill = false)
                                ) {
                                    Text(
                                        text = place.place.locationName,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (place.place.isVisited) {
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = "Visited",
                                            tint = Color(0xFF2DCE89),
                                            modifier = Modifier.size(13.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isNearby) "NEARBY" else "PREVIEW",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (isNearby) Color(0xFF2DCE89) else Color(0xFFFFD700),
                                    modifier = Modifier
                                        .background(
                                            if (isNearby) Color(0xFF2DCE89).copy(alpha = 0.15f) else Color(0xFFFFD700).copy(alpha = 0.12f),
                                            RoundedCornerShape(6.dp)
                                        )
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = place.place.shortDescription,
                                fontSize = 12.sp,
                                color = WhiteTranslucent,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                lineHeight = 16.sp
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // Micro info tags
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(6.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.School,
                                        contentDescription = null,
                                        tint = ElectricCyan.copy(alpha = 0.8f),
                                        modifier = Modifier.size(10.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "${place.showcases.size} Projects",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = WhiteTranslucent
                                    )
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(6.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Gamepad,
                                        contentDescription = null,
                                        tint = ElectricCyan.copy(alpha = 0.8f),
                                        modifier = Modifier.size(10.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "${place.games.size} Quests",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = WhiteTranslucent
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "Explore Zone",
                            tint = WhiteTranslucent.copy(alpha = 0.4f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun MeScreen(
    viewModel: BleViewModel,
    allPlaces: List<PlaceWithDetails>
) {
    val completedQuests by viewModel.completedQuests.collectAsState()
    
    val totalGamesCount = remember(allPlaces) {
        allPlaces.sumOf { it.games.size }
    }
    
    val xp = completedQuests.size * 100
    
    val rank = when {
        completedQuests.size >= 5 -> "Exhibition Grandmaster"
        completedQuests.size >= 3 -> "HCOE Explorer Pro"
        completedQuests.size >= 1 -> "Cyber Scout"
        else -> "Exhibition Guest"
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
        }

        item {
            GlassmorphicCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                borderColor = ElectricCyan.copy(alpha = 0.25f),
                backgroundColor = DeepNavy.copy(alpha = 0.65f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .background(ElectricCyan.copy(alpha = 0.15f), CircleShape)
                            .border(2.dp, ElectricCyan, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Group,
                            contentDescription = "Avatar",
                            tint = ElectricCyan,
                            modifier = Modifier.size(40.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "EXHIBITION EXPLORER",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = ElectricCyan,
                        letterSpacing = 2.sp
                    )
                    Text(
                        text = "HCOE Smart Guest",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                    Text(
                        text = rank,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFFFFD700)
                    )

                    Spacer(modifier = Modifier.height(20.dp))
                    HorizontalDivider(color = Color.White.copy(alpha = 0.08f), thickness = 1.dp)
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "TOTAL XP",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = WhiteTranslucent
                            )
                            Text(
                                text = "$xp XP",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = ElectricCyan
                            )
                        }

                        Spacer(
                            modifier = Modifier
                                .width(1.dp)
                                .height(32.dp)
                                .background(Color.White.copy(alpha = 0.08f))
                        )

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "QUESTS DONE",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = WhiteTranslucent
                            )
                            Text(
                                text = "${completedQuests.size} / $totalGamesCount",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }

        item {
            Text(
                text = "YOUR ACCOMPLISHMENTS",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = ElectricCyan,
                letterSpacing = 1.5.sp,
                modifier = Modifier.padding(start = 4.dp)
            )
        }

        if (completedQuests.isEmpty()) {
            item {
                GlassmorphicCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    borderColor = Color.White.copy(alpha = 0.05f),
                    backgroundColor = TranslucentSurface.copy(alpha = 0.25f)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.HelpOutline,
                            contentDescription = null,
                            tint = WhiteTranslucent,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No quests completed yet",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Explore the exhibition stalls in person to activate live arcade games and mark them as completed!",
                            color = WhiteTranslucent,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        } else {
            items(completedQuests.toList()) { questTitle ->
                GlassmorphicCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    borderColor = Color(0xFF2DCE89).copy(alpha = 0.3f),
                    backgroundColor = Color(0xFF0F261D).copy(alpha = 0.45f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Completed badge",
                                tint = Color(0xFF2DCE89),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = questTitle,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "Exhibition Challenge Cleared",
                                    fontSize = 11.sp,
                                    color = Color(0xFF2DCE89)
                                )
                            }
                        }
                        
                        Text(
                            text = "+100 XP",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF2DCE89)
                        )
                    }
                }
            }
            
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        completedQuests.forEach { viewModel.toggleQuestCompletion(it) }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.12f)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .border(1.dp, Color.Red.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                ) {
                    Text(
                        text = "RESET ALL ACHIEVEMENTS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.Red
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}

// ==========================================
// LEVEL 2: PROXIMITY DISCOVERY BOTTOM SHEET
// ==========================================

@Composable
fun Level2ProximityDiscovery(
    place: PlaceWithDetails,
    isWithinProximity: Boolean,
    onExploreClick: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    GlassmorphicCard(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .padding(16.dp)
            .navigationBarsPadding(),
        shape = RoundedCornerShape(32.dp),
        borderColor = if (isWithinProximity) ElectricCyan.copy(alpha = 0.4f) else Color(0xFFFFD700).copy(alpha = 0.3f),
        backgroundColor = DeepNavy.copy(alpha = 0.94f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isWithinProximity) Icons.Default.LocationOn else Icons.Default.Info,
                        contentDescription = null,
                        tint = if (isWithinProximity) ElectricCyan else Color(0xFFFFD700),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isWithinProximity) "PROXIMITY UNLOCKED" else "PREVIEW / REMOTE STATE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = if (isWithinProximity) ElectricCyan else Color(0xFFFFD700),
                        letterSpacing = 1.sp
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close Sheet",
                        tint = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LocationImage(
                    imageName = place.place.imageAsset,
                    contentDescription = place.place.locationName,
                    modifier = Modifier
                        .size(88.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .border(1.5.dp, if (isWithinProximity) ElectricCyan.copy(alpha = 0.3f) else Color(0xFFFFD700).copy(alpha = 0.2f), RoundedCornerShape(20.dp))
                )

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = place.place.locationName,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = place.place.shortDescription,
                        fontSize = 13.sp,
                        color = WhiteTranslucent,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 18.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onExploreClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isWithinProximity) ElectricCyan else Color(0xFFFFD700)
                ),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("explore_deep_dive_button")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = if (isWithinProximity) "Explore this Zone →" else "Preview Zone Info",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = DeepNavy
                    )
                }
            }
        }
    }
}

// ==========================================
// LEVEL 3: DEEP-DIVE FULLSCREEN IMMERSIVE PANEL
// ==========================================

@Composable
fun Level3DeepDivePanel(
    place: PlaceWithDetails,
    isWithinProximity: Boolean,
    completedQuests: Set<String>,
    onToggleQuest: (String) -> Unit,
    onBack: () -> Unit,
    onDismiss: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabTitles = listOf("ABOUT", "SHOWCASES", "LIVE GAMES")

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            ) {
                LocationImage(
                    imageName = place.place.imageAsset,
                    contentDescription = place.place.locationName,
                    modifier = Modifier.fillMaxSize()
                )

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, DarkBg)
                            )
                        )
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                            .size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Go Back",
                            tint = Color.White
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                            .size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Exit",
                            tint = Color.White
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp)
                ) {
                    Text(
                        text = "DEEP-DIVE DETAILED SUMMARY",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = ElectricCyan,
                        letterSpacing = 1.5.sp
                    )
                    Text(
                        text = place.place.locationName,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                }
            }

            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = TranslucentSurface.copy(alpha = 0.4f),
                contentColor = Color.White,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = ElectricCyan
                    )
                },
                divider = { HorizontalDivider(color = ElectricCyan.copy(alpha = 0.12f)) }
            ) {
                tabTitles.forEachIndexed { index, title ->
                    val isSelected = selectedTab == index
                    Tab(
                        selected = isSelected,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                text = title,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Bold,
                                color = if (isSelected) ElectricCyan else WhiteTranslucent,
                                letterSpacing = 1.sp
                            )
                        }
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(16.dp)
            ) {
                when (selectedTab) {
                    0 -> TabAboutContent(place = place)
                    1 -> TabShowcasesContent(place = place, isWithinProximity = isWithinProximity)
                    2 -> TabGamesContent(
                        place = place,
                        isWithinProximity = isWithinProximity,
                        completedQuests = completedQuests,
                        onToggleQuest = onToggleQuest
                    )
                }
            }
        }
    }
}

// ==========================================
// LEVEL 3: TAB CONTENT DETAILS
// ==========================================

@Composable
fun TabAboutContent(place: PlaceWithDetails) {
    val context = LocalContext.current
    val pathways = remember(place.place.aboutNavigationJson) {
        if (!place.place.aboutNavigationJson.isNullOrEmpty()) {
            try {
                val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
                val type = Types.newParameterizedType(List::class.java, JsonPathway::class.java)
                val adapter = moshi.adapter<List<JsonPathway>>(type)
                adapter.fromJson(place.place.aboutNavigationJson)
            } catch (e: Exception) {
                null
            }
        } else {
            null
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            GlassmorphicCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                backgroundColor = TranslucentSurface.copy(alpha = 0.35f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    Text(
                        text = "ABOUT THE ZONE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = ElectricCyan,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = place.place.about,
                        fontSize = 14.sp,
                        color = Color.White,
                        lineHeight = 22.sp
                    )
                }
            }
        }

        item {
            GlassmorphicCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                backgroundColor = TranslucentSurface.copy(alpha = 0.35f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.School,
                        contentDescription = null,
                        tint = ElectricCyan,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = "Himalaya College of Engineering",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Avenue of HCOE, Lalitpur, Nepal",
                            color = WhiteTranslucent,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        pathways?.let { pathwayList ->
            if (pathwayList.isNotEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                    ) {
                        Text(
                            text = "NEARBY DESTINATIONS & PATHWAYS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = ElectricCyan,
                            letterSpacing = 1.5.sp,
                            modifier = Modifier.padding(start = 4.dp, bottom = 12.dp)
                        )
                        
                        pathwayList.forEach { pathway ->
                            GlassmorphicCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 14.dp),
                                shape = RoundedCornerShape(20.dp),
                                backgroundColor = TranslucentSurface.copy(alpha = 0.3f),
                                borderColor = Color.White.copy(alpha = 0.05f)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp)
                                ) {
                                    Text(
                                        text = pathway.title,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = pathway.desc,
                                        fontSize = 12.sp,
                                        color = WhiteTranslucent,
                                        lineHeight = 18.sp
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    
                                    if (pathway.images.isNotEmpty()) {
                                        if (pathway.images.size == 1) {
                                            val imgResName = pathway.images[0]
                                            val imgResId = remember(imgResName) {
                                                context.resources.getIdentifier(imgResName, "drawable", context.packageName)
                                            }
                                            if (imgResId != 0) {
                                                Image(
                                                    painter = painterResource(id = imgResId),
                                                    contentDescription = pathway.title,
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(140.dp)
                                                        .clip(RoundedCornerShape(16.dp)),
                                                    contentScale = ContentScale.Crop
                                                )
                                            }
                                        } else {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                                            ) {
                                                pathway.images.forEach { imgResName ->
                                                    val imgResId = remember(imgResName) {
                                                        context.resources.getIdentifier(imgResName, "drawable", context.packageName)
                                                    }
                                                    if (imgResId != 0) {
                                                        Image(
                                                            painter = painterResource(id = imgResId),
                                                            contentDescription = pathway.title,
                                                            modifier = Modifier
                                                                .weight(1f)
                                                                .height(110.dp)
                                                                .clip(RoundedCornerShape(16.dp)),
                                                            contentScale = ContentScale.Crop
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LockStateBanner() {
    GlassmorphicCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        shape = RoundedCornerShape(24.dp),
        borderColor = Color(0xFFFFD700).copy(alpha = 0.3f),
        backgroundColor = Color(0xFF2C1E0A).copy(alpha = 0.65f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                tint = Color(0xFFFFD700),
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Visit this stall in person to unlock live student showcases and participate in challenges!",
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "🔒 Proximity Lock Active",
                color = Color(0xFFFFD700),
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.sp
            )
        }
    }
}

@Composable
fun TabShowcasesContent(place: PlaceWithDetails, isWithinProximity: Boolean) {
    if (!isWithinProximity) {
        LockStateBanner()
        return
    }

    if (place.showcases.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.HelpOutline,
                    contentDescription = null,
                    tint = WhiteTranslucent,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "No Student Showcases Listed",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Check adjacent stalls for awesome student projects!",
                    color = WhiteTranslucent,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    } else {
        val isBasketballCourt = place.place.uid == "HEX_BEACON_03"
        var selectedSubIndex by remember { mutableStateOf(0) }
        
        val filteredShowcases = if (isBasketballCourt) {
            val activeCategory = when (selectedSubIndex) {
                0 -> "Right Side (HECC Club)"
                1 -> "Middle Zone (IT Club)"
                2 -> "Left Side (HRC Club)"
                else -> "Right Side (HECC Club)"
            }
            place.showcases.filter { it.category == activeCategory }
        } else {
            place.showcases
        }

        Column(modifier = Modifier.fillMaxSize()) {
            if (isBasketballCourt) {
                TabRow(
                    selectedTabIndex = selectedSubIndex,
                    containerColor = Color.Transparent,
                    contentColor = Color.White,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedSubIndex]),
                            color = ElectricCyan
                        )
                    },
                    divider = { HorizontalDivider(color = ElectricCyan.copy(alpha = 0.08f)) }
                ) {
                    val subCategories = listOf("Right (HECC)", "Middle (IT)", "Left (HRC)")
                    subCategories.forEachIndexed { index, title ->
                        val isSelected = selectedSubIndex == index
                        Tab(
                            selected = isSelected,
                            onClick = { selectedSubIndex = index },
                            text = {
                                Text(
                                    text = title,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Bold,
                                    color = if (isSelected) ElectricCyan else WhiteTranslucent
                                )
                            }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            if (filteredShowcases.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.HelpOutline,
                            contentDescription = null,
                            tint = WhiteTranslucent,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No Student Showcases Listed",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            } else {
                var expandedTitle by remember { mutableStateOf<String?>(null) }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredShowcases, key = { it.title }) { showcase ->
                        val isExpanded = expandedTitle == showcase.title
                        
                        GlassmorphicCard(
                            onClick = { expandedTitle = if (isExpanded) null else showcase.title },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            backgroundColor = TranslucentSurface.copy(alpha = 0.45f)
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                // High-end project cover photo banner
                                CoilImageWithFallback(
                                    imageUrl = showcase.imageUrl,
                                    fallbackIcon = Icons.Default.School,
                                    contentDescription = showcase.title,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(130.dp)
                                        .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp)),
                                    localFallbackResId = com.example.R.drawable.ic_project_default
                                )

                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.School,
                                                contentDescription = null,
                                                tint = ElectricCyan,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "STUDENT PROJECT",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = ElectricCyan,
                                                letterSpacing = 1.sp
                                            )
                                        }

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Group,
                                                contentDescription = null,
                                                tint = WhiteTranslucent,
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = if (isExpanded) "CLOSE SPEC ×" else "TAP FOR SPECS",
                                                fontSize = 8.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isExpanded) Color(0xFFFFD700) else WhiteTranslucent
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Text(
                                        text = showcase.title,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Text(
                                        text = showcase.desc,
                                        fontSize = 13.sp,
                                        color = WhiteTranslucent,
                                        lineHeight = 18.sp
                                    )

                                    // High-contrast smooth expansion detailed specifications sub-panel
                                    androidx.compose.animation.AnimatedVisibility(
                                        visible = isExpanded,
                                        enter = androidx.compose.animation.expandVertically() + fadeIn(),
                                        exit = androidx.compose.animation.shrinkVertically() + fadeOut()
                                    ) {
                                        val specs = remember(showcase.title) {
                                            when {
                                                // Civil
                                                showcase.title.contains("Siphon") -> Triple("Civil Engineering", "HydroFlow Team", "Automated Siphon Trigger")
                                                showcase.title.contains("Foundations") -> Triple("Geotechnical Engineering", "TerraFirm Builders", "Safe Load capacity 250kN")
                                                showcase.title.contains("Rainwater") -> Triple("Environmental Engineering", "GreenFlow Lab", "95% Catchment Efficiency")
                                                
                                                // HECC Club
                                                showcase.title.contains("Lens") -> Triple("Computer Vision & AI", "HECC VisionSquad", "98% Acc, 80ms Fit Latency")
                                                showcase.title.contains("Quiz") -> Triple("AI & Interactive Tech", "HECC Quizmasters", "Sync Latency <15ms")
                                                showcase.title.contains("Drum") -> Triple("Embedded Systems", "HECC Beats", "FSR Response <10ms")
                                                showcase.title.contains("Cart") -> Triple("IoT & Automation", "HECC SmartCart Team", "RFID, Real-Time Billing Screen")
                                                
                                                // IT Club
                                                showcase.title.contains("Security") -> Triple("IoT Mesh Systems", "IT SecureHome", "Multi-Sensor Biometric Security")
                                                showcase.title.contains("Irrigation") -> Triple("Smart Agriculture", "IT SmartAgri", "Soil Moisture Valve Sync")
                                                showcase.title.contains("Car") -> Triple("Robotics & Wireless", "IT RC Builders", "BT range 15m, 12V High torque")
                                                
                                                // HRC Club
                                                showcase.title.contains("Balloon") -> Triple("Combat Robotics", "HRC Poppers", "Dual Spin blades, RC mode")
                                                showcase.title.contains("Line") -> Triple("Autonomous Systems", "HRC LineRunners", "PID control, IR 5-array sensor")
                                                showcase.title.contains("UAV") || showcase.title.contains("Drone") -> Triple("Aerospace Systems", "HRC AeroSquad", "Real-time HD telemetry, Dual-GPS")
                                                showcase.title.contains("Arm") -> Triple("AI & Robotics", "HRC ArmTech", "Hand gesture tracker, 5 DOF")
                                                
                                                // Architecture
                                                showcase.title.contains("Residential") -> Triple("Architectural Design", "ArchRes Team", "Passive Solar, Green Roof")
                                                showcase.title.contains("School") -> Triple("Architectural Design", "ArchSchool Devs", "Universal Access, 500 Capacity")
                                                
                                                else -> Triple("Engineering Design", "HEX Department Club", "Verified Active State")
                                            }
                                        }

                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(top = 12.dp)
                                                .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                                                .border(0.5.dp, ElectricCyan.copy(alpha = 0.25f), RoundedCornerShape(10.dp))
                                                .padding(12.dp)
                                        ) {
                                            Text(
                                                text = "TECHNICAL SPECIFICATIONS",
                                                fontSize = 8.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = ElectricCyan,
                                                letterSpacing = 1.sp,
                                                modifier = Modifier.padding(bottom = 6.dp)
                                            )
                                            
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text("Category:", fontSize = 11.sp, color = WhiteTranslucent)
                                                Text(specs.first, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text("Team Name:", fontSize = 11.sp, color = WhiteTranslucent)
                                                Text(specs.second, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text("Core Metrics:", fontSize = 11.sp, color = WhiteTranslucent)
                                                Text(specs.third, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Green)
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text("Active Status:", fontSize = 11.sp, color = WhiteTranslucent)
                                                Text("True", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2DCE89))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TabGamesContent(
    place: PlaceWithDetails,
    isWithinProximity: Boolean,
    completedQuests: Set<String>,
    onToggleQuest: (String) -> Unit
) {
    if (!isWithinProximity) {
        LockStateBanner()
        return
    }

    if (place.games.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.Gamepad,
                    contentDescription = null,
                    tint = WhiteTranslucent,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "No Live Quests Available",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Visit other zones to discover live arcade challenges!",
                    color = WhiteTranslucent,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    text = "COMPLETE LIVE QUESTS & CHALLENGES",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = ElectricCyan,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(start = 2.dp, bottom = 4.dp)
                )
            }

            items(place.games) { game ->
                val isCompleted = completedQuests.contains(game.title)

                GlassmorphicCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    borderColor = if (isCompleted) Color(0xFF2DCE89).copy(alpha = 0.4f) else ElectricCyan.copy(alpha = 0.2f),
                    backgroundColor = if (isCompleted) Color(0xFF0F261D).copy(alpha = 0.5f) else TranslucentSurface.copy(alpha = 0.45f)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // High-end Mini-Game Banner
                        CoilImageWithFallback(
                            imageUrl = game.imageUrl,
                            fallbackIcon = Icons.Default.Gamepad,
                            contentDescription = game.title,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(110.dp)
                                .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp)),
                            localFallbackResId = com.example.R.drawable.ic_game_default
                        )

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.VideogameAsset,
                                        contentDescription = null,
                                        tint = if (isCompleted) Color(0xFF2DCE89) else ElectricCyan,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "LIVE STALL MINI-GAME",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isCompleted) Color(0xFF2DCE89) else ElectricCyan,
                                        letterSpacing = 1.sp
                                    )
                                }

                                if (isCompleted) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .background(Color(0xFF2DCE89).copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = Color(0xFF2DCE89),
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "COMPLETED",
                                            fontSize = 8.sp,
                                            color = Color(0xFF2DCE89),
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                } else {
                                    Text(
                                        text = "100 XP",
                                        fontSize = 9.sp,
                                        color = ElectricCyan,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier
                                            .background(ElectricCyan.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = game.title,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = game.desc,
                                fontSize = 13.sp,
                                color = WhiteTranslucent,
                                lineHeight = 18.sp
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            Button(
                                onClick = { onToggleQuest(game.title) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isCompleted) Color(0xFF2DCE89).copy(alpha = 0.15f) else ElectricCyan.copy(alpha = 0.12f)
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(36.dp)
                                    .border(
                                        1.dp,
                                        if (isCompleted) Color(0xFF2DCE89).copy(alpha = 0.5f) else ElectricCyan.copy(alpha = 0.3f),
                                        RoundedCornerShape(10.dp)
                                    )
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (isCompleted) Icons.Default.CheckCircle else Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        tint = if (isCompleted) Color(0xFF2DCE89) else ElectricCyan,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isCompleted) "RESET CHALLENGE" else "MARK QUEST AS COMPLETED",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Black,
                                        color = if (isCompleted) Color(0xFF2DCE89) else ElectricCyan
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HeaderSection(isScanning: Boolean) {
    GlassmorphicCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        shape = RoundedCornerShape(20.dp),
        borderColor = if (isScanning) ElectricCyan.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.08f),
        backgroundColor = DeepNavy.copy(alpha = 0.7f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Circular outer glowing container for logo
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(ElectricCyan.copy(alpha = 0.8f), NeonBlue.copy(alpha = 0.8f))
                            ),
                            RoundedCornerShape(12.dp)
                        )
                        .padding(1.5.dp)
                ) {
                    Image(
                        painter = painterResource(id = com.example.R.drawable.logo),
                        contentDescription = "App Logo",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(11.dp))
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "HEXplore",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = "SPATIAL NAV RADAR",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = ElectricCyan,
                        letterSpacing = 1.5.sp
                    )
                }
            }

            // Dynamic Pulsing Status Badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .background(
                        if (isScanning) Color(0xFF2DCE89).copy(alpha = 0.12f) else Color(0xFFFF4D4D).copy(alpha = 0.12f),
                        RoundedCornerShape(30.dp)
                    )
                    .border(
                        width = 1.dp,
                        color = if (isScanning) Color(0xFF2DCE89).copy(alpha = 0.3f) else Color(0xFFFF4D4D).copy(alpha = 0.3f),
                        shape = RoundedCornerShape(30.dp)
                    )
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(if (isScanning) Color(0xFF2DCE89) else Color(0xFFFF4D4D), CircleShape)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isScanning) "ACTIVE" else "OFFLINE",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isScanning) Color(0xFF2DCE89) else Color(0xFFFF4D4D),
                    letterSpacing = 1.sp
                )
            }
        }
    }
}
