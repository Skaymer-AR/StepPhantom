package com.stepphantom.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Purple = Color(0xFF7C4DFF)
private val PurpleDark = Color(0xFFB388FF)
private val Teal = Color(0xFF00BFA5)

private val DarkColors = darkColorScheme(
    primary = PurpleDark,
    secondary = Teal,
)

private val LightColors = lightColorScheme(
    primary = Purple,
    secondary = Teal,
)

@Composable
fun StepPhantomTheme(content: @Composable () -> Unit) {
    val colors = if (isSystemInDarkTheme()) DarkColors else LightColors
    MaterialTheme(colorScheme = colors, content = content)
}
