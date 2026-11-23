package com.suspended.music.ui.screens

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.Player
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.suspended.music.data.model.Song
import com.suspended.music.ui.components.BatteryOptimizedBlurBackground
import com.suspended.music.ui.components.PlaylistPickerDialog
import com.suspended.music.ui.utils.rememberHapticFeedback
import com.suspended.music.ui.utils.rememberReducedMotion
import com.suspended.music.ui.viewmodel.MainViewModel
import kotlin.math.abs

@Composable
fun NowPlayingScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToQueue: () -> Unit,
    onNavigateToEqualizer: () -> Unit,
    onNavigateToTagEditor: (Long) -> Unit,
    onNavigateToSearch: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val currentSong by viewModel.currentSong.collectAsStateWithLifecycle()
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    val currentPosition by viewModel.currentPosition.collectAsStateWithLifecycle()
    val duration by viewModel.duration.collectAsStateWithLifecycle()
    val repeatMode by viewModel.repeatMode.collectAsStateWithLifecycle()
    val shuffleEnabled by viewModel.shuffleEnabled.collectAsStateWithLifecycle()
    val sleepTimerRemainingMs by viewModel.sleepTimerRemainingMs.collectAsStateWithLifecycle()

    var showAddToPlaylist by remember { mutableStateOf(false) }
    var showSleepTimer by remember { mutableStateOf(false) }

    val hapticFeedback = rememberHapticFeedback()
    val song = currentSong
    if (song == null) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF2C2C2E),
                            Color(0xFF1C1C1E),
                            Color(0xFF000000)
                        )
                    )
                )
        )
        return
    }

    // Genuine back navigation is handled by NavDisplay's predictive-back /
    // onBack callback; a local BackHandler would double-pop.

    val progressVal by remember {
        derivedStateOf {
            if (duration > 0) currentPosition.toFloat() / duration.toFloat() else 0f
        }
    }

    val playButtonScale by animateFloatAsState(
        targetValue = if (isPlaying) 1f else 0.94f,
        animationSpec = tween(durationMillis = 150),
        label = "play_scale"
    )

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val isWide = configuration.screenWidthDp >= 840

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF2C2C2E),
                        Color(0xFF1C1C1E),
                        Color(0xFF000000)
                    )
                )
            )
    ) {
        // ── 1. Ambient Blurred Backdrop ──
        // The full-screen blur is the most expensive effect on this screen, so
        // skip it entirely when the platform asks for reduced motion.
        val reducedMotion = rememberReducedMotion()
        if (!reducedMotion) {
            BatteryOptimizedBlurBackground(
                albumArtUri = song.albumArtUri.toString(),
                modifier = Modifier.fillMaxSize(),
                blurRadius = 32f,
                alpha = 0.65f
            )
        }

        // ── 2. Frosted Gradient Overlay (Apple: light touch, let art glow through) ──
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.0f to Color.Black.copy(alpha = 0.22f),
                        0.40f to Color.Black.copy(alpha = 0.08f),
                        0.55f to Color.Black.copy(alpha = 0.30f),
                        0.75f to Color.Black.copy(alpha = 0.50f),
                        1.0f to Color.Black.copy(alpha = 0.62f)
                    )
                )
        )

        // ── 3. Content Layout ──
        if (isLandscape && !isWide) {
            // Landscape Phone: Two Columns
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                // Left Column: Top bar + Album Art
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    NowPlayingTopBar(
                        sleepTimerRemainingMs = sleepTimerRemainingMs,
                        onNavigateBack = onNavigateBack,
                        onNavigateToSearch = onNavigateToSearch,
                        onNavigateToQueue = onNavigateToQueue,
                        onNavigateToEqualizer = onNavigateToEqualizer,
                        onSleepTimerClick = { showSleepTimer = true },
                        onNavigateToTagEditor = { onNavigateToTagEditor(song.id) },
                        onNavigateToSettings = onNavigateToSettings,
                        hapticFeedback = hapticFeedback
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        NowPlayingAlbumArt(
                            song = song,
                            onSkipNext = { viewModel.skipToNext() },
                            onSkipPrevious = { viewModel.skipToPrevious() },
                            onDismiss = onNavigateBack,
                            hapticFeedback = hapticFeedback,
                            modifier = Modifier
                                .fillMaxHeight()
                                .aspectRatio(1f, matchHeightConstraintsFirst = true)
                        )
                    }
                }

                // Right Column: Metadata, Progress, Controls
                Column(
                    modifier = Modifier
                        .weight(1.1f)
                        .fillMaxHeight()
                        .padding(start = 16.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    NowPlayingMetadataRow(
                        song = song,
                        onFavoriteClick = { viewModel.toggleFavorite(song) },
                        onAddToPlaylistClick = { showAddToPlaylist = true },
                        hapticFeedback = hapticFeedback
                    )

                    Spacer(Modifier.height(16.dp))

                    NowPlayingSeekBar(
                        progress = { progressVal },
                        currentPosition = currentPosition,
                        duration = duration,
                        onSeekTo = { pos -> viewModel.seekTo(pos) }
                    )

                    Spacer(Modifier.height(18.dp))

                    NowPlayingControlsRow(
                        isPlaying = isPlaying,
                        shuffleEnabled = shuffleEnabled,
                        repeatMode = repeatMode,
                        playButtonScale = playButtonScale,
                        onTogglePlayPause = { viewModel.togglePlayPause() },
                        onSkipPrevious = { viewModel.skipToPrevious() },
                        onSkipNext = { viewModel.skipToNext() },
                        onToggleShuffle = { viewModel.toggleShuffle() },
                        onToggleRepeat = { viewModel.toggleRepeatMode() },
                        hapticFeedback = hapticFeedback
                    )
                }
            }
        } else {
            // Portrait / Tablet Layout
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(horizontal = if (isWide) 48.dp else 20.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Bar
                NowPlayingTopBar(
                    sleepTimerRemainingMs = sleepTimerRemainingMs,
                    onNavigateBack = onNavigateBack,
                    onNavigateToSearch = onNavigateToSearch,
                    onNavigateToQueue = onNavigateToQueue,
                    onNavigateToEqualizer = onNavigateToEqualizer,
                    onSleepTimerClick = { showSleepTimer = true },
                    onNavigateToTagEditor = { onNavigateToTagEditor(song.id) },
                    onNavigateToSettings = onNavigateToSettings,
                    hapticFeedback = hapticFeedback
                )

                Spacer(Modifier.height(if (isWide) 16.dp else 8.dp))

                // Album Art Section (Takes top ~50% of available height)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    NowPlayingAlbumArt(
                        song = song,
                        onSkipNext = { viewModel.skipToNext() },
                        onSkipPrevious = { viewModel.skipToPrevious() },
                        onDismiss = onNavigateBack,
                        hapticFeedback = hapticFeedback,
                        modifier = Modifier
                            .fillMaxHeight(0.96f)
                            .aspectRatio(1f)
                    )
                }

                Spacer(Modifier.height(12.dp))

                // Song Title, Artist, Album with Star (left) and Add (right)
                NowPlayingMetadataRow(
                    song = song,
                    onFavoriteClick = { viewModel.toggleFavorite(song) },
                    onAddToPlaylistClick = { showAddToPlaylist = true },
                    hapticFeedback = hapticFeedback
                )

                Spacer(Modifier.height(16.dp))

                // Seekbar & Timestamps
                NowPlayingSeekBar(
                    progress = { progressVal },
                    currentPosition = currentPosition,
                    duration = duration,
                    onSeekTo = { pos -> viewModel.seekTo(pos) }
                )

                Spacer(Modifier.height(20.dp))

                // Playback Controls (Shuffle, Previous, Play/Pause, Next, Repeat)
                NowPlayingControlsRow(
                    isPlaying = isPlaying,
                    shuffleEnabled = shuffleEnabled,
                    repeatMode = repeatMode,
                    playButtonScale = playButtonScale,
                    onTogglePlayPause = { viewModel.togglePlayPause() },
                    onSkipPrevious = { viewModel.skipToPrevious() },
                    onSkipNext = { viewModel.skipToNext() },
                    onToggleShuffle = { viewModel.toggleShuffle() },
                    onToggleRepeat = { viewModel.toggleRepeatMode() },
                    hapticFeedback = hapticFeedback
                )

                Spacer(Modifier.height(if (isWide) 28.dp else 16.dp))
            }
        }
    }

    // Add to Playlist Dialog
    if (showAddToPlaylist) {
        val playlists by viewModel.playlists.collectAsStateWithLifecycle()
        val editablePlaylists = remember(playlists) { playlists.filterNot { it.isSmartPlaylist } }
        PlaylistPickerDialog(
            playlists = editablePlaylists,
            title = "Add '${song.title}' to playlist",
            emptyMessage = "Create a playlist first.",
            onDismiss = { showAddToPlaylist = false },
            onPlaylistSelected = { playlist ->
                viewModel.addSongsToPlaylist(playlist.id, listOf(song.id))
                showAddToPlaylist = false
            }
        )
    }

    // Sleep Timer Dialog — dismissing (back/scrim) must NOT cancel a running
    // timer; only the explicit "Cancel timer" button does.
    if (showSleepTimer) {
        SleepTimerDialog(
            remainingMs = sleepTimerRemainingMs,
            onDismiss = { showSleepTimer = false },
            onCancelTimer = { viewModel.cancelSleepTimer(); showSleepTimer = false },
            onSelect = { minutes ->
                viewModel.startSleepTimer(minutes)
                showSleepTimer = false
            }
        )
    }
}

