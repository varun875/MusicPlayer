package com.suspended.music.ui.theme

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.EaseOutExpo
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically

/**
 * Shared transition specs for consistent motion across the app.
 *
 * Reference these directly (e.g. `SharedTransitionSpecs.albumCoverEnter`) —
 * they're immutable constants, so no CompositionLocal is needed.
 *
 * Every component of a transition (scale/slide AND fade) shares the same
 * easing and duration, so the motion reads as one cohesive gesture rather
 * than a scale easing differently from its fade.
 */
object SharedTransitionSpecs {

    const val albumCoverTransitionDuration = 350
    const val screenTransitionDuration = 300

    val albumCoverEnter: EnterTransition =
        scaleIn(
            animationSpec = tween(albumCoverTransitionDuration, easing = EaseOutExpo),
            initialScale = 0.94f
        ) + fadeIn(
            animationSpec = tween(albumCoverTransitionDuration, easing = EaseOutExpo)
        )

    val albumCoverExit: ExitTransition =
        scaleOut(
            animationSpec = tween(albumCoverTransitionDuration, easing = EaseOutExpo),
            targetScale = 0.94f
        ) + fadeOut(
            animationSpec = tween(albumCoverTransitionDuration, easing = EaseOutExpo)
        )

    val screenSlideUp: EnterTransition =
        slideInVertically(
            animationSpec = tween(screenTransitionDuration, easing = EaseOutExpo),
            initialOffsetY = { it }
        ) + fadeIn(
            animationSpec = tween(screenTransitionDuration, easing = EaseOutExpo)
        )

    val screenSlideDown: ExitTransition =
        slideOutVertically(
            animationSpec = tween(screenTransitionDuration, easing = EaseOutExpo),
            targetOffsetY = { it }
        ) + fadeOut(
            animationSpec = tween(screenTransitionDuration, easing = EaseOutExpo)
        )
}
