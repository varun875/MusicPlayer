package com.suspended.music.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.UnstableApi
import com.suspended.music.data.local.AlbumDao
import com.suspended.music.data.local.ArtistDao
import com.suspended.music.data.local.PlaylistDao
import com.suspended.music.data.local.SongDao
import com.suspended.music.data.model.Album
import com.suspended.music.data.model.Artist
import com.suspended.music.data.model.AppSettings
import com.suspended.music.data.model.Playlist
import com.suspended.music.data.model.Song
import com.suspended.music.data.preferences.SettingsManager
import com.suspended.music.data.repository.MediaScanner
import com.suspended.music.data.repository.ScanResult
import com.suspended.music.data.repository.ScanPhase
import com.suspended.music.data.repository.ScanProgress
import com.suspended.music.data.services.UpdateCheckService
import com.suspended.music.data.services.UpdateCheckResult
import com.suspended.music.player.MusicPlayer
import com.suspended.music.player.AudioEffectController
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import com.suspended.music.data.model.AudioTags
import com.suspended.music.data.repository.TagRepository
import com.suspended.music.util.ErrorHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import androidx.paging.cachedIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds
import javax.inject.Inject
import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview

@UnstableApi
@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class MainViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val songDao: SongDao,
    private val albumDao: AlbumDao,
    private val artistDao: ArtistDao,
    private val playlistDao: PlaylistDao,
    private val mediaScanner: MediaScanner,
    private val musicPlayer: MusicPlayer,
    private val audioEffectController: AudioEffectController,
    private val tagRepository: TagRepository,
    private val updateCheckService: UpdateCheckService,
    private val settingsManager: SettingsManager
) : ViewModel() {
    

    private val shareWhileSubscribed = SharingStarted.WhileSubscribed(5_000)
    
    // Library data with error handling
    val songs: StateFlow<List<Song>> = songDao.getAllSongs()
        .catch { emit(emptyList()) }
        .distinctUntilChanged()
        .stateIn(viewModelScope, shareWhileSubscribed, emptyList())

    // Paged library data for large collections
    val pagedSongs: Flow<PagingData<Song>> = androidx.paging.Pager(
        config = androidx.paging.PagingConfig(
            pageSize = 50,
            enablePlaceholders = true,
            initialLoadSize = 100
        ),
        pagingSourceFactory = { songDao.getAllSongsPaged() }
    ).flow.cachedIn(scope = viewModelScope)
    
    val albums: StateFlow<List<Album>> = albumDao.getAllAlbums()
        .catch { emit(emptyList()) }
        .distinctUntilChanged()
        .stateIn(viewModelScope, shareWhileSubscribed, emptyList())
    
    val artists: StateFlow<List<Artist>> = artistDao.getAllArtists()
        .catch { emit(emptyList()) }
        .distinctUntilChanged()
        .stateIn(viewModelScope, shareWhileSubscribed, emptyList())
    
    val playlists: StateFlow<List<Playlist>> = playlistDao.getAllPlaylists()
        .catch { emit(emptyList()) }
        .distinctUntilChanged()
        .stateIn(viewModelScope, shareWhileSubscribed, emptyList())
    
    val recentlyPlayed: StateFlow<List<Song>> = songDao.getRecentlyPlayed(20)
        .catch { emit(emptyList()) }
        .distinctUntilChanged()
        .stateIn(viewModelScope, shareWhileSubscribed, emptyList())
    
    val favorites: StateFlow<List<Song>> = songDao.getFavorites()
        .catch { emit(emptyList()) }
        .distinctUntilChanged()
        .stateIn(viewModelScope, shareWhileSubscribed, emptyList())

    // Sorting
    private val _sortOption = MutableStateFlow(SortOption.TITLE)
    val sortOption: StateFlow<SortOption> = _sortOption.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val sortedSongs: StateFlow<List<Song>> = kotlinx.coroutines.flow.combine(songs, _sortOption) { songList, sort ->
        com.suspended.music.util.PerfLogger.measure("SortSongs(${sort.name}, n=${songList.size})") {
            when (sort) {
                SortOption.TITLE -> songList.sortedBy { it.title.lowercase() }
                SortOption.ARTIST -> songList.sortedBy { it.artist.lowercase() }
                SortOption.ALBUM -> songList.sortedBy { it.album.lowercase() }
                SortOption.DATE_ADDED -> songList.sortedByDescending { it.dateAdded }
                SortOption.DURATION -> songList.sortedByDescending { it.duration }
            }
        }
    }
    .flowOn(Dispatchers.Default)
    .stateIn(viewModelScope, shareWhileSubscribed, emptyList())

    fun updateSortOption(option: SortOption) {
        _sortOption.value = option
    }
    
    // Settings
    val appSettings: StateFlow<AppSettings> = settingsManager.settingsFlow
        .stateIn(viewModelScope, shareWhileSubscribed, AppSettings())
    
    // Update notification state
    private val _updateNotification = MutableStateFlow<UpdateNotification?>(null)
    val updateNotification: StateFlow<UpdateNotification?> = _updateNotification.asStateFlow()
    
    // Scan state
    val scanProgress: StateFlow<ScanProgress> = mediaScanner.progress

    val isScanning: StateFlow<Boolean> = mediaScanner.progress
        .map { it.phase != ScanPhase.DONE }
        .distinctUntilChanged()
        .stateIn(viewModelScope, shareWhileSubscribed, false)

    private val _scanResult = MutableStateFlow<ScanResult?>(null)
    val scanResult: StateFlow<ScanResult?> = _scanResult.asStateFlow()
    
    // Player state (delegated to MusicPlayer)
    val currentSong = musicPlayer.currentSong
    val isPlaying = musicPlayer.isPlaying
    val currentPosition = musicPlayer.currentPosition
    val duration = musicPlayer.duration
    val repeatMode = musicPlayer.repeatMode
    val shuffleEnabled = musicPlayer.shuffleEnabled
    val queue = musicPlayer.queue
    val playbackSpeed = musicPlayer.playbackSpeed

    // EQ State
    val eqBands = audioEffectController.bands
    val eqPresets = audioEffectController.presets
    val eqCurrentPreset = audioEffectController.currentPreset
    val bassBoostStrength = audioEffectController.bassBoostStrength
    val virtualizerStrength = audioEffectController.virtualizerStrength
    
    init {
        // Start position update loop — slower cadence keeps seekbar smooth without
        // flooding recomposition.  500ms gives ~2 fps which is plenty for a slider.
        viewModelScope.launch {
            while (isActive) {
                if (musicPlayer.isPlaying.value) {
                    musicPlayer.updatePosition()
                    delay(500)
                } else {
                    delay(1000)
                }
            }
        }
    }
    
    // Audio Effects
    fun setEqBandLevel(bandId: Int, level: Int) {
        audioEffectController.setBandLevel(bandId, level)
    }
    
    fun setEqPreset(presetIndex: Int) {
        audioEffectController.usePreset(presetIndex)
    }
    
    fun setBassBoost(strength: Int) {
        audioEffectController.setBassBoost(strength)
    }
    
    fun setVirtualizer(strength: Int) {
        audioEffectController.setVirtualizer(strength)
    }
    
    fun scanMedia() {
        viewModelScope.launch {
            com.suspended.music.util.PerfLogger.startTrace("MediaScan")
            _scanResult.value = mediaScanner.scanMedia()
            com.suspended.music.util.PerfLogger.endTrace("MediaScan")
            com.suspended.music.util.PerfLogger.logEvent("ScanComplete", "songs=${(_scanResult.value as? ScanResult.Success)?.songCount ?: 0}")
        }
    }
    
    fun playSong(song: Song) {
        // Use the full sorted list as the queue so auto-advance works.
        // Find the tapped song's position in the current sorted list.
        val queue = sortedSongs.value
        val index = queue.indexOfFirst { it.id == song.id }
        if (index >= 0) {
            musicPlayer.playSongs(queue, index)
        } else {
            // Song not in sorted list (e.g. recently played widget) — fall back to single
            musicPlayer.playSong(song)
        }
        viewModelScope.launch {
            songDao.incrementPlayCount(song.id)
        }
    }
    
    fun playSongs(songs: List<Song>, startIndex: Int = 0) {
        musicPlayer.playSongs(songs, startIndex)
        viewModelScope.launch {
            songs.getOrNull(startIndex)?.let {
                songDao.incrementPlayCount(it.id)
            }
        }
    }
    
    fun getAlbum(albumId: Long): kotlinx.coroutines.flow.Flow<Album?> = albumDao.getAlbumById(albumId)
    
    fun getAlbumSongs(albumId: Long): kotlinx.coroutines.flow.Flow<List<Song>> = songDao.getSongsByAlbum(albumId)

    fun playAlbum(albumId: Long, startIndex: Int = 0) {
        viewModelScope.launch {
            val albumSongs = songDao.getSongsByAlbumSync(albumId)
            if (albumSongs.isNotEmpty()) {
                playSongs(albumSongs, startIndex)
            }
        }
    }

    fun playArtist(artistName: String, startIndex: Int = 0) {
        viewModelScope.launch {
            val artistSongs = songDao.getSongsByArtistSync(artistName)
            if (artistSongs.isNotEmpty()) {
                playSongs(artistSongs, startIndex)
            }
        }
    }

    fun getSongsByArtist(artistName: String) = songDao.getSongsByArtist(artistName)
    
    fun togglePlayPause() = musicPlayer.togglePlayPause()
    fun skipToNext() = musicPlayer.skipToNext()
    fun skipToPrevious() = musicPlayer.skipToPrevious()
    fun seekTo(position: Long) = musicPlayer.seekTo(position)
    fun toggleRepeatMode() = musicPlayer.toggleRepeatMode()
    fun toggleShuffle() = musicPlayer.toggleShuffle()
    fun setPlaybackSpeed(speed: Float) = musicPlayer.setPlaybackSpeed(speed)
    
    fun toggleFavorite(song: Song) {
        viewModelScope.launch {
            songDao.updateFavorite(song.id, !song.isFavorite)
        }
    }
    
    fun deleteSong(song: Song) {
        viewModelScope.launch {
            try {
                // Delete from database
                songDao.deleteSongById(song.id)

                // Delete physical file
                withContext(Dispatchers.IO) {
                    val file = java.io.File(song.path)
                    if (file.exists()) {
                        val deleted = file.delete()
                        if (!deleted) {
                            ErrorHandler.logError("MainViewModel", "Failed to delete file: ${file.absolutePath}")
                        }
                    }
                }
            } catch (e: Exception) {
                ErrorHandler.logError("MainViewModel", "Error deleting song: ${e.message}", e)
                // Could show user-friendly error message here
            }
        }
    }
    
    fun addToQueue(song: Song) = musicPlayer.addToQueue(song)
    fun addToQueue(songs: List<Song>) = musicPlayer.addToQueue(songs)
    fun clearQueue() = musicPlayer.clearQueue()
    fun skipToIndex(index: Int) = musicPlayer.skipToIndex(index)
    fun removeFromQueue(index: Int) = musicPlayer.removeFromQueue(index)
    
    // Search with Flow operators for debounce + distinct until changed
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    
    private val _searchFilters = MutableStateFlow<com.suspended.music.ui.components.SearchFilters?>(null)
    val searchFilters: StateFlow<com.suspended.music.ui.components.SearchFilters?> = _searchFilters.asStateFlow()
    
    // Debounced search results: 300ms debounce + distinct until changed + error handling
    // Combine query AND filters so changes to either trigger a new search
    val searchResults: StateFlow<List<Song>> = kotlinx.coroutines.flow.combine(
        _searchQuery.debounce(300.milliseconds).distinctUntilChanged(),
        _searchFilters
    ) { query, filters -> query to filters }
        .flatMapLatest { (query, filters) ->
            if (filters != null) {
                songDao.searchSongsWithFilters(
                    query = query,
                    artist = filters.artist,
                    album = filters.album,
                    genre = filters.genre,
                    minDuration = filters.minDuration,
                    maxDuration = filters.maxDuration,
                    favoritesOnly = if (filters.favorites == true) 1 else 0
                ).catch { emit(emptyList()) }
            } else if (query.isNotEmpty()) {
                songDao.searchSongs(query).catch { emit(emptyList()) }
            } else {
                songs // Return all songs when no filters and empty query
            }
        }
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, shareWhileSubscribed, emptyList())

    // Paged search results for large datasets
    // Combine query AND filters so changes to either trigger a new search
    val pagedSearchResults: Flow<PagingData<Song>> = kotlinx.coroutines.flow.combine(
        _searchQuery.debounce(300.milliseconds).distinctUntilChanged(),
        _searchFilters
    ) { query, filters -> query to filters }
        .flatMapLatest { (query, filters) ->
            if (filters != null) {
                androidx.paging.Pager(
                    config = androidx.paging.PagingConfig(
                        pageSize = 50,
                        enablePlaceholders = true,
                        initialLoadSize = 100
                    ),
                    pagingSourceFactory = {
                        songDao.searchSongsWithFiltersPaged(
                            query = query,
                            artist = filters.artist,
                            album = filters.album,
                            genre = filters.genre,
                            minDuration = filters.minDuration,
                            maxDuration = filters.maxDuration,
                            favoritesOnly = if (filters.favorites == true) 1 else 0
                        )
                    }
                ).flow
            } else if (query.isNotEmpty()) {
                androidx.paging.Pager(
                    config = androidx.paging.PagingConfig(
                        pageSize = 50,
                        enablePlaceholders = true,
                        initialLoadSize = 100
                    ),
                    pagingSourceFactory = { songDao.searchSongsPaged(query) }
                ).flow
            } else {
                pagedSongs // Return paged all songs when no filters and empty query
            }
        }
        .cachedIn(viewModelScope)
    
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }
    
    fun updateSearchFilters(filters: com.suspended.music.ui.components.SearchFilters?) {
        _searchFilters.value = filters
    }
    
    // Tag Editor
    private val _tags = MutableStateFlow<AudioTags?>(null)
    val tags: StateFlow<AudioTags?> = _tags.asStateFlow()
    
    fun loadTags(songId: Long) {
        viewModelScope.launch(ErrorHandler.coroutineExceptionHandler) {
             val song = songDao.getSongById(songId)
             if (song != null) {
                 val result = withContext(Dispatchers.IO + ErrorHandler.coroutineExceptionHandler) {
                     tagRepository.readTags(song.path)
                 }
                 result.onSuccess { _tags.value = it }
                 result.onFailure {
                     ErrorHandler.logError("MainViewModel", "Failed to load tags for song: ${song.title}", it)
                 }
             }
        }
    }

    fun saveTags(songId: Long, newTags: AudioTags) {
        viewModelScope.launch(ErrorHandler.coroutineExceptionHandler) {
             val song = songDao.getSongById(songId)
             if (song != null) {
                 val result = withContext(Dispatchers.IO + ErrorHandler.coroutineExceptionHandler) {
                     tagRepository.writeTags(song.path, newTags)
                 }
                 if (result.isSuccess) {
                     val updatedSong = song.copy(
                         title = newTags.title.takeIf { it.isNotEmpty() } ?: song.title,
                         artist = newTags.artist.takeIf { it.isNotEmpty() } ?: song.artist,
                         album = newTags.album.takeIf { it.isNotEmpty() } ?: song.album
                     )
                     songDao.updateSong(updatedSong)
                     // Clean up any temp artwork files from readTags
                     tagRepository.cleanupTempFiles()
                 } else {
                     ErrorHandler.logError("MainViewModel", "Failed to save tags for song: ${song.title}", result.exceptionOrNull())
                 }
             }
        }
    }
    
    // ── Theme Settings ────────────────────────────────────────────────
    fun setDarkMode(isDark: Boolean?) = settingsManager.setDarkMode(isDark)
    fun setAmoledBlack(enabled: Boolean) = settingsManager.setAmoledBlack(enabled)
    fun setDynamicColors(enabled: Boolean) = settingsManager.setDynamicColors(enabled)
    fun setPrimaryColor(color: String) = settingsManager.setPrimaryColor(color)
    fun setSecondaryColor(color: String) = settingsManager.setSecondaryColor(color)
    fun setTertiaryColor(color: String) = settingsManager.setTertiaryColor(color)

    // ── Playback Settings ────────────────────────────────────────────
    

    fun setGaplessPlayback(enabled: Boolean) = settingsManager.setGaplessPlayback(enabled)
    fun setAutoResume(enabled: Boolean) = settingsManager.setAutoResume(enabled)
    fun setSkipSilence(enabled: Boolean) = settingsManager.setSkipSilence(enabled)
    fun setRememberLastPosition(enabled: Boolean) = settingsManager.setRememberLastPosition(enabled)

    // ── Library Settings ─────────────────────────────────────────────
    fun setMinTrackDuration(seconds: Int) = settingsManager.setMinTrackDuration(seconds)
    fun setShowDownloadsFolder(enabled: Boolean) = settingsManager.setShowDownloadsFolder(enabled)

    // ── Notification Settings ────────────────────────────────────────
    fun setShowAlbumArt(enabled: Boolean) = settingsManager.setShowAlbumArt(enabled)
    fun setCompactControls(enabled: Boolean) = settingsManager.setCompactControls(enabled)

    // ── Updates ──────────────────────────────────────────────────────
    fun setCheckUpdatesOnStartup(enabled: Boolean) = settingsManager.setCheckUpdatesOnStartup(enabled)
    
    fun checkForUpdates() {
        viewModelScope.launch {
            settingsManager.setLastUpdateCheckTime(System.currentTimeMillis())
            withContext(Dispatchers.IO) {
                try {
                    // Get current version from BuildConfig
                    val currentVersion = com.suspended.music.BuildConfig.VERSION_NAME
                    val owner = "suspended" // GitHub username
                    val repo = "MusicPlayer"
                    
                    val result = updateCheckService.checkForUpdates(owner, repo)
                    
                    when (result) {
                        is UpdateCheckResult.Success -> {
                            val latestVersion = result.release.tagName.removePrefix("v")
                            val isNewVersion = compareVersions(latestVersion, currentVersion) > 0
                            
                            _updateNotification.emit(
                                UpdateNotification(
                                    title = if (isNewVersion) "Update Available" else "App is Up to Date",
                                    message = if (isNewVersion) 
                                        "Version $latestVersion is available" 
                                    else 
                                        "You're running the latest version",
                                    version = latestVersion,
                                    downloadUrl = result.release.htmlUrl,
                                    isNewVersion = isNewVersion
                                )
                            )
                        }
                        is UpdateCheckResult.Error -> {
                            _updateNotification.emit(
                                UpdateNotification(
                                    title = "Update Check Failed",
                                    message = result.message,
                                    version = "",
                                    downloadUrl = "",
                                    isNewVersion = false
                                )
                            )
                        }
                    }
                } catch (e: Exception) {
                    _updateNotification.emit(
                        UpdateNotification(
                            title = "Update Check Failed",
                            message = e.message ?: "Unknown error occurred",
                            version = "",
                            downloadUrl = "",
                            isNewVersion = false
                        )
                    )
                }
            }
        }
    }
    
    fun dismissUpdateNotification() {
        viewModelScope.launch {
            _updateNotification.emit(null)
        }
    }
    

    // Playlist Management
    fun createPlaylist(name: String, description: String = "") {
        viewModelScope.launch {
            val playlist = Playlist(
                name = name,
                description = description,
                dateCreated = System.currentTimeMillis(),
                dateModified = System.currentTimeMillis()
            )
            playlistDao.insertPlaylist(playlist)
        }
    }
    
    fun updatePlaylist(playlist: Playlist) {
        viewModelScope.launch {
            playlistDao.updatePlaylist(playlist)
        }
    }
    
    fun deletePlaylist(playlistId: Long) {
        viewModelScope.launch {
            playlistDao.deletePlaylistById(playlistId)
        }
    }

    // Playlist detail methods
    fun getPlaylist(playlistId: Long): kotlinx.coroutines.flow.Flow<Playlist?> {
        return playlistDao.getPlaylistByIdFlow(playlistId)
    }

    fun getPlaylistSongs(playlistId: Long): kotlinx.coroutines.flow.Flow<List<Song>> {
        return playlistDao.getSongsInPlaylist(playlistId)
    }

    fun playPlaylist(songs: List<Song>, startIndex: Int = 0) {
        if (songs.isNotEmpty()) {
            playSongs(songs, startIndex)
        }
    }

    fun removeSongsFromPlaylist(playlistId: Long, songIds: List<Long>) {
        viewModelScope.launch {
            if (songIds.isNotEmpty()) {
                playlistDao.removeSongsFromPlaylist(playlistId, songIds)
            }
        }
    }

    fun addSongsToPlaylist(playlistId: Long, songIds: List<Long>) {
        viewModelScope.launch {
            if (songIds.isNotEmpty()) {
                playlistDao.addSongsToPlaylist(playlistId, songIds)
            }
        }
    }

    // Library layout settings
    fun setLibraryLayoutMode(mode: String) = settingsManager.setLibraryLayoutMode(mode)
    fun setLibraryGridSize(size: Int) = settingsManager.setLibraryGridSize(size)
    fun setLibraryListDensity(density: String) = settingsManager.setLibraryListDensity(density)

    // Search filter data sources
    val availableArtists: StateFlow<List<String>> = songDao.getAllArtistNames()
        .catch { emit(emptyList()) }
        .distinctUntilChanged()
        .stateIn(viewModelScope, shareWhileSubscribed, emptyList())

    val availableAlbums: StateFlow<List<String>> = songDao.getAllAlbumNames()
        .catch { emit(emptyList()) }
        .distinctUntilChanged()
        .stateIn(viewModelScope, shareWhileSubscribed, emptyList())

    val availableGenres: StateFlow<List<String>> = songDao.getAllGenres()
        .catch { emit(emptyList()) }
        .distinctUntilChanged()
        .stateIn(viewModelScope, shareWhileSubscribed, emptyList())
}
data class UpdateNotification(
    val title: String,
    val message: String,
    val version: String,
    val downloadUrl: String,
    val isNewVersion: Boolean
)

/**
 * Compares two semantic version strings (e.g., "1.3.0" vs "1.12.1").
 * Returns positive if [a] > [b], negative if [a] < [b], zero if equal.
 */
private fun compareVersions(a: String, b: String): Int {
    val partsA = a.split(".").map { it.toIntOrNull() ?: 0 }
    val partsB = b.split(".").map { it.toIntOrNull() ?: 0 }
    val maxLen = maxOf(partsA.size, partsB.size)
    for (i in 0 until maxLen) {
        val pa = partsA.getOrElse(i) { 0 }
        val pb = partsB.getOrElse(i) { 0 }
        if (pa != pb) return pa - pb
    }
    return 0
}

enum class SortOption(val label: String) {
    TITLE("Title"),
    ARTIST("Artist"),
    ALBUM("Album"),
    DATE_ADDED("Date Added"),
    DURATION("Duration")
}
