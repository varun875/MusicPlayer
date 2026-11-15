package com.suspended.music.ui.utils

import android.provider.Settings
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Window size classes, derived from [LocalConfiguration] so the app needs no
 * extra dependency on `material3-window-size-class` / `material3-adaptive`.
 *
 * Width breakpoints follow the Material 3 window-size-class spec, with an
 * extra [WindowWidthSizeClass.Wide] bucket for very large desktop/landscape
 * surfaces where layouts should stop stretching and cap their content width.
 */
enum class WindowWidthSizeClass { Compact, Medium, Expanded, Wide }

enum class WindowHeightSizeClass { Compact, Medium }

@Immutable
data class AdaptiveLayoutInfo(
    val widthClass: WindowWidthSizeClass,
    val heightClass: WindowHeightSizeClass,
    val isLandscape: Boolean,
    /** True when there is enough width to show list + player side-by-side. */
    val isTwoPane: Boolean,
    /** True on tablets and unfolded foldables (portrait or landscape). */
    val isTabletOrFoldableUnfolded: Boolean
)

/** Remember the current [AdaptiveLayoutInfo], recomputed on configuration change. */
@Composable
fun rememberAdaptiveLayoutInfo(): AdaptiveLayoutInfo {
    val configuration = LocalConfiguration.current
    val widthDp = configuration.screenWidthDp
    val heightDp = configuration.screenHeightDp
    return remember(widthDp, heightDp) {
        val widthClass = when {
            widthDp < 600 -> WindowWidthSizeClass.Compact
            widthDp < 840 -> WindowWidthSizeClass.Medium
            widthDp < 1200 -> WindowWidthSizeClass.Expanded
            else -> WindowWidthSizeClass.Wide
        }
        val heightClass = if (heightDp < 480) {
            WindowHeightSizeClass.Compact
        } else {
            WindowHeightSizeClass.Medium
        }
        val isLandscape = widthDp > heightDp
        val isTwoPane = widthClass == WindowWidthSizeClass.Expanded ||
            widthClass == WindowWidthSizeClass.Wide
        AdaptiveLayoutInfo(
            widthClass = widthClass,
            heightClass = heightClass,
            isLandscape = isLandscape,
            isTwoPane = isTwoPane,
            isTabletOrFoldableUnfolded = isTwoPane
        )
    }
}

/** Widest the app's primary content should grow before it stops being readable. */
val MaxReadableContentWidth: Dp = 1200.dp

/**
 * True when the platform asks for reduced motion (animator duration scale 0).
 * Finite Compose animations are scaled automatically; use this to turn off
 * looping/decorative motion and expensive animated effects.
 */
@Composable
fun rememberReducedMotion(): Boolean {
    val context = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    var reducedMotion by remember { mutableStateOf(false) }
    DisposableEffect(context, lifecycleOwner) {
        fun read(): Boolean = Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f
        ) == 0f
        val animatorUri = Settings.Global.getUriFor(Settings.Global.ANIMATOR_DURATION_SCALE)
        val observer = object : android.database.ContentObserver(android.os.Handler(android.os.Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean, uri: android.net.Uri?) {
                reducedMotion = read()
            }
        }
        reducedMotion = read()
        context.contentResolver.registerContentObserver(animatorUri, false, observer)
        onDispose {
            context.contentResolver.unregisterContentObserver(observer)
        }
    }
    return reducedMotion
}

/**
 * Centers [content] and caps its width on large windows so a compact phone
 * layout is never stretched edge-to-edge across a desktop-class canvas.
 */
@Composable
fun MaxContentWidth(
    modifier: Modifier = Modifier,
    maxWidth: Dp = MaxReadableContentWidth,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .widthIn(max = maxWidth)
        ) {
            content()
        }
    }
}
