package com.suspended.music.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.suspended.music.ui.utils.rememberHapticFeedback
import com.suspended.music.ui.viewmodel.MainViewModel
import kotlin.math.abs
import kotlin.math.sin

// ══════════════════════════════════════════════
// Theme‑aware colour palette
// ══════════════════════════════════════════════

@Immutable
private data class NpPalette(
    val bgTop: Color,
    val bgBottom: Color,
    val primary: Color,
    val accent: Color,
    val accentLight: Color,
    val textTitle: Color,
    val textSub: Color,
    val waveActive: Color,
    val waveInactive: Color,
    val controlPill: Color,
    val iconMuted: Color,
    val albumRing: Color,
    val albumRingBorder: Color,
    val albumBg: Color,
    val playGradientTop: Color,
    val playGradientBottom: Color,
    val playIcon: Color,
    val backButtonBg: Color,
)

private val LightPalette = NpPalette(
    bgTop              = Color(0xFFEDE6F5),
    bgBottom            = Color(0xFFDFD5EE),
    primary             = Color(0xFF4A3B6B),
    accent              = Color(0xFF6C5B9E),
    accentLight         = Color(0xFF8B7BBF),
    textTitle           = Color(0xFF2A1F42),
    textSub             = Color(0xFF6D6088),
    waveActive          = Color(0xFF7566A8),
    waveInactive        = Color(0xFFD5CCE6),
    controlPill         = Color(0xFFE0D6F0),
    iconMuted           = Color(0xFF9990B2),
    albumRing           = Color(0xFFF7F3FC),
    albumRingBorder     = Color.White.copy(alpha = 0.9f),
    albumBg             = Color.White,
    playGradientTop     = Color(0xFF7B66BE),
    playGradientBottom  = Color(0xFF5A46A0),
    playIcon            = Color.White,
    backButtonBg        = Color.White.copy(alpha = 0.6f),
)

private val DarkPalette = NpPalette(
    bgTop              = Color(0xFF0E0E10),
    bgBottom            = Color(0xFF09090B),
    primary             = Color(0xFFCBBEE8),
    accent              = Color(0xFF9B86D6),
    accentLight         = Color(0xFFB8A6E0),
    textTitle           = Color(0xFFEDE8F5),
    textSub             = Color(0xFFA099B8),
    waveActive          = Color(0xFF9B86D6),
    waveInactive        = Color(0xFF2E2A38),
    controlPill         = Color(0xFF1E1B26),
    iconMuted           = Color(0xFF6E6685),
    albumRing           = Color(0xFF18161E),
    albumRingBorder     = Color(0xFF2A2635).copy(alpha = 0.7f),
    albumBg             = Color(0xFF141218),
    playGradientTop     = Color(0xFF9B86D6),
    playGradientBottom  = Color(0xFF6B55B0),
    playIcon            = Color.White,
    backButtonBg        = Color(0xFF1E1B26).copy(alpha = 0.8f),
)

@Composable
private fun nowPlayingPalette(): NpPalette {
    // Use MaterialTheme surface luminance to detect actual theme, respecting
    // the user's manual theme override instead of raw system dark mode.
    val surfaceLuminance = MaterialTheme.colorScheme.surface.luminance()
    return if (surfaceLuminance < 0.5f) DarkPalette else LightPalette
}

// ══════════════════════════════════════════════

