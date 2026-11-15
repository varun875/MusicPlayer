package com.suspended.music.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Album
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.QueueMusic
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * Type-safe navigation destinations for Navigation 3.
 *
 * Every destination is a `@Serializable NavKey` (Kotlin serialization), so the
 * back stack can be saved and restored across configuration changes and
 * process death. Argumented routes carry their ids as typed properties —
 * no string routes, no `navArgument` plumbing.
 *
 * Bottom-bar destinations additionally expose icon/title metadata; see
 * [bottomNavItems].
 */
@Serializable
sealed interface MusicRoute : NavKey {

    // Main navigation destinations (also shown in the bottom bar / rail)
    @Serializable
    data object Home : MusicRoute

    @Serializable
    data object Songs : MusicRoute

    @Serializable
    data object Albums : MusicRoute

    @Serializable
    data object Artists : MusicRoute

    @Serializable
    data object Playlists : MusicRoute

    @Serializable
    data object Settings : MusicRoute

    // Argumented detail routes
    @Serializable
    data class AlbumDetail(val albumId: Long) : MusicRoute

    @Serializable
    data class ArtistDetail(val artistId: Long) : MusicRoute

    @Serializable
    data class PlaylistDetail(val playlistId: Long) : MusicRoute

    @Serializable
    data class TagEditor(val songId: Long) : MusicRoute

    // Modal / focused screens
    @Serializable
    data object NowPlaying : MusicRoute

    @Serializable
    data object Queue : MusicRoute

    @Serializable
    data object Equalizer : MusicRoute

    @Serializable
    data object Search : MusicRoute

    @Serializable
    data object PlaylistsManagement : MusicRoute
}

/** Icon + label metadata for a bottom-bar / rail destination. */
data class BottomBarItem(
    val route: MusicRoute,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
)

/** Bottom navigation destinations in display order. */
val bottomNavItems: List<BottomBarItem> = listOf(
    BottomBarItem(MusicRoute.Home, "Listen Now", Icons.Filled.Home, Icons.Outlined.Home),
    BottomBarItem(MusicRoute.Songs, "Songs", Icons.Filled.LibraryMusic, Icons.Outlined.LibraryMusic),
    BottomBarItem(MusicRoute.Albums, "Albums", Icons.Filled.Album, Icons.Outlined.Album),
    BottomBarItem(MusicRoute.Artists, "Artists", Icons.Filled.Person, Icons.Outlined.Person),
)

/** Routes whose selection state the bottom bar / rail should highlight. */
private val bottomBarRoutes: Set<MusicRoute> = bottomNavItems.map { it.route }.toSet()

/** True when [route] is a bottom-bar destination (used for transitions + chrome). */
fun isBottomBarRoute(route: MusicRoute?): Boolean = route in bottomBarRoutes
