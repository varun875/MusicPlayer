package com.suspended.music.ui.theme

import androidx.compose.animation.core.EaseOutExpo
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition

/**
 * Shared transition animation specs for consistent UI transitions
 */
object SharedTransitionSpecs {
    
    val albumCoverTransitionDuration = 500
    val screenTransitionDuration = 300
    
    val albumCoverEnter: EnterTransition = scaleIn(
        animationSpec = tween(
            durationMillis = albumCoverTransitionDuration,
            easing = EaseOutExpo
        ),
        initialScale = 0.8f
    ) + fadeIn(animationSpec = tween(durationMillis = albumCoverTransitionDuration))
    
    val albumCoverExit: ExitTransition = scaleOut(
        animationSpec = tween(
            durationMillis = albumCoverTransitionDuration,
            easing = EaseOutExpo
        ),
        targetScale = 0.8f
    ) + fadeOut(animationSpec = tween(durationMillis = albumCoverTransitionDuration))
    
    val screenSlideUp: EnterTransition = slideInVertically(
        animationSpec = tween(
            durationMillis = screenTransitionDuration,
            easing = EaseOutExpo
        ),
        initialOffsetY = { it }
    ) + fadeIn(animationSpec = tween(durationMillis = screenTransitionDuration))
    
    val screenSlideDown: ExitTransition = slideOutVertically(
        animationSpec = tween(
            durationMillis = screenTransitionDuration,
            easing = EaseOutExpo
        ),
        targetOffsetY = { it }
    ) + fadeOut(animationSpec = tween(durationMillis = screenTransitionDuration))
}

/**
 * CompositionLocal for shared transition configuration
 */
val LocalSharedTransitionSpec = staticCompositionLocalOf {
    SharedTransitionSpecs
}
