package com.suspended.music.ui.screens

import android.content.Intent
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Update
import androidx.compose.material.icons.outlined.BrightnessHigh
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.suspended.music.BuildConfig
import com.suspended.music.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val settings by viewModel.appSettings.collectAsStateWithLifecycle()
    val updateNotification by viewModel.updateNotification.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(
        rememberTopAppBarState()
    )

    var isCheckingUpdates by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {

            // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
            // SECTION 1 — APPEARANCE
            // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
            item {
                SectionHeader(icon = Icons.Filled.Palette, title = "Appearance")
            }

            // Theme Mode
            item {
                SettingCard {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Theme Mode",
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ThemeModeChip(
                                label = "System",
                                icon = Icons.Outlined.PhoneAndroid,
                                isSelected = settings.theme.isDarkMode == null,
                                onClick = { viewModel.setDarkMode(null) },
                                modifier = Modifier.weight(1f)
                            )
                            ThemeModeChip(
                                label = "Light",
                                icon = Icons.Outlined.BrightnessHigh,
                                isSelected = settings.theme.isDarkMode == false,
                                onClick = { viewModel.setDarkMode(false) },
                                modifier = Modifier.weight(1f)
                            )
                            ThemeModeChip(
                                label = "Dark",
                                icon = Icons.Outlined.DarkMode,
                                isSelected = settings.theme.isDarkMode == true,
                                onClick = { viewModel.setDarkMode(true) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // AMOLED Black
            item {
                SettingCard {
                    SettingToggleRow(
                        title = "AMOLED Black",
                        subtitle = "Pure black surfaces in dark mode for OLED screens",
                        checked = settings.theme.useAmoledBlack,
                        onCheckedChange = { viewModel.setAmoledBlack(it) }
                    )
                }
            }

            // Dynamic Colors (Material You)
            item {
                SettingCard {
                    SettingToggleRow(
                        title = "Dynamic Colors",
                        subtitle = "Use colors from your wallpaper (Material You)",
                        checked = settings.theme.useDynamicColors,
                        onCheckedChange = { viewModel.setDynamicColors(it) }
                    )
                }
            }

            // Theme Colors
            item {
                SettingCard {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Theme Colors",
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        Text(
                            "Customize app accent colors",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        ThemeColorPicker(
                            title = "Primary",
                            currentColor = settings.theme.primaryColor,
                            onColorChange = { viewModel.setPrimaryColor(it) }
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 12.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                        )
                        ThemeColorPicker(
                            title = "Secondary",
                            currentColor = settings.theme.secondaryColor,
                            onColorChange = { viewModel.setSecondaryColor(it) }
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 12.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                        )
                        ThemeColorPicker(
                            title = "Tertiary",
                            currentColor = settings.theme.tertiaryColor,
                            onColorChange = { viewModel.setTertiaryColor(it) }
                        )
                    }
                }
            }

            // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
            // SECTION 2 — PLAYBACK
            // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
            item {
                SectionHeader(icon = Icons.Filled.MusicNote, title = "Playback")
            }

            // Playback Speed
            item {
                SettingCard {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 4.dp)
                        ) {
                            Icon(
                                Icons.Filled.Speed,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Playback Speed",
                                style = MaterialTheme.typography.titleSmall
                            )
                        }
                        Text(
                            "Adjust audio playback rate",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        val currentSpeed by viewModel.playbackSpeed.collectAsStateWithLifecycle()
                        var selectedSpeed by remember(currentSpeed) { mutableStateOf(currentSpeed) }

                        val speedsRow1 = listOf(0.75f, 0.85f, 0.95f, 1.0f, 1.05f)
                        val speedsRow2 = listOf(1.15f, 1.25f, 1.5f, 2.0f)

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            SpeedChipRow(
                                speeds = speedsRow1,
                                selectedSpeed = selectedSpeed,
                                onSpeedSelected = { speed ->
                                    selectedSpeed = speed
                                    viewModel.setPlaybackSpeed(speed)
                                }
                            )
                            SpeedChipRow(
                                speeds = speedsRow2,
                                selectedSpeed = selectedSpeed,
                                onSpeedSelected = { speed ->
                                    selectedSpeed = speed
                                    viewModel.setPlaybackSpeed(speed)
                                }
                            )
                        }
                    }
                }
            }

            // Gapless Playback
            item {
                SettingCard {
                    SettingToggleRow(
                        title = "Gapless Playback",
                        subtitle = "Seamless transitions between tracks with no silence gap",
                        checked = settings.playback.gaplessPlayback,
                        onCheckedChange = { viewModel.setGaplessPlayback(it) }
                    )
                }
            }

            // Skip Silence
            item {
                SettingCard {
                    SettingToggleRow(
                        title = "Skip Silence",
                        subtitle = "Automatically skip silent portions of audio tracks",
                        checked = settings.playback.skipSilence,
                        onCheckedChange = { viewModel.setSkipSilence(it) }
                    )
                }
            }

            // Auto-Resume
            item {
                SettingCard {
                    SettingToggleRow(
                        title = "Auto-Resume",
                        subtitle = "Resume playback when headphones are reconnected",
                        checked = settings.playback.autoResume,
                        onCheckedChange = { viewModel.setAutoResume(it) }
                    )
                }
            }

            // Remember Last Position
            item {
                SettingCard {
                    SettingToggleRow(
                        title = "Remember Position",
                        subtitle = "Restore playback position after app restart",
                        checked = settings.playback.rememberLastPosition,
                        onCheckedChange = { viewModel.setRememberLastPosition(it) }
                    )
                }
            }

            // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
            // SECTION 3 — LIBRARY
            // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
            item {
                SectionHeader(icon = Icons.Filled.LibraryMusic, title = "Library")
            }

            // Layout Mode
            item {
                SettingCard {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            "Layout Mode",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            FilterChip(
                                selected = settings.library.layoutMode == "LIST",
                                onClick = { viewModel.setLibraryLayoutMode("LIST") },
                                label = {
                                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                        Text("List", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
                                    }
                                },
                                modifier = Modifier.weight(1f).height(48.dp),
                                shape = RoundedCornerShape(24.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                border = null
                            )
                            FilterChip(
                                selected = settings.library.layoutMode == "GRID",
                                onClick = { viewModel.setLibraryLayoutMode("GRID") },
                                label = {
                                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                        Text("Grid", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
                                    }
                                },
                                modifier = Modifier.weight(1f).height(48.dp),
                                shape = RoundedCornerShape(24.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                border = null
                            )
                        }
                    }
                }
            }

            // Grid Size
            item {
                SettingCard {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            "Grid Size",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        Text(
                            "Number of columns in grid view",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            listOf(2, 3, 4).forEach { size ->
                                FilterChip(
                                    selected = settings.library.gridSize == size,
                                    onClick = { viewModel.setLibraryGridSize(size) },
                                    label = {
                                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                            Text("${size}x", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
                                        }
                                    },
                                    modifier = Modifier.weight(1f).height(48.dp),
                                    shape = RoundedCornerShape(24.dp),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    border = null
                                )
                            }
                        }
                    }
                }
            }

            // Minimum Track Duration
            item {
                SettingCard {
                    Column(modifier = Modifier.padding(20.dp)) {
                        val currentDuration = settings.library.minTrackDuration
                        var sliderValue by remember(currentDuration) {
                            mutableFloatStateOf(currentDuration.toFloat())
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Minimum Track Duration",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    "Hide tracks shorter than this from your library",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(start = 12.dp)
                            ) {
                                Text(
                                    "${sliderValue.toInt()}s",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Slider(
                            value = sliderValue,
                            onValueChange = { sliderValue = it },
                            onValueChangeFinished = {
                                viewModel.setMinTrackDuration(sliderValue.toInt())
                            },
                            valueRange = 0f..120f,
                            steps = 11,
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary,
                                activeTrackColor = MaterialTheme.colorScheme.primary,
                                inactiveTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                            )
                        )
                    }
                }
            }

            // Show Downloads Folder
            item {
                SettingCard {
                    SettingToggleRow(
                        title = "Include Downloads",
                        subtitle = "Show audio files from your Downloads folder",
                        checked = settings.library.showDownloadsFolder,
                        onCheckedChange = { viewModel.setShowDownloadsFolder(it) }
                    )
                }
            }

            // Rescan Library
            item {
                val isScanning by viewModel.isScanning.collectAsStateWithLifecycle()
                val scanResult by viewModel.scanResult.collectAsStateWithLifecycle()
                val scanProgress by viewModel.scanProgress.collectAsStateWithLifecycle()
                
                SettingCard {
                    Column(modifier = Modifier.padding(16.dp)) {
                        FilledTonalButton(
                            onClick = { viewModel.scanMedia() },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isScanning,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            if (isScanning) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                val phaseLabel = when (scanProgress.phase) {
                                    com.suspended.music.data.repository.ScanPhase.QUERYING -> "Loading library..."
                                    com.suspended.music.data.repository.ScanPhase.SCANNING -> {
                                        if (scanProgress.total > 0) "Scanning ${scanProgress.current}/${scanProgress.total}..."
                                        else "Scanning..."
                                    }
                                    com.suspended.music.data.repository.ScanPhase.ENRICHING -> {
                                        if (scanProgress.total > 0) "Reading metadata ${scanProgress.current}/${scanProgress.total}..."
                                        else "Reading metadata..."
                                    }
                                    com.suspended.music.data.repository.ScanPhase.AGGREGATING -> "Processing..."
                                    com.suspended.music.data.repository.ScanPhase.PERSISTING -> "Saving..."
                                    com.suspended.music.data.repository.ScanPhase.DONE -> "Scanning..."
                                }
                                Text(phaseLabel)
                            } else {
                                Icon(
                                    imageVector = Icons.Filled.Refresh,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Rescan Library")
                            }
                        }

                        // Show progress bar during scan
                        if (isScanning && scanProgress.fraction > 0f) {
                            Spacer(modifier = Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = { scanProgress.fraction },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp)),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        }
                        
                        // Show scan result feedback
                        if (scanResult != null && !isScanning) {
                            Spacer(modifier = Modifier.height(12.dp))
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            when (scanResult) {
                                is com.suspended.music.data.repository.ScanResult.Success -> {
                                    val result = scanResult as com.suspended.music.data.repository.ScanResult.Success
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Filled.CheckCircle,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                "Scan Complete",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                "${result.songCount} songs • ${result.albumCount} albums • ${result.artistCount} artists",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                                is com.suspended.music.data.repository.ScanResult.Error -> {
                                    val result = scanResult as com.suspended.music.data.repository.ScanResult.Error
                                    Text(
                                        "Error: ${result.message}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                                else -> {}
                            }
                        }
                    }
                }
            }

            // Permission Info
            item {
                SettingCard {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                Icons.Filled.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Permissions for Full Library Scanning",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "For comprehensive music discovery:",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    "• Android 13+: Audio file permission\n" +
                                    "• Android 11-12: \"All files access\" permission\n" +
                                    "• Android 10 & below: Storage permission",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(start = 0.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "Grant these in Settings → Apps → Permissions to enable full device scanning.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.tertiary,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }

            // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
            // SECTION 4 — NOTIFICATIONS & UPDATES
            // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
            item {
                SectionHeader(icon = Icons.Filled.Notifications, title = "Notifications & Updates")
            }

            // Album Art in Notification
            item {
                SettingCard {
                    SettingToggleRow(
                        title = "Album Art in Notification",
                        subtitle = "Display album artwork in the playback notification",
                        checked = settings.notification.showAlbumArt,
                        onCheckedChange = { viewModel.setShowAlbumArt(it) }
                    )
                }
            }

            // Compact Notification
            item {
                SettingCard {
                    SettingToggleRow(
                        title = "Compact Controls",
                        subtitle = "Show smaller notification with fewer buttons",
                        checked = settings.notification.compactControls,
                        onCheckedChange = { viewModel.setCompactControls(it) }
                    )
                }
            }

            // Auto-check Updates
            item {
                SettingCard {
                    SettingToggleRow(
                        title = "Auto-check Updates",
                        subtitle = "Notify when a new version is available on launch",
                        checked = settings.checkUpdatesOnStartup,
                        onCheckedChange = { viewModel.setCheckUpdatesOnStartup(it) }
                    )
                }
            }

            // Check Now Button
            item {
                SettingCard {
                    Column(modifier = Modifier.padding(16.dp)) {
                        FilledTonalButton(
                            onClick = {
                                isCheckingUpdates = true
                                viewModel.checkForUpdates()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isCheckingUpdates,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                Icons.Filled.Update,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (isCheckingUpdates) "Checking..." else "Check for Updates")
                        }
                    }
                }
            }

            // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
            // SECTION 5 — ABOUT
            // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
            item {
                SectionHeader(icon = Icons.Filled.Info, title = "About")
            }

            item {
                SettingCard {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "Pulse Music",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Version ${BuildConfig.VERSION_NAME}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        AboutRow(label = "Built with", value = "Jetpack Compose + Material 3")
                        AboutRow(label = "Audio Engine", value = "Media3 / ExoPlayer")
                        AboutRow(label = "Architecture", value = "MVVM + Hilt DI")
                        AboutRow(label = "Database", value = "Room + Paging 3")
                    }
                }
            }

            // GitHub link
            item {
                val context = LocalContext.current
                SettingCard {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val intent = Intent(
                                    Intent.ACTION_VIEW,
                                    android.net.Uri.parse("https://github.com/varun-r-boop/MusicPlayer")
                                )
                                context.startActivity(intent)
                            }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Source Code",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                "View on GitHub",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Icon(
                            Icons.Filled.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Bottom spacer for navigation bar clearance
            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }

    // Update notification dialog
    if (updateNotification != null) {
        isCheckingUpdates = false
        UpdateNotificationDialog(
            notification = updateNotification,
            onDismiss = { viewModel.dismissUpdateNotification() }
        )
    }
}

// ── Reusable Components ─────────────────────────────────────────────────

@Composable
private fun SectionHeader(icon: ImageVector, title: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(top = 20.dp, bottom = 8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun SettingCard(content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 1.dp
    ) {
        content()
    }
}

@Composable
private fun SettingToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    noPadding: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (noPadding) Modifier
                else Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 12.dp)
        ) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                checkedTrackColor = MaterialTheme.colorScheme.primary,
                uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                uncheckedBorderColor = MaterialTheme.colorScheme.outline
            )
        )
    }
}

