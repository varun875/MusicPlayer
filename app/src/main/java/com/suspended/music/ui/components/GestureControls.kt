package com.suspended.music.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.clickable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlin.math.abs

data class GestureState(
    val isDragging: Boolean = false,
    val dragAmountX: Float = 0f,
    val dragAmountY: Float = 0f,
    val swipeDirection: SwipeDirection? = null
)

enum class SwipeDirection {
    LEFT, RIGHT, UP, DOWN, NONE
}

@Composable
fun GestureControlOverlay(
    modifier: Modifier = Modifier,
    onSwipeLeft: () -> Unit = {},
    onSwipeRight: () -> Unit = {},
    onSwipeUp: () -> Unit = {},
    onSwipeDown: () -> Unit = {},
    onTap: () -> Unit = {},
    onDoubleTap: () -> Unit = {},
    onSeekForward: (Float) -> Unit = {},
    onSeekBackward: (Float) -> Unit = {},
    content: @Composable () -> Unit
) {
    val density = LocalDensity.current
    var gestureState by remember { mutableStateOf(GestureState()) }
    var dragStartX by remember { mutableStateOf(0f) }
    var dragStartY by remember { mutableStateOf(0f) }
    
    val overlayAlpha by animateFloatAsState(
        targetValue = if (gestureState.isDragging) 0.3f else 0f,
        animationSpec = tween(300),
        label = "gesture_overlay_alpha"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onTap() },
                    onDoubleTap = { onDoubleTap() }
                )
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        dragStartX = offset.x
                        dragStartY = offset.y
                        gestureState = gestureState.copy(isDragging = true)
                    },
                    onDragEnd = {
                        val dragAmountX = gestureState.dragAmountX
                        val dragAmountY = gestureState.dragAmountY
                        
                        // Determine swipe direction
                        val swipeThreshold = with(density) { 50.dp.toPx() }
                        
                        when {
                            dragAmountX > 0 && dragAmountX > abs(dragAmountY) -> {
                                // Horizontal swipe right
                                if (dragAmountX > swipeThreshold) {
                                    onSwipeRight()
                                } else if (dragAmountX > with(density) { 20.dp.toPx() }) {
                                    // Small horizontal drag - seek
                                    onSeekForward(dragAmountX)
                                }
                            }
                            dragAmountX < 0 && abs(dragAmountX) > abs(dragAmountY) -> {
                                // Horizontal swipe left
                                if (abs(dragAmountX) > swipeThreshold) {
                                    onSwipeLeft()
                                } else if (abs(dragAmountX) > with(density) { 20.dp.toPx() }) {
                                    // Small horizontal drag - seek
                                    onSeekBackward(abs(dragAmountX))
                                }
                            }
                            dragAmountY > 0 && dragAmountY > abs(dragAmountX) -> {
                                // Vertical swipe down
                                if (dragAmountY > swipeThreshold) {
                                    onSwipeDown()
                                }
                            }
                            dragAmountY < 0 && abs(dragAmountY) > abs(dragAmountX) -> {
                                // Vertical swipe up
                                if (abs(dragAmountY) > swipeThreshold) {
                                    onSwipeUp()
                                }
                            }
                        }
                        
                        gestureState = gestureState.copy(
                            isDragging = false,
                            dragAmountX = 0f,
                            dragAmountY = 0f,
                            swipeDirection = null
                        )
                    }
                ) { change, _ ->
                    val currentDragAmountX = change.position.x - dragStartX
                    val currentDragAmountY = change.position.y - dragStartY
                    
                    gestureState = gestureState.copy(
                        dragAmountX = currentDragAmountX,
                        dragAmountY = currentDragAmountY,
                        swipeDirection = when {
                            kotlin.math.abs(currentDragAmountX) > kotlin.math.abs(currentDragAmountY) -> {
                                if (currentDragAmountX > 0) SwipeDirection.RIGHT else SwipeDirection.LEFT
                            }
                            kotlin.math.abs(currentDragAmountY) > 0 -> {
                                if (currentDragAmountY > 0) SwipeDirection.DOWN else SwipeDirection.UP
                            }
                            else -> SwipeDirection.NONE
                        }
                    )
                }
            }
    ) {
        // Content
        content()
        
        // Gesture overlay
        if (gestureState.isDragging) {
            GestureOverlay(
                gestureState = gestureState,
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(overlayAlpha)
            )
        }
    }
}

