package com.suspended.music.data.preferences

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.suspended.music.data.model.AppSettings
import com.suspended.music.data.model.ScrobblingConfig
import com.suspended.music.data.model.ThemeConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(
        "app_settings",
        Context.MODE_PRIVATE
    )

    private val _settingsFlow = MutableStateFlow(loadSettings())
    val settingsFlow: StateFlow<AppSettings> = _settingsFlow.asStateFlow()

    // Dark Mode
    fun setDarkMode(isDark: Boolean?) {
        prefs.edit {
            if (isDark == null) {
                remove("dark_mode")
            } else {
                putBoolean("dark_mode", isDark)
            }
        }
        _settingsFlow.value = loadSettings()
    }

    fun isDarkMode(): Boolean? {
        return when {
            !prefs.contains("dark_mode") -> null
            else -> prefs.getBoolean("dark_mode", false)
        }
    }

    // Theme Colors
    fun setPrimaryColor(color: String) {
        prefs.edit { putString("primary_color", color) }
        _settingsFlow.value = loadSettings()
    }

    fun setSecondaryColor(color: String) {
        prefs.edit { putString("secondary_color", color) }
        _settingsFlow.value = loadSettings()
    }

    fun setTertiaryColor(color: String) {
        prefs.edit { putString("tertiary_color", color) }
        _settingsFlow.value = loadSettings()
    }

    fun setAmoledBlack(enabled: Boolean) {
        prefs.edit { putBoolean("amoled_black", enabled) }
        _settingsFlow.value = loadSettings()
    }

    fun getPrimaryColor(): String = prefs.getString("primary_color", "9D00FF") ?: "9D00FF"
    fun getSecondaryColor(): String = prefs.getString("secondary_color", "00D9FF") ?: "00D9FF"
    fun getTertiaryColor(): String = prefs.getString("tertiary_color", "FF6B3D") ?: "FF6B3D"

    // Scrobbling Settings
    fun setLastFmEnabled(enabled: Boolean) {
        prefs.edit { putBoolean("lastfm_enabled", enabled) }
        _settingsFlow.value = loadSettings()
    }

    fun setLastFmCredentials(username: String, apiKey: String) {
        prefs.edit {
            putString("lastfm_username", username)
            putString("lastfm_api_key", apiKey)
        }
        _settingsFlow.value = loadSettings()
    }

    fun setListenBrainzEnabled(enabled: Boolean) {
        prefs.edit { putBoolean("listenbrainz_enabled", enabled) }
        _settingsFlow.value = loadSettings()
    }

    fun setListenBrainzToken(token: String) {
        prefs.edit { putString("listenbrainz_token", token) }
        _settingsFlow.value = loadSettings()
    }

    // Update Check
    fun setCheckUpdatesOnStartup(enabled: Boolean) {
        prefs.edit { putBoolean("check_updates", enabled) }
        _settingsFlow.value = loadSettings()
    }

    fun setLastUpdateCheckTime(time: Long) {
        prefs.edit { putLong("last_update_check", time) }
    }

    fun getCheckUpdatesOnStartup(): Boolean = prefs.getBoolean("check_updates", true)
    fun getLastUpdateCheckTime(): Long = prefs.getLong("last_update_check", 0)

    fun setLibraryLayoutMode(mode: String) {
        prefs.edit { putString("library_layout_mode", mode) }
        _settingsFlow.value = loadSettings()
    }

    fun setLibraryGridSize(size: Int) {
        prefs.edit { putInt("library_grid_size", size) }
        _settingsFlow.value = loadSettings()
    }

    fun setLibraryListDensity(density: String) {
        prefs.edit { putString("library_list_density", density) }
        _settingsFlow.value = loadSettings()
    }
    
    private fun loadSettings(): AppSettings {
        return AppSettings(
            scrobbling = ScrobblingConfig(
                lastFmEnabled = prefs.getBoolean("lastfm_enabled", false),
                lastFmUsername = prefs.getString("lastfm_username", "") ?: "",
                lastFmApiKey = prefs.getString("lastfm_api_key", "") ?: "",
                listenBrainzEnabled = prefs.getBoolean("listenbrainz_enabled", false),
                listenBrainzUserToken = prefs.getString("listenbrainz_token", "") ?: ""
            ),
            theme = ThemeConfig(
                isDarkMode = if (prefs.contains("dark_mode")) prefs.getBoolean("dark_mode", false) else null,
                primaryColor = prefs.getString("primary_color", "9D00FF") ?: "9D00FF",
                secondaryColor = prefs.getString("secondary_color", "00D9FF") ?: "00D9FF",
                tertiaryColor = prefs.getString("tertiary_color", "FF6B3D") ?: "FF6B3D",
                useAmoledBlack = prefs.getBoolean("amoled_black", false)
            ),
            library = com.suspended.music.data.model.LibraryConfig(
                layoutMode = prefs.getString("library_layout_mode", "LIST") ?: "LIST",
                gridSize = prefs.getInt("library_grid_size", 2),
                listDensity = prefs.getString("library_list_density", "NORMAL") ?: "NORMAL"
            ),
            checkUpdatesOnStartup = prefs.getBoolean("check_updates", true),
            lastUpdateCheckTime = prefs.getLong("last_update_check", 0)
        )
    }
}
