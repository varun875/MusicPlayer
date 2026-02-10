package com.suspended.music.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage

/**
 * Shared element transition for album art
 * Use this when transitioning between album list and album detail screens
 *
 * @param imageUrl The URL of the image to display and transition
 * @param isVisible Whether the image should be visible (true during transition)
 * @param startSize The starting size of the image
 * @param endSize The ending/expanded size of the image
 * @param contentDescription Description for accessibility
 * @param modifier Modifier for the container
 * @param onTransitionComplete Callback when transition completes
 */
@Composable
fun SharedElementAlbumArt(
    imageUrl: String,
    isVisible: Boolean,
    startSize: Dp = 48.dp,
    endSize: Dp = 256.dp,
    contentDescription: String? = null,
    modifier: Modifier = Modifier,
    onTransitionComplete: () -> Unit = {}
) {
    val size by animateDpAsState(
        targetValue = if (isVisible) endSize else startSize,
        animationSpec = spring(dampingRatio = 0.75f),
        label = "album_art_size",
        finishedListener = { onTransitionComplete() }
    )
    
    val cornerRadius by animateDpAsState(
        targetValue = if (isVisible) 16.dp else 4.dp,
        animationSpec = spring(dampingRatio = 0.75f),
        label = "album_art_radius"
    )
    
    val alpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0.7f,
        animationSpec = spring(dampingRatio = 0.75f),
        label = "album_art_alpha"
    )
    
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(cornerRadius))
            .background(Color.DarkGray)
    ) {
        AsyncImage(
            model = imageUrl,
            contentDescription = contentDescription,
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(cornerRadius)),
            contentScale = ContentScale.Crop,
            alpha = alpha
        )
    }
}

/**
 * Expandable hero transition from thumbnail to full-screen album detail
 * Provides visual continuity between list and detail screens
 *
 * @param thumbnailUrl URL of the thumbnail/smaller image
 * @param detailUrl URL of the full-size image (can be same as thumbnail)
 * @param isTransitioning Whether we're in transition state
 * @param modifier Modifier for the root container
 * @param onThumbnailClick Callback when thumbnail is clicked (start transition)
 * @param detailContent The detail screen content to show after transition
 */
@Composable
fun HeroTransition(
    thumbnailUrl: String,
    detailUrl: String,
    isTransitioning: Boolean,
    modifier: Modifier = Modifier,
    onThumbnailClick: () -> Unit = {},
    detailContent: @Composable () -> Unit = {}
) {
    AnimatedVisibility(
        visible = !isTransitioning,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        // Show thumbnail
        AsyncImage(
            model = thumbnailUrl,
            contentDescription = null,
            modifier = modifier
                .size(64.dp)
                .clip(RoundedCornerShape(8.dp)),
            contentScale = ContentScale.Crop
        )
    }
    
    AnimatedVisibility(
        visible = isTransitioning,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut()
    ) {
        // Show expanded content
        Box(modifier = Modifier.fillMaxSize()) {
            detailContent()
        }
    }
}

/**
 * Cross-fade transition between two images
 * Use for smooth image swaps in galleries or detail views
 *
 * @param imageUrl The current image URL
 * @param previousImageUrl The previous image URL (for cross-fade effect)
 * @param isLoading Whether the new image is still loading
 * @param modifier Modifier for the image container
 * @param contentDescription Description for accessibility
 */
@Composable
fun CrossFadeImage(
    imageUrl: String,
    previousImageUrl: String? = null,
    isLoading: Boolean = false,
    modifier: Modifier = Modifier,
    contentDescription: String? = null
) {
    val alpha by animateFloatAsState(
        targetValue = if (isLoading) 0f else 1f,
        animationSpec = spring(dampingRatio = 0.8f),
        label = "cross_fade_alpha"
    )
    
    Box(modifier = modifier) {
        // Previous image as background (during loading)
        if (previousImageUrl != null && isLoading) {
            AsyncImage(
                model = previousImageUrl,
                contentDescription = contentDescription,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                alpha = 0.5f
            )
        }
        
        // New image with fade-in
        AsyncImage(
            model = imageUrl,
            contentDescription = contentDescription,
            modifier = Modifier
                .fillMaxSize()
                .let { if (alpha < 1f) it else it },
            contentScale = ContentScale.Crop,
            alpha = alpha
        )
    }
}
