package com.suspended.music.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.suspended.music.data.model.Song
import com.suspended.music.ui.components.EmptyState
import com.suspended.music.ui.components.SearchFilters
import com.suspended.music.ui.components.SearchFiltersSheet
import com.suspended.music.ui.components.SongInfoDialog
import com.suspended.music.ui.components.SongItem
import com.suspended.music.ui.components.SongOptionsSheet
import com.suspended.music.ui.viewmodel.MainViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    viewModel: MainViewModel,
    onBackClick: () -> Unit,
    onNavigateToNowPlaying: () -> Unit,
    modifier: Modifier = Modifier
) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val searchFilters by viewModel.searchFilters.collectAsState()
    val currentSong by viewModel.currentSong.collectAsState()

    // Filter sheet data
    val artists by viewModel.availableArtists.collectAsState()
    val albums by viewModel.availableAlbums.collectAsState()
    val genres by viewModel.availableGenres.collectAsState()

    val scope = rememberCoroutineScope()
    val filterSheetState = rememberModalBottomSheetState()
    var showFilters by remember { mutableStateOf(false) }

    val optionsSheetState = rememberModalBottomSheetState()
    var selectedSong by remember { mutableStateOf<Song?>(null) }
    var showInfoDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        topBar = {
            SearchBar(
                query = searchQuery,
                onQueryChange = { viewModel.updateSearchQuery(it) },
                onSearch = { /* Search is reactive via Flow */ },
                active = false,
                onActiveChange = { },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Search songs, artists, albums...") },
                leadingIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = {
                            viewModel.updateSearchQuery("")
                            viewModel.updateSearchFilters(null)
                        }) {
                            Icon(Icons.Filled.Clear, contentDescription = "Clear")
                        }
                    } else {
                        IconButton(onClick = { showFilters = true }) {
                            Icon(
                                Icons.Filled.FilterList,
                                contentDescription = "Filters",
                                tint = if (searchFilters != null) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                colors = SearchBarDefaults.colors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) { }
        }
    ) { padding ->
        if (searchResults.isEmpty() && searchQuery.isNotEmpty()) {
            EmptyState(
                icon = {
                    Icon(
                        imageVector = Icons.Filled.SearchOff,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                title = "No results found",
                subtitle = "Try a different search term or adjust filters",
                modifier = Modifier.padding(padding)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(bottom = 100.dp)
            ) {
                // Results count header
                item {
                    if (searchQuery.isNotEmpty() || searchFilters != null) {
                        Text(
                            text = "${searchResults.size} results",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                }

                itemsIndexed(
                    items = searchResults,
                    key = { _, song -> song.id }
                ) { index, song ->
                    SongItem(
                        song = song,
                        isPlaying = currentSong?.id == song.id,
                        onClick = {
                            viewModel.playSongs(searchResults, index)
                            onNavigateToNowPlaying()
                        },
                        onMoreClick = {
                            selectedSong = song
                            scope.launch { optionsSheetState.show() }
                        },
                        onFavoriteClick = { viewModel.toggleFavorite(song) }
                    )
                }
            }
        }
    }

    // Filters bottom sheet
    if (showFilters) {
        SearchFiltersSheet(
            currentFilters = searchFilters ?: SearchFilters(),
            availableArtists = artists,
            availableAlbums = albums,
            availableGenres = genres,
            sheetState = filterSheetState,
            onDismiss = { showFilters = false },
            onApplyFilters = { filters ->
                viewModel.updateSearchFilters(filters)
                showFilters = false
            }
        )
    }

    selectedSong?.let { song ->
        SongOptionsSheet(
            song = song,
            sheetState = optionsSheetState,
            onDismiss = {
                scope.launch {
                    optionsSheetState.hide()
                    selectedSong = null
                }
            },
            onDelete = { viewModel.deleteSong(song) },
            onAddToPlaylist = { /* TODO */ },
            onEdit = { /* TODO */ },
            onShowInfo = { showInfoDialog = true }
        )
    }

    if (showInfoDialog && selectedSong != null) {
        SongInfoDialog(
            song = selectedSong!!,
            onDismiss = { showInfoDialog = false }
        )
    }
}
