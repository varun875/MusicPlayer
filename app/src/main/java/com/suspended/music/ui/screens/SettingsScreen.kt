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
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Update
import androidx.compose.material.icons.outlined.BrightnessHigh
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.suspended.music.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val settings by viewModel.appSettings.collectAsState()
    val updateNotification by viewModel.updateNotification.collectAsState()
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
            // -- DISPLAY -----------------------------------------------
            item {
                SectionHeader(icon = Icons.Filled.Palette, title = "Display")
            }

            // Dark Mode
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

            // -- PLAYER ------------------------------------------------
            item {
                SectionHeader(icon = Icons.Filled.MusicNote, title = "Player")
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

                        val currentSpeed by viewModel.playbackSpeed.collectAsState()
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



            // -- GENERAL -----------------------------------------------
            item {
                SectionHeader(icon = Icons.Filled.Update, title = "General")
            }

            // Updates
            item {
                SettingCard {
                    Column(modifier = Modifier.padding(16.dp)) {
                        SettingToggleRow(
                            title = "Auto-check Updates",
                            subtitle = "Notify when a new version is available on launch",
                            checked = settings.checkUpdatesOnStartup,
                            onCheckedChange = { viewModel.setCheckUpdatesOnStartup(it) },
                            noPadding = true
                        )
                        Spacer(modifier = Modifier.height(12.dp))
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
                            Text(if (isCheckingUpdates) "Checking..." else "Check Now")
                        }
                    }
                }
            }

            // -- ABOUT -------------------------------------------------
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
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Version 1.4.0",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            "Material 3  |  Jetpack Compose",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
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

// --- Reusable Components ------------------------------------------------

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
