package com.suspended.music.data.preferences

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.suspended.music.data.model.AppSettings
import com.suspended.music.data.model.LibraryConfig
import com.suspended.music.data.model.NotificationConfig
import com.suspended.music.data.model.PlaybackConfig
import com.suspended.music.data.model.ThemeConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Owns app settings: persisted to [SharedPreferences], exposed reactively via
 * [settingsFlow].
 *
 * The flow is the single source of truth at runtime. It is hydrated from disk
 * once at construction; every setter persists its value and then patches the
 * flow atomically (via [MutableStateFlow.update]), so reads can never diverge
 * from the last write and no setter triggers a full re-read of the store.
 */
class SettingsManager(private val prefs: SharedPreferences) {

    constructor(context: Context) : this(
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    )

    private companion object {
        const val PREFS_NAME = "app_settings"
    }

    /** Every preference key, defined once. A typo now fails to compile. */
    private object Keys {
        const val DARK_MODE = "dark_mode"
        const val PRIMARY_COLOR = "primary_color"
        const val SECONDARY_COLOR = "secondary_color"
        const val TERTIARY_COLOR = "tertiary_color"
        const val AMOLED_BLACK = "amoled_black"
        const val DYNAMIC_COLORS = "dynamic_colors"
        const val GAPLESS = "gapless_playback"
        const val AUTO_RESUME = "auto_resume"
        const val SKIP_SILENCE = "skip_silence"
        const val REMEMBER_POSITION = "remember_last_position"
        const val LAYOUT_MODE = "library_layout_mode"
        const val GRID_SIZE = "library_grid_size"
        const val LIST_DENSITY = "library_list_density"
        const val MIN_DURATION = "min_track_duration"
        const val SHOW_DOWNLOADS = "show_downloads_folder"
        const val NOTIF_ALBUM_ART = "notification_album_art"
        const val NOTIF_COMPACT = "notification_compact"
        const val CHECK_UPDATES = "check_updates"
        const val LAST_UPDATE_CHECK = "last_update_check"
        const val RECENT_SEARCHES = "recent_searches"
    }

    /** Every default, defined once — loader and getters stay in lockstep. */
    private object Defaults {
        const val RECENT_SEARCHES_MAX = 10
        const val PRIMARY_COLOR = "9D00FF"
        const val SECONDARY_COLOR = "00D9FF"
        const val TERTIARY_COLOR = "FF6B3D"
        const val AMOLED_BLACK = false
        const val DYNAMIC_COLORS = false
        const val GAPLESS = true
        const val AUTO_RESUME = false
        const val SKIP_SILENCE = false
        const val REMEMBER_POSITION = true
        const val LAYOUT_MODE = "LIST"
        const val GRID_SIZE = 2
        const val LIST_DENSITY = "NORMAL"
        const val MIN_DURATION = 0
        const val SHOW_DOWNLOADS = true
        const val NOTIF_ALBUM_ART = true
        const val NOTIF_COMPACT = false
        const val CHECK_UPDATES = true
        const val LAST_UPDATE_CHECK = 0L
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _settingsFlow = MutableStateFlow(AppSettings())
    val settingsFlow: StateFlow<AppSettings> = _settingsFlow.asStateFlow()

    init {
        // Hydrate off the main thread: SharedPreferences reads hit disk, and
        // this singleton is first injected from the main thread (via
        // hiltViewModel) at startup.
        scope.launch { _settingsFlow.value = loadSettings() }
    }

    // ── Theme ────────────────────────────────────────────────────────────

    /** Tri-state: `null` = follow system, `true` = dark, `false` = light. */
    fun setDarkMode(isDark: Boolean?) {
        prefs.edit {
            if (isDark == null) remove(Keys.DARK_MODE) else putBoolean(Keys.DARK_MODE, isDark)
        }
        updateTheme { it.copy(isDarkMode = isDark) }
    }

    fun isDarkMode(): Boolean? = settingsFlow.value.theme.isDarkMode

    fun setPrimaryColor(color: String) {
        prefs.edit { putString(Keys.PRIMARY_COLOR, color) }
        updateTheme { it.copy(primaryColor = color) }
    }

    fun setSecondaryColor(color: String) {
        prefs.edit { putString(Keys.SECONDARY_COLOR, color) }
        updateTheme { it.copy(secondaryColor = color) }
    }

    fun setTertiaryColor(color: String) {
        prefs.edit { putString(Keys.TERTIARY_COLOR, color) }
        updateTheme { it.copy(tertiaryColor = color) }
    }

