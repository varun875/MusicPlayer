package com.suspended.music.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
import com.suspended.music.data.services.UpdateCheckService
import com.suspended.music.data.services.UpdateCheckResult
import com.suspended.music.player.MusicPlayer
import com.suspended.music.player.AudioEffectController
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import com.suspended.music.data.model.AudioTags
import com.suspended.music.data.repository.TagRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

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
    private val updateCheckService: UpdateCheckService
) : ViewModel() {
    
    private val settingsManager = SettingsManager(context)
    
    // Library data
    val songs: StateFlow<List<Song>> = songDao.getAllSongs()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    
    val albums: StateFlow<List<Album>> = albumDao.getAllAlbums()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    
    val artists: StateFlow<List<Artist>> = artistDao.getAllArtists()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    
    val playlists: StateFlow<List<Playlist>> = playlistDao.getAllPlaylists()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    
    val recentlyPlayed: StateFlow<List<Song>> = songDao.getRecentlyPlayed(20)
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    
    val favorites: StateFlow<List<Song>> = songDao.getFavorites()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    
    // Settings
    val appSettings: StateFlow<AppSettings> = settingsManager.settingsFlow
        .stateIn(viewModelScope, SharingStarted.Lazily, AppSettings())
    
    // Update notification state
    private val _updateNotification = MutableStateFlow<UpdateNotification?>(null)
    val updateNotification: StateFlow<UpdateNotification?> = _updateNotification.asStateFlow()
    
    // Scan state
    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()
    
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

    // EQ State
    val eqBands = audioEffectController.bands
    val eqPresets = audioEffectController.presets
    val eqCurrentPreset = audioEffectController.currentPreset
    val bassBoostStrength = audioEffectController.bassBoostStrength
    val virtualizerStrength = audioEffectController.virtualizerStrength
    
    init {
        // Start position update loop - optimized to 250ms for smoother updates with less overhead
        viewModelScope.launch {
            while (isActive) {
                // Only update position if currently playing to save CPU
                if (musicPlayer.isPlaying.value) {
                    musicPlayer.updatePosition()
                }
                delay(250)
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
            _isScanning.value = true
            _scanResult.value = mediaScanner.scanMedia()
            _isScanning.value = false
        }
    }
    
    fun playSong(song: Song) {
        musicPlayer.playSong(song)
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
            songDao.getSongsByAlbum(albumId).collect { albumSongs ->
                if (albumSongs.isNotEmpty()) {
                    playSongs(albumSongs, startIndex)
                }
            }
        }
    }
    
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
    
    fun addToQueue(song: Song) = musicPlayer.addToQueue(song)
    fun addToQueue(songs: List<Song>) = musicPlayer.addToQueue(songs)
    fun clearQueue() = musicPlayer.clearQueue()
    fun skipToIndex(index: Int) = musicPlayer.skipToIndex(index)
    fun removeFromQueue(index: Int) = musicPlayer.removeFromQueue(index)
    
    // Search
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    
    private val _searchResults = MutableStateFlow<List<Song>>(emptyList())
    val searchResults: StateFlow<List<Song>> = _searchResults.asStateFlow()
    
    fun search(query: String) {
        _searchQuery.value = query
        viewModelScope.launch {
            songDao.searchSongs(query).collect { results ->
                _searchResults.value = results
            }
        }
    }
    
    // Tag Editor
    private val _tags = MutableStateFlow<AudioTags?>(null)
    val tags: StateFlow<AudioTags?> = _tags.asStateFlow()
    
    fun loadTags(songId: Long) {
        viewModelScope.launch {
             val song = songDao.getSongById(songId)
             if (song != null) {
                 val result = withContext(Dispatchers.IO) { tagRepository.readTags(song.path) }
                 result.onSuccess { _tags.value = it }
             }
        }
    }
    
    fun saveTags(songId: Long, newTags: AudioTags) {
        viewModelScope.launch {
             val song = songDao.getSongById(songId)
             if (song != null) {
                 val result = withContext(Dispatchers.IO) { tagRepository.writeTags(song.path, newTags) }
                 if (result.isSuccess) {
                     val updatedSong = song.copy(
                         title = newTags.title.takeIf { it.isNotEmpty() } ?: song.title,
                         artist = newTags.artist.takeIf { it.isNotEmpty() } ?: song.artist,
                         album = newTags.album.takeIf { it.isNotEmpty() } ?: song.album
                     )
                     songDao.updateSong(updatedSong)
                 }
             }
        }
    }
    
    // Settings Methods
    fun setDarkMode(isDark: Boolean?) = settingsManager.setDarkMode(isDark)
    
    fun setPrimaryColor(color: String) = settingsManager.setPrimaryColor(color)
    fun setSecondaryColor(color: String) = settingsManager.setSecondaryColor(color)
    fun setTertiaryColor(color: String) = settingsManager.setTertiaryColor(color)
    
    fun setLastFmEnabled(enabled: Boolean) = settingsManager.setLastFmEnabled(enabled)
    fun setLastFmCredentials(username: String, apiKey: String) {
        settingsManager.setLastFmCredentials(username, apiKey)
    }
    
    fun setListenBrainzEnabled(enabled: Boolean) = settingsManager.setListenBrainzEnabled(enabled)
    fun setListenBrainzToken(token: String) = settingsManager.setListenBrainzToken(token)
    
    fun setCheckUpdatesOnStartup(enabled: Boolean) = settingsManager.setCheckUpdatesOnStartup(enabled)
    
    fun checkForUpdates() {
        viewModelScope.launch {
            settingsManager.setLastUpdateCheckTime(System.currentTimeMillis())
            withContext(Dispatchers.IO) {
                try {
                    // Get current version from BuildConfig or manifest
                    val currentVersion = "1.1" // This should ideally come from BuildConfig.VERSION_NAME
                    val owner = "YourGitHubUsername" // Replace with your GitHub username
                    val repo = "MusicPlayer" // Replace with your repo name
                    
                    val result = updateCheckService.checkForUpdates(owner, repo)
                    
                    when (result) {
                        is UpdateCheckResult.Success -> {
                            val latestVersion = result.release.tagName.removePrefix("v")
                            val isNewVersion = latestVersion > currentVersion
                            
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
}
data class UpdateNotification(
    val title: String,
    val message: String,
    val version: String,
    val downloadUrl: String,
    val isNewVersion: Boolean
)
