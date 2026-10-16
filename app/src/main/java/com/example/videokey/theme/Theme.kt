package com.example.videokey.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val VideokeColorScheme = darkColorScheme(
    primary = Color(0xFFFFC107),      // Amber Videoke Yellow
    onPrimary = Color.Black,
    secondary = Color(0xFF00E676),    // Digital Green
    onSecondary = Color.Black,
    tertiary = Color(0xFF2979FF),     // Remote Blue
    background = Color(0xFF090D16),   // Deep Dark Stage
    onBackground = Color.White,
    surface = Color(0xFF0E131F),      // Remote Console Dark
    onSurface = Color.White
)

@Composable
fun VideoKeyTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = VideokeColorScheme,
        typography = Typography,
        content = content
    )
}
