package com.suspended.music.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Album
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.QueueMusic
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector? = null,
    val unselectedIcon: ImageVector? = null
) {
    // Main navigation destinations
    data object Home : Screen(
        route = "home",
        title = "Home",
        selectedIcon = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home
    )
    
    data object Songs : Screen(
        route = "songs",
        title = "Songs",
        selectedIcon = Icons.Filled.LibraryMusic,
        unselectedIcon = Icons.Outlined.LibraryMusic
    )
    
    data object Albums : Screen(
        route = "albums",
        title = "Albums",
        selectedIcon = Icons.Filled.Album,
        unselectedIcon = Icons.Outlined.Album
    )
    
    data object Artists : Screen(
        route = "artists",
        title = "Artists",
        selectedIcon = Icons.Filled.Person,
        unselectedIcon = Icons.Outlined.Person
    )
    
    data object Playlists : Screen(
        route = "playlists",
        title = "Playlists",
        selectedIcon = Icons.Filled.QueueMusic,
        unselectedIcon = Icons.Outlined.QueueMusic
    )
    
    data object Folders : Screen(
        route = "folders",
        title = "Folders",
        selectedIcon = Icons.Filled.Folder,
        unselectedIcon = Icons.Outlined.Folder
    )
    
    data object Settings : Screen(
        route = "settings",
        title = "Settings",
        selectedIcon = Icons.Filled.Settings,
        unselectedIcon = Icons.Outlined.Settings
    )
    
    // Detail screens
    data object AlbumDetail : Screen(
        route = "album/{albumId}",
        title = "Album"
    ) {
        fun createRoute(albumId: Long) = "album/$albumId"
    }
    
    data object ArtistDetail : Screen(
        route = "artist/{artistId}",
        title = "Artist"
    ) {
        fun createRoute(artistId: Long) = "artist/$artistId"
    }
    
    data object PlaylistDetail : Screen(
        route = "playlist/{playlistId}",
        title = "Playlist"
    ) {
        fun createRoute(playlistId: Long) = "playlist/$playlistId"
    }
    
    data object FolderDetail : Screen(
        route = "folder/{folderPath}",
        title = "Folder"
    ) {
        fun createRoute(folderPath: String) = "folder/${java.net.URLEncoder.encode(folderPath, "UTF-8")}"
    }

    data object TagEditor : Screen(
        route = "tageditor/{songId}",
        title = "Tag Editor"
    ) {
        fun createRoute(songId: Long) = "tageditor/$songId"
    }
    
    data object NowPlaying : Screen(
        route = "now_playing",
        title = "Now Playing"
    )
    
    data object Queue : Screen(
        route = "queue",
        title = "Queue"
    )
    
    data object Equalizer : Screen(
        route = "equalizer",
        title = "Equalizer"
    )
    
    data object Search : Screen(
        route = "search",
        title = "Search"
    )
    
    data object PlaylistsManagement : Screen(
        route = "playlists_management",
        title = "Manage Playlists"
    )
    
    companion object {
        val bottomNavItems = listOf(Home, Songs, Albums, Artists, Settings)
    }
}
