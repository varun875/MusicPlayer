package com.suspended.music.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.History
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.suspended.music.data.model.Song
import com.suspended.music.ui.components.EmptyState
import com.suspended.music.ui.components.PlaylistPickerDialog
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
    onNavigateToTagEditor: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()
    val searchFilters by viewModel.searchFilters.collectAsStateWithLifecycle()
    val recentSearches by viewModel.recentSearches.collectAsStateWithLifecycle()
    val currentSong by viewModel.currentSong.collectAsStateWithLifecycle()
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // Filter sheet data
    val artists by viewModel.availableArtists.collectAsStateWithLifecycle()
    val albums by viewModel.availableAlbums.collectAsStateWithLifecycle()
    val genres by viewModel.availableGenres.collectAsStateWithLifecycle()
    val composers by viewModel.availableComposers.collectAsStateWithLifecycle()
    val folders by viewModel.availableFolders.collectAsStateWithLifecycle()

    val filterSheetState = rememberModalBottomSheetState()
    val optionsSheetState = rememberModalBottomSheetState()
    var showFilters by remember { mutableStateOf(false) }
    var selectedSong by remember { mutableStateOf<Song?>(null) }
    var showInfoDialog by remember { mutableStateOf(false) }
    var showAddToPlaylistDialog by remember { mutableStateOf(false) }

    val activeFilters = searchFilters
    val hasQuery = searchQuery.isNotBlank()
    val showRecents = !hasQuery && activeFilters == null && recentSearches.isNotEmpty()

    // Highlighted index for keyboard/IME navigation through results.
    var highlightedIndex by remember { mutableStateOf(-1) }
    val listState = rememberLazyListState()

    Scaffold(
        modifier = modifier,
        topBar = {
            SearchBar(
                query = searchQuery,
                onQueryChange = {
                    viewModel.updateSearchQuery(it)
                    highlightedIndex = -1
                },
                onSearch = { query -> viewModel.commitSearch(query) },
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = {
                                viewModel.updateSearchQuery("")
                                viewModel.updateSearchFilters(null)
                            }) {
                                Icon(Icons.Filled.Clear, contentDescription = "Clear")
                            }
                        }
                        // Filters stay reachable even after typing a query.
                        IconButton(onClick = { showFilters = true }) {
                            Icon(
                                Icons.Filled.FilterList,
                                contentDescription = "Filters",
                                tint = if (activeFilters != null) MaterialTheme.colorScheme.primary
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
        val highlightColor = MaterialTheme.colorScheme.primaryContainer
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .focusable()
                .onPreviewKeyEvent { event ->
                    if (event.type != KeyEventType.KeyUp) return@onPreviewKeyEvent false
                    if (!hasQuery && activeFilters == null) return@onPreviewKeyEvent false
                    when (event.key) {
                        Key.DirectionDown -> {
                            if (searchResults.isNotEmpty()) {
                                highlightedIndex =
                                    (highlightedIndex + 1).coerceIn(0, searchResults.lastIndex)
                                scope.launch { listState.animateScrollToItem(highlightedIndex) }
                            }
                            true
                        }
                        Key.DirectionUp -> {
                            if (searchResults.isNotEmpty()) {
                                highlightedIndex =
                                    (highlightedIndex - 1).coerceIn(0, searchResults.size - 1)
                                scope.launch { listState.animateScrollToItem(highlightedIndex) }
                            }
                            true
                        }
                        Key.Enter, Key.NumPadEnter -> {
                            if (highlightedIndex in searchResults.indices) {
                                viewModel.playSongs(searchResults, highlightedIndex)
                                viewModel.commitSearch(searchQuery)
                                onNavigateToNowPlaying()
                            }
                            highlightedIndex >= 0
                        }
                        else -> false
                    }
                }
        ) {
            if (showRecents) {
                SectionHeader(title = "Recent searches") {
                    IconButton(onClick = { viewModel.clearRecentSearches() }) {
                        Icon(Icons.Filled.Close, contentDescription = "Clear recent", modifier = Modifier.size(18.dp))
                    }
                }
                LazyColumn(contentPadding = PaddingValues(bottom = 100.dp)) {
                    items(recentSearches.size) { index ->
                        val recent = recentSearches[index]
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.updateSearchQuery(recent)
                                }
                                .padding(horizontal = 20.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Filled.History,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = recent,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            } else if (searchResults.isEmpty() && hasQuery) {
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
                    modifier = Modifier.weight(1f),
                    action = if (activeFilters != null) {
                        {
                            androidx.compose.material3.TextButton(
                                onClick = { viewModel.updateSearchFilters(null) }
                            ) {
                                Text("Clear filters")
                            }
                        }
                    } else {
                        null
                    }
                )
            } else {
                // Results header
                if (hasQuery || activeFilters != null) {
                    Text(
                        text = "${searchResults.size} result${if (searchResults.size == 1) "" else "s"}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }

                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 100.dp)
                ) {
                    items(
                        count = searchResults.size,
                        key = { index -> searchResults[index].id },
                        contentType = { "search_item" }
                    ) { index ->
                        val song = searchResults[index]
                        val isHighlighted = index == highlightedIndex
                        SongItem(
                            song = song,
                            isPlaying = currentSong?.id == song.id,
                            showArtwork = true,
                            modifier = Modifier.background(
                                if (isHighlighted) MaterialTheme.colorScheme.primaryContainer
                                else Color.Transparent,
                                RoundedCornerShape(12.dp)
                            ),
                            onClick = {
                                viewModel.playSongs(searchResults, index)
                                viewModel.commitSearch(searchQuery)
                                onNavigateToNowPlaying()
                            },
                            onMoreClick = {
                                selectedSong = it
                                scope.launch { optionsSheetState.show() }
                            },
                            onFavoriteClick = { viewModel.toggleFavorite(it) }
                        )
                    }
                }
            }
        }
    }

    // Filters bottom sheet
    if (showFilters) {
        SearchFiltersSheet(
            currentFilters = activeFilters ?: SearchFilters(),
            availableArtists = artists,
            availableAlbums = albums,
            availableGenres = genres,
            availableComposers = composers,
            availableFolders = folders,
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
                }
                if (!showInfoDialog && !showAddToPlaylistDialog) {
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
private fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    trailing: @Composable () -> Unit = {}
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        trailing()
    }
}
