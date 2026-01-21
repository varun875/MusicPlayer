package com.suspended.music.player

import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.PresetReverb
import android.media.audiofx.Virtualizer
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AudioEffectController @Inject constructor(
    private val musicPlayer: MusicPlayer
) {
    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null
    
    private val scope = CoroutineScope(Dispatchers.Main + Job())
    
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
            // Initialize Equalizer
            equalizer = Equalizer(0, sessionId).apply {
                enabled = true
            }
            
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
            bassBoost = BassBoost(0, sessionId).apply {
                enabled = true
            }
            _bassBoostStrength.value = bassBoost?.roundedStrength?.toInt() ?: 0
            
            // Initialize Virtualizer
            virtualizer = Virtualizer(0, sessionId).apply {
                enabled = true
            }
            _virtualizerStrength.value = virtualizer?.roundedStrength?.toInt() ?: 0
            
            Log.d("AudioEffectController", "Effects initialized for session $sessionId")
            
        } catch (e: Exception) {
            Log.e("AudioEffectController", "Error initializing effects", e)
        }
    }
    
    fun setBandLevel(bandId: Int, level: Int) {
        equalizer?.setBandLevel(bandId.toShort(), level.toShort())
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
        _bassBoostStrength.value = strength
    }
    
    fun setVirtualizer(strength: Int) {
        virtualizer?.setStrength(strength.toShort())
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
}

data class EqBand(
    val id: Int,
    val centerFreq: Int, // In Hz
    val level: Int,
    val minLevel: Int,
    val maxLevel: Int
)