@Composable
private fun GestureOverlay(
    gestureState: GestureState,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer {
                when (gestureState.swipeDirection) {
                    SwipeDirection.LEFT -> {
                        translationX = gestureState.dragAmountX * 0.3f
                        rotationZ = gestureState.dragAmountX * 0.01f
                    }
                    SwipeDirection.RIGHT -> {
                        translationX = gestureState.dragAmountX * 0.3f
                        rotationZ = gestureState.dragAmountX * 0.01f
                    }
                    SwipeDirection.UP -> {
                        translationY = gestureState.dragAmountY * 0.3f
                    }
                    SwipeDirection.DOWN -> {
                        translationY = gestureState.dragAmountY * 0.3f
                    }
                    SwipeDirection.NONE -> { /* No transformation */ }
                    null -> { /* No transformation */ }
                }
            }
    ) {
        // Visual feedback for gestures
        when (gestureState.swipeDirection) {
            SwipeDirection.LEFT -> {
                SwipeIndicator(
                    direction = "SKIP NEXT",
                    color = Color(0xFF4CAF50),
                    progress = abs(gestureState.dragAmountX) / with(LocalDensity.current) { 200.dp.toPx() }
                )
            }
            SwipeDirection.RIGHT -> {
                SwipeIndicator(
                    direction = "SKIP PREVIOUS",
                    color = Color(0xFFFF9800),
                    progress = abs(gestureState.dragAmountX) / with(LocalDensity.current) { 200.dp.toPx() }
                )
            }
            SwipeDirection.UP -> {
                SwipeIndicator(
                    direction = "VOLUME UP",
                    color = Color(0xFF2196F3),
                    progress = abs(gestureState.dragAmountY) / with(LocalDensity.current) { 200.dp.toPx() }
                )
            }
            SwipeDirection.DOWN -> {
                SwipeIndicator(
                    direction = "VOLUME DOWN",
                    color = Color(0xFF9C27B0),
                    progress = abs(gestureState.dragAmountY) / with(LocalDensity.current) { 200.dp.toPx() }
                )
            }
            SwipeDirection.NONE -> {
                if (abs(gestureState.dragAmountX) > with(LocalDensity.current) { 10.dp.toPx() }) {
                    SeekIndicator(
                        direction = if (gestureState.dragAmountX > 0) "FORWARD" else "BACKWARD",
                        progress = abs(gestureState.dragAmountX) / with(LocalDensity.current) { 100.dp.toPx() }
                    )
                }
            }
            null -> { /* No gesture */ }
        }
    }
}

@Composable
private fun SwipeIndicator(
    direction: String,
    color: Color,
    progress: Float
) {
    // This would be a custom composable showing swipe direction
    // For now, it's a placeholder
    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                alpha = progress.coerceIn(0f, 1f)
            }
    ) {
        // Visual feedback would go here
    }
}

@Composable
private fun SeekIndicator(
    direction: String,
    progress: Float
) {
    // This would be a custom composable showing seek direction
    // For now, it's a placeholder
    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                alpha = progress.coerceIn(0f, 1f)
            }
    ) {
        // Visual feedback would go here
    }
}

@Composable
fun AlbumArtGestureControls(
    isPlaying: Boolean,
    onPlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onSeek: (Float) -> Unit = {},
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val density = LocalDensity.current
    
    GestureControlOverlay(
        modifier = modifier,
        onSwipeLeft = onSkipNext,
        onSwipeRight = onSkipPrevious,
        onTap = onPlayPause,
        onSeekForward = { amount -> onSeek(amount / with(density) { 100.dp.toPx() }) },
        onSeekBackward = { amount -> onSeek(-amount / with(density) { 100.dp.toPx() }) },
        content = content
    )
}
