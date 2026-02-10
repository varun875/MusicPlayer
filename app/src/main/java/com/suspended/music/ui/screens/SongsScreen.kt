package com.suspended.music.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Height
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.suspended.music.data.model.Song
import com.suspended.music.ui.components.EmptyState
import com.suspended.music.ui.components.SongInfoDialog
import com.suspended.music.ui.components.SongItem
import com.suspended.music.ui.components.SongOptionsSheet
import com.suspended.music.ui.viewmodel.MainViewModel
import kotlinx.coroutines.launch

enum class SortOption(val label: String) {
    TITLE("Title"),
    ARTIST("Artist"),
    ALBUM("Album"),
    DATE_ADDED("Date Added"),
    DURATION("Duration")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongsScreen(
    viewModel: MainViewModel,
    onNavigateToSearch: () -> Unit,
    onNavigateToNowPlaying: () -> Unit,
    modifier: Modifier = Modifier
) {
    val songs by viewModel.songs.collectAsState()
    val currentSong by viewModel.currentSong.collectAsState()
    val appSettings by viewModel.appSettings.collectAsState()
    val libraryConfig = appSettings.library
    
    var showSortMenu by remember { mutableStateOf(false) }
    var sortOption by remember { mutableStateOf(SortOption.TITLE) }
    var selectedSong by remember { mutableStateOf<Song?>(null) }
    var showInfoDialog by remember { mutableStateOf(false) }
    
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState()
    
    val sortedSongs = remember(songs, sortOption) {
        when (sortOption) {
            SortOption.TITLE -> songs.sortedBy { it.title.lowercase() }
            SortOption.ARTIST -> songs.sortedBy { it.artist.lowercase() }
            SortOption.ALBUM -> songs.sortedBy { it.album.lowercase() }
            SortOption.DATE_ADDED -> songs.sortedByDescending { it.dateAdded }
            SortOption.DURATION -> songs.sortedByDescending { it.duration }
        }
    }
    
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(
        state = rememberTopAppBarState()
    )
    
    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(text = "Songs")
                },
                actions = {
                    IconButton(onClick = onNavigateToSearch) {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = "Search"
                        )
                    }
                    
                    Box {
                        IconButton(onClick = { showSortMenu = true }) {
                            Icon(
                                imageVector = Icons.Filled.Sort,
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
                                            color = if (sortOption == option) 
                                                MaterialTheme.colorScheme.primary 
                                            else MaterialTheme.colorScheme.onSurface
                                        )
                                    },
                                    onClick = {
                                        sortOption = option
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
                            imageVector = if (libraryConfig.layoutMode == "GRID") Icons.Filled.List else Icons.Filled.GridView,
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
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        if (songs.isEmpty()) {
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
            LazyVerticalGrid(
                columns = if (libraryConfig.layoutMode == "GRID") 
                    GridCells.Fixed(libraryConfig.gridSize) 
                else 
                    GridCells.Fixed(1),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(bottom = 100.dp)
            ) {
                itemsIndexed(
                    items = sortedSongs,
                    key = { _, song -> song.id }
                ) { index, song ->
                    if (libraryConfig.layoutMode == "GRID") {
                        com.suspended.music.ui.components.SongCard(
                            song = song,
                            onClick = { viewModel.playSongs(sortedSongs, index) }
                        )
                    } else {
                        SongItem(
                            song = song,
                            isPlaying = currentSong?.id == song.id,
                            isCompact = libraryConfig.listDensity == "COMPACT",
                            onClick = { viewModel.playSongs(sortedSongs, index) },
                            onMoreClick = {
                                selectedSong = song
                                scope.launch { sheetState.show() }
                            },
                            onFavoriteClick = { viewModel.toggleFavorite(song) },
                            modifier = Modifier.animateItem()
                        )
                    }
                }
            }
        }
    }
    
    // Song options bottom sheet
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
            onAddToPlaylist = { /* TODO: Navigate to playlist selector */ },
            onEdit = { /* TODO: Navigate to tag editor */ },
            onShowInfo = { showInfoDialog = true }
        )
    }
    
    // Song info dialog
    if (showInfoDialog && selectedSong != null) {
        SongInfoDialog(
            song = selectedSong!!,
            onDismiss = { showInfoDialog = false }
        )
    }
}