// ══════════════════════════════════════════════
// 1. Top Bar Header
// ══════════════════════════════════════════════

@Composable
private fun NowPlayingTopBar(
    sleepTimerRemainingMs: Long?,
    onNavigateBack: () -> Unit,
    onNavigateToSearch: () -> Unit,
    onNavigateToQueue: () -> Unit,
    onNavigateToEqualizer: () -> Unit,
    onSleepTimerClick: () -> Unit,
    onNavigateToTagEditor: () -> Unit,
    onNavigateToSettings: () -> Unit,
    hapticFeedback: com.suspended.music.ui.utils.HapticFeedbackManager
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Collapse Chevron Button
        IconButton(
            onClick = { hapticFeedback.lightTap(); onNavigateBack() },
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.KeyboardArrowDown,
                contentDescription = "Collapse",
                tint = Color.White.copy(alpha = 0.9f),
                modifier = Modifier.size(30.dp)
            )
        }

        // Action Icons: Search, Queue, More
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            IconButton(
                onClick = { hapticFeedback.lightTap(); onNavigateToSearch() },
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = "Search",
                    tint = Color.White.copy(alpha = 0.9f),
                    modifier = Modifier.size(24.dp)
                )
            }

            IconButton(
                onClick = { hapticFeedback.lightTap(); onNavigateToQueue() },
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                    contentDescription = "Queue",
                    tint = Color.White.copy(alpha = 0.9f),
                    modifier = Modifier.size(24.dp)
                )
            }

            Box {
                var showMenu by remember { mutableStateOf(false) }
                IconButton(
                    onClick = { hapticFeedback.lightTap(); showMenu = true },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.MoreVert,
                        contentDescription = "More options",
                        tint = Color.White.copy(alpha = 0.9f),
                        modifier = Modifier.size(24.dp)
                    )
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                    modifier = Modifier.background(MaterialTheme.colorScheme.surfaceContainerHigh)
                ) {
                    DropdownMenuItem(
                        text = { Text("Equalizer") },
                        onClick = { hapticFeedback.lightTap(); showMenu = false; onNavigateToEqualizer() },
                        leadingIcon = { Icon(Icons.Filled.Equalizer, null) }
                    )
                    DropdownMenuItem(
                        text = {
                            Text(
                                if (sleepTimerRemainingMs != null)
                                    "Sleep Timer (${formatRemaining(sleepTimerRemainingMs)})"
                                else
                                    "Sleep Timer"
                            )
                        },
                        onClick = { hapticFeedback.lightTap(); showMenu = false; onSleepTimerClick() },
                        leadingIcon = { Icon(Icons.Filled.Timer, null) }
                    )
                    DropdownMenuItem(
                        text = { Text("Edit Tags") },
                        onClick = { hapticFeedback.lightTap(); showMenu = false; onNavigateToTagEditor() },
                        leadingIcon = { Icon(Icons.Filled.Edit, null) }
                    )
                    DropdownMenuItem(
                        text = { Text("Settings") },
                        onClick = { hapticFeedback.lightTap(); showMenu = false; onNavigateToSettings() },
                        leadingIcon = { Icon(Icons.Filled.Settings, null) }
                    )
                }
            }
        }
    }
}

