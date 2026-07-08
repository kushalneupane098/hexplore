package com.example.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cabin
import androidx.compose.material.icons.filled.DeveloperMode
import androidx.compose.material.icons.filled.Engineering
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.VideogameAsset
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.example.ble.BleSignalTracker
import com.example.data.PlaceWithDetails
import com.example.ui.theme.DeepNavy
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.NeonBlue
import com.example.ui.theme.TranslucentSurface
import com.example.ui.theme.WhiteTranslucent

@Composable
fun RevolutionizedRadar(
    centerPlace: PlaceWithDetails?,
    onBubbleClick: (PlaceWithDetails) -> Unit,
    allPlaces: List<PlaceWithDetails>,
    isScanning: Boolean,
    heading: Float = 0f
) {
    if (centerPlace == null) {
        // EMPTY STATE (No Beacons Mapped): Pulsing concentric lines with premium logo centered
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(340.dp),
            contentAlignment = Alignment.Center
        ) {
            PulsatingRadarBackground(isScanning = isScanning)
            
            val infiniteTransition = rememberInfiniteTransition()
            val pulseScale by infiniteTransition.animateFloat(
                initialValue = 0.94f,
                targetValue = 1.06f,
                animationSpec = infiniteRepeatable(
                    animation = tween(2200, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                )
            )
            
            HexploreLogoIcon(
                modifier = Modifier.graphicsLayer(scaleX = pulseScale, scaleY = pulseScale)
            )
        }
        return
    }

    val nav = centerPlace.navigation
    val infiniteTransition = rememberInfiniteTransition()

    // Smooth breathing float animation for adjacent bubbles
    val floatAnim by infiniteTransition.animateFloat(
        initialValue = -4f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(340.dp),
        contentAlignment = Alignment.Center
    ) {
        // Underlay Pulsating Radar Background Lines
        PulsatingRadarBackground(isScanning = isScanning)

        // 1. CENTRAL HUB LOOP
        CentralHubLoop(place = centerPlace, isScanning = isScanning)

        // 2. ADJACENT FLOATING BUBBLES orbiting dynamically based on the compass heading
        val neighbors = remember(nav) {
            mutableListOf<Pair<String, Float>>().apply {
                if (nav != null) {
                    if (!nav.inFrontUid.isNullOrEmpty()) add(Pair(nav.inFrontUid, 0f))
                    if (!nav.rightUid.isNullOrEmpty()) add(Pair(nav.rightUid, 90f))
                    if (!nav.behindUid.isNullOrEmpty()) add(Pair(nav.behindUid, 180f))
                    if (!nav.leftUid.isNullOrEmpty()) add(Pair(nav.leftUid, 270f))
                }
            }
        }

        neighbors.forEach { (targetUid, nodeCompassAngle) ->
            allPlaces.find { it.place.uid == targetUid }?.let { targetPlace ->
                if (targetPlace.place.uid != centerPlace.place.uid) {
                    val angleDeg = nodeCompassAngle - 90f
                    val angleRad = Math.toRadians(angleDeg.toDouble()).toFloat()
                    val radiusDp = 108f // orbit radius of the nodes
                    val offsetX = radiusDp * kotlin.math.cos(angleRad)
                    val offsetY = radiusDp * kotlin.math.sin(angleRad)
                    
                    val directionLabel = when (nodeCompassAngle) {
                        0f -> "N"
                        90f -> "E"
                        180f -> "S"
                        270f -> "W"
                        else -> "N"
                    }
                    
                    FloatingRadarBubble(
                        place = targetPlace,
                        directionLabel = directionLabel,
                        onClick = { onBubbleClick(targetPlace) },
                        modifier = Modifier
                            .align(Alignment.Center)
                            .offset(
                                x = offsetX.dp,
                                y = (offsetY + floatAnim).dp
                            )
                    )
                }
            }
        }
    }
}

