package com.suspended.music.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Height
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import com.suspended.music.BuildConfig
import com.suspended.music.data.model.Song
import com.suspended.music.ui.components.EmptyState
import com.suspended.music.ui.components.PlaylistPickerDialog
import com.suspended.music.ui.components.SongInfoDialog
import com.suspended.music.ui.components.SongItem
import com.suspended.music.ui.components.SongOptionsSheet
import com.suspended.music.ui.viewmodel.MainViewModel
import com.suspended.music.ui.viewmodel.SortOption
import com.suspended.music.util.PerfLogger
import kotlinx.coroutines.launch

private const val ENABLE_SONGS_RECOMPOSITION_TRACE = false

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongsScreen(
    viewModel: MainViewModel,
    onNavigateToSearch: () -> Unit,
    onNavigateToNowPlaying: () -> Unit,
    onNavigateToTagEditor: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    SongsRecomposeGuard(tag = "SongsScreen")

    val pagedSongs = viewModel.pagedSortedSongs.collectAsLazyPagingItems()
    val currentSong by viewModel.currentSong.collectAsStateWithLifecycle()
    val appSettings by viewModel.appSettings.collectAsStateWithLifecycle()
    val sortOption by viewModel.sortOption.collectAsStateWithLifecycle()
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // ❌ REMOVED: SideEffect logger runs on EVERY recomposition (scrolling = lag)
    // If you need debugging, use Android Studio Profiler instead.
    
    val libraryConfig by remember(appSettings.library) { derivedStateOf { appSettings.library } }
    val currentSongId by remember(currentSong?.id) { derivedStateOf { currentSong?.id } }
    
    var showSortMenu by remember { mutableStateOf(false) }
    var selectedSong by remember { mutableStateOf<Song?>(null) }
    var showInfoDialog by remember { mutableStateOf(false) }
    var showAddToPlaylistDialog by remember { mutableStateOf(false) }
    
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState()
    
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    
    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text(text = "Songs") },
                actions = {
                    IconButton(onClick = onNavigateToSearch) {
                        Icon(imageVector = Icons.Filled.Search, contentDescription = "Search")
                    }
                    
                    Box {
                        IconButton(onClick = { showSortMenu = true }) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.Sort, contentDescription = "Sort")
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
                                            color = if (sortOption == option) 
                                                MaterialTheme.colorScheme.primary 
                                            else MaterialTheme.colorScheme.onSurface
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
                    
                    // Layout toggles removed to enforce heavily optimized pure list view
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        val isInitialLoading = pagedSongs.loadState.refresh is LoadState.Loading
        val isEmpty = pagedSongs.loadState.refresh is LoadState.NotLoading && pagedSongs.itemCount == 0

        if (isInitialLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 0.dp)
                )
            }
        } else if (isEmpty) {
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
                modifier = Modifier.padding(paddingValues)
            )
        } else {
            SongsRecomposeGuard(tag = "SongsList")
            SongsInternalList(
                songs = pagedSongs,
                currentSongId = currentSongId,
                isCompact = false, // High performance standard height list
                onSongClick = { song ->
                    viewModel.playSong(song)
                    onNavigateToNowPlaying()
                },
                onMoreClick = { song ->
                    selectedSong = song
                    scope.launch { sheetState.show() }
                },
                onFavoriteClick = { song -> viewModel.toggleFavorite(song) },
                contentPadding = paddingValues
            )
        }
    }
    
    selectedSong?.let { song ->
        SongOptionsSheet(
            song = song,
            sheetState = sheetState,
            onDismiss = {
                scope.launch {
                    sheetState.hide()
                }
                if (!showAddToPlaylistDialog && !showInfoDialog) {
                    selectedSong = null
                }
            },
            onDelete = { viewModel.deleteSong(song) },
            onAddToPlaylist = { showAddToPlaylistDialog = true },
            onEdit = { onNavigateToTagEditor(song.id) },
            onShowInfo = { showInfoDialog = true }
        )
    }
    
    if (showInfoDialog && selectedSong != null) {
        SongInfoDialog(
            song = selectedSong!!,
            onDismiss = {
                showInfoDialog = false
                if (!showAddToPlaylistDialog) {
                    selectedSong = null
                }
            }
        )
    }

    if (showAddToPlaylistDialog && selectedSong != null) {
        val editablePlaylists = playlists.filterNot { it.isSmartPlaylist }
        PlaylistPickerDialog(
            playlists = editablePlaylists,
            title = "Add '${selectedSong!!.title}' to playlist",
            emptyMessage = "Create a playlist first to add songs.",
            onDismiss = {
                showAddToPlaylistDialog = false
                selectedSong = null
            },
            onPlaylistSelected = { playlist ->
                viewModel.addSongsToPlaylist(playlist.id, listOf(selectedSong!!.id))
                showAddToPlaylistDialog = false
                selectedSong = null
                Toast.makeText(context, "Added to ${playlist.name}", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

@Composable
private fun SongsRecomposeGuard(tag: String) {
    if (!BuildConfig.DEBUG || !ENABLE_SONGS_RECOMPOSITION_TRACE) return

    val count = remember { mutableIntStateOf(0) }
    SideEffect {
        count.intValue += 1
        if (count.intValue % 40 == 0) {
            PerfLogger.logEvent("Recompose:$tag", "count=${count.intValue}")
        }
    }
}

@Composable
private fun SongsInternalList(
    songs: LazyPagingItems<Song>,
    currentSongId: Long?,
    isCompact: Boolean,
    onSongClick: (Song) -> Unit,
    onMoreClick: (Song) -> Unit,
    onFavoriteClick: (Song) -> Unit,
    contentPadding: PaddingValues
) {
    val listThumbPx = if (isCompact) 104 else 128

    LazyColumn(
        modifier = Modifier
            .fillMaxSize(),
        contentPadding = PaddingValues(
            top = contentPadding.calculateTopPadding(),
            bottom = contentPadding.calculateBottomPadding() + 100.dp
        )
    ) {
        items(
            count = songs.itemCount,
            // Removing peek() key solves Paging 3 Compose lagging issues natively
            contentType = { _ -> "song_item" }
        ) { index ->
            val song = songs[index] ?: return@items
            SongItem(
                song = song,
                isPlaying = currentSongId == song.id,
                isCompact = isCompact,
                thumbnailSizePx = listThumbPx,
                onClick = { onSongClick(song) },
                onMoreClick = { onMoreClick(song) },
                onFavoriteClick = { onFavoriteClick(song) }
            )
        }
    }
}
