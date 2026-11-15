package com.suspended.music.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import com.suspended.music.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EqualizerScreen(
    viewModel: MainViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Genuine back navigation is handled by NavDisplay's predictive-back /
    // onBack callback; a local BackHandler would double-pop.

    val bands by viewModel.eqBands.collectAsStateWithLifecycle()
    val presets by viewModel.eqPresets.collectAsStateWithLifecycle()
    val currentPreset by viewModel.eqCurrentPreset.collectAsStateWithLifecycle()
    val bassBoost by viewModel.bassBoostStrength.collectAsStateWithLifecycle()
    val virtualizer by viewModel.virtualizerStrength.collectAsStateWithLifecycle()
    
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(
        rememberTopAppBarState()
    )
    
    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text("Equalizer") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            
            // Audio Effects Section
            EffectSlider(
                title = "Bass Boost",
                value = bassBoost,
                onValueChange = { viewModel.setBassBoost(it) },
                range = 0f..1000f,
                valueLabel = { "${(it / 10).toInt()}%" }
            )
            
            EffectSlider(
                title = "3D Virtualizer",
                value = virtualizer,
                onValueChange = { viewModel.setVirtualizer(it) },
                range = 0f..1000f,
                valueLabel = { "${(it / 10).toInt()}%" }
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Presets Header
            Text(
                text = "Presets",
                style = MaterialTheme.typography.titleLarge
            )
            
            // Prescription Buttons Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                presets.forEachIndexed { index, name ->
                    val isSelected = index == currentPreset
                    Button(
                        onClick = { viewModel.setEqPreset(index) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                            contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = if (isSelected) 8.dp else 4.dp,
                            pressedElevation = 12.dp
                        )
                    ) {
                        Text(name, style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // EQ Bands (Simplified: Horizontal for now to ensure build)
            if (bands.isNotEmpty()) {
                Column {
                    bands.forEach { band ->
                        val freqLabel = if (band.centerFreq >= 1000) "${band.centerFreq / 1000}k" else "${band.centerFreq}"
                        EffectSlider(
                            title = "$freqLabel Hz",
                            value = band.level,
                            onValueChange = { viewModel.setEqBandLevel(band.id, it) },
                            range = band.minLevel.toFloat()..band.maxLevel.toFloat(),
                            valueLabel = { "${(it / 100).toInt()}dB" }
                        )
                    }
                }
            } else {
                Text("Equalizer not available")
            }
            
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
fun EffectSlider(
    title: String,
    value: Int,
    onValueChange: (Int) -> Unit,
    range: ClosedFloatingPointRange<Float>,
    valueLabel: (Float) -> String
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                valueLabel(value.toFloat()),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Slider(
            value = value.toFloat(),
            onValueChange = { onValueChange(it.toInt()) },
            valueRange = range,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        )
    }
}