@Composable
fun PulsatingRadarBackground(isScanning: Boolean) {
    val infiniteTransition = rememberInfiniteTransition()

    val scale1 by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )
    val alpha1 by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    val scale2 by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, delayMillis = 2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )
    val alpha2 by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, delayMillis = 2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    Box(
        modifier = Modifier.size(300.dp),
        contentAlignment = Alignment.Center
    ) {
        // Static ring grid
        Box(modifier = Modifier.size(280.dp).border(0.5.dp, ElectricCyan.copy(alpha = 0.08f), CircleShape))
        Box(modifier = Modifier.size(200.dp).border(0.5.dp, ElectricCyan.copy(alpha = 0.12f), CircleShape))
        Box(modifier = Modifier.size(120.dp).border(0.5.dp, ElectricCyan.copy(alpha = 0.15f), CircleShape))

        // Grid axis lines
        Spacer(modifier = Modifier.width(280.dp).height(0.5.dp).background(ElectricCyan.copy(alpha = 0.05f)))
        Spacer(modifier = Modifier.height(280.dp).width(0.5.dp).background(ElectricCyan.copy(alpha = 0.05f)))

        if (isScanning) {
            Box(
                modifier = Modifier
                    .size(240.dp)
                    .graphicsLayer(scaleX = scale1, scaleY = scale1, alpha = alpha1)
                    .border(1.5.dp, ElectricCyan.copy(alpha = 0.5f), CircleShape)
            )
            Box(
                modifier = Modifier
                    .size(240.dp)
                    .graphicsLayer(scaleX = scale2, scaleY = scale2, alpha = alpha2)
                    .border(1.5.dp, NeonBlue.copy(alpha = 0.3f), CircleShape)
            )
        }
    }
}

@Composable
fun CentralHubLoop(place: PlaceWithDetails, isScanning: Boolean) {
    val detectedBeacon by BleSignalTracker.detectedBeacon.collectAsState()
    val isPhysicalCurrent = (detectedBeacon != null && detectedBeacon?.uid == place.place.uid)

    val infiniteTransition = rememberInfiniteTransition()
    val spinAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    Box(
        modifier = Modifier
            .size(120.dp)
            .background(
                Brush.radialGradient(
                    colors = if (isScanning) listOf(ElectricCyan.copy(alpha = 0.25f), Color.Transparent)
                    else listOf(Color.Transparent, Color.Transparent)
                ),
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        // SPINNING ACTIVE FOCUS OUTER RING
        Box(
            modifier = Modifier
                .size(108.dp)
                .graphicsLayer(rotationZ = spinAngle)
                .border(
                    width = 2.dp,
                    brush = Brush.sweepGradient(
                        colors = listOf(ElectricCyan, NeonBlue, Color.Transparent, ElectricCyan)
                    ),
                    shape = CircleShape
                )
        )

        // HIGHLY-CURVED AVATAR INNER FRAME
        Box(
            modifier = Modifier
                .size(90.dp)
                .clip(CircleShape)
                .border(2.dp, Color.White.copy(alpha = 0.8f), CircleShape)
        ) {
            CoilImageWithFallback(
                imageUrl = place.place.imageUrl,
                fallbackIcon = getIconForLocation(place.place.imageAsset),
                contentDescription = place.place.locationName,
                modifier = Modifier.fillMaxSize(),
                containerColor = Color.Transparent,
                localFallbackResId = com.example.R.drawable.img_zone_default
            )
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(y = 4.dp)
                .background(
                    if (isPhysicalCurrent) Color(0xFF2DCE89) else NeonBlue,
                    shape = RoundedCornerShape(12.dp)
                )
                .border(1.dp, Color.White.copy(alpha = 0.5f), shape = RoundedCornerShape(12.dp))
                .padding(horizontal = 8.dp, vertical = 2.dp)
        ) {
            Text(
                text = if (isPhysicalCurrent) "HERE" else "FOCUS",
                fontSize = 8.sp,
                color = Color.White,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp
            )
        }
    }
}

@Composable
fun HexploreLogoIcon(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(80.dp)
            .background(
                Brush.radialGradient(
                    colors = listOf(ElectricCyan.copy(alpha = 0.25f), Color.Transparent)
                ),
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.foundation.Canvas(modifier = Modifier.size(54.dp)) {
            val width = size.width
            val height = size.height
            val path = androidx.compose.ui.graphics.Path().apply {
                val radius = width / 2f
                val centerX = width / 2f
                val centerY = height / 2f
                for (i in 0..5) {
                    val angle = Math.toRadians((i * 60 - 30).toDouble())
                    val x = (centerX + radius * kotlin.math.cos(angle)).toFloat()
                    val y = (centerY + radius * kotlin.math.sin(angle)).toFloat()
                    if (i == 0) moveTo(x, y) else lineTo(x, y)
                }
                close()
            }
            
            // Outer glowing neon border
            drawPath(
                path = path,
                color = ElectricCyan,
                style = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = 3.dp.toPx(),
                    cap = androidx.compose.ui.graphics.StrokeCap.Round,
                    join = androidx.compose.ui.graphics.StrokeJoin.Round
                )
            )
            
            drawPath(
                path = path,
                color = NeonBlue.copy(alpha = 0.45f),
                style = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = 7.dp.toPx()
                )
            )
        }
        
        Text(
            text = "H",
            fontSize = 26.sp,
            fontWeight = FontWeight.Black,
            color = Color.White,
            letterSpacing = 0.sp
        )
    }
}

