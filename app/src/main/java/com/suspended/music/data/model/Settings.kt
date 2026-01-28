package com.suspended.music.data.model

data class ScrobblingConfig(
    val lastFmEnabled: Boolean = false,
    val lastFmUsername: String = "",
    val lastFmApiKey: String = "",
    val listenBrainzEnabled: Boolean = false,
    val listenBrainzUserToken: String = ""
)

data class ThemeConfig(
    val isDarkMode: Boolean? = null, // null = follow system
    val primaryColor: String = "9D00FF", // Expressive Primary
    val secondaryColor: String = "00D9FF", // Expressive Secondary
    val tertiaryColor: String = "FF6B3D", // Expressive Tertiary
    val useAmoledBlack: Boolean = false
)

data class LibraryConfig(
    val layoutMode: String = "GRID", // GRID or LIST
    val gridSize: Int = 2,
    val listDensity: String = "NORMAL" // COMPACT or NORMAL
)

data class AppSettings(
    val scrobbling: ScrobblingConfig = ScrobblingConfig(),
    val theme: ThemeConfig = ThemeConfig(),
    val library: LibraryConfig = LibraryConfig(),
    val checkUpdatesOnStartup: Boolean = true,
    val lastUpdateCheckTime: Long = 0
)
