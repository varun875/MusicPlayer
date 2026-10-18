package com.suspended.music.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

data class SearchFilters(
    val minDuration: Long? = null, // in milliseconds
    val maxDuration: Long? = null,
    val minYear: Int? = null,
    val maxYear: Int? = null,
    val artist: String? = null,
    val album: String? = null,
    val genre: String? = null,
    val composer: String? = null,
    val folder: String? = null,
    val lyrics: String? = null,
    val favorites: Boolean? = null,
    val hiRes: Boolean? = null, // Filter by hi-res audio
    val scope: SearchScope = SearchScope.ALL // which metadata columns to match
)

enum class SearchScope(val label: String) {
    ALL("Title, Artist, Album, Composer"),
    TITLE("Title only"),
    ARTIST("Artist only"),
    ALBUM("Album only"),
    COMPOSER("Composer only")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchFiltersSheet(
    currentFilters: SearchFilters,
    availableArtists: List<String>,
    availableAlbums: List<String>,
    availableGenres: List<String>,
    availableComposers: List<String>,
    availableFolders: List<String>,
    sheetState: SheetState,
    onDismiss: () -> Unit,
    onApplyFilters: (SearchFilters) -> Unit,
    modifier: Modifier = Modifier
) {
    var filters by remember { mutableStateOf(currentFilters) }
    var durationRange by remember {
        mutableStateOf(
            (currentFilters.minDuration?.toFloat() ?: 0f)..(currentFilters.maxDuration?.toFloat() ?: 600000f)
        )
    }
    var yearRange by remember {
        mutableStateOf(
            (currentFilters.minYear?.toFloat() ?: 1900f)..(currentFilters.maxYear?.toFloat() ?: 2026f)
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Text(
                text = "Search Filters",
                style = MaterialTheme.typography.headlineSmall
            )
            
            Spacer(modifier = Modifier.height(16.dp))

            // Duration filter
            Text(
                text = "Duration: ${formatDuration(durationRange.start.toLong())} - ${formatDuration(durationRange.endInclusive.toLong())}",
                style = MaterialTheme.typography.bodyMedium
            )
            RangeSlider(
                value = durationRange,
                onValueChange = { range ->
                    durationRange = range
                    filters = filters.copy(
                        minDuration = range.start.toLong(),
                        maxDuration = range.endInclusive.toLong()
                    )
                },
                valueRange = 0f..600000f, // 0 to 10 minutes
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Year filter
            Text(
                text = "Year: ${yearRange.start.toInt()} - ${yearRange.endInclusive.toInt()}",
                style = MaterialTheme.typography.bodyMedium
            )
            RangeSlider(
                value = yearRange,
                onValueChange = { range ->
                    yearRange = range
                    filters = filters.copy(
                        minYear = range.start.toInt(),
                        maxYear = range.endInclusive.toInt()
                    )
                },
                valueRange = 1900f..2026f,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Favorites filter
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Favorites only",
                    style = MaterialTheme.typography.bodyLarge
                )
                FilterChip(
                    selected = filters.favorites == true,
                    onClick = {
                        filters = filters.copy(
                            favorites = if (filters.favorites == true) null else true
                        )
                    },
                    label = { Text(if (filters.favorites == true) "On" else "Off") }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Hi-Res audio filter
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Hi-Res Audio Only",
                    style = MaterialTheme.typography.bodyLarge
                )
                FilterChip(
                    selected = filters.hiRes == true,
                    onClick = {
                        filters = filters.copy(
                            hiRes = if (filters.hiRes == true) null else true
                        )
                    },
                    label = { Text(if (filters.hiRes == true) "On" else "Off") }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Artist filter
            FilterDropdown(
                label = "Artist",
                selectedValue = filters.artist,
                options = availableArtists,
                onValueSelected = { filters = filters.copy(artist = it) }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Album filter
            FilterDropdown(
                label = "Album",
                selectedValue = filters.album,
                options = availableAlbums,
                onValueSelected = { filters = filters.copy(album = it) }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Genre filter
            FilterDropdown(
                label = "Genre",
                selectedValue = filters.genre,
                options = availableGenres,
                onValueSelected = { filters = filters.copy(genre = it) }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Composer filter
            FilterDropdown(
                label = "Composer",
                selectedValue = filters.composer,
                options = availableComposers,
                onValueSelected = { filters = filters.copy(composer = it) }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Folder filter
            FilterDropdown(
                label = "Folder",
                selectedValue = filters.folder,
                options = availableFolders,
                onValueSelected = { filters = filters.copy(folder = it) }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Lyrics search scope (free text within lyrics)
            Text(
                text = "Lyrics",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            androidx.compose.material3.OutlinedTextField(
                value = filters.lyrics ?: "",
                onValueChange = { filters = filters.copy(lyrics = it.takeIf { s -> s.isNotBlank() }) },
                placeholder = { Text("Search inside lyrics...") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Metadata scope
            Text(
                text = "Search in",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SearchScope.entries.forEach { scope ->
                    FilterChip(
                        selected = filters.scope == scope,
                        onClick = { filters = filters.copy(scope = scope) },
                        label = { Text(scope.label, maxLines = 1) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = {
                    filters = SearchFilters()
                    durationRange = 0f..600000f
                    yearRange = 1900f..2026f
                }) {
                    Text("Clear")
                }
                Spacer(modifier = Modifier.width(8.dp))
                TextButton(onClick = {
                    onApplyFilters(filters)
                    onDismiss()
                }) {
                    Text("Apply")
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun FilterDropdown(
    label: String,
    selectedValue: String?,
    options: List<String>,
    onValueSelected: (String?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge
        )
        
        IconButton(onClick = { expanded = true }) {
            Icon(
                imageVector = Icons.Filled.FilterList,
                contentDescription = "Select $label"
            )
        }
        
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text("All") },
                onClick = {
                    onValueSelected(null)
                    expanded = false
                }
            )
            options.take(20).forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onValueSelected(option)
                        expanded = false
                    }
                )
            }
        }
        
        selectedValue?.let {
            FilterChip(
                selected = true,
                onClick = { onValueSelected(null) },
                label = { Text(it, maxLines = 1) }
            )
        }
    }
}

private fun formatDuration(millis: Long): String {
    val minutes = (millis / 1000) / 60
    val seconds = (millis / 1000) % 60
    return "%d:%02d".format(minutes, seconds)
}
