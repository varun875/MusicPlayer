package com.suspended.music.ui.components

import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade

private const val BACKDROP_SIZE_BLURRED = 256   // API 31+: blur hides the low resolution
private const val BACKDROP_SIZE_FALLBACK = 720  // API < 31: no blur, so keep it presentable

/**
 * A full-bleed, blurred album-art backdrop.
 *
 * The optimization here is feeding the blur shader a small bitmap, not gating
 * on the lifecycle: Compose stops drawing the moment the window is hidden, so
 * a paused screen costs nothing whether or not it's blurred. The real cost is
 * the GPU blur over many pixels while visible — and since a 15dp blur destroys
 * all detail, a tiny downsampled source looks identical for a fraction of the
 * fill rate.
 */
@Composable
fun BatteryOptimizedBlurBackground(
    albumArtUri: String?,
    modifier: Modifier = Modifier,
    blurRadius: Float = 15f,
    alpha: Float = 0.6f
) {
    if (albumArtUri == null) return

    // RenderEffect blur only runs on API 31+; Modifier.blur is a no-op below
    // that, so use a larger source there to avoid a blocky, un-blurred backdrop.
    val sourceSize = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        BACKDROP_SIZE_BLURRED
    } else {
        BACKDROP_SIZE_FALLBACK
    }

    AsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
            .data(albumArtUri)
            .size(sourceSize)
            .crossfade(durationMillis = 300)
            // Cache the small backdrop separately from any full-res art usage,
            // so the two don't evict each other from the memory cache.
            .memoryCacheKey("${albumArtUri}_backdrop")
            .diskCacheKey("${albumArtUri}_backdrop")
            .build(),
        contentDescription = null,
        modifier = modifier
            .blur(blurRadius.dp)
            .alpha(alpha)
    )
}
