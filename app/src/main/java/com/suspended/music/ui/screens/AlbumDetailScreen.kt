package com.suspended.music.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.suspended.music.data.model.Album
import com.suspended.music.data.model.Song
import com.suspended.music.ui.components.PlaylistPickerDialog
import com.suspended.music.ui.components.SongItem
import com.suspended.music.ui.components.SongOptionsSheet
import com.suspended.music.ui.theme.AlbumArtShape
import com.suspended.music.ui.viewmodel.MainViewModel
import kotlinx.coroutines.launch

/** Size the hero art is decoded at — blurred backdrop + small card don't need full res. */
private const val HERO_ART_SIZE = 512

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumDetailScreen(
    albumId: Long,
    viewModel: MainViewModel,
    onBackClick: () -> Unit,
    onNavigateToNowPlaying: () -> Unit,
    onNavigateToTagEditor: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    // Genuine back navigation is handled by NavDisplay's predictive-back /
    // onBack callback; a local BackHandler would double-pop.

    val album by viewModel.getAlbum(albumId).collectAsStateWithLifecycle(initialValue = null)
    val songs by viewModel.getAlbumSongs(albumId).collectAsStateWithLifecycle(initialValue = emptyList())
    val currentSong by viewModel.currentSong.collectAsStateWithLifecycle()
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()

    var songForOptions by remember { mutableStateOf<Song?>(null) }
    var songForPlaylist by remember { mutableStateOf<Song?>(null) }
    val optionsSheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text(album?.title ?: "") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { padding ->
        val currentAlbum = album
        if (currentAlbum == null) {
            AlbumDetailLoading(padding)
        } else {
            AlbumDetailContent(
                album = currentAlbum,
                songs = songs,
                padding = padding,
                onPlayAlbum = {
                    viewModel.playAlbum(currentAlbum.id)
                    onNavigateToNowPlaying()
                },
                onPlayFrom = { index ->
                    viewModel.playAlbum(currentAlbum.id, index)
                    onNavigateToNowPlaying()
                },
                onToggleFavorite = { viewModel.toggleFavorite(it) },
                onMoreClick = { song ->
                    songForOptions = song
                    scope.launch { optionsSheetState.show() }
                },
                currentSongId = currentSong?.id,
                isPlaying = isPlaying
            )
        }
    }

    songForOptions?.let { song ->
        SongOptionsSheet(
            song = song,
            sheetState = optionsSheetState,
            onDismiss = { songForOptions = null },
            onDelete = {
                viewModel.deleteSong(song)
                songForOptions = null
            },
            onAddToPlaylist = {
                songForPlaylist = song
                songForOptions = null
            },
            onEdit = {
                songForOptions = null
                onNavigateToTagEditor(song.id)
            },
            onShowInfo = { songForOptions = null }
        )
    }

    songForPlaylist?.let { song ->
        val playlists by viewModel.playlists.collectAsStateWithLifecycle()
        val editablePlaylists = remember(playlists) { playlists.filterNot { it.isSmartPlaylist } }
        PlaylistPickerDialog(
            playlists = editablePlaylists,
            title = "Add '${song.title}' to playlist",
            emptyMessage = "Create a playlist first.",
            onDismiss = { songForPlaylist = null },
            onPlaylistSelected = { playlist ->
                viewModel.addSongsToPlaylist(playlist.id, listOf(song.id))
                songForPlaylist = null
            }
        )
    }
}

// ── Loaded content ─────────────────────────────────────────────────────

@Composable
private fun AlbumDetailContent(
    album: Album,
    songs: List<Song>,
    padding: PaddingValues,
    onPlayAlbum: () -> Unit,
    onPlayFrom: (Int) -> Unit,
    onToggleFavorite: (Song) -> Unit,
    onMoreClick: (Song) -> Unit,
    currentSongId: Long?,
    isPlaying: Boolean
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 100.dp)
    ) {
        item(key = "header") {
            AlbumHeroHeader(
                album = album,
                songCount = songs.size,
                topInset = padding.calculateTopPadding(),
                onPlay = onPlayAlbum
            )
        }

        itemsIndexed(items = songs, key = { _, song -> song.id }) { index, song ->
            SongItem(
                song = song,
                index = index,
                onClick = { onPlayFrom(index) },
                isPlaying = song.id == currentSongId && isPlaying,
                onMoreClick = { onMoreClick(it) },
                onFavoriteClick = { onToggleFavorite(it) }
            )
        }
    }
}

/**
 * Full-bleed hero: the album art, blurred and darkened, fills the top of the
 * screen behind a solid art card and left-aligned metadata. The blur uses a
 * small decoded bitmap (see [HERO_ART_SIZE]) so the GPU cost stays trivial.
 */
@Composable
private fun AlbumHeroHeader(
    album: Album,
    songCount: Int,
    topInset: Dp,
    onPlay: () -> Unit
) {
    val context = LocalContext.current
    val artModel = remember(album.albumArtUri) {
        ImageRequest.Builder(context)
            .data(album.albumArtUri)
            .size(HERO_ART_SIZE)
            .crossfade(true)
            .build()
    }

    Box(modifier = Modifier.fillMaxWidth()) {
        // Blurred, darkened backdrop — same source, so it's a cache hit.
        AsyncImage(
            model = artModel,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .height(320.dp)
                .blur(28.dp)
                .graphicsLayer { alpha = 0.9f }
        )
        // Scrim so the white text stays legible over any artwork.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(320.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Black.copy(alpha = 0.15f), Color.Black.copy(alpha = 0.6f))
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = topInset + 56.dp, bottom = 20.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(180.dp)
                    .clip(AlbumArtShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                AsyncImage(
                    model = artModel,
                    contentDescription = "Album art for ${album.title}",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = album.title,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.5).sp
                ),
                color = Color.White,
                maxLines = 2
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = album.artist,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White.copy(alpha = 0.85f),
                maxLines = 1
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = albumMetaLine(album.year, songCount),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.65f)
            )

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = onPlay,
                modifier = Modifier.widthIn(max = 320.dp)
            ) {
                Icon(Icons.Filled.PlayArrow, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Play Album")
            }
        }
    }
}

private fun albumMetaLine(year: Int, songCount: Int): String = buildString {
    if (year > 0) append(year).append("  •  ")
    append(songCount).append(if (songCount == 1) " song" else " songs")
}

// ── Loading state ──────────────────────────────────────────────────────

@Composable
private fun AlbumDetailLoading(padding: PaddingValues) {
    // One slow pulse — a shimmer without the noise of a full skeleton grid.
    val pulse = rememberInfiniteTransition(label = "albumLoading").animateFloat(
        initialValue = 0.45f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(tween(1100), RepeatMode.Reverse),
        label = "pulse"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(180.dp)
                    .graphicsLayer { alpha = pulse.value }
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )
            Spacer(modifier = Modifier.height(24.dp))
            CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 2.5.dp)
        }
    }
}