@Composable
fun CoilImageWithFallback(
    imageUrl: String?,
    fallbackIcon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    containerColor: Color = TranslucentSurface.copy(alpha = 0.35f),
    iconColor: Color = ElectricCyan,
    localFallbackResId: Int? = null
) {
    var isError by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(containerColor),
        contentAlignment = Alignment.Center
    ) {
        if (!imageUrl.isNullOrEmpty() && !isError) {
            Image(
                painter = rememberAsyncImagePainter(
                    model = imageUrl,
                    onError = { isError = true }
                ),
                contentDescription = contentDescription,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else if (localFallbackResId != null) {
            Image(
                painter = rememberAsyncImagePainter(localFallbackResId),
                contentDescription = contentDescription,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = fallbackIcon,
                    contentDescription = contentDescription,
                    tint = iconColor,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

@Composable
fun FloatingRadarBubble(
    place: PlaceWithDetails,
    directionLabel: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .width(84.dp)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(68.dp)
                .background(TranslucentSurface.copy(alpha = 0.85f), CircleShape)
                .border(1.5.dp, ElectricCyan.copy(alpha = 0.4f), CircleShape)
                .testTag("bubble_${place.place.uid}"),
            contentAlignment = Alignment.Center
        ) {
            LocationImage(
                imageName = place.place.imageAsset,
                contentDescription = place.place.locationName,
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
            )

            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(18.dp)
                    .background(ElectricCyan, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = directionLabel.take(1),
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Black,
                    color = DeepNavy
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = place.place.locationName,
            fontSize = 9.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 2.dp)
        )
    }
}

@Composable
fun LocationImage(
    imageName: String,
    contentDescription: String?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val resId = remember(imageName) {
        context.resources.getIdentifier(imageName, "drawable", context.packageName)
    }

    val finalResId = if (resId != 0) resId else com.example.R.drawable.img_zone_default

    Image(
        painter = rememberAsyncImagePainter(finalResId),
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = ContentScale.Crop
    )
}

fun getIconForLocation(imageAsset: String): androidx.compose.ui.graphics.vector.ImageVector {
    return when {
        imageAsset.contains("soccer") || imageAsset.contains("robo") -> Icons.Default.SmartToy
        imageAsset.contains("arch") -> Icons.Default.Cabin
        imageAsset.contains("civil") -> Icons.Default.Engineering
        else -> Icons.Default.DeveloperMode
    }
}

