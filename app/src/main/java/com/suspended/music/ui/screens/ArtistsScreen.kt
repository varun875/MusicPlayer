package com.suspended.music.ui.screens

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import com.suspended.music.ui.components.ArtistItem
import com.suspended.music.ui.components.EmptyState
import com.suspended.music.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArtistsScreen(
    viewModel: MainViewModel,
    onNavigateToSearch: () -> Unit,
    onNavigateToArtist: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val artists by viewModel.artists.collectAsState()
    val appSettings by viewModel.appSettings.collectAsState()
    val libraryConfig = appSettings.library
    
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(
        state = rememberTopAppBarState()
    )
    
    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(text = "Artists")
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
                            imageVector = if (libraryConfig.layoutMode == "GRID") Icons.Filled.List else Icons.Filled.GridView,
                            contentDescription = "Toggle Layout"
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
                contentPadding = PaddingValues(
                    start = 16.dp, 
                    end = 16.dp, 
                    top = 8.dp, 
                    bottom = 100.dp
                )
            ) {
                items(
                    items = artists,
                    key = { it.id }
                ) { artist ->
                    if (libraryConfig.layoutMode == "GRID") {
                        com.suspended.music.ui.components.ArtistCard(
                            artist = artist,
                            onClick = { onNavigateToArtist(artist.id) }
                        )
                    } else {
                        ArtistItem(
                            artist = artist,
                            onClick = { onNavigateToArtist(artist.id) }
                        )
                    }
                }
            }
        }
    }
}