// ══════════════════════════════════════════════
// 2. Album Artwork Display
// ══════════════════════════════════════════════

@Composable
private fun NowPlayingAlbumArt(
    song: Song,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onDismiss: () -> Unit,
    hapticFeedback: com.suspended.music.ui.utils.HapticFeedbackManager,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val artSizePx = remember(density) {
        with(density) { 380.dp.roundToPx() }
    }

    val albumArtRequest = remember(song.id, song.albumArtUri, artSizePx) {
        ImageRequest.Builder(context)
            .data(song.albumArtUri)
            .crossfade(durationMillis = 250)
            .size(artSizePx)
            .memoryCacheKey("now_playing_art_${song.albumId}")
            .diskCacheKey("now_playing_art_${song.albumId}")
            .build()
    }

    var totalDragX by remember { mutableFloatStateOf(0f) }
    var totalDragY by remember { mutableFloatStateOf(0f) }
    var isDraggingArt by remember { mutableStateOf(false) }

    val visualOffsetX by animateFloatAsState(
        targetValue = if (isDraggingArt) (totalDragX * 0.35f).coerceIn(-90f, 90f) else 0f,
        animationSpec = tween(durationMillis = if (isDraggingArt) 0 else 180),
        label = "art_drag_x"
    )
    val visualOffsetY by animateFloatAsState(
        targetValue = if (isDraggingArt) (totalDragY.coerceAtLeast(0f) * 0.35f).coerceIn(0f, 90f) else 0f,
        animationSpec = tween(durationMillis = if (isDraggingArt) 0 else 180),
        label = "art_drag_y"
    )

    Box(
        modifier = modifier
            .graphicsLayer {
                translationX = visualOffsetX
                translationY = visualOffsetY
            }
            .shadow(
                elevation = 24.dp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = Color.Black.copy(alpha = 0.45f),
                spotColor = Color.Black.copy(alpha = 0.3f)
            )
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF1E2830))
            .pointerInput(song.id) {
                detectDragGestures(
                    onDragStart = {
                        isDraggingArt = true
                        totalDragX = 0f
                        totalDragY = 0f
                    },
                    onDragCancel = {
                        isDraggingArt = false
                        totalDragX = 0f
                        totalDragY = 0f
                    },
                    onDragEnd = {
                        val threshold = 70.dp.toPx()
                        if (abs(totalDragX) > abs(totalDragY)) {
                            if (totalDragX < -threshold) {
                                hapticFeedback.lightTap()
                                onSkipNext()
                            } else if (totalDragX > threshold) {
                                hapticFeedback.lightTap()
                                onSkipPrevious()
                            }
                        } else if (totalDragY > threshold * 1.5f) {
                            hapticFeedback.lightTap()
                            onDismiss()
                        }
                        isDraggingArt = false
                        totalDragX = 0f
                        totalDragY = 0f
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        totalDragX += dragAmount.x
                        totalDragY += dragAmount.y
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        // High fidelity placeholder background
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF1F4A57),
                            Color(0xFF11252C),
                            Color(0xFF0A1418)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.MusicNote,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.45f),
                modifier = Modifier.size(80.dp)
            )
        }

        // Album Art overlay
        AsyncImage(
            model = albumArtRequest,
            contentDescription = "${song.title} artwork",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
    }
}

