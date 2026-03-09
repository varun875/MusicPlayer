package com.suspended.music.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed as gridItemsIndexed
import androidx.compose.foundation.lazy.items as listItems // ✅ Changed to items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Height
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.suspended.music.data.model.Song
import com.suspended.music.ui.components.EmptyState
import com.suspended.music.ui.components.SongInfoDialog
import com.suspended.music.ui.components.AddToPlaylistDialog
import com.suspended.music.ui.components.SongItem
import com.suspended.music.ui.components.SongOptionsSheet
import com.suspended.music.ui.viewmodel.MainViewModel
import com.suspended.music.ui.viewmodel.SortOption
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongsScreen(
    viewModel: MainViewModel,
    onNavigateToSearch: () -> Unit,
    onNavigateToNowPlaying: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sortedSongs by viewModel.sortedSongs.collectAsStateWithLifecycle()
    val currentSong by viewModel.currentSong.collectAsStateWithLifecycle()
    val appSettings by viewModel.appSettings.collectAsStateWithLifecycle()
    val sortOption by viewModel.sortOption.collectAsStateWithLifecycle()
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()

    // ❌ REMOVED: SideEffect logger runs on EVERY recomposition (scrolling = lag)
    // If you need debugging, use Android Studio Profiler instead.
    
    val libraryConfig = appSettings.library
    val currentSongId = currentSong?.id
    
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
                    
                    IconButton(onClick = {
                        val nextMode = if (libraryConfig.layoutMode == "GRID") "LIST" else "GRID"
                        viewModel.setLibraryLayoutMode(nextMode)
                    }) {
                        Icon(
                            imageVector = if (libraryConfig.layoutMode == "GRID") Icons.AutoMirrored.Filled.List else Icons.Filled.GridView,
                            contentDescription = "Toggle Layout"
                        )
                    }

                    IconButton(onClick = {
                        val nextDensity = if (libraryConfig.listDensity == "NORMAL") "COMPACT" else "NORMAL"
                        viewModel.setLibraryListDensity(nextDensity)
                    }) {
                        Icon(
                            imageVector = Icons.Filled.Height,
                            contentDescription = "Toggle Density",
                            tint = if (libraryConfig.listDensity == "COMPACT") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }

                    if (libraryConfig.layoutMode == "GRID") {
                        IconButton(onClick = {
                            val nextSize = if (libraryConfig.gridSize >= 4) 2 else libraryConfig.gridSize + 1
                            viewModel.setLibraryGridSize(nextSize)
                        }) {
                            Text(
                                text = libraryConfig.gridSize.toString(),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        if (sortedSongs.isEmpty()) {
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
            if (libraryConfig.layoutMode == "GRID") {
                SongsInternalGrid(
                    songs = sortedSongs,
                    gridSize = libraryConfig.gridSize,
                    onSongClick = { song -> viewModel.playSong(song) }, // ✅ Pass Song directly
                    contentPadding = paddingValues
                )
            } else {
                SongsInternalList(
                    songs = sortedSongs,
                    currentSongId = currentSongId,
                    isCompact = libraryConfig.listDensity == "COMPACT",
                    onSongClick = { song -> viewModel.playSong(song) }, // ✅ Pass Song directly
                    onMoreClick = { song ->
                        selectedSong = song
                        scope.launch { sheetState.show() }
                    },
                    onFavoriteClick = { song -> viewModel.toggleFavorite(song) },
                    contentPadding = paddingValues
                )
            }
        }
    }
    
    selectedSong?.let { song ->
        SongOptionsSheet(
            song = song,
            sheetState = sheetState,
            onDismiss = {
                scope.launch {
                    sheetState.hide()
                    selectedSong = null
                }
            },
            onDelete = { viewModel.deleteSong(song) },
            onAddToPlaylist = { showAddToPlaylistDialog = true },
            onEdit = { /* TODO */ },
            onShowInfo = { showInfoDialog = true }
        )
    }

    if (showAddToPlaylistDialog && selectedSong != null) {
        AddToPlaylistDialog(
            playlists = playlists,
            onDismissRequest = { showAddToPlaylistDialog = false },
            onPlaylistSelected = { playlist ->
                viewModel.addSongsToPlaylist(playlist.id, listOf(selectedSong!!.id))
                showAddToPlaylistDialog = false
            }
        )
    }
    
    if (showInfoDialog && selectedSong != null) {
        SongInfoDialog(
            song = selectedSong!!,
            onDismiss = { showInfoDialog = false }
        )
    }
}

@Composable
private fun SongsInternalList(
    songs: List<Song>,
    currentSongId: Long?,
    isCompact: Boolean,
    onSongClick: (Song) -> Unit, // ✅ Changed to Song
    onMoreClick: (Song) -> Unit,
    onFavoriteClick: (Song) -> Unit,
    contentPadding: PaddingValues
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 100.dp),
        contentPadding = PaddingValues(
            top = contentPadding.calculateTopPadding(),
            bottom = contentPadding.calculateBottomPadding()
        )
    ) {
        listItems(
            items = songs,
            key = { song -> song.id }, // ✅ Cleaner key
            contentType = { "song_item" } // ✅ Constant content type
        ) { song ->
            SongItem(
                song = song,
                isPlaying = currentSongId == song.id,
                isCompact = isCompact,
                onClick = { onSongClick(song) },
                onMoreClick = { onMoreClick(song) },
                onFavoriteClick = { onFavoriteClick(song) }
            )
        }
    }
}

@Composable
private fun SongsInternalGrid(
    songs: List<Song>,
    gridSize: Int,
    onSongClick: (Song) -> Unit,
    contentPadding: PaddingValues
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(gridSize),
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 100.dp),
        contentPadding = PaddingValues(
            top = contentPadding.calculateTopPadding(),
            bottom = contentPadding.calculateBottomPadding()
        )
    ) {
        gridItemsIndexed(
            items = songs,
            key = { _, song -> song.id },
            contentType = { _, _ -> "song_card" }
        ) { index, song ->
            AnimatedItem(index = index) {
                com.suspended.music.ui.components.SongCard(
                    song = song,
                    onClick = { onSongClick(song) }
                )
            }
        }
    }
}

@Composable
private fun AnimatedItem(
    index: Int,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val animatable = remember { Animatable(0.5f) }
    var hasAnimated by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        if (!hasAnimated) {
            val delay = if (index < 12) index * 30L else 0L
            if (delay > 0) kotlinx.coroutines.delay(delay)
            
            animatable.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = 300,
                    easing = FastOutSlowInEasing
                )
            )
            hasAnimated = true
        } else {
            animatable.snapTo(1f)
        }
    }

    Box(
        modifier = modifier.graphicsLayer {
            alpha = animatable.value
            translationY = 30f * (1f - animatable.value)
        }
    ) {
        content()
    }
}