package com.suspended.music.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalView
import androidx.lifecycle.Lifecycle
import kotlinx.coroutines.delay

@Composable
fun BatteryOptimizedWaveform(
    isPlaying: Boolean,
    isVisible: Boolean = true,
    modifier: Modifier = Modifier
) {
    val animatedProgress = remember { Animatable(0f) }
    val lifecycleOwner = LocalLifecycleOwner.current
    val view = LocalView.current
    
    // Track lifecycle state
    var isResumed by remember { mutableStateOf(false) }
    LaunchedEffect(lifecycleOwner) {
        snapshotFlow { lifecycleOwner.lifecycle.currentState }
            .collect { state ->
                isResumed = state == Lifecycle.State.RESUMED
            }
    }
    
    // Only animate when playing, visible, and app is in foreground
    val shouldAnimate = isPlaying && isVisible && isResumed && view.isAttachedToWindow
    
    LaunchedEffect(shouldAnimate) {
        if (shouldAnimate) {
            // Slower animation for battery efficiency
            animatedProgress.animateTo(
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(
                        durationMillis = 3000, // Slower than 2000ms
                        easing = LinearEasing
                    ),
                    repeatMode = RepeatMode.Restart
                )
            )
        } else {
            animatedProgress.stop()
        }
    }
    
    // Only render when visible
    if (isVisible) {
        WaveformVisualization(
            isPlaying = shouldAnimate,
            modifier = modifier
        )
    }
}