// ══════════════════════════════════════════════
// 3. Song Metadata & Quick Action Row
// ══════════════════════════════════════════════

@Composable
private fun NowPlayingMetadataRow(
    song: Song,
    onFavoriteClick: () -> Unit,
    onAddToPlaylistClick: () -> Unit,
    hapticFeedback: com.suspended.music.ui.utils.HapticFeedbackManager,
    modifier: Modifier = Modifier
) {
    key(song.id) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Star Favorite Button
            IconButton(
                onClick = { hapticFeedback.lightTap(); onFavoriteClick() },
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = if (song.isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                    contentDescription = if (song.isFavorite) "Remove from favorites" else "Add to favorites",
                    tint = if (song.isFavorite) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.size(28.dp)
                )
            }

            // Song Title, Artist, Album
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontSize = 21.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        letterSpacing = (-0.2).sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = song.artist,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Normal,
                        color = Color.White.copy(alpha = 0.85f)
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
                if (song.album.isNotBlank() && song.album != song.title) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = song.album,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Normal,
                            color = Color.White.copy(alpha = 0.65f)
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Add to Playlist Button
            IconButton(
                onClick = { hapticFeedback.lightTap(); onAddToPlaylistClick() },
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.AddCircleOutline,
                    contentDescription = "Add to playlist",
                    tint = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

// ══════════════════════════════════════════════
// 4. Seekbar & Timestamps
// ══════════════════════════════════════════════

@Composable
private fun NowPlayingSeekBar(
    progress: () -> Float,
    currentPosition: Long,
    duration: Long,
    onSeekTo: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        var sliderPosition by remember { mutableFloatStateOf(0f) }
        var isDragging by remember { mutableStateOf(false) }
        var pendingSeekFraction by remember { mutableStateOf<Float?>(null) }

        LaunchedEffect(Unit) {
            snapshotFlow { progress() }.collect { p ->
                val pending = pendingSeekFraction
                if (pending != null && abs(p - pending) < 0.02f) {
                    pendingSeekFraction = null
                }
                if (!isDragging && pendingSeekFraction == null) {
                    sliderPosition = p
                }
            }
        }

        LaunchedEffect(pendingSeekFraction) {
            if (pendingSeekFraction != null) {
                kotlinx.coroutines.delay(1500L)
                pendingSeekFraction = null
            }
        }

        val heldProgress = pendingSeekFraction
        val displayProgress = when {
            isDragging -> sliderPosition
            heldProgress != null -> heldProgress
            else -> progress()
        }
        val displayedPositionMs = when {
            isDragging -> (sliderPosition * duration).toLong()
            heldProgress != null -> (heldProgress * duration).toLong()
            else -> currentPosition
        }

        // A real M3 Slider so the screen's primary control is focusable, exposes
        // progressBarRangeInfo to assistive tech, and meets the 48dp target.
        Slider(
            value = displayProgress.coerceIn(0f, 1f),
            onValueChange = { value ->
                isDragging = true
                pendingSeekFraction = null
                sliderPosition = value
            },
            onValueChangeFinished = {
                if (duration > 0) {
                    pendingSeekFraction = sliderPosition
                    onSeekTo((sliderPosition * duration).toLong())
                }
                isDragging = false
            },
            enabled = duration > 0,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .semantics { stateDescription = formatDuration(displayedPositionMs) },
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = Color.White,
                inactiveTrackColor = Color.White.copy(alpha = 0.28f),
                disabledThumbColor = Color.White.copy(alpha = 0.4f),
                disabledActiveTrackColor = Color.White.copy(alpha = 0.3f),
                disabledInactiveTrackColor = Color.White.copy(alpha = 0.16f)
            )
        )

        // Timestamps below seekbar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = formatDuration(displayedPositionMs),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White.copy(alpha = 0.72f)
                )
            )
            Text(
                text = formatDuration(duration),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White.copy(alpha = 0.72f)
                )
            )
        }
    }
}