    fun setAmoledBlack(enabled: Boolean) {
        prefs.edit { putBoolean(Keys.AMOLED_BLACK, enabled) }
        updateTheme { it.copy(useAmoledBlack = enabled) }
    }

    fun setDynamicColors(enabled: Boolean) {
        prefs.edit { putBoolean(Keys.DYNAMIC_COLORS, enabled) }
        updateTheme { it.copy(useDynamicColors = enabled) }
    }

    fun getPrimaryColor(): String = settingsFlow.value.theme.primaryColor
    fun getSecondaryColor(): String = settingsFlow.value.theme.secondaryColor
    fun getTertiaryColor(): String = settingsFlow.value.theme.tertiaryColor

    // ── Playback ─────────────────────────────────────────────────────────

    fun setGaplessPlayback(enabled: Boolean) {
        prefs.edit { putBoolean(Keys.GAPLESS, enabled) }
        updatePlayback { it.copy(gaplessPlayback = enabled) }
    }

    fun setAutoResume(enabled: Boolean) {
        prefs.edit { putBoolean(Keys.AUTO_RESUME, enabled) }
        updatePlayback { it.copy(autoResume = enabled) }
    }

    fun setSkipSilence(enabled: Boolean) {
        prefs.edit { putBoolean(Keys.SKIP_SILENCE, enabled) }
        updatePlayback { it.copy(skipSilence = enabled) }
    }

    fun setRememberLastPosition(enabled: Boolean) {
        prefs.edit { putBoolean(Keys.REMEMBER_POSITION, enabled) }
        updatePlayback { it.copy(rememberLastPosition = enabled) }
    }

    // ── Library ──────────────────────────────────────────────────────────

    fun setLibraryLayoutMode(mode: String) {
        prefs.edit { putString(Keys.LAYOUT_MODE, mode) }
        updateLibrary { it.copy(layoutMode = mode) }
    }

    fun setLibraryGridSize(size: Int) {
        val safe = size.coerceAtLeast(1)   // a zero/negative span would crash a grid
        prefs.edit { putInt(Keys.GRID_SIZE, safe) }
        updateLibrary { it.copy(gridSize = safe) }
    }

    fun setLibraryListDensity(density: String) {
        prefs.edit { putString(Keys.LIST_DENSITY, density) }
        updateLibrary { it.copy(listDensity = density) }
    }

    fun setMinTrackDuration(seconds: Int) {
        prefs.edit { putInt(Keys.MIN_DURATION, seconds) }
        updateLibrary { it.copy(minTrackDuration = seconds) }
    }

    fun setShowDownloadsFolder(enabled: Boolean) {
        prefs.edit { putBoolean(Keys.SHOW_DOWNLOADS, enabled) }
        updateLibrary { it.copy(showDownloadsFolder = enabled) }
    }

    // ── Notification ─────────────────────────────────────────────────────

    fun setShowAlbumArt(enabled: Boolean) {
        prefs.edit { putBoolean(Keys.NOTIF_ALBUM_ART, enabled) }
        updateNotification { it.copy(showAlbumArt = enabled) }
    }

    fun setCompactControls(enabled: Boolean) {
        prefs.edit { putBoolean(Keys.NOTIF_COMPACT, enabled) }
        updateNotification { it.copy(compactControls = enabled) }
    }

    // ── Updates ──────────────────────────────────────────────────────────

    fun setCheckUpdatesOnStartup(enabled: Boolean) {
        prefs.edit { putBoolean(Keys.CHECK_UPDATES, enabled) }
        _settingsFlow.update { it.copy(checkUpdatesOnStartup = enabled) }
    }

    fun setLastUpdateCheckTime(time: Long) {
        prefs.edit { putLong(Keys.LAST_UPDATE_CHECK, time) }
        _settingsFlow.update { it.copy(lastUpdateCheckTime = time) }
    }

    fun getCheckUpdatesOnStartup(): Boolean = settingsFlow.value.checkUpdatesOnStartup
    fun getLastUpdateCheckTime(): Long = settingsFlow.value.lastUpdateCheckTime

    // ── Recent searches ─────────────────────────────────────────────────

    /** Most-recent-first list of past search queries (max [Defaults.RECENT_SEARCHES_MAX]). */
    fun getRecentSearches(): List<String> {
        // getStringSet doesn't guarantee order, so each entry stores "index:value"
        // where index 0 is the most recent. Re-sort by that index here.
        return prefs.getStringSet(Keys.RECENT_SEARCHES, emptySet()).orEmpty()
            .mapNotNull { entry ->
                val idx = entry.indexOf(':')
                if (idx < 0) return@mapNotNull null
                val value = entry.substring(idx + 1).takeIf { it.isNotBlank() } ?: return@mapNotNull null
                val index = entry.substring(0, idx).toIntOrNull() ?: Int.MAX_VALUE
                index to value
            }
            .sortedBy { it.first }
            .map { it.second }
    }

