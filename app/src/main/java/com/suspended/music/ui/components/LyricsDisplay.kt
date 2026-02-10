package com.suspended.music.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class LyricLine(
    val timestamp: Long, // in milliseconds
    val text: String,
    val translation: String? = null
)

@Composable
fun LyricsDisplay(
    lyrics: List<LyricLine>,
    currentPosition: Long,
    isVisible: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    
    // Find current lyric index
    val currentLyricIndex by remember(currentPosition, lyrics) {
        derivedStateOf {
            lyrics.indexOfLast { it.timestamp <= currentPosition }
        }
    }

    // Auto-scroll to current lyric
    LaunchedEffect(currentLyricIndex) {
        if (currentLyricIndex >= 0) {
            listState.animateScrollToItem(
                index = currentLyricIndex,
                scrollOffset = listState.layoutInfo.visibleItemsInfo
                    .getOrNull(listState.firstVisibleItemIndex)?.size?.div(2) ?: 0
            )
        }
    }

    val overlayAlpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(300),
        label = "lyrics_overlay_alpha"
    )

    Box(modifier = modifier.fillMaxSize()) {
        // Background overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.8f * overlayAlpha))
                .clickable(onClick = onDismiss)
        )

        // Lyrics content
        if (isVisible) {
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                shadowElevation = 16.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp)
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Lyrics",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "Close",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Progress indicator
                    LinearProgressIndicator(
                        progress = { 
                            if (lyrics.isNotEmpty() && currentLyricIndex >= 0) {
                                currentLyricIndex.toFloat() / lyrics.size.toFloat()
                            } else 0f
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Lyrics list
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center
                    ) {
                        itemsIndexed(lyrics) { index, lyric ->
                            LyricLineItem(
                                lyric = lyric,
                                isActive = index == currentLyricIndex,
                                isPast = index < currentLyricIndex,
                                isFuture = index > currentLyricIndex
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LyricLineItem(
    lyric: LyricLine,
    isActive: Boolean,
    isPast: Boolean,
    isFuture: Boolean
) {
    val alpha by animateFloatAsState(
        targetValue = when {
            isActive -> 1f
            isPast -> 0.6f
            isFuture -> 0.4f
            else -> 0.3f
        },
        animationSpec = tween(300),
        label = "lyric_alpha"
    )

    val fontSize by animateFloatAsState(
        targetValue = when {
            isActive -> 20f
            isPast -> 16f
            isFuture -> 14f
            else -> 12f
        },
        animationSpec = tween(300),
        label = "lyric_font_size"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .alpha(alpha)
    ) {
        if (isActive) {
            // Active lyric with highlight
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = lyric.text,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontSize = fontSize.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (!lyric.translation.isNullOrEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = lyric.translation,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = (fontSize - 4).sp
                            ),
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        } else {
            // Inactive lyric
            Text(
                text = lyric.text,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = fontSize.sp,
                    fontWeight = if (isPast) FontWeight.Medium else FontWeight.Normal
                ),
                color = MaterialTheme.colorScheme.onSurface.copy(
                    alpha = if (isPast) 0.8f else 0.5f
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            if (!lyric.translation.isNullOrEmpty() && isPast) {
                Text(
                    text = lyric.translation,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = (fontSize - 2).sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

// Sample lyrics data for testing
val sampleLyrics = listOf(
    LyricLine(0, "♪ Instrumental Intro ♪"),
    LyricLine(5000, "Welcome to the music player"),
    LyricLine(8000, "Where melodies come alive"),
    LyricLine(12000, "Every note tells a story", "每个音符都在讲述一个故事"),
    LyricLine(16000, "Every beat touches your heart", "每个节拍都触动你的心"),
    LyricLine(20000, "♪ Bridge ♪"),
    LyricLine(24000, "Music is the universal language"),
    LyricLine(28000, "That connects us all together"),
    LyricLine(32000, "In this moment, we are one", "在这一刻，我们是一体的"),
    LyricLine(36000, "♪ Outro ♪"),
    LyricLine(40000, "Thank you for listening")
)