// ══════════════════════════════════════════════
// 5. Playback Controls Row
// ══════════════════════════════════════════════

@Composable
private fun NowPlayingControlsRow(
    isPlaying: Boolean,
    shuffleEnabled: Boolean,
    repeatMode: Int,
    playButtonScale: Float,
    onTogglePlayPause: () -> Unit,
    onSkipPrevious: () -> Unit,
    onSkipNext: () -> Unit,
    onToggleShuffle: () -> Unit,
    onToggleRepeat: () -> Unit,
    hapticFeedback: com.suspended.music.ui.utils.HapticFeedbackManager,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // ── 1. Shuffle ──
        IconButton(
            onClick = { hapticFeedback.lightTap(); onToggleShuffle() },
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Shuffle,
                contentDescription = "Shuffle",
                tint = if (shuffleEnabled) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.55f),
                modifier = Modifier.size(24.dp)
            )
        }

        // ── 2. Previous (plain Apple control) ──
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = androidx.compose.material3.ripple(bounded = true, color = Color.White),
                    role = Role.Button,
                    onClick = { hapticFeedback.lightTap(); onSkipPrevious() }
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.SkipPrevious,
                contentDescription = "Previous",
                tint = Color.White,
                modifier = Modifier.size(34.dp)
            )
        }

        // ── 3. Play / Pause Centerpiece (Apple solid) ──
        Box(
            modifier = Modifier
                .size(76.dp)
                .graphicsLayer {
                    scaleX = playButtonScale
                    scaleY = playButtonScale
                }
                .shadow(
                    elevation = 12.dp,
                    shape = CircleShape,
                    ambientColor = Color.Black.copy(alpha = 0.4f),
                    spotColor = Color.Black.copy(alpha = 0.3f)
                )
                .clip(CircleShape)
                .background(Color.White)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = androidx.compose.material3.ripple(bounded = true),
                    role = Role.Button,
                    onClick = { hapticFeedback.mediumTap(); onTogglePlayPause() }
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = if (isPlaying) "Pause" else "Play",
                tint = Color.Black,
                modifier = Modifier.size(40.dp)
            )
        }

        // ── 4. Next (plain Apple control) ──
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = androidx.compose.material3.ripple(bounded = true, color = Color.White),
                    role = Role.Button,
                    onClick = { hapticFeedback.lightTap(); onSkipNext() }
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.SkipNext,
                contentDescription = "Next",
                tint = Color.White,
                modifier = Modifier.size(34.dp)
            )
        }

        // ── 5. Repeat ──
        IconButton(
            onClick = { hapticFeedback.lightTap(); onToggleRepeat() },
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                imageVector = when (repeatMode) {
                    Player.REPEAT_MODE_ONE -> Icons.Filled.RepeatOne
                    else -> Icons.Filled.Repeat
                },
                contentDescription = "Repeat",
                tint = when (repeatMode) {
                    Player.REPEAT_MODE_OFF -> Color.White.copy(alpha = 0.55f)
                    else -> MaterialTheme.colorScheme.primary
                },
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

// ══════════════════════════════════════════════
// 6. Dialogs & Helpers
// ══════════════════════════════════════════════

@Composable
private fun SleepTimerDialog(
    remainingMs: Long?,
    onDismiss: () -> Unit,
    onCancelTimer: () -> Unit,
    onSelect: (Int) -> Unit
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) { Text("Close") }
        },
        title = { Text("Sleep Timer", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column {
                if (remainingMs != null) {
                    Text(
                        text = "Sleeping in ${formatRemaining(remainingMs)} — volume will fade out gradually before pausing.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    androidx.compose.material3.TextButton(
                        onClick = onCancelTimer,
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) { Text("Cancel timer") }
                } else {
                    Text(
                        text = "Playback will gradually fade out at the end of the timer and then pause.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }

                val options = listOf(15, 30, 45, 60, 90)
                FlowGrid(
                    items = options,
                    onSelect = onSelect,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    )
}

@Composable
private fun FlowGrid(
    items: List<Int>,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            items.take(3).forEach { minutes ->
                androidx.compose.material3.FilledTonalButton(
                    onClick = { onSelect(minutes) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("$minutes min")
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            items.drop(3).forEach { minutes ->
                androidx.compose.material3.FilledTonalButton(
                    onClick = { onSelect(minutes) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("$minutes min")
                }
            }
        }
    }
}

private fun formatRemaining(ms: Long): String {
    val totalSeconds = ms / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) "%d:%02d:%02d".format(hours, minutes, seconds)
    else "%d:%02d".format(minutes, seconds)
}

private fun formatDuration(millis: Long): String {
    val totalSeconds = millis / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0)
        String.format(java.util.Locale.getDefault(), "%d:%02d:%02d", hours, minutes, seconds)
    else
        String.format(java.util.Locale.getDefault(), "%02d:%02d", minutes, seconds)
}
