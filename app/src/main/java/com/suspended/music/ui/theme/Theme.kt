package com.suspended.music.ui.theme

import android.app.Activity
import android.graphics.Color.parseColor
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.suspended.music.data.model.ThemeConfig

private val DarkColorScheme = darkColorScheme(
    primary = ExpressivePrimary,
    onPrimary = Color.White,
    primaryContainer = ExpressivePrimaryContainer,
    onPrimaryContainer = ExpressiveOnPrimaryContainer,
    secondary = ExpressiveSecondary,
    onSecondary = Color.Black,
    secondaryContainer = ExpressiveSecondaryContainer,
    onSecondaryContainer = ExpressiveOnSecondaryContainer,
    tertiary = ExpressiveTertiary,
    onTertiary = Color.White,
    tertiaryContainer = ExpressiveTertiaryContainer,
    onTertiaryContainer = ExpressiveOnTertiaryContainer,
    background = DarkBackground,
    onBackground = TextPrimary,
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = TextTertiary,
    outlineVariant = DarkSurfaceElevated,
    error = Color(0xFFFF6B6B),
    onError = Color.White,
    errorContainer = Color(0xFFFFD9D6),
    onErrorContainer = Color(0xFF410002)
)

private val LightColorScheme = lightColorScheme(
    primary = ExpressivePrimary,
    onPrimary = Color.White,
    primaryContainer = ExpressivePrimaryContainer,
    onPrimaryContainer = ExpressiveOnPrimaryContainer,
    secondary = ExpressiveSecondary,
    onSecondary = Color.White,
    secondaryContainer = ExpressiveSecondaryContainer,
    onSecondaryContainer = ExpressiveOnSecondaryContainer,
    tertiary = ExpressiveTertiary,
    onTertiary = Color.White,
    tertiaryContainer = ExpressiveTertiaryContainer,
    onTertiaryContainer = ExpressiveOnTertiaryContainer,
    background = LightBackground,
    onBackground = TextPrimaryLight,
    surface = LightSurface,
    onSurface = TextPrimaryLight,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = TextSecondaryLight,
    outline = TextTertiaryLight,
    outlineVariant = LightSurfaceElevated,
    error = Color(0xFFB3261E),
    onError = Color.White,
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF410E0B)
)

@Composable
fun MusicPlayerTheme(
    themeConfig: ThemeConfig = ThemeConfig(),
    dynamicColor: Boolean = true, // Material You dynamic colors from wallpaper
    content: @Composable () -> Unit
) {
    val darkTheme = themeConfig.isDarkMode ?: isSystemInDarkTheme()

    // Get dynamic colors from wallpaper on Android 12+
    val baseScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val useAmoledBlack = darkTheme && themeConfig.useAmoledBlack

    // Parse custom colors with fallback to dynamic/default colors
    val primary = parseHexColor(themeConfig.primaryColor, baseScheme.primary)
    val secondary = parseHexColor(themeConfig.secondaryColor, baseScheme.secondary)
    val tertiary = parseHexColor(themeConfig.tertiaryColor, baseScheme.tertiary)

    // Apply AMOLED black mode if enabled
    val colorScheme = baseScheme.copy(
        primary = primary,
        primaryContainer = primary,
        secondary = secondary,
        secondaryContainer = secondary,
        tertiary = tertiary,
        tertiaryContainer = tertiary
    ).let { scheme ->
        if (useAmoledBlack) {
            scheme.copy(
                background = Color.Black,
                surface = Color.Black,
                surfaceVariant = Color(0xFF0D0D0D),
                surfaceTint = Color.Black,
                scrim = Color.Black
            )
        } else {
            scheme
        }
    }
    
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // Enable edge-to-edge with transparent status/nav bars
            @Suppress("DEPRECATION")
            window.statusBarColor = Color.Transparent.toArgb()
            @Suppress("DEPRECATION")
            window.navigationBarColor = Color.Transparent.toArgb()
            // Set icon colors based on theme (light on dark, dark on light)
            WindowCompat.getInsetsController(window, view)?.apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }
    
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = AppShapes,
        content = content
    )
}

private fun parseHexColor(hex: String, fallback: Color): Color {
    val value = hex.trim().trimStart('#')
    return runCatching { Color(parseColor("#$value")) }.getOrElse { fallback }
}
