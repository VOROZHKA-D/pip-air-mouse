package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = OrangePrimary,
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF4E2000),
    onPrimaryContainer = OrangePrimaryLight,
    secondary = AmberGold,
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF382300),
    onSecondaryContainer = Color(0xFFFFD54F),
    tertiary = OrangeFlame,
    onTertiary = Color.White,
    background = ObsidianDark,
    onBackground = TextHighEmphasis,
    surface = SlateCarbon,
    onSurface = TextHighEmphasis,
    surfaceVariant = SurfaceElevated,
    onSurfaceVariant = TextMediumEmphasis,
    outline = BorderSubtle,
    error = NeonCrimson,
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFFD84315),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFCCBC),
    onPrimaryContainer = Color(0xFF3E1200),
    secondary = Color(0xFFE65100),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFE0B2),
    onSecondaryContainer = Color(0xFF3E1700),
    tertiary = Color(0xFFBF360C),
    onTertiary = Color.White,
    background = Color(0xFFFBF8F6),
    onBackground = Color(0xFF1F1B18),
    surface = Color.White,
    onSurface = Color(0xFF1F1B18),
    surfaceVariant = Color(0xFFF3ECE7),
    onSurfaceVariant = Color(0xFF52443C),
    outline = Color(0xFF85746B),
    error = Color(0xFFBA1A1A),
    onError = Color.White
)

@Composable
fun PipAirMouseTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
