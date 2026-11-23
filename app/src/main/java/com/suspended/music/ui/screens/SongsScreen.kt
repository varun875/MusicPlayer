package com.suspended.music.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.suspended.music.data.model.Song
import com.suspended.music.ui.components.AlphabetIndex
import com.suspended.music.ui.components.EmptyState
import com.suspended.music.ui.components.LibrarySectionHeader
import com.suspended.music.ui.components.PlaylistPickerDialog
import com.suspended.music.ui.components.SongInfoDialog
import com.suspended.music.ui.components.SongItem
import com.suspended.music.ui.components.SongOptionsSheet
import com.suspended.music.ui.utils.rememberAdaptiveLayoutInfo
import com.suspended.music.ui.viewmodel.MainViewModel
import com.suspended.music.ui.viewmodel.SongRow
import com.suspended.music.ui.viewmodel.SongsListState
import com.suspended.music.ui.viewmodel.SortOption
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongsScreen(
    viewModel: MainViewModel,
    onNavigateToSearch: () -> Unit,
    onNavigateToNowPlaying: () -> Unit,
    onNavigateToTagEditor: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val songsListState by viewModel.songsListState.collectAsStateWithLifecycle()
    val currentSong by viewModel.currentSong.collectAsStateWithLifecycle()
    val sortOption by viewModel.sortOption.collectAsStateWithLifecycle()
    val isScanning by viewModel.isScanning.collectAsStateWithLifecycle()
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    val adaptiveInfo = rememberAdaptiveLayoutInfo()

    val currentSongId = currentSong?.id

    var showSortMenu by remember { mutableStateOf(false) }
    var selectedSong by remember { mutableStateOf<Song?>(null) }
    var songPendingDelete by remember { mutableStateOf<Song?>(null) }
    var showInfoDialog by remember { mutableStateOf(false) }
    var showAddToPlaylistDialog by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState()
    val snackbarHostState = remember { SnackbarHostState() }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(
        state = rememberTopAppBarState()
    )

    val listState = rememberLazyListState()

    // Scroll back to the top whenever the user switches sort order.
    LaunchedEffect(sortOption) {
        if (listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 0) {
            listState.scrollToItem(0)
        }
    }

    val onEntryClick: (SongRow.Entry) -> Unit = remember(viewModel, onNavigateToNowPlaying, songsListState.sortedSongs) {
        { entry ->
            viewModel.playSongs(songsListState.sortedSongs, entry.songIndex)
            onNavigateToNowPlaying()
        }
    }
    val onMoreClick: (Song) -> Unit = remember {
        { song ->
            selectedSong = song
            scope.launch { sheetState.show() }
        }
    }
    val onFavoriteClick: (Song) -> Unit = remember(viewModel) {
        { song -> viewModel.toggleFavorite(song) }
    }

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        text = "Songs",
                        style = MaterialTheme.typography.displaySmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.5).sp
                        )
                    )
                },
                actions = {
                    IconButton(onClick = onNavigateToSearch) {
                        Icon(imageVector = Icons.Filled.Search, contentDescription = "Search")
                    }

                    Box {
                        IconButton(onClick = { showSortMenu = true }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Sort,
                                contentDescription = "Sort"
                            )
                        }

                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false }
                        ) {
                            SortOption.entries.forEach { option ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = option.label,
                                            color = if (sortOption == option) {
                                                MaterialTheme.colorScheme.primary
                                            } else {
                                                MaterialTheme.colorScheme.onSurface
                                            }
                                        )
                                    },
                                    onClick = {
                                        viewModel.updateSortOption(option)
                                        showSortMenu = false
                                    }
                                )
                            }
                        }
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        if (songsListState.rows.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                EmptyState(
                    icon = {
                        Icon(
                            imageVector = Icons.Filled.MusicNote,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    title = "No songs found",
                    subtitle = "Scan your device to find music",
                    action = {
                        Button(onClick = { viewModel.scanMedia() }, enabled = !isScanning) {
                            Text(if (isScanning) "Scanning…" else "Scan library")
                        }
                    }
                )
            }
        } else {
            val showAlphabetIndex = songsListState.letters.size > 1
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.TopCenter
            ) {
                Box(
                    modifier = if (adaptiveInfo.isTwoPane) Modifier.fillMaxWidth(0.78f)
                             else Modifier.fillMaxSize()
                ) {
                    SongsList(
                        state = songsListState,
                        listState = listState,
                        currentSongId = currentSongId,
                        reserveEndInset = showAlphabetIndex,
                        onEntryClick = onEntryClick,
                        onMoreClick = onMoreClick,
                        onFavoriteClick = onFavoriteClick
                    )
                }

                if (showAlphabetIndex) {
                    AlphabetIndex(
                        letters = songsListState.letters,
                        onLetterSelected = { letter ->
                            val target = songsListState.letterIndexMap[letter]
                            if (target != null) {
                                scope.launch { listState.scrollToItem(target) }
                            }
                        },
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(bottom = 100.dp)
                    )
                }
            }
        }

        if (isScanning) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
    }

    selectedSong?.let { song ->
        SongOptionsSheet(
            song = song,
            sheetState = sheetState,
            onDismiss = {
                scope.launch {
                    sheetState.hide()
                    if (!showAddToPlaylistDialog && !showInfoDialog) {
                        selectedSong = null
                    }
                }
            },
            onDelete = {
                songPendingDelete = song
                selectedSong = null
            },
            onAddToPlaylist = { showAddToPlaylistDialog = true },
            onEdit = {
                val targetId = song.id
                selectedSong = null
                onNavigateToTagEditor(targetId)
            },
            onShowInfo = { showInfoDialog = true }
        )
    }

    songPendingDelete?.let { song ->
        AlertDialog(
            onDismissRequest = { songPendingDelete = null },
            title = { Text("Delete song?") },
            text = { Text("Remove \"${song.title}\" from your library?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteSong(song)
                        songPendingDelete = null
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { songPendingDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showInfoDialog) {
        selectedSong?.let { song ->
            SongInfoDialog(
                song = song,
                onDismiss = {
                    showInfoDialog = false
                    if (!showAddToPlaylistDialog) {
                        selectedSong = null
                    }
                }
            )
        }
    }

    if (showAddToPlaylistDialog) {
        selectedSong?.let { song ->
            val editablePlaylists = playlists.filterNot { it.isSmartPlaylist }
            PlaylistPickerDialog(
                playlists = editablePlaylists,
                title = "Add '${song.title}' to playlist",
                emptyMessage = "Create a playlist first to add songs.",
                onDismiss = {
                    showAddToPlaylistDialog = false
                    selectedSong = null
                },
                onPlaylistSelected = { playlist ->
                    viewModel.addSongsToPlaylist(playlist.id, listOf(song.id))
                    showAddToPlaylistDialog = false
                    selectedSong = null
                    scope.launch {
                        snackbarHostState.showSnackbar("Added to ${playlist.name}")
                    }
                }
            )
        }
    }
}

@Composable
private fun SongsList(
    state: SongsListState,
    listState: LazyListState,
    currentSongId: Long?,
    reserveEndInset: Boolean,
    onEntryClick: (SongRow.Entry) -> Unit,
    onMoreClick: (Song) -> Unit,
    onFavoriteClick: (Song) -> Unit
) {
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            end = if (reserveEndInset) 24.dp else 0.dp,
            bottom = 100.dp
        )
    ) {
        items(
            items = state.rows,
            key = { row ->
                when (row) {
                    is SongRow.Header -> "h_${row.letter}"
                    is SongRow.Entry -> "s_${row.song.id}"
                }
            },
            contentType = { row ->
                when (row) {
                    is SongRow.Header -> "header"
                    is SongRow.Entry -> "song"
                }
            }
        ) { row ->
            when (row) {
                is SongRow.Header -> LibrarySectionHeader(letter = row.letter.toString())
                is SongRow.Entry -> SongItem(
                    song = row.song,
                    isPlaying = currentSongId == row.song.id,
                    showArtwork = true,
                    onClick = { onEntryClick(row) },
                    onMoreClick = onMoreClick,
                    onFavoriteClick = onFavoriteClick
                )
            }
        }
    }
}
