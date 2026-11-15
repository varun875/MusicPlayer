package com.suspended.music.player

import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.PresetReverb
import android.media.audiofx.Virtualizer
import android.util.Log
import androidx.media3.common.util.UnstableApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@UnstableApi
@Singleton
class AudioEffectController @Inject constructor(
    private val musicPlayer: MusicPlayer
) {
    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null
    
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    // EQ State
    private val _bands = MutableStateFlow<List<EqBand>>(emptyList())
    val bands: StateFlow<List<EqBand>> = _bands.asStateFlow()
    
    private val _presets = MutableStateFlow<List<String>>(emptyList())
    val presets: StateFlow<List<String>> = _presets.asStateFlow()
    
    private val _currentPreset = MutableStateFlow(0)
    val currentPreset: StateFlow<Int> = _currentPreset.asStateFlow()
    
    private val _bassBoostStrength = MutableStateFlow(0)
    val bassBoostStrength: StateFlow<Int> = _bassBoostStrength.asStateFlow()
    
    private val _virtualizerStrength = MutableStateFlow(0)
    val virtualizerStrength: StateFlow<Int> = _virtualizerStrength.asStateFlow()
    
    private var currentSessionId = 0

    // Remember user choices so they survive an audio-session change (a new session
    // resets the effects to hardware defaults).
    private var userPreset: Int? = null
    private val userBandLevels = HashMap<Int, Int>()
    private var userBassBoost: Int? = null
    private var userVirtualizer: Int? = null
    
    init {
        scope.launch {
            musicPlayer.audioSessionId.collectLatest { sessionId ->
                if (sessionId != 0 && sessionId != currentSessionId) {
                    currentSessionId = sessionId
                    releaseEffects()
                    initEffects(sessionId)
                }
            }
        }
    }
    
    private fun initEffects(sessionId: Int) {
        try {
            // Initialize Equalizer. Use a local first so a failure inside
            // `apply { enabled = true }` can't strand the constructed effect
            // unreleased — released-effects ownership is transferred only after
            // successful enable.
            val eq = Equalizer(0, sessionId)
            try { eq.enabled = true } catch (t: Throwable) { eq.release(); throw t }
            equalizer = eq

            // Extract bands
            val numBands = equalizer?.numberOfBands ?: 0
            val minLevel = equalizer?.bandLevelRange?.get(0) ?: 0
            val maxLevel = equalizer?.bandLevelRange?.get(1) ?: 0
            
            val newBands = mutableListOf<EqBand>()
            for (i in 0 until numBands) {
                val centerFreq = equalizer?.getCenterFreq(i.toShort()) ?: 0
                val level = equalizer?.getBandLevel(i.toShort()) ?: 0
                newBands.add(
                    EqBand(
                        id = i,
                        centerFreq = centerFreq / 1000,
                        level = level.toInt(),
                        minLevel = minLevel.toInt(),
                        maxLevel = maxLevel.toInt()
                    )
                )
            }
            _bands.value = newBands
            
            // Extract presets
            val numPresets = equalizer?.numberOfPresets ?: 0
            val presetList = mutableListOf<String>()
            for (i in 0 until numPresets) {
                presetList.add(equalizer?.getPresetName(i.toShort()) ?: "Preset $i")
            }
            _presets.value = presetList
            _currentPreset.value = equalizer?.currentPreset?.toInt() ?: 0
            
            // Initialize BassBoost
            val bb = BassBoost(0, sessionId)
            try { bb.enabled = true } catch (t: Throwable) { bb.release(); throw t }
            bassBoost = bb
            _bassBoostStrength.value = bassBoost?.roundedStrength?.toInt() ?: 0

            // Initialize Virtualizer
            val vz = Virtualizer(0, sessionId)
            try { vz.enabled = true } catch (t: Throwable) { vz.release(); throw t }
            virtualizer = vz
            _virtualizerStrength.value = virtualizer?.roundedStrength?.toInt() ?: 0
            
            // Re-apply the user's settings onto the freshly created effects.
            userPreset?.let { equalizer?.usePreset(it.toShort()) }
            if (userPreset == null) {
                userBandLevels.forEach { (id, level) -> equalizer?.setBandLevel(id.toShort(), level.toShort()) }
            }
            userBassBoost?.let { bassBoost?.setStrength(it.toShort()) }
            userVirtualizer?.let { virtualizer?.setStrength(it.toShort()) }

            // Reflect the restored values back into the exposed state.
            val restoredBands = _bands.value.toMutableList()
            for (i in 0 until numBands) {
                if (i < restoredBands.size) {
                    val level = equalizer?.getBandLevel(i.toShort()) ?: 0
                    restoredBands[i] = restoredBands[i].copy(level = level.toInt())
                }
            }
            _bands.value = restoredBands
            _currentPreset.value = equalizer?.currentPreset?.toInt() ?: 0
            _bassBoostStrength.value = bassBoost?.roundedStrength?.toInt() ?: 0
            _virtualizerStrength.value = virtualizer?.roundedStrength?.toInt() ?: 0

            Log.d("AudioEffectController", "Effects initialized for session $sessionId")
            
        } catch (e: Exception) {
            Log.e("AudioEffectController", "Error initializing effects", e)
        }
    }
    
    fun setBandLevel(bandId: Int, level: Int) {
        equalizer?.setBandLevel(bandId.toShort(), level.toShort())
        // A manual band change means the preset no longer describes the EQ.
        userPreset = null
        userBandLevels[bandId] = level
        // Update local state to reflect change
        val currentBands = _bands.value.toMutableList()
        val index = currentBands.indexOfFirst { it.id == bandId }
        if (index != -1) {
            currentBands[index] = currentBands[index].copy(level = level)
            _bands.value = currentBands
        }
    }
    
    fun usePreset(presetIndex: Int) {
        equalizer?.usePreset(presetIndex.toShort())
        userPreset = presetIndex
        userBandLevels.clear()
        _currentPreset.value = presetIndex
        // Refresh bands from equalizer as they have changed
        val numBands = equalizer?.numberOfBands ?: 0
        val currentBands = _bands.value.toMutableList()
        for (i in 0 until numBands) {
            val level = equalizer?.getBandLevel(i.toShort()) ?: 0
            if (i < currentBands.size) {
                currentBands[i] = currentBands[i].copy(level = level.toInt())
            }
        }
        _bands.value = currentBands
    }
    
    fun setBassBoost(strength: Int) {
        bassBoost?.setStrength(strength.toShort())
        userBassBoost = strength
        _bassBoostStrength.value = strength
    }
    
    fun setVirtualizer(strength: Int) {
        virtualizer?.setStrength(strength.toShort())
        userVirtualizer = strength
        _virtualizerStrength.value = strength
    }
    
    private fun releaseEffects() {
        equalizer?.release()
        bassBoost?.release()
        virtualizer?.release()
        equalizer = null
        bassBoost = null
        virtualizer = null
    }

    /**
     * Releases all audio effect resources to prevent memory leaks
     */
    fun release() {
        releaseEffects()
        scope.cancel()
    }
}

data class EqBand(
    val id: Int,
    val centerFreq: Int, // In Hz
    val level: Int,
    val minLevel: Int,
    val maxLevel: Int
)