    fun addRecentSearch(query: String) {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return
        val current = getRecentSearches().filterNot { it.equals(trimmed, ignoreCase = true) }
        val merged = (listOf(trimmed) + current).take(Defaults.RECENT_SEARCHES_MAX)
        prefs.edit { putStringSet(Keys.RECENT_SEARCHES, merged.mapIndexed { i, v -> "$i:$v" }.toSet()) }
    }

    fun clearRecentSearches() {
        prefs.edit { remove(Keys.RECENT_SEARCHES) }
    }

    // ── Internal ─────────────────────────────────────────────────────────

    private fun updateTheme(transform: (ThemeConfig) -> ThemeConfig) =
        _settingsFlow.update { it.copy(theme = transform(it.theme)) }

    private fun updatePlayback(transform: (PlaybackConfig) -> PlaybackConfig) =
        _settingsFlow.update { it.copy(playback = transform(it.playback)) }

    private fun updateLibrary(transform: (LibraryConfig) -> LibraryConfig) =
        _settingsFlow.update { it.copy(library = transform(it.library)) }

    private fun updateNotification(transform: (NotificationConfig) -> NotificationConfig) =
        _settingsFlow.update { it.copy(notification = transform(it.notification)) }

    /** Hydrates the flow from disk. Runs once, at construction. */
    private fun loadSettings(): AppSettings = AppSettings(
        theme = ThemeConfig(
            isDarkMode = if (prefs.contains(Keys.DARK_MODE)) {
                prefs.getBoolean(Keys.DARK_MODE, false)
            } else {
                null
            },
            primaryColor = prefs.getString(Keys.PRIMARY_COLOR, Defaults.PRIMARY_COLOR)
                ?: Defaults.PRIMARY_COLOR,
            secondaryColor = prefs.getString(Keys.SECONDARY_COLOR, Defaults.SECONDARY_COLOR)
                ?: Defaults.SECONDARY_COLOR,
            tertiaryColor = prefs.getString(Keys.TERTIARY_COLOR, Defaults.TERTIARY_COLOR)
                ?: Defaults.TERTIARY_COLOR,
            useAmoledBlack = prefs.getBoolean(Keys.AMOLED_BLACK, Defaults.AMOLED_BLACK),
            useDynamicColors = prefs.getBoolean(Keys.DYNAMIC_COLORS, Defaults.DYNAMIC_COLORS)
        ),
        playback = PlaybackConfig(
            gaplessPlayback = prefs.getBoolean(Keys.GAPLESS, Defaults.GAPLESS),
            autoResume = prefs.getBoolean(Keys.AUTO_RESUME, Defaults.AUTO_RESUME),
            skipSilence = prefs.getBoolean(Keys.SKIP_SILENCE, Defaults.SKIP_SILENCE),
            rememberLastPosition = prefs.getBoolean(Keys.REMEMBER_POSITION, Defaults.REMEMBER_POSITION)
        ),
        library = LibraryConfig(
            layoutMode = prefs.getString(Keys.LAYOUT_MODE, Defaults.LAYOUT_MODE)
                ?: Defaults.LAYOUT_MODE,
            gridSize = prefs.getInt(Keys.GRID_SIZE, Defaults.GRID_SIZE),
            listDensity = prefs.getString(Keys.LIST_DENSITY, Defaults.LIST_DENSITY)
                ?: Defaults.LIST_DENSITY,
            minTrackDuration = prefs.getInt(Keys.MIN_DURATION, Defaults.MIN_DURATION),
            showDownloadsFolder = prefs.getBoolean(Keys.SHOW_DOWNLOADS, Defaults.SHOW_DOWNLOADS)
        ),
        notification = NotificationConfig(
            showAlbumArt = prefs.getBoolean(Keys.NOTIF_ALBUM_ART, Defaults.NOTIF_ALBUM_ART),
            compactControls = prefs.getBoolean(Keys.NOTIF_COMPACT, Defaults.NOTIF_COMPACT)
        ),
        checkUpdatesOnStartup = prefs.getBoolean(Keys.CHECK_UPDATES, Defaults.CHECK_UPDATES),
        lastUpdateCheckTime = prefs.getLong(Keys.LAST_UPDATE_CHECK, Defaults.LAST_UPDATE_CHECK)
    )
}
