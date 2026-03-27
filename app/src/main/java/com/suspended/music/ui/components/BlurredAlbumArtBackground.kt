package com.suspended.music.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade

@Composable
fun BlurredAlbumArtBackground(
    albumArtUri: String?,
    modifier: Modifier = Modifier,
    blurRadius: Float = 25f,
    alpha: Float = 0.6f
) {
    val context = LocalContext.current
    
    // Remember the blur effect to avoid recomputation
    val blurModifier = remember(blurRadius) {
        Modifier.blur(blurRadius.dp)
    }
    
    Box(modifier = modifier.fillMaxSize()) {
        // Background color fallback
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.background
                        )
                    )
                )
        )
        
        // Blurred album art
        if (albumArtUri != null) {
            val imageRequest = remember(context, albumArtUri) {
                ImageRequest.Builder(context)
                    .data(albumArtUri)
                    .crossfade(durationMillis = 500)
                    .build()
            }

            AsyncImage(
                model = imageRequest,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .then(blurModifier),
                contentScale = ContentScale.Crop
            )
            
            // Dark overlay for better text visibility
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = alpha),
                                Color.Black.copy(alpha = alpha * 0.8f),
                                Color.Black.copy(alpha = alpha * 0.6f)
                            )
                        )
                    )
            )
        }
    }
}