@Composable
fun NowPlayingScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToQueue: () -> Unit,
    onNavigateToEqualizer: () -> Unit,
    onNavigateToTagEditor: (Long) -> Unit,
    onNavigateToSettings: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val colors = nowPlayingPalette()

    val currentSong by viewModel.currentSong.collectAsStateWithLifecycle()
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    val currentPosition by viewModel.currentPosition.collectAsStateWithLifecycle()
    val duration by viewModel.duration.collectAsStateWithLifecycle()
    val repeatMode by viewModel.repeatMode.collectAsStateWithLifecycle()
    val shuffleEnabled by viewModel.shuffleEnabled.collectAsStateWithLifecycle()

    val hapticFeedback = rememberHapticFeedback()
    val song = currentSong ?: return

    val progress by remember(currentPosition, duration) {
        derivedStateOf {
            if (duration > 0) currentPosition.toFloat() / duration.toFloat() else 0f
        }
    }

    val playButtonScale by animateFloatAsState(
        targetValue = if (isPlaying) 1f else 0.92f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "play_scale"
    )

    // ── Background ──
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(colors = listOf(colors.bgTop, colors.bgBottom))
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // ════════════════════════════════
            //  TOP BAR
            // ════════════════════════════════
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    onClick = { hapticFeedback.lightTap(); onNavigateBack() },
                    shape = CircleShape,
                    color = colors.backButtonBg,
                    modifier = Modifier.size(42.dp)
                ) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.KeyboardArrowDown, "Close", tint = colors.primary, modifier = Modifier.size(26.dp))
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    IconButton(onClick = { hapticFeedback.lightTap() }, Modifier.size(38.dp)) {
                        Icon(Icons.Filled.VolumeUp, "Volume", tint = colors.primary, modifier = Modifier.size(21.dp))
                    }
                    IconButton(onClick = { hapticFeedback.lightTap(); onNavigateToEqualizer() }, Modifier.size(38.dp)) {
                        Icon(Icons.Filled.Equalizer, "Equalizer", tint = colors.primary, modifier = Modifier.size(21.dp))
                    }
                    Box {
                        var showMenu by remember { mutableStateOf(false) }
                        IconButton(onClick = { hapticFeedback.lightTap(); showMenu = true }, Modifier.size(38.dp)) {
                            Icon(Icons.Filled.MoreVert, "More", tint = colors.primary, modifier = Modifier.size(21.dp))
                        }
                        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                            DropdownMenuItem(
                                text = { Text("Equalizer") },
                                onClick = { hapticFeedback.lightTap(); showMenu = false; onNavigateToEqualizer() },
                                leadingIcon = { Icon(Icons.Filled.Equalizer, null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Edit Tags") },
                                onClick = { hapticFeedback.lightTap(); showMenu = false; onNavigateToTagEditor(song.id) },
                                leadingIcon = { Icon(Icons.Filled.Edit, null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Settings") },
                                onClick = { hapticFeedback.lightTap(); showMenu = false; onNavigateToSettings() },
                                leadingIcon = { Icon(Icons.Default.Settings, null) }
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // ════════════════════════════════
            //  ALBUM ART
            // ════════════════════════════════
            key(song.id) {
                Box(Modifier.size(300.dp), contentAlignment = Alignment.Center) {
                    // Outer ring
                    Box(
                        modifier = Modifier
                            .size(300.dp)
                            .clip(CircleShape)
                            .background(colors.albumRing)
                            .border(
                                width = 6.dp,
                                brush = Brush.linearGradient(
                                    colors = listOf(colors.albumRingBorder, colors.controlPill.copy(alpha = 0.4f))
                                ),
                                shape = CircleShape
                            )
                    )
                    // Inner circle
                    Box(
                        modifier = Modifier
                            .size(260.dp)
                            .shadow(20.dp, CircleShape, ambientColor = colors.accent.copy(alpha = 0.18f), spotColor = colors.accent.copy(alpha = 0.12f))
                            .clip(CircleShape)
                            .background(colors.albumBg),
                        contentAlignment = Alignment.Center
                    ) {
                        // albumArtUri always returns a valid Uri; Coil handles
                        // missing artwork gracefully via its built-in error state
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(song.albumArtUri)
                                .crossfade(durationMillis = 400)
                                .build(),
                            contentDescription = "Album Art",
                            modifier = Modifier.fillMaxSize().clip(CircleShape),
                            contentScale = ContentScale.Crop,
                            error = androidx.compose.ui.graphics.painter.ColorPainter(colors.albumBg),
                            fallback = androidx.compose.ui.graphics.painter.ColorPainter(colors.albumBg)
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // ════════════════════════════════
            //  SONG TITLE & ARTIST
            // ════════════════════════════════
            key(song.id) {
                Column(
                    Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        song.title,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = colors.textTitle,
                            fontSize = 21.sp,
                            letterSpacing = (-0.3).sp
                        ),
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        song.artist,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = colors.textSub,
                            fontSize = 14.sp
                        ),
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(Modifier.height(18.dp))

            // ════════════════════════════════
            //  ACTION ROW
            // ════════════════════════════════
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 56.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { hapticFeedback.lightTap(); onNavigateToQueue() }, Modifier.size(42.dp)) {
                    Icon(Icons.Filled.QueueMusic, "Queue", tint = colors.iconMuted, modifier = Modifier.size(23.dp))
                }
                IconButton(onClick = { hapticFeedback.lightTap(); viewModel.toggleFavorite(song) }, Modifier.size(42.dp)) {
                    Icon(
                        if (song.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        "Favorite",
                        tint = if (song.isFavorite) colors.accent else colors.iconMuted,
                        modifier = Modifier.size(23.dp)
                    )
                }
                IconButton(onClick = { hapticFeedback.lightTap() }, Modifier.size(42.dp)) {
                    Icon(Icons.Filled.Add, "Add to playlist", tint = colors.iconMuted, modifier = Modifier.size(23.dp))
                }
            }

            Spacer(Modifier.height(10.dp))

            // ════════════════════════════════
            //  AUDIO INFO STRIP
            // ════════════════════════════════
            key(song.id) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val infoPills = remember(song.id, song.codec, song.bitrate, song.sampleRate, song.size) {
                        buildList {
                            add(song.codec)
                            if (song.bitrate > 0) add("${song.bitrate} kbps")
                            if (song.sampleRate > 0) add(song.formattedSampleRate)
                            if (song.size > 0) {
                                val sizeMb = song.size / 1_000_000.0
                                add(if (sizeMb >= 1.0) "%.1f MB".format(sizeMb) else "%.0f KB".format(song.size / 1_000.0))
                            }
                        }
                    }

                    infoPills.forEachIndexed { index, label ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = colors.controlPill.copy(alpha = 0.6f),
                            modifier = Modifier.padding(horizontal = 3.dp)
                        ) {
                            Text(
                                label,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = colors.textSub,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    letterSpacing = 0.3.sp
                                )
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            // ════════════════════════════════
            //  WAVEFORM SEEKBAR
            // ════════════════════════════════
            key(song.id) {
                Column(Modifier.fillMaxWidth()) {
                    var sliderPosition by remember { mutableFloatStateOf(0f) }
                    var isDragging by remember { mutableStateOf(false) }

                    LaunchedEffect(progress) {
                        if (!isDragging) sliderPosition = progress
                    }
                    val displayProgress = if (isDragging) sliderPosition else progress

                    val barHeights = remember(song.id) {
                        val count = 60
                        FloatArray(count) { i ->
                            val n = i.toFloat() / count
                            val w1 = abs(sin(n * Math.PI * 2.5 + 0.8)).toFloat()
                            val w2 = abs(sin(n * Math.PI * 5.2 + 2.1)).toFloat()
                            val w3 = abs(sin(n * Math.PI * 8.7 + 4.3)).toFloat()
                            (0.18f + 0.42f * w1 + 0.25f * w2 + 0.15f * w3).coerceIn(0.10f, 0.95f)
                        }
                    }

                    // Capture colors for Canvas (non‑composable scope)
                    val waveActiveColor = colors.waveActive
                    val waveInactiveColor = colors.waveInactive
                    val playheadColor = colors.primary

                    Box(Modifier.fillMaxWidth().height(52.dp), contentAlignment = Alignment.Center) {
                        Canvas(Modifier.fillMaxWidth().height(38.dp)) {
                            val w = size.width; val h = size.height; val cy = h / 2
                            val n = barHeights.size; val gap = w / n; val barW = gap * 0.48f
                            val rounding = CornerRadius(barW / 2)

                            for (i in 0 until n) {
                                val x = i * gap + gap / 2
                                val barH = h * barHeights[i]
                                val active = i.toFloat() / n < displayProgress
                                drawRoundRect(
                                    color = if (active) waveActiveColor else waveInactiveColor,
                                    topLeft = Offset(x - barW / 2, cy - barH / 2),
                                    size = Size(barW, barH),
                                    cornerRadius = rounding
                                )
                            }
                            val px = displayProgress * w
                            drawLine(playheadColor, Offset(px, 0f), Offset(px, h), 2.dp.toPx(), cap = StrokeCap.Round)
                        }

                        Slider(
                            value = displayProgress,
                            onValueChange = { isDragging = true; sliderPosition = it },
                            onValueChangeFinished = { viewModel.seekTo((sliderPosition * duration).toLong()); isDragging = false },
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            colors = SliderDefaults.colors(
                                thumbColor = Color.Transparent,
                                activeTrackColor = Color.Transparent,
                                inactiveTrackColor = Color.Transparent,
                                activeTickColor = Color.Transparent,
                                inactiveTickColor = Color.Transparent
                            )
                        )
                    }

                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            formatDuration(if (isDragging) (sliderPosition * duration).toLong() else currentPosition),
                            style = MaterialTheme.typography.labelSmall.copy(color = colors.textSub, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        )
                        Text(
                            formatDuration(duration),
                            style = MaterialTheme.typography.labelSmall.copy(color = colors.textSub, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        )
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            // ════════════════════════════════
            //  PLAYBACK CONTROLS
            // ════════════════════════════════
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Shuffle
                IconButton(onClick = { hapticFeedback.lightTap(); viewModel.toggleShuffle() }, Modifier.size(44.dp)) {
                    Icon(Icons.Filled.Shuffle, "Shuffle",
                        tint = if (shuffleEnabled) colors.accent else colors.iconMuted,
                        modifier = Modifier.size(21.dp))
                }

                // Previous
                Surface(
                    onClick = { hapticFeedback.lightTap(); viewModel.skipToPrevious() },
                    shape = RoundedCornerShape(18.dp),
                    color = colors.controlPill,
                    modifier = Modifier.size(width = 56.dp, height = 52.dp)
                ) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.SkipPrevious, "Previous", tint = colors.primary, modifier = Modifier.size(28.dp))
                    }
                }

                // Play / Pause
                Surface(
                    onClick = { hapticFeedback.mediumTap(); viewModel.togglePlayPause() },
                    shape = CircleShape,
                    color = Color.Transparent,
                    shadowElevation = 14.dp,
                    modifier = Modifier
                        .size(74.dp)
                        .graphicsLayer { scaleX = playButtonScale; scaleY = playButtonScale }
                ) {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(listOf(colors.playGradientTop, colors.playGradientBottom)),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            if (isPlaying) "Pause" else "Play",
                            tint = colors.playIcon,
                            modifier = Modifier.size(38.dp)
                        )
                    }
                }

                // Next
                Surface(
                    onClick = { hapticFeedback.lightTap(); viewModel.skipToNext() },
                    shape = RoundedCornerShape(18.dp),
                    color = colors.controlPill,
                    modifier = Modifier.size(width = 56.dp, height = 52.dp)
                ) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.SkipNext, "Next", tint = colors.primary, modifier = Modifier.size(28.dp))
                    }
                }

                // Repeat
                IconButton(onClick = { hapticFeedback.lightTap(); viewModel.toggleRepeatMode() }, Modifier.size(44.dp)) {
                    Icon(
                        when (repeatMode) { Player.REPEAT_MODE_ONE -> Icons.Filled.RepeatOne; else -> Icons.Filled.Repeat },
                        "Repeat",
                        tint = when (repeatMode) { Player.REPEAT_MODE_OFF -> colors.iconMuted; else -> colors.accent },
                        modifier = Modifier.size(21.dp)
                    )
                }
            }

            Spacer(Modifier.height(28.dp))
        }
    }
}

private fun formatDuration(millis: Long): String {
    val totalSeconds = millis / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(java.util.Locale.getDefault(), "%02d:%02d", minutes, seconds)
}
