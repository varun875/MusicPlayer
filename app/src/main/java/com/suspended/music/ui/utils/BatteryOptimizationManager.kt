package com.suspended.music.ui.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.PowerManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

// ══════════════════════════════════════════════
// #6: Implemented as a Singleton (object) for best lifecycle management.
// Call initialize(applicationContext) once in your Application class or MainActivity.
// ══════════════════════════════════════════════
object BatteryOptimizationManager {
    private val _isPowerSaveMode = MutableStateFlow(false)
    val isPowerSaveMode: StateFlow<Boolean> = _isPowerSaveMode

    // #3: Properly implemented low battery state
    private val _isLowBattery = MutableStateFlow(false)
    val isLowBattery: StateFlow<Boolean> = _isLowBattery

    private var isInitialized = false

    /**
     * Call once from your Application class or main Activity to start listening
     * for system battery and power-save state changes.
     */
    fun initialize(appContext: Context) {
        if (isInitialized) return
        isInitialized = true

        // Initial check
        refreshPowerState(appContext)

        // Register receivers for reactive updates
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                when (intent.action) {
                    PowerManager.ACTION_POWER_SAVE_MODE_CHANGED,
                    Intent.ACTION_BATTERY_CHANGED -> refreshPowerState(context)
                }
            }
        }

        val filter = IntentFilter().apply {
            addAction(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED)
            addAction(Intent.ACTION_BATTERY_CHANGED)
        }

        appContext.registerReceiver(receiver, filter)
    }

    // #8: Renamed function to better match its responsibility
    fun refreshPowerState(context: Context) {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        _isPowerSaveMode.value = powerManager.isPowerSaveMode

        // #3: Actual battery level check using BatteryManager
        val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        val level = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        
        // Treat <= 15% as low battery, handle unknown states safely
        _isLowBattery.value = level in 0..15
    }
}

@Composable
fun BatteryAwareAnimation(
    // Removed unused `isPlaying` parameter
    batteryOptimizationManager: BatteryOptimizationManager = BatteryOptimizationManager,
    normalAnimation: @Composable () -> Unit,
    reducedAnimation: @Composable () -> Unit
) {
    val isPowerSaveMode by batteryOptimizationManager.isPowerSaveMode.collectAsStateWithLifecycle()

    // Removed LaunchedEffect because the Singleton handles its own reactive updates
    // via BroadcastReceiver from the moment initialize() is called.
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
    
    // Removed frame rate constants as they cannot be enforced by Compose
}