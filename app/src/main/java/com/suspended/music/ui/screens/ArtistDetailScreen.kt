package com.suspended.music.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.suspended.music.data.model.Song
import com.suspended.music.ui.components.PlaylistPickerDialog
import com.suspended.music.ui.components.SongItem
import com.suspended.music.ui.components.SongOptionsSheet
import com.suspended.music.ui.viewmodel.MainViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArtistDetailScreen(
    artistId: Long,
    viewModel: MainViewModel,
    onBackClick: () -> Unit,
    onNavigateToNowPlaying: () -> Unit,
    onNavigateToTagEditor: (Long) -> Unit = {},
    modifier: Modifier = Modifier
) {
    // Genuine back navigation is handled by NavDisplay's predictive-back /
    // onBack callback; a local BackHandler would double-pop.

    val allArtists by viewModel.artists.collectAsStateWithLifecycle()
    val artist = allArtists.find { it.id == artistId }

    val artistSongs by remember(artist?.name) { viewModel.getSongsByArtist(artist?.name ?: "") }
        .collectAsStateWithLifecycle(initialValue = emptyList())

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
                title = { Text(artist?.name ?: "Artist") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {
            itemsIndexed(
                items = artistSongs,
                key = { _, song -> song.id }
            ) { index, song ->
                SongItem(
                    song = song,
                    index = index,
                    isPlaying = song.id == currentSong?.id && isPlaying,
                    onClick = {
                        viewModel.playSongs(artistSongs, index)
                        onNavigateToNowPlaying()
                    },
                    onMoreClick = {
                        songForOptions = song
                        scope.launch { optionsSheetState.show() }
                    },
                    onFavoriteClick = { viewModel.toggleFavorite(it) }
                )
            }
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