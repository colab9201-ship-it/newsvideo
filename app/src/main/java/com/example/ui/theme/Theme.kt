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
    primary = NewsRed,
    onPrimary = NewsWhite,
    primaryContainer = NewsRedDark,
    onPrimaryContainer = NewsWhite,
    secondary = NewsSlate,
    onSecondary = NewsWhite,
    tertiary = NewsAccentGold,
    onTertiary = NewsBlack,
    background = NewsBlack,
    onBackground = NewsWhite,
    surface = NewsCardBackground,
    onSurface = NewsWhite,
    surfaceVariant = NewsSlate,
    onSurfaceVariant = NewsWhite
)

private val LightColorScheme = lightColorScheme(
    primary = NewsRed,
    onPrimary = Color.White,
    primaryContainer = NewsRedLight,
    onPrimaryContainer = NewsRedDark,
    secondary = NewsSlate,
    onSecondary = Color.White,
    tertiary = NewsAccentGold,
    onTertiary = NewsBlack,
    background = NewsWhite,
    onBackground = NewsBlack,
    surface = Color.White,
    onSurface = NewsBlack,
    surfaceVariant = Color(0xFFEEEEEE),
    onSurfaceVariant = NewsBlack
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // We disable dynamicColor by default to enforce the professional TV News (Red + Black) brand theme.
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
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
