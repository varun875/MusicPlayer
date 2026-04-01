package com.suspended.music.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.runtime.Composable
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavController
import androidx.navigation.compose.composable

/**
 * Duration for page transitions in milliseconds
 */
private const val TRANSITION_DURATION = 150
private const val FAST_TRANSITION_DURATION = 100

/**
 * Creates smooth slide and fade transitions between screens (forward)
 */
fun slideAndFadeTransition(): AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
    slideIntoContainer(
        towards = AnimatedContentTransitionScope.SlideDirection.Left,
        animationSpec = tween(TRANSITION_DURATION, easing = EaseInOut)
    ) + fadeIn(animationSpec = tween(TRANSITION_DURATION, easing = EaseInOut))
}

/**
 * Creates smooth slide and fade transitions for back navigation
 * Includes a scale effect for visual depth
 */
fun slideAndFadeTransitionBack(): AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
    slideIntoContainer(
        towards = AnimatedContentTransitionScope.SlideDirection.Right,
        animationSpec = tween(TRANSITION_DURATION, easing = EaseInOut)
    ) + fadeIn(animationSpec = tween(TRANSITION_DURATION, easing = EaseInOut)) +
    scaleIn(
        animationSpec = tween(TRANSITION_DURATION, easing = EaseOut),
        initialScale = 0.95f
    )
}

/**
 * Creates smooth slide and fade exit transitions
 */
fun slideAndFadeExitTransition(): AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
    slideOutOfContainer(
        towards = AnimatedContentTransitionScope.SlideDirection.Left,
        animationSpec = tween(TRANSITION_DURATION, easing = EaseInOut)
    ) + fadeOut(animationSpec = tween(TRANSITION_DURATION, easing = EaseInOut))
}

/**
 * Creates smooth slide and fade exit transitions for back navigation
 */
fun slideAndFadeExitTransitionBack(): AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
    slideOutOfContainer(
        towards = AnimatedContentTransitionScope.SlideDirection.Right,
        animationSpec = tween(TRANSITION_DURATION, easing = EaseInOut)
    ) + fadeOut(animationSpec = tween(TRANSITION_DURATION, easing = EaseInOut)) +
    scaleOut(
        animationSpec = tween(TRANSITION_DURATION, easing = EaseOut),
        targetScale = 0.95f
    )
}

/**
 * Creates fade-only transitions for modal-like screens
 */
fun fadeTransition(): AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
    fadeIn(animationSpec = tween(FAST_TRANSITION_DURATION, easing = EaseInOut))
}

fun fadeExitTransition(): AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
    fadeOut(animationSpec = tween(FAST_TRANSITION_DURATION, easing = EaseInOut))
}

/**
 * Extension function for adding composable with smooth fade+slide transitions
 * Call this for standard forward navigation
 */
fun NavGraphBuilder.animatedComposable(
    route: String,
    content: @Composable (NavBackStackEntry) -> Unit
) {
    composable(
        route = route,
        enterTransition = {
            slideIntoContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Left,
                animationSpec = tween(TRANSITION_DURATION, easing = EaseInOut)
            ) + fadeIn(animationSpec = tween(TRANSITION_DURATION, easing = EaseInOut))
        },
        exitTransition = {
            slideOutOfContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Left,
                animationSpec = tween(TRANSITION_DURATION, easing = EaseInOut)
            ) + fadeOut(animationSpec = tween(TRANSITION_DURATION, easing = EaseInOut))
        },
        popEnterTransition = {
            slideIntoContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Right,
                animationSpec = tween(TRANSITION_DURATION, easing = EaseInOut)
            ) + fadeIn(animationSpec = tween(TRANSITION_DURATION, easing = EaseInOut)) +
            scaleIn(
                animationSpec = tween(TRANSITION_DURATION, easing = EaseOut),
                initialScale = 0.95f
            )
        },
        popExitTransition = {
            slideOutOfContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Right,
                animationSpec = tween(TRANSITION_DURATION, easing = EaseInOut)
            ) + fadeOut(animationSpec = tween(TRANSITION_DURATION, easing = EaseInOut)) +
            scaleOut(
                animationSpec = tween(TRANSITION_DURATION, easing = EaseOut),
                targetScale = 0.95f
            )
        }
    ) { backStackEntry ->
        content(backStackEntry)
    }
}

/**
 * Extension function for NavController with smooth fade+slide navigation
 * Applies to forward navigation with standard layout pop-up behavior
 */
fun NavController.navigateWithAnimation(route: String) {
    navigate(route) {
        popUpTo(graph.startDestinationId) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}

/**
 * Navigate with back navigation to a specific destination
 * Includes special handling for back stack state
 */
fun NavController.navigateBack() {
    popBackStack()
}