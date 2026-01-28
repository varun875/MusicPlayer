package com.suspended.music.ui.screens

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Update
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
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
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
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
    val switchColors = SwitchDefaults.colors(
        checkedThumbColor = MaterialTheme.colorScheme.primary,
        checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
        uncheckedThumbColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
        uncheckedBorderColor = Color.Transparent,
        checkedBorderColor = Color.Transparent
    )

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
                scrollBehavior = scrollBehavior
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Text(
                    "Display",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
                )
            }

            // Dark Mode Section
            item {
                SettingCard {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Dark Mode", style = MaterialTheme.typography.bodyLarge)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            DarkModeOption(
                                label = "System",
                                isSelected = settings.theme.isDarkMode == null,
                                onClick = { viewModel.setDarkMode(null) },
                                modifier = Modifier.weight(1f)
                            )
                            DarkModeOption(
                                label = "Light",
                                isSelected = settings.theme.isDarkMode == false,
                                onClick = { viewModel.setDarkMode(false) },
                                modifier = Modifier.weight(1f)
                            )
                            DarkModeOption(
                                label = "Dark",
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
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("AMOLED Black", style = MaterialTheme.typography.bodyLarge)
                            Text(
                                text = "Use pure black surfaces in dark mode",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                        Switch(
                            checked = settings.theme.useAmoledBlack,
                            onCheckedChange = { viewModel.setAmoledBlack(it) },
                            colors = switchColors
                        )
                    }
                }
            }

            // Theme Customization
            item {
                SettingCard {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Theme Colors",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        ThemeColorPicker(
                            title = "Primary Color",
                            currentColor = settings.theme.primaryColor,
                            onColorChange = { viewModel.setPrimaryColor(it) }
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        ThemeColorPicker(
                            title = "Secondary Color",
                            currentColor = settings.theme.secondaryColor,
                            onColorChange = { viewModel.setSecondaryColor(it) }
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        ThemeColorPicker(
                            title = "Tertiary Color",
                            currentColor = settings.theme.tertiaryColor,
                            onColorChange = { viewModel.setTertiaryColor(it) }
                        )
                    }
                }
            }

            item {
                Text(
                    "Scrobbling",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
                )
            }

            // Last.fm
            item {
                SettingCard {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Last.fm", style = MaterialTheme.typography.bodyLarge)
                            Switch(
                                checked = settings.scrobbling.lastFmEnabled,
                                onCheckedChange = { viewModel.setLastFmEnabled(it) },
                                colors = switchColors
                            )
                        }
                        if (settings.scrobbling.lastFmEnabled) {
                            Spacer(modifier = Modifier.height(12.dp))
                            var username by remember {
                                mutableStateOf(settings.scrobbling.lastFmUsername)
                            }
                            var apiKey by remember {
                                mutableStateOf(settings.scrobbling.lastFmApiKey)
                            }
                            TextField(
                                value = username,
                                onValueChange = { username = it },
                                label = { Text("Username") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            TextField(
                                value = apiKey,
                                onValueChange = { apiKey = it },
                                label = { Text("API Key") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = {
                                    viewModel.setLastFmCredentials(username, apiKey)
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Save Credentials")
                            }
                        }
                    }
                }
            }

            // ListenBrainz
            item {
                SettingCard {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("ListenBrainz", style = MaterialTheme.typography.bodyLarge)
                            Switch(
                                checked = settings.scrobbling.listenBrainzEnabled,
                                onCheckedChange = { viewModel.setListenBrainzEnabled(it) },
                                colors = switchColors
                            )
                        }
                        if (settings.scrobbling.listenBrainzEnabled) {
                            Spacer(modifier = Modifier.height(12.dp))
                            var token by remember {
                                mutableStateOf(settings.scrobbling.listenBrainzUserToken)
                            }
                            TextField(
                                value = token,
                                onValueChange = { token = it },
                                label = { Text("User Token") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = { viewModel.setListenBrainzToken(token) },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Save Token")
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    "Player",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
                )
            }

            // Playback Speed
            item {
                SettingCard {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Playback Speed",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        
                        var selectedSpeed by remember { mutableStateOf(1.0f) }
                        val speeds = listOf(0.75f, 0.85f, 0.95f, 1.0f, 1.05f, 1.15f, 1.25f, 1.5f, 2.0f)
                        
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            speeds.forEach { speed ->
                                val isSelected = selectedSpeed == speed
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (isSelected) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.surfaceVariant
                                        )
                                        .clickable { selectedSpeed = speed; viewModel.setPlaybackSpeed(speed) }
                                        .padding(8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        "${String.format("%.2f", speed)}x",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    "General",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
                )
            }

            // Updates
            item {
                SettingCard {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Check for Updates", style = MaterialTheme.typography.bodyLarge)
                            Switch(
                                checked = settings.checkUpdatesOnStartup,
                                onCheckedChange = { viewModel.setCheckUpdatesOnStartup(it) },
                                colors = switchColors
                            )
                        }
                        Button(
                            onClick = {
                                isCheckingUpdates = true
                                viewModel.checkForUpdates()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            enabled = !isCheckingUpdates
                        ) {
                            Icon(Icons.Filled.Update, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                            Text(if (isCheckingUpdates) "Checking..." else "Check Now")
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
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

@Composable
fun SettingCard(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        content()
    }
}

@Composable
fun DarkModeOption(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            )
            .clickable(onClick = onClick)
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        RadioButton(selected = isSelected, onClick = onClick)
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
fun ThemeColorPicker(
    title: String,
    currentColor: String,
    onColorChange: (String) -> Unit
) {
    val colors = listOf(
        "9D00FF", // Expressive Primary
        "E6B3FF", // Expressive Primary Light
        "00D9FF", // Expressive Secondary
        "80F2FF", // Expressive Secondary Light
        "FF6B3D", // Expressive Tertiary
        "FFB399", // Expressive Tertiary Light
        "00DD88", // Expressive Green
        "FF1744"  // Red
    )

    Column {
        Text(title, style = MaterialTheme.typography.labelMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            colors.forEach { hexColor ->
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(android.graphics.Color.parseColor("#$hexColor")))
                        .clickable { onColorChange(hexColor) }
                        .padding(2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (currentColor.equals(hexColor, ignoreCase = true)) {
                        Icon(
                            Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
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
        icon = {
            if (notification.isNewVersion) {
                Icon(
                    Icons.Filled.Update,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(40.dp)
                )
            } else {
                Icon(
                    Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(40.dp)
                )
            }
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
                        // Open download link
                        val intent = Intent(
                            Intent.ACTION_VIEW,
                            android.net.Uri.parse(notification.downloadUrl)
                        )
                        ContextCompat.startActivity(context, intent, null)
                        onDismiss()
                    }
                ) {
                    Text("Download")
                }
            } else {
                Button(onClick = onDismiss) {
                    Text("OK")
                }
            }
        },
        dismissButton = {
            if (notification.isNewVersion) {
                TextButton(onClick = onDismiss) {
                    Text("Later")
                }
            }
        }
    )
}
