package com.suspended.music.ui.utils

import android.content.Context
import android.os.PowerManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class BatteryOptimizationManager {
    private val _isPowerSaveMode = MutableStateFlow(false)
    val isPowerSaveMode: StateFlow<Boolean> = _isPowerSaveMode
    
    private val _isLowBattery = MutableStateFlow(false)
    val isLowBattery: StateFlow<Boolean> = _isLowBattery
    
    fun updatePowerSaveState(context: Context) {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        _isPowerSaveMode.value = powerManager.isPowerSaveMode
        
        // Check battery level (you might need to register a battery receiver)
        // For now, using power save mode as proxy
        _isLowBattery.value = powerManager.isPowerSaveMode
    }
}

@Composable
fun BatteryAwareAnimation(
    isPlaying: Boolean,
    batteryOptimizationManager: BatteryOptimizationManager,
    normalAnimation: @Composable () -> Unit,
    reducedAnimation: @Composable () -> Unit
) {
    val isPowerSaveMode by batteryOptimizationManager.isPowerSaveMode.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        batteryOptimizationManager.updatePowerSaveState(context)
    }

    if (isPowerSaveMode) {
        reducedAnimation()
    } else {
        normalAnimation()
    }
}

// Animation presets for different battery modes
object BatteryOptimizedAnimations {
    const val NORMAL_ANIMATION_DURATION = 2000
    const val POWER_SAVE_ANIMATION_DURATION = 4000
    const val NORMAL_BLUR_RADIUS = 25f
    const val POWER_SAVE_BLUR_RADIUS = 10f
    const val NORMAL_FRAME_RATE = 60
    const val POWER_SAVE_FRAME_RATE = 30
}
