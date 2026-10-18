package com.suspended.music.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.suspended.music.data.model.Artist
import com.suspended.music.ui.components.AlphabetIndex
import com.suspended.music.ui.components.ArtistCard
import com.suspended.music.ui.components.ArtistItem
import com.suspended.music.ui.components.EmptyState
import com.suspended.music.ui.components.LibrarySectionHeader
import com.suspended.music.ui.utils.rememberAdaptiveLayoutInfo
import com.suspended.music.ui.viewmodel.MainViewModel
import kotlinx.coroutines.launch

private sealed interface ArtistRow {
    data class Header(val letter: Char) : ArtistRow
    data class Entry(val artist: Artist) : ArtistRow
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArtistsScreen(
    viewModel: MainViewModel,
    onNavigateToSearch: () -> Unit,
    onNavigateToArtist: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val artists by viewModel.artists.collectAsStateWithLifecycle()
    val isScanning by viewModel.isScanning.collectAsStateWithLifecycle()
    val appSettings by viewModel.appSettings.collectAsStateWithLifecycle()
    val libraryConfig = appSettings.library
    val adaptiveInfo = rememberAdaptiveLayoutInfo()

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(
        state = rememberTopAppBarState()
    )

    // Alphabetical sections: letters first, then '#' for anything non-alpha.
    val sections: List<Pair<Char, List<Artist>>> = remember(artists) {
        artists
            .sortedBy { it.name.lowercase() }
            .groupBy { it.name.firstOrNull()?.uppercaseChar()?.takeIf { c -> c in 'A'..'Z' } ?: '#' }
            .entries
            .sortedWith(compareBy({ it.key == '#' }, { it.key }))
            .map { it.key to it.value }
    }
    val rows: List<ArtistRow> = remember(sections) {
        buildList {
            sections.forEach { (letter, list) ->
                add(ArtistRow.Header(letter))
                list.forEach { add(ArtistRow.Entry(it)) }
            }
        }
    }
    val letters = remember(sections) { sections.map { it.first } }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        text = "Artists",
                        style = MaterialTheme.typography.displaySmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.5).sp
                        )
                    )
                },
                actions = {
                    IconButton(onClick = onNavigateToSearch) {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = "Search"
                        )
                    }
                    IconButton(onClick = {
                        val nextMode = if (libraryConfig.layoutMode == "GRID") "LIST" else "GRID"
                        viewModel.setLibraryLayoutMode(nextMode)
                    }) {
                        Icon(
                            imageVector = if (libraryConfig.layoutMode == "GRID") {
                                Icons.Filled.List
                            } else {
                                Icons.Filled.GridView
                            },
                            contentDescription = if (libraryConfig.layoutMode == "GRID") {
                                "Switch to list view"
                            } else {
                                "Switch to grid view"
                            }
                        )
                    }
                    if (libraryConfig.layoutMode == "GRID") {
                        IconButton(
                            onClick = {
                                val nextSize = if (libraryConfig.gridSize >= 4) 2 else libraryConfig.gridSize + 1
                                viewModel.setLibraryGridSize(nextSize)
                            },
                            modifier = Modifier.semantics {
                                contentDescription =
                                    "Grid columns: ${libraryConfig.gridSize}, tap to change"
                            }
                        ) {
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
                    scrolledContainerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        if (artists.isEmpty()) {
            EmptyState(
                icon = {
                    Icon(
                        imageVector = Icons.Filled.Person,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                title = "No artists found",
                subtitle = "Scan your device to find music",
                modifier = Modifier.padding(paddingValues),
                action = {
                    Button(
                        onClick = { viewModel.scanMedia() },
                        enabled = !isScanning
                    ) {
                        Text(if (isScanning) "Scanning…" else "Scan library")
                    }
                }
            )
        } else if (libraryConfig.layoutMode == "GRID") {
            LazyVerticalGrid(
                columns = if (adaptiveInfo.isTwoPane) {
                    GridCells.Adaptive(minSize = 160.dp)
                } else {
                    GridCells.Fixed(libraryConfig.gridSize)
                },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 8.dp,
                    bottom = 100.dp
                )
            ) {
                items(items = artists, key = { it.id }) { artist ->
                    ArtistCard(
                        artist = artist,
                        onClick = { onNavigateToArtist(artist.id) }
                    )
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                ArtistsSectionedList(
                    rows = rows,
                    listState = listState,
                    onArtistClick = { onNavigateToArtist(it.id) }
                )

                if (letters.size > 1) {
                    AlphabetIndex(
                        letters = letters,
                        onLetterSelected = { letter ->
                            val target = rows.indexOfFirst {
                                it is ArtistRow.Header && it.letter == letter
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
    }
}

@Composable
private fun ArtistsSectionedList(
    rows: List<ArtistRow>,
    listState: LazyListState,
    onArtistClick: (Artist) -> Unit
) {
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 100.dp)
    ) {
        items(
            items = rows,
            key = { row ->
                when (row) {
                    is ArtistRow.Header -> "h_${row.letter}"
                    is ArtistRow.Entry -> "a_${row.artist.id}"
                }
            },
            contentType = { row ->
                if (row is ArtistRow.Header) "header" else "artist"
            }
        ) { row ->
            when (row) {
                is ArtistRow.Header -> LibrarySectionHeader(letter = row.letter.toString())
                is ArtistRow.Entry -> ArtistItem(
                    artist = row.artist,
                    onClick = { onArtistClick(row.artist) }
                )
            }
        }
    }
}