@Composable
private fun AboutRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun ThemeModeChip(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val containerColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surfaceContainerHigh,
        label = "chip_bg"
    )
    val contentColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
        else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "chip_fg"
    )

    Surface(
        modifier = modifier.height(56.dp),
        shape = RoundedCornerShape(12.dp),
        color = containerColor,
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = contentColor,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = contentColor,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}

@Composable
private fun SpeedChipRow(
    speeds: List<Float>,
    selectedSpeed: Float,
    onSpeedSelected: (Float) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        speeds.forEach { speed ->
            val isSelected = selectedSpeed == speed
            val is1x = speed == 1.0f

            FilterChip(
                selected = isSelected,
                onClick = { onSpeedSelected(speed) },
                label = {
                    Text(
                        text = if (is1x) "1x" else "${String.format(java.util.Locale.getDefault(), "%.2f", speed)}x",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        maxLines = 1
                    )
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(10.dp),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                border = if (is1x && !isSelected) FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = false,
                    borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                ) else FilterChipDefaults.filterChipBorder(enabled = true, selected = isSelected)
            )
        }
    }
}

@Composable
fun ThemeColorPicker(
    title: String,
    currentColor: String,
    onColorChange: (String) -> Unit
) {
    val colors = listOf(
        "9D00FF" to "Violet",
        "E6B3FF" to "Lavender",
        "00D9FF" to "Cyan",
        "80F2FF" to "Aqua",
        "FF6B3D" to "Orange",
        "00DD88" to "Mint",
        "FF1744" to "Red",
        "FFD600" to "Gold"
    )

    Column {
        Text(
            title,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            colors.forEach { (hexColor, name) ->
                val parsedColor = Color(android.graphics.Color.parseColor("#$hexColor"))
                val isSelected = currentColor.equals(hexColor, ignoreCase = true)

                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(parsedColor)
                        .then(
                            if (isSelected) Modifier.border(
                                width = 3.dp,
                                color = MaterialTheme.colorScheme.onSurface,
                                shape = CircleShape
                            ) else Modifier
                        )
                        .clickable { onColorChange(hexColor) },
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Icon(
                            Icons.Filled.CheckCircle,
                            contentDescription = "Selected: $name",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun UpdateNotificationDialog(
    notification: com.suspended.music.ui.viewmodel.UpdateNotification?,
    onDismiss: () -> Unit
) {
    if (notification == null) return

    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        icon = {
            Icon(
                imageVector = if (notification.isNewVersion) Icons.Filled.Update
                else Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(40.dp)
            )
        },
        title = {
            Text(notification.title, style = MaterialTheme.typography.headlineSmall)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(notification.message, style = MaterialTheme.typography.bodyMedium)
                if (notification.version.isNotEmpty()) {
                    Text(
                        "Version: ${notification.version}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        },
        confirmButton = {
            if (notification.isNewVersion) {
                Button(
                    onClick = {
                        val intent = Intent(
                            Intent.ACTION_VIEW,
                            android.net.Uri.parse(notification.downloadUrl)
                        )
                        context.startActivity(intent)
                        onDismiss()
                    }
                ) {
                    Text("Download")
                }
            } else {
                Button(onClick = onDismiss) { Text("OK") }
            }
        },
        dismissButton = {
            if (notification.isNewVersion) {
                TextButton(onClick = onDismiss) { Text("Later") }
            }
        }
    )
}
