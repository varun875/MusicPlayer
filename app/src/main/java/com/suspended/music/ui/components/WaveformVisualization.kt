package com.suspended.music.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.sin

@Composable
fun WaveformVisualization(
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    waveColor: Color = MaterialTheme.colorScheme.primary,
    backgroundColor: Color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
) {
    val animatedProgress = remember { Animatable(0f) }
    
    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            animatedProgress.animateTo(
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(
                        durationMillis = 2000,
                        easing = LinearEasing
                    ),
                    repeatMode = RepeatMode.Restart
                )
            )
        } else {
            animatedProgress.stop()
        }
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(120.dp)
            .clip(RoundedCornerShape(16.dp)),
        color = backgroundColor,
        shadowElevation = 4.dp
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            drawWaveform(
                progress = animatedProgress.value,
                waveColor = waveColor,
                isPlaying = isPlaying
            )
        }
    }
}

@Composable
fun CircularWaveformVisualization(
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    waveColor: Color = MaterialTheme.colorScheme.primary,
    backgroundColor: Color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
) {
    val animatedProgress = remember { Animatable(0f) }
    val rotationAngle = remember { Animatable(0f) }
    
    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            animatedProgress.animateTo(
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(
                        durationMillis = 1500,
                        easing = LinearEasing
                    ),
                    repeatMode = RepeatMode.Restart
                )
            )
            
            rotationAngle.animateTo(
                targetValue = 360f,
                animationSpec = infiniteRepeatable(
                    animation = tween(
                        durationMillis = 8000,
                        easing = LinearEasing
                    ),
                    repeatMode = RepeatMode.Restart
                )
            )
        } else {
            animatedProgress.stop()
            rotationAngle.stop()
        }
    }

    Surface(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        color = Color.Transparent,
        shape = RoundedCornerShape(16.dp)
    ) {
        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            val canvasScope = this
            canvasScope.drawCircularWaveform(
                progress = animatedProgress.value,
                rotation = rotationAngle.value,
                waveColor = waveColor,
                isPlaying = isPlaying
            )
        }
    }
}

private fun DrawScope.drawWaveform(
    progress: Float,
    waveColor: Color,
    isPlaying: Boolean
) {
    val width = size.width
    val height = size.height
    val centerY = height / 2
    val barCount = 60
    val barWidth = width / barCount
    val maxBarHeight = height * 0.8f

    for (i in 0 until barCount) {
        val x = i * barWidth + barWidth / 2
        
        // Create wave pattern
        val normalizedX = i.toFloat() / barCount
        val waveHeight = when {
            isPlaying -> {
                val sineWave = sin(normalizedX * PI * 4 + progress * PI * 2).toFloat()
                val noise = (kotlin.random.Random.nextFloat() - 0.5f) * 0.3f
                (sineWave * 0.5f + 0.5f + noise) * maxBarHeight
            }
            else -> {
                sin(normalizedX * PI * 2).toFloat() * maxBarHeight * 0.3f
            }
        }

        // Calculate bar height with animation
        val animatedHeight = if (isPlaying) {
            waveHeight * (0.5f + progress * 0.5f)
        } else {
            waveHeight * 0.3f
        }

        val barHeight = kotlin.math.abs(animatedHeight).coerceAtLeast(2f)
        
        // Create gradient for each bar
        val gradient = Brush.verticalGradient(
            colors = listOf(
                waveColor.copy(alpha = 0.8f),
                waveColor.copy(alpha = 0.4f)
            )
        )

        // Draw bar
        drawRoundRect(
            brush = gradient,
            topLeft = Offset(
                x = x - barWidth / 4,
                y = centerY - barHeight / 2
            ),
            size = androidx.compose.ui.geometry.Size(
                width = barWidth / 2,
                height = barHeight
            ),
            alpha = if (isPlaying) 0.8f + progress * 0.2f else 0.4f
        )
    }
}

private fun DrawScope.drawCircularWaveform(
    progress: Float,
    rotation: Float,
    waveColor: Color,
    isPlaying: Boolean
) {
    val centerX = size.width / 2
    val centerY = size.height / 2
    val radius = kotlin.math.min(centerX, centerY) * 0.7f
    val barCount = 72
    val angleStep = (360f / barCount).toFloat()

    rotate(rotation, pivot = Offset(centerX, centerY)) {
        for (i in 0 until barCount) {
            val angle = i * angleStep
            val angleRad = kotlin.math.PI * angle / 180f
            
            // Calculate bar position
            val startX = centerX + kotlin.math.cos(angleRad).toFloat() * (radius * 0.8f)
            val startY = centerY + kotlin.math.sin(angleRad).toFloat() * (radius * 0.8f)
            
            // Create wave pattern for bar height
            val normalizedIndex = i.toFloat() / barCount
            val waveHeight = when {
                isPlaying -> {
                    val sineWave = sin(normalizedIndex * PI * 6 + progress * PI * 4).toFloat()
                    (sineWave * 0.5f + 0.5f) * radius * 0.4f
                }
                else -> {
                    sin(normalizedIndex * PI * 3).toFloat() * radius * 0.1f
                }
            }

            val animatedHeight = if (isPlaying) {
                waveHeight * (0.5f + progress * 0.5f)
            } else {
                waveHeight * 0.3f
            }

            val endX = centerX + kotlin.math.cos(angleRad).toFloat() * (radius * 0.8f + animatedHeight)
            val endY = centerY + kotlin.math.sin(angleRad).toFloat() * (radius * 0.8f + animatedHeight)

            // Create gradient for each bar
            val gradient = Brush.linearGradient(
                colors = listOf(
                    waveColor.copy(alpha = 0.9f),
                    waveColor.copy(alpha = 0.3f)
                ),
                start = Offset(startX, startY),
                end = Offset(endX, endY)
            )

            // Draw bar
            drawLine(
                brush = gradient,
                start = Offset(startX, startY),
                end = Offset(endX, endY),
                strokeWidth = 3f,
                alpha = if (isPlaying) 0.8f + progress * 0.2f else 0.4f
            )
        }
    }
}

@Composable
fun MiniWaveformVisualization(
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    waveColor: Color = MaterialTheme.colorScheme.primary
) {
    val animatedProgress = remember { Animatable(0f) }
    
    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            animatedProgress.animateTo(
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(
                        durationMillis = 1000,
                        easing = LinearEasing
                    ),
                    repeatMode = RepeatMode.Restart
                )
            )
        } else {
            animatedProgress.stop()
        }
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(40.dp)
    ) {
        val width = size.width
        val height = size.height
        val centerY = height / 2
        val barCount = 20
        val barWidth = width / barCount

        for (i in 0 until barCount) {
            val x = i * barWidth + barWidth / 2
            
            val normalizedX = i.toFloat() / barCount
            val waveHeight = if (isPlaying) {
                val sineWave = sin(normalizedX * PI * 2 + animatedProgress.value * PI * 2).toFloat()
                (sineWave * 0.5f + 0.5f) * height * 0.6f
            } else {
                height * 0.2f
            }

            val animatedHeight = if (isPlaying) {
                waveHeight * (0.3f + animatedProgress.value * 0.7f)
            } else {
                waveHeight * 0.3f
            }

            drawRoundRect(
                color = waveColor,
                topLeft = Offset(
                    x = x - barWidth / 3,
                    y = centerY - animatedHeight / 2
                ),
                size = androidx.compose.ui.geometry.Size(
                    width = barWidth / 1.5f,
                    height = animatedHeight
                ),
                alpha = if (isPlaying) 0.7f + animatedProgress.value * 0.3f else 0.3f
            )
        }
    }
}
