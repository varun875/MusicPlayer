package com.suspended.music.data.model

data class ThemeConfig(
    val isDarkMode: Boolean? = null, // null = follow system
    val primaryColor: String = "9D00FF",
    val secondaryColor: String = "00D9FF",
    val tertiaryColor: String = "FF6B3D",
    val useAmoledBlack: Boolean = false,
    val useDynamicColors: Boolean = false
)

data class PlaybackConfig(
    val gaplessPlayback: Boolean = true,
    val autoResume: Boolean = false,
    val skipSilence: Boolean = false,
    val rememberLastPosition: Boolean = true
)

data class LibraryConfig(
    val layoutMode: String = "LIST",   // GRID or LIST
    val gridSize: Int = 2,
    val listDensity: String = "NORMAL", // COMPACT or NORMAL
    val minTrackDuration: Int = 30,     // seconds
    val showDownloadsFolder: Boolean = true
)

data class NotificationConfig(
    val showAlbumArt: Boolean = true,
    val compactControls: Boolean = false
)

data class AppSettings(
    val theme: ThemeConfig = ThemeConfig(),
    val playback: PlaybackConfig = PlaybackConfig(),
    val library: LibraryConfig = LibraryConfig(),
    val notification: NotificationConfig = NotificationConfig(),
    val checkUpdatesOnStartup: Boolean = true,
    val lastUpdateCheckTime: Long = 0
)
