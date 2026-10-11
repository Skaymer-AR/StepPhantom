package com.stepphantom.ui.theme

import android.os.Build
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Opción 2: superficies oscuras, verde bosque y menta con contraste alto.
private val DarkClearMotion = darkColorScheme(
    primary = Color(0xFF72E3AD),
    onPrimary = Color(0xFF003921),
    primaryContainer = Color(0xFF075D3D),
    onPrimaryContainer = Color(0xFFB8F3D4),
    secondary = Color(0xFFB4DCC5),
    onSecondary = Color(0xFF19382A),
    secondaryContainer = Color(0xFF284B39),
    onSecondaryContainer = Color(0xFFD0F0DC),
    tertiary = Color(0xFFF2C878),
    onTertiary = Color(0xFF402D00),
    tertiaryContainer = Color(0xFF5B4300),
    onTertiaryContainer = Color(0xFFFFE2A5),
    error = Color(0xFFFFB4AB),
    errorContainer = Color(0xFF690005),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF07140F),
    onBackground = Color(0xFFE3F2E9),
    surface = Color(0xFF0D1D16),
    onSurface = Color(0xFFE3F2E9),
    surfaceVariant = Color(0xFF1C3429),
    onSurfaceVariant = Color(0xFFB7CFC0),
    outline = Color(0xFF526D5C)
)

// Opción 3: consola oscura de alto contraste para compilar como vista previa.
private val DarkPhantomConsole = darkColorScheme(
    primary = Color(0xFFB4FF52),
    onPrimary = Color(0xFF203600),
    primaryContainer = Color(0xFF375900),
    onPrimaryContainer = Color(0xFFD8FFAA),
    secondary = Color(0xFF55E7FF),
    onSecondary = Color(0xFF003640),
    secondaryContainer = Color(0xFF004F5B),
    onSecondaryContainer = Color(0xFFB7F2FF),
    tertiary = Color(0xFFFFC04D),
    onTertiary = Color(0xFF402D00),
    tertiaryContainer = Color(0xFF5B4300),
    onTertiaryContainer = Color(0xFFFFE2A4),
    error = Color(0xFFFFB4AB),
    errorContainer = Color(0xFF690005),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF080C0D),
    onBackground = Color(0xFFE5ECEB),
    surface = Color(0xFF101718),
    onSurface = Color(0xFFE5ECEB),
    surfaceVariant = Color(0xFF202A2B),
    onSurfaceVariant = Color(0xFFB8C7C8),
    outline = Color(0xFF536466)
)

// Tipografía expresiva: titulares grandes y con más peso.
private val ExpressiveType = Typography(
    headlineSmall = TextStyle(fontSize = 26.sp, lineHeight = 32.sp, fontWeight = FontWeight.Bold),
    titleLarge = TextStyle(fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = TextStyle(fontSize = 17.sp, lineHeight = 24.sp, fontWeight = FontWeight.SemiBold),
    labelLarge = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold)
)

// Esquinas generosas, look redondeado.
private val ExpressiveShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(26.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

private val ConsoleType = ExpressiveType.copy(
    headlineSmall = ExpressiveType.headlineSmall.copy(fontFamily = FontFamily.Monospace),
    titleLarge = ExpressiveType.titleLarge.copy(fontFamily = FontFamily.Monospace),
    titleMedium = ExpressiveType.titleMedium.copy(fontFamily = FontFamily.Monospace),
    labelLarge = ExpressiveType.labelLarge.copy(fontFamily = FontFamily.Monospace)
)

private val ConsoleShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(18.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

@Composable
fun StepPhantomTheme(
    useDynamicColor: Boolean,
    useConsoleStyle: Boolean,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colors = when {
        useConsoleStyle -> DarkPhantomConsole
        useDynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            dynamicDarkColorScheme(context)
        else -> DarkClearMotion
    }
    MaterialTheme(
        colorScheme = colors,
        typography = if (useConsoleStyle) ConsoleType else ExpressiveType,
        shapes = if (useConsoleStyle) ConsoleShapes else ExpressiveShapes,
        content = content
    )
}
