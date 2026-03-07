package com.suspended.music.data.preferences

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.suspended.music.data.model.AppSettings
import com.suspended.music.data.model.LibraryConfig
import com.suspended.music.data.model.NotificationConfig
import com.suspended.music.data.model.PlaybackConfig
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

    // ── Theme ────────────────────────────────────────────────────────────

    fun setDarkMode(isDark: Boolean?) {
        prefs.edit {
            if (isDark == null) remove("dark_mode")
            else putBoolean("dark_mode", isDark)
        }
        reload()
    }

    fun isDarkMode(): Boolean? {
        return if (!prefs.contains("dark_mode")) null
        else prefs.getBoolean("dark_mode", false)
    }

    fun setPrimaryColor(color: String) {
        prefs.edit { putString("primary_color", color) }
        reload()
    }

    fun setSecondaryColor(color: String) {
        prefs.edit { putString("secondary_color", color) }
        reload()
    }

    fun setTertiaryColor(color: String) {
        prefs.edit { putString("tertiary_color", color) }
        reload()
    }

    fun setAmoledBlack(enabled: Boolean) {
        prefs.edit { putBoolean("amoled_black", enabled) }
        reload()
    }

    fun setDynamicColors(enabled: Boolean) {
        prefs.edit { putBoolean("dynamic_colors", enabled) }
        reload()
    }

    fun getPrimaryColor(): String = prefs.getString("primary_color", "9D00FF") ?: "9D00FF"
    fun getSecondaryColor(): String = prefs.getString("secondary_color", "00D9FF") ?: "00D9FF"
    fun getTertiaryColor(): String = prefs.getString("tertiary_color", "FF6B3D") ?: "FF6B3D"

    // ── Playback ─────────────────────────────────────────────────────────

    fun setGaplessPlayback(enabled: Boolean) {
        prefs.edit { putBoolean("gapless_playback", enabled) }
        reload()
    }

    fun setAutoResume(enabled: Boolean) {
        prefs.edit { putBoolean("auto_resume", enabled) }
        reload()
    }

    fun setSkipSilence(enabled: Boolean) {
        prefs.edit { putBoolean("skip_silence", enabled) }
        reload()
    }

    fun setRememberLastPosition(enabled: Boolean) {
        prefs.edit { putBoolean("remember_last_position", enabled) }
        reload()
    }

    // ── Library ──────────────────────────────────────────────────────────

    fun setLibraryLayoutMode(mode: String) {
        prefs.edit { putString("library_layout_mode", mode) }
        reload()
    }

    fun setLibraryGridSize(size: Int) {
        prefs.edit { putInt("library_grid_size", size) }
        reload()
    }

    fun setLibraryListDensity(density: String) {
        prefs.edit { putString("library_list_density", density) }
        reload()
    }

    fun setMinTrackDuration(seconds: Int) {
        prefs.edit { putInt("min_track_duration", seconds) }
        reload()
    }

    fun setShowDownloadsFolder(enabled: Boolean) {
        prefs.edit { putBoolean("show_downloads_folder", enabled) }
        reload()
    }

    // ── Notification ─────────────────────────────────────────────────────

    fun setShowAlbumArt(enabled: Boolean) {
        prefs.edit { putBoolean("notification_album_art", enabled) }
        reload()
    }

    fun setCompactControls(enabled: Boolean) {
        prefs.edit { putBoolean("notification_compact", enabled) }
        reload()
    }

    // ── Updates ──────────────────────────────────────────────────────────

    fun setCheckUpdatesOnStartup(enabled: Boolean) {
        prefs.edit { putBoolean("check_updates", enabled) }
        reload()
    }

    fun setLastUpdateCheckTime(time: Long) {
        prefs.edit { putLong("last_update_check", time) }
    }

    fun getCheckUpdatesOnStartup(): Boolean = prefs.getBoolean("check_updates", true)
    fun getLastUpdateCheckTime(): Long = prefs.getLong("last_update_check", 0)

    // ── Internal ─────────────────────────────────────────────────────────

    private fun reload() {
        _settingsFlow.value = loadSettings()
    }

    private fun loadSettings(): AppSettings {
        return AppSettings(
            theme = ThemeConfig(
                isDarkMode = if (prefs.contains("dark_mode")) prefs.getBoolean("dark_mode", false) else null,
                primaryColor = prefs.getString("primary_color", "9D00FF") ?: "9D00FF",
                secondaryColor = prefs.getString("secondary_color", "00D9FF") ?: "00D9FF",
                tertiaryColor = prefs.getString("tertiary_color", "FF6B3D") ?: "FF6B3D",
                useAmoledBlack = prefs.getBoolean("amoled_black", false),
                useDynamicColors = prefs.getBoolean("dynamic_colors", false)
            ),
            playback = PlaybackConfig(
                gaplessPlayback = prefs.getBoolean("gapless_playback", true),
                autoResume = prefs.getBoolean("auto_resume", false),
                skipSilence = prefs.getBoolean("skip_silence", false),
                rememberLastPosition = prefs.getBoolean("remember_last_position", true)
            ),
            library = LibraryConfig(
                layoutMode = prefs.getString("library_layout_mode", "LIST") ?: "LIST",
                gridSize = prefs.getInt("library_grid_size", 2),
                listDensity = prefs.getString("library_list_density", "NORMAL") ?: "NORMAL",
                minTrackDuration = prefs.getInt("min_track_duration", 30),
                showDownloadsFolder = prefs.getBoolean("show_downloads_folder", true)
            ),
            notification = NotificationConfig(
                showAlbumArt = prefs.getBoolean("notification_album_art", true),
                compactControls = prefs.getBoolean("notification_compact", false)
            ),
            checkUpdatesOnStartup = prefs.getBoolean("check_updates", true),
            lastUpdateCheckTime = prefs.getLong("last_update_check", 0)
        )
    }
}
