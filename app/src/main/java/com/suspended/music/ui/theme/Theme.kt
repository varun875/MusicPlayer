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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
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
    onSecondary = ExpressiveOnSecondary,
    secondaryContainer = ExpressiveSecondaryContainer,
    onSecondaryContainer = ExpressiveOnSecondaryContainer,
    tertiary = ExpressiveTertiary,
    onTertiary = ExpressiveOnTertiary,
    tertiaryContainer = ExpressiveTertiaryContainer,
    onTertiaryContainer = ExpressiveOnTertiaryContainer,
    background = DarkBackground,
    onBackground = TextPrimary,
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    surfaceDim = DarkSurfaceDim,
    surfaceBright = DarkSurfaceBright,
    surfaceContainerLowest = DarkSurfaceContainerLowest,
    surfaceContainerLow = DarkSurfaceContainerLow,
    surfaceContainer = DarkSurfaceContainer,
    surfaceContainerHigh = DarkSurfaceContainerHigh,
    surfaceContainerHighest = DarkSurfaceContainerHighest,
    inverseSurface = DarkInverseSurface,
    inverseOnSurface = DarkInverseOnSurface,
    inversePrimary = DarkInversePrimary,
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
    onSecondary = ExpressiveOnSecondary,
    secondaryContainer = ExpressiveSecondaryContainer,
    onSecondaryContainer = ExpressiveOnSecondaryContainer,
    tertiary = ExpressiveTertiary,
    onTertiary = ExpressiveOnTertiary,
    tertiaryContainer = ExpressiveTertiaryContainer,
    onTertiaryContainer = ExpressiveOnTertiaryContainer,
    background = LightBackground,
    onBackground = TextPrimaryLight,
    surface = LightSurface,
    onSurface = TextPrimaryLight,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = TextSecondaryLight,
    surfaceDim = LightSurfaceDim,
    surfaceBright = LightSurfaceBright,
    surfaceContainerLowest = LightSurfaceContainerLowest,
    surfaceContainerLow = LightSurfaceContainerLow,
    surfaceContainer = LightSurfaceContainer,
    surfaceContainerHigh = LightSurfaceContainerHigh,
    surfaceContainerHighest = LightSurfaceContainerHighest,
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
    val context = LocalContext.current

    // Dynamic color extraction from wallpaper is expensive; only recompute it when
    // the relevant inputs (context, dark/light, dynamic toggle) actually change.
    val baseScheme = remember(dynamicColor, darkTheme, context) {
        when {
            dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            }
            darkTheme -> DarkColorScheme
            else -> LightColorScheme
        }
    }

    val useAmoledBlack = darkTheme && themeConfig.useAmoledBlack

    // Memoize the full custom scheme so we don't reallocate ~30 Color slots and
    // 3 compositeOver calls on every recomposition of the root tree.
    val colorScheme = remember(
        baseScheme,
        themeConfig.primaryColor,
        themeConfig.secondaryColor,
        themeConfig.tertiaryColor,
        useAmoledBlack
    ) {
        val primary = parseHexColor(themeConfig.primaryColor, baseScheme.primary)
        val secondary = parseHexColor(themeConfig.secondaryColor, baseScheme.secondary)
        val tertiary = parseHexColor(themeConfig.tertiaryColor, baseScheme.tertiary)

        val primaryContainer = primary.copy(alpha = 0.3f).compositeOver(baseScheme.surface)
        val secondaryContainer = secondary.copy(alpha = 0.3f).compositeOver(baseScheme.surface)
        val tertiaryContainer = tertiary.copy(alpha = 0.3f).compositeOver(baseScheme.surface)

        val scheme = baseScheme.copy(
            primary = primary,
            onPrimary = contentColorFor(primary),
            primaryContainer = primaryContainer,
            onPrimaryContainer = contentColorFor(primaryContainer),
            secondary = secondary,
            onSecondary = contentColorFor(secondary),
            secondaryContainer = secondaryContainer,
            onSecondaryContainer = contentColorFor(secondaryContainer),
            tertiary = tertiary,
            onTertiary = contentColorFor(tertiary),
            tertiaryContainer = tertiaryContainer,
            onTertiaryContainer = contentColorFor(tertiaryContainer)
        )
        if (useAmoledBlack) {
            scheme.copy(
                background = Color.Black,
                surface = Color.Black,
                surfaceVariant = Color(0xFF0D0D0D),
                surfaceDim = Color.Black,
                surfaceBright = Color(0xFF1A1A1A),
                surfaceContainerLowest = Color.Black,
                surfaceContainerLow = Color(0xFF0A0A0A),
                surfaceContainer = Color(0xFF101010),
                surfaceContainerHigh = Color(0xFF161616),
                surfaceContainerHighest = Color(0xFF1C1C1C),
                surfaceTint = Color.Black,
                scrim = Color.Black
            )
        } else {
            scheme
        }
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        // Only resync system bar appearance when dark/light actually toggles, instead
        // of on every successful composition the way SideEffect would.
        LaunchedEffect(darkTheme, view) {
            val window = (view.context as Activity).window
            @Suppress("DEPRECATION")
            window.statusBarColor = Color.Transparent.toArgb()
            @Suppress("DEPRECATION")
            window.navigationBarColor = Color.Transparent.toArgb()
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

/**
 * Picks a readable foreground for [background] so custom brand colors can never
 * pair a dark container with dark content (or vice versa).
 */
private fun contentColorFor(background: Color): Color =
    if (background.luminance() > 0.5f) TextPrimaryLight else Color.White
