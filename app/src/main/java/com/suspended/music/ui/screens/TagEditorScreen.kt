package com.suspended.music.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.outlined.ImageSearch
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.suspended.music.data.model.AudioTags
import com.suspended.music.ui.viewmodel.MainViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TagEditorScreen(
    songId: Long,
    viewModel: MainViewModel = hiltViewModel(),
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBackClick)

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    
    LaunchedEffect(songId) {
        viewModel.loadTags(songId)
    }
    
    val tags by viewModel.tags.collectAsStateWithLifecycle()
    
    // Form state
    var title by remember { mutableStateOf("") }
    var artist by remember { mutableStateOf("") }
    var album by remember { mutableStateOf("") }
    var year by remember { mutableStateOf("") }
    var genre by remember { mutableStateOf("") }
    var trackNumber by remember { mutableStateOf("") }
    var discNumber by remember { mutableStateOf("") }
    var composer by remember { mutableStateOf("") }
    var comment by remember { mutableStateOf("") }
    var albumArtist by remember { mutableStateOf("") }
    var lyrics by remember { mutableStateOf("") }
    var artworkUri by remember { mutableStateOf<Uri?>(null) }
    var hasChanges by remember { mutableStateOf(false) }
    
    // UI state
    var showBasicFields by remember { mutableStateOf(true) }
    var showAdvancedFields by remember { mutableStateOf(false) }
    var showLyrics by remember { mutableStateOf(false) }
    var showArtworkEditor by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    
    // Artwork picker
    val artworkPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            artworkUri = it
            hasChanges = true
        }
    }
    
    LaunchedEffect(tags) {
        tags?.let {
            title = it.title
            artist = it.artist
            album = it.album
            year = it.year
            genre = it.genre
            trackNumber = it.trackNumber
            discNumber = it.discNumber
            composer = it.composer
            comment = it.comment
            albumArtist = it.albumArtist
            lyrics = it.lyrics
            artworkUri = it.artworkUri
        }
    }
    
    // Detect changes
    LaunchedEffect(title, artist, album, year, genre, trackNumber, discNumber, composer, comment, albumArtist, lyrics, artworkUri) {
        hasChanges = tags?.let { current ->
            title != current.title ||
            artist != current.artist ||
            album != current.album ||
            year != current.year ||
            genre != current.genre ||
            trackNumber != current.trackNumber ||
            discNumber != current.discNumber ||
            composer != current.composer ||
            comment != current.comment ||
            albumArtist != current.albumArtist ||
            lyrics != current.lyrics ||
            artworkUri != current.artworkUri
        } ?: false
    }
    
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            LargeTopAppBar(
                title = { 
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Brush,
                            contentDescription = null,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text("Edit Tags")
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    AnimatedVisibility(
                        visible = hasChanges,
                        enter = scaleIn() + fadeIn(),
                        exit = scaleOut() + fadeOut()
                    ) {
                        Button(
                            onClick = {
                                if (title.isBlank()) {
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Title cannot be empty")
                                    }
                                    return@Button
                                }
                                
                                isSaving = true
                                val newTags = AudioTags(
                                    title = title,
                                    artist = artist,
                                    album = album,
                                    year = year,
                                    genre = genre,
                                    trackNumber = trackNumber,
                                    discNumber = discNumber,
                                    composer = composer,
                                    comment = comment,
                                    albumArtist = albumArtist,
                                    lyrics = lyrics,
                                    artworkUri = artworkUri,
                                    hasArtworkChanged = artworkUri != tags?.artworkUri
                                )
                                
                                viewModel.saveTags(songId, newTags)
                                isSaving = false
                                onBackClick()
                            },
                            enabled = !isSaving
                        ) {
                            if (isSaving) {
                                androidx.compose.material3.CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(Icons.Filled.Save, contentDescription = null)
                            }
                            Spacer(modifier = Modifier.padding(horizontal = 8.dp))
                            Text("Save")
                        }
                    }
                },
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    title = tags?.title ?: ""
                    artist = tags?.artist ?: ""
                    album = tags?.album ?: ""
                    year = tags?.year ?: ""
                    genre = tags?.genre ?: ""
                    trackNumber = tags?.trackNumber ?: ""
                    discNumber = tags?.discNumber ?: ""
                    composer = tags?.composer ?: ""
                    comment = tags?.comment ?: ""
                    albumArtist = tags?.albumArtist ?: ""
                    lyrics = tags?.lyrics ?: ""
                    artworkUri = tags?.artworkUri
                },
                icon = { Icon(Icons.Filled.Restore, contentDescription = null) },
                text = { Text("Reset") }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Artwork Section
            ArtworkSection(
                artworkUri = artworkUri,
                onPickArtwork = { artworkPickerLauncher.launch("image/*") },
                onEditArtwork = { showArtworkEditor = true },
                onRemoveArtwork = { 
                    artworkUri = null
                    hasChanges = true
                }
            )
            
            // Quick Actions
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = showBasicFields,
                    onClick = { showBasicFields = !showBasicFields },
                    label = { Text("Basic") },
                    leadingIcon = { Icon(Icons.Default.MusicNote, contentDescription = null) }
                )
                
                FilterChip(
                    selected = showAdvancedFields,
                    onClick = { showAdvancedFields = !showAdvancedFields },
                    label = { Text("Advanced") },
                    leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) }
                )
                
                FilterChip(
                    selected = showLyrics,
                    onClick = { showLyrics = !showLyrics },
                    label = { Text("Lyrics") },
                    leadingIcon = { Icon(Icons.Default.Description, contentDescription = null) }
                )
                
                AssistChip(
                    onClick = {
                        title = tags?.title ?: ""
                        artist = tags?.artist ?: ""
                        album = tags?.album ?: ""
                        year = tags?.year ?: ""
                        genre = tags?.genre ?: ""
                        trackNumber = tags?.trackNumber ?: ""
                        discNumber = tags?.discNumber ?: ""
                        composer = tags?.composer ?: ""
                        comment = tags?.comment ?: ""
                        albumArtist = tags?.albumArtist ?: ""
                        artworkUri = tags?.artworkUri
                        lyrics = tags?.lyrics ?: ""
                    },
                    label = { Text("Reset All") },
                    leadingIcon = { Icon(Icons.Default.Restore, contentDescription = null) }
                )
            }
            
            HorizontalDivider()
            
            // Basic Fields
            AnimatedVisibility(visible = showBasicFields) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Basic Information",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    TagTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = "Title",
                        icon = Icons.Default.MusicNote,
                        required = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                    )
                    
                    TagTextField(
                        value = artist,
                        onValueChange = { artist = it },
                        label = "Artist",
                        icon = Icons.Default.Person,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                    )
                    
                    TagTextField(
                        value = album,
                        onValueChange = { album = it },
                        label = "Album",
                        icon = Icons.Default.Album,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        TagTextField(
                            value = year,
                            onValueChange = { year = it },
                            label = "Year",
                            icon = Icons.Default.Info,
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Next
                            )
                        )
                        
                        TagTextField(
                            value = genre,
                            onValueChange = { genre = it },
                            label = "Genre",
                            icon = Icons.Default.MusicNote,
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
                        )
                    }
                }
            }
            
            // Advanced Fields
            AnimatedVisibility(visible = showAdvancedFields) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Advanced Information",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        TagTextField(
                            value = trackNumber,
                            onValueChange = { trackNumber = it },
                            label = "Track",
                            icon = Icons.Default.MusicNote,
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Next
                            )
                        )
                        
                        TagTextField(
                            value = discNumber,
                            onValueChange = { discNumber = it },
                            label = "Disc",
                            icon = Icons.Default.Album,
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Next
                            )
                        )
                    }
                    
                    TagTextField(
                        value = albumArtist,
                        onValueChange = { albumArtist = it },
                        label = "Album Artist",
                        icon = Icons.Default.Person,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                    )
                    
                    TagTextField(
                        value = composer,
                        onValueChange = { composer = it },
                        label = "Composer",
                        icon = Icons.Default.Edit,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                    )
                    
                    TagTextField(
                        value = comment,
                        onValueChange = { comment = it },
                        label = "Comment",
                        icon = Icons.Default.Info,
                        singleLine = false,
                        maxLines = 3,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
                    )
                }
            }
            
            // Lyrics
            AnimatedVisibility(visible = showLyrics) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Lyrics",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                    
                    TagTextField(
                        value = lyrics,
                        onValueChange = { lyrics = it },
                        label = "Song Lyrics",
                        icon = Icons.Default.Description,
                        singleLine = false,
                        maxLines = 20,
                        modifier = Modifier.height(300.dp),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Quick Genre Suggestions
            if (genre.isNotEmpty()) {
                val suggestedGenres = listOf(
                    "Rock", "Pop", "Electronic", "Classical", "Jazz", "Hip Hop",
                    "Country", "R&B", "Metal", "Folk", "Blues", "Reggae"
                ).filter { it.contains(genre, ignoreCase = true) }
                
                if (suggestedGenres.isNotEmpty()) {
                    Text(
                        text = "Suggested Genres",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        suggestedGenres.take(6).forEach { suggestedGenre ->
                            InputChip(
                                selected = false,
                                onClick = { genre = suggestedGenre },
                                label = { Text(suggestedGenre) }
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(100.dp)) // Space for FAB
        }
    }
}

@Composable
private fun ArtworkSection(
    artworkUri: Uri?,
    onPickArtwork: () -> Unit,
    onEditArtwork: () -> Unit,
    onRemoveArtwork: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.surfaceVariant,
                                MaterialTheme.colorScheme.surface
                            )
                        )
                    )
                    .clickable { onPickArtwork() },
                contentAlignment = Alignment.Center
            ) {
                if (artworkUri != null) {
                    val imageModifier = Modifier.fillMaxSize()
                    
                    AsyncImage(
                        model = artworkUri,
                        contentDescription = "Album Artwork",
                        modifier = imageModifier,
                        contentScale = ContentScale.Crop
                    )
                    
                    // Overlay buttons
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Color.Black.copy(alpha = 0.3f),
                                RoundedCornerShape(12.dp)
                            )
                    ) {
                        Row(
                            modifier = Modifier.align(Alignment.BottomCenter),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            IconButton(
                                onClick = onPickArtwork,
                                modifier = Modifier
                                    .background(
                                        MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                                        CircleShape
                                    )
                            ) {
                                Icon(Icons.Outlined.ImageSearch, contentDescription = "Change Artwork")
                            }
                            
                            IconButton(
                                onClick = onRemoveArtwork,
                                modifier = Modifier
                                    .background(
                                        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.9f),
                                        CircleShape
                                    )
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Remove Artwork")
                            }
                        }
                    }
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.Image,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Tap to add artwork",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = if (artworkUri != null) "Artwork Loaded" else "No Artwork",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TagTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    required: Boolean = false,
    singleLine: Boolean = true,
    maxLines: Int = 1,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { 
            Text(
                text = if (required) "$label *" else label,
                color = if (required && value.isEmpty()) 
                    MaterialTheme.colorScheme.error 
                else 
                    MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        leadingIcon = {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        modifier = modifier,
        singleLine = singleLine,
        maxLines = maxLines,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        shape = RoundedCornerShape(12.dp),
        textStyle = MaterialTheme.typography.bodyLarge
    )
}