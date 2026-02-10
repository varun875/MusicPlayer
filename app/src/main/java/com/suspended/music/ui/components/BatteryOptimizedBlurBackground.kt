package com.suspended.music.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import kotlinx.coroutines.delay

@Composable
fun BatteryOptimizedBlurBackground(
    albumArtUri: String?,
    modifier: Modifier = Modifier,
    blurRadius: Float = 15f, // Reduced from 25f
    alpha: Float = 0.6f
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    var shouldBlur by remember { mutableStateOf(false) }
    
    // Only apply blur when app is in foreground and after a delay
    LaunchedEffect(Unit) {
        // Observe lifecycle state changes
        snapshotFlow { lifecycleOwner.lifecycle.currentState }
            .collect { state ->
                when (state) {
                    Lifecycle.State.RESUMED -> {
                        delay(500) // Delay blur application
                        shouldBlur = true
                    }
                    else -> {
                        shouldBlur = false
                    }
                }
            }
    }
    
    if (albumArtUri != null) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(albumArtUri)
                .crossfade(durationMillis = 300) // Reduced from 500ms
                .memoryCacheKey("${albumArtUri}_blur_optimized")
                .diskCacheKey("${albumArtUri}_blur_optimized")
                .build(),
            contentDescription = null,
            modifier = modifier.then(
                if (shouldBlur) {
                    Modifier.blur(blurRadius.dp)
                } else {
                    Modifier
                }
            )
        )
    }
}
