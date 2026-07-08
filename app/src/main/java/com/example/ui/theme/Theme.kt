package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = ElectricCyan,
    onPrimary = DeepNavy,
    primaryContainer = NeonBlue,
    onPrimaryContainer = LightBlueGlow,
    secondary = LightBlueGlow,
    onSecondary = DeepNavy,
    background = DarkBg,
    onBackground = androidx.compose.ui.graphics.Color.White,
    surface = TranslucentSurface,
    onSurface = androidx.compose.ui.graphics.Color.White,
    surfaceVariant = TranslucentSurface,
    onSurfaceVariant = WhiteTranslucent
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Force dark theme for the high-tech sci-fi vibe
    dynamicColor: Boolean = false, // Disable dynamic colors to preserve our branding
    content: @Composable () -> Unit,
) {
    val colorScheme = DarkColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
