package com.stepphantom.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Dark = darkColorScheme(primary = Color(0xFFB388FF), secondary = Color(0xFF00BFA5))
private val Light = lightColorScheme(primary = Color(0xFF6A3DE8), secondary = Color(0xFF00897B))

@Composable
fun StepPhantomTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) Dark else Light, content = content)
}
