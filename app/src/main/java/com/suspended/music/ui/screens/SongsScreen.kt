package com.suspended.music.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Search
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
import com.suspended.music.ui.viewmodel.SortOption
import kotlinx.coroutines.launch

private sealed interface SongRow {
    data class Header(val letter: Char) : SongRow
    data class Entry(val song: Song) : SongRow
}

private fun sectionKey(song: Song, sort: SortOption): Char? {
    val label = when (sort) {
        SortOption.TITLE -> song.title
        SortOption.ARTIST -> song.artist
        SortOption.ALBUM -> song.album
        else -> return null
    }
    return label.firstOrNull()?.uppercaseChar()?.takeIf { it in 'A'..'Z' } ?: '#'
}

private fun sortSongs(songs: List<Song>, sort: SortOption): List<Song> {
    val byText = String.CASE_INSENSITIVE_ORDER
    return when (sort) {
        SortOption.TITLE -> songs.sortedWith(compareBy(byText) { it.title })
        SortOption.ARTIST -> songs.sortedWith(compareBy(byText) { it.artist })
        SortOption.ALBUM -> songs.sortedWith(compareBy(byText) { it.album })
        SortOption.DURATION -> songs.sortedBy { it.duration }
        SortOption.DATE_ADDED -> songs.sortedByDescending { it.dateAdded }
        SortOption.DATE_MODIFIED -> songs.sortedByDescending { it.dateModified }
        SortOption.PLAY_COUNT -> songs.sortedByDescending { it.playCount }
        SortOption.SIZE -> songs.sortedByDescending { it.size }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongsScreen(
    viewModel: MainViewModel,
    onNavigateToSearch: () -> Unit,
    onNavigateToNowPlaying: () -> Unit,
    onNavigateToTagEditor: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val allSongs by viewModel.songs.collectAsStateWithLifecycle()
    val currentSong by viewModel.currentSong.collectAsStateWithLifecycle()
    val sortOption by viewModel.sortOption.collectAsStateWithLifecycle()
    val isScanning by viewModel.isScanning.collectAsStateWithLifecycle()
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    val adaptiveInfo = rememberAdaptiveLayoutInfo()

    val currentSongId = currentSong?.id

    var showSortMenu by remember { mutableStateOf(false) }
    var selectedSong by remember { mutableStateOf<Song?>(null) }
    var showInfoDialog by remember { mutableStateOf(false) }
    var showAddToPlaylistDialog by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState()
    val snackbarHostState = remember { SnackbarHostState() }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(
        state = rememberTopAppBarState()
    )

    val sortedSongs = remember(allSongs, sortOption) { sortSongs(allSongs, sortOption) }

    // Sectioned rows: a bold letter header before each group. Numeric sorts get
    // no headers, since a leading letter wouldn't mean anything there.
    val rows: List<SongRow> = remember(sortedSongs, sortOption) {
        buildList {
            var lastLetter: Char? = null
            sortedSongs.forEach { song ->
                val key = sectionKey(song, sortOption)
                if (key != null && key != lastLetter) {
                    add(SongRow.Header(key))
                    lastLetter = key
                }
                add(SongRow.Entry(song))
            }
        }
    }
    val letters = remember(rows) {
        rows.filterIsInstance<SongRow.Header>().map { it.letter }
    }
    val listState = rememberLazyListState()

    val onSongClick: (Song) -> Unit = remember(viewModel, onNavigateToNowPlaying) {
        { song ->
            viewModel.playSong(song)
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
        if (allSongs.isEmpty()) {
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
                        rows = rows,
                        listState = listState,
                        currentSongId = currentSongId,
                        onSongClick = onSongClick,
                        onMoreClick = onMoreClick,
                        onFavoriteClick = onFavoriteClick
                    )
                }

                if (letters.size > 1) {
                    AlphabetIndex(
                        letters = letters,
                        onLetterSelected = { letter ->
                            val target = rows.indexOfFirst {
                                it is SongRow.Header && it.letter == letter
                            }
                            if (target >= 0) {
                                scope.launch { listState.animateScrollToItem(target) }
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
            onDelete = { viewModel.deleteSong(song) },
            onAddToPlaylist = { showAddToPlaylistDialog = true },
            onEdit = { onNavigateToTagEditor(song.id) },
            onShowInfo = { showInfoDialog = true }
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SongsList(
    rows: List<SongRow>,
    listState: LazyListState,
    currentSongId: Long?,
    onSongClick: (Song) -> Unit,
    onMoreClick: (Song) -> Unit,
    onFavoriteClick: (Song) -> Unit
) {
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 100.dp)
    ) {
        rows.forEach { row ->
            when (row) {
                is SongRow.Header -> stickyHeader(
                    key = "h_${row.letter}",
                    contentType = "header"
                ) {
                    LibrarySectionHeader(letter = row.letter.toString())
                }

                is SongRow.Entry -> item(
                    key = "s_${row.song.id}",
                    contentType = "song"
                ) {
                    SongItem(
                        song = row.song,
                        isPlaying = currentSongId == row.song.id,
                        showArtwork = true,
                        onClick = onSongClick,
                        onMoreClick = onMoreClick,
                        onFavoriteClick = onFavoriteClick
                    )
                }
            }
        }
    }
}
