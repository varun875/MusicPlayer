package com.suspended.music.ui.viewmodel

import android.app.PendingIntent
import android.app.RecoverableSecurityException
import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.SystemClock
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.UnstableApi
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.suspended.music.BuildConfig
import com.suspended.music.data.local.AlbumDao
import com.suspended.music.data.local.ArtistDao
import com.suspended.music.data.local.PlaylistDao
import com.suspended.music.data.local.SongDao
import com.suspended.music.data.model.Album
import com.suspended.music.data.model.AppSettings
import com.suspended.music.data.model.Artist
import com.suspended.music.data.model.AudioTags
import com.suspended.music.data.model.Playlist
import com.suspended.music.data.model.Song
import com.suspended.music.data.preferences.SettingsManager
import com.suspended.music.data.repository.MediaScanner
import com.suspended.music.data.repository.ScanPhase
import com.suspended.music.data.repository.ScanProgress
import com.suspended.music.data.repository.ScanResult
import com.suspended.music.data.repository.TagRepository
import com.suspended.music.data.worker.MediaScanWorkManager
import com.suspended.music.data.worker.ScanStatusStore
import com.suspended.music.player.AudioEffectController
import com.suspended.music.player.MusicPlayer
import com.suspended.music.ui.components.SearchFilters
import com.suspended.music.ui.components.SearchScope
import com.suspended.music.util.ErrorHandler
import com.suspended.music.util.FuzzySearch
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

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
    private val settingsManager: SettingsManager,
    private val mediaScanWorkManager: MediaScanWorkManager,
    private val scanStatusStore: ScanStatusStore
) : ViewModel() {

    private companion object {
        const val POSITION_POLL_FOREGROUND_MS = 650L

        /** How long a track must actually play before it counts as a play. */
        const val PLAY_COUNT_DELAY_MS = 3_000L

        /** Minimum gap between library scans triggered by external "open with" intents. */
        const val INTENT_SCAN_THROTTLE_MS = 60_000L
    }

    private val shareWhileSubscribed = SharingStarted.WhileSubscribed(5_000)

    private val isAppInForeground = MutableStateFlow(
        ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
    )

    private val appLifecycleObserver = object : DefaultLifecycleObserver {
        override fun onStart(owner: LifecycleOwner) {
            isAppInForeground.value = true
            musicPlayer.refreshPositionSnapshot()
        }

        override fun onStop(owner: LifecycleOwner) {
            isAppInForeground.value = false
        }
    }

    // ── Library data ───────────────────────────────────────────────────

    val songs: StateFlow<List<Song>> = songDao.getAllSongs()
        .logAndFallback(emptyList())
        .distinctUntilChanged()
        .stateIn(viewModelScope, shareWhileSubscribed, emptyList())

    val songCount: StateFlow<Int> = songDao.observeSongCount()
        .logAndFallback(0)
        .distinctUntilChanged()
        .stateIn(viewModelScope, shareWhileSubscribed, 0)

    val albumCount: StateFlow<Int> = albumDao.observeAlbumCount()
        .logAndFallback(0)
        .distinctUntilChanged()
        .stateIn(viewModelScope, shareWhileSubscribed, 0)

    val artistCount: StateFlow<Int> = artistDao.observeArtistCount()
        .logAndFallback(0)
        .distinctUntilChanged()
        .stateIn(viewModelScope, shareWhileSubscribed, 0)

    val favoriteCount: StateFlow<Int> = songDao.observeFavoriteCount()
        .logAndFallback(0)
        .distinctUntilChanged()
        .stateIn(viewModelScope, shareWhileSubscribed, 0)

    // Lightweight list for the Home screen — avoids materializing the entire albums table.
    val topAlbums: StateFlow<List<Album>> = albumDao.getTopAlbums(10)
        .logAndFallback(emptyList())
        .distinctUntilChanged()
        .stateIn(viewModelScope, shareWhileSubscribed, emptyList())

    // Paged library data for large collections.
    val pagedSongs: Flow<PagingData<Song>> = Pager(
        config = PagingConfig(
            pageSize = 50,
            enablePlaceholders = false,
            initialLoadSize = 100,
            prefetchDistance = 25
        ),
        pagingSourceFactory = { songDao.getAllSongsPaged() }
    ).flow.cachedIn(scope = viewModelScope)

    val albums: StateFlow<List<Album>> = albumDao.getAllAlbums()
        .logAndFallback(emptyList())
        .distinctUntilChanged()
        .stateIn(viewModelScope, shareWhileSubscribed, emptyList())

    val artists: StateFlow<List<Artist>> = artistDao.getAllArtists()
        .logAndFallback(emptyList())
        .distinctUntilChanged()
        .stateIn(viewModelScope, shareWhileSubscribed, emptyList())

    val playlists: StateFlow<List<Playlist>> = playlistDao.getAllPlaylists()
        .logAndFallback(emptyList())
        .distinctUntilChanged()
        .stateIn(viewModelScope, shareWhileSubscribed, emptyList())

    val recentlyPlayed: StateFlow<List<Song>> = songDao.getRecentlyPlayed(20)
        .logAndFallback(emptyList())
        .distinctUntilChanged()
        .stateIn(viewModelScope, shareWhileSubscribed, emptyList())

    val mostPlayed: StateFlow<List<Song>> = songDao.getMostPlayed(10)
        .logAndFallback(emptyList())
        .distinctUntilChanged()
        .stateIn(viewModelScope, shareWhileSubscribed, emptyList())

    val favorites: StateFlow<List<Song>> = songDao.getFavorites()
        .logAndFallback(emptyList())
        .distinctUntilChanged()
        .stateIn(viewModelScope, shareWhileSubscribed, emptyList())

    // ── Sorting ────────────────────────────────────────────────────────

    private val _sortOption = MutableStateFlow(SortOption.TITLE)
    val sortOption: StateFlow<SortOption> = _sortOption.asStateFlow()

    val pagedSortedSongs: Flow<PagingData<Song>> = _sortOption
        .flatMapLatest { sort ->
            Pager(
                config = PagingConfig(
                    pageSize = 60,
                    initialLoadSize = 120,
                    prefetchDistance = 30,
                    enablePlaceholders = false
                ),
                pagingSourceFactory = {
                    when (sort) {
                        SortOption.TITLE -> songDao.getAllSongsPaged()
                        SortOption.ARTIST -> songDao.getAllSongsPagedByArtist()
                        SortOption.ALBUM -> songDao.getAllSongsPagedByAlbum()
                        SortOption.DATE_ADDED -> songDao.getAllSongsPagedByDateAdded()
                        SortOption.DURATION -> songDao.getAllSongsPagedByDuration()
                        SortOption.PLAY_COUNT -> songDao.getAllSongsPagedByPlayCount()
                        SortOption.SIZE -> songDao.getAllSongsPagedBySize()
                        SortOption.DATE_MODIFIED -> songDao.getAllSongsPagedByDateModified()
                    }
                }
            ).flow
        }
        .cachedIn(scope = viewModelScope)

    fun updateSortOption(option: SortOption) {
        _sortOption.value = option
    }

    // ── Settings ───────────────────────────────────────────────────────

    val appSettings: StateFlow<AppSettings> = settingsManager.settingsFlow
        .stateIn(viewModelScope, shareWhileSubscribed, AppSettings())



    // ── Scan state ─────────────────────────────────────────────────────

    val scanProgress: StateFlow<ScanProgress> = mediaScanner.progress

    val isScanning: StateFlow<Boolean> = mediaScanner.progress
        .map { it.phase != ScanPhase.DONE }
        .distinctUntilChanged()
        .stateIn(viewModelScope, shareWhileSubscribed, false)

    /** Last scan outcome, published by [com.suspended.music.data.worker.MediaScanWorker]. */
    val scanResult: StateFlow<ScanResult?> = scanStatusStore.result

    // ── One-shot user-facing messages (errors that need a recovery prompt) ──
    // Buffer is generous so rapid successive errors are queued, not dropped
    // by tryEmit — a dropped second message is silently lost to the user.
    private val _uiMessage = MutableSharedFlow<String>(extraBufferCapacity = 16)
    val uiMessage: SharedFlow<String> = _uiMessage.asSharedFlow()

    // ── Delete consent (scoped storage) ────────────────────────────────

    private val _deleteConsentRequest =
        MutableSharedFlow<DeleteConsentRequest>(extraBufferCapacity = 16)
    val deleteConsentRequest: SharedFlow<DeleteConsentRequest> = _deleteConsentRequest.asSharedFlow()

    // ── Player state (delegated to MusicPlayer) ────────────────────────

    val currentSong = musicPlayer.currentSong
    val isPlaying = musicPlayer.isPlaying
    val currentPosition = musicPlayer.currentPosition
    val duration = musicPlayer.duration
    val repeatMode = musicPlayer.repeatMode
    val shuffleEnabled = musicPlayer.shuffleEnabled
    val queue = musicPlayer.queue
    val playbackSpeed = musicPlayer.playbackSpeed

    // ── EQ state ───────────────────────────────────────────────────────

    val eqBands = audioEffectController.bands
    val eqPresets = audioEffectController.presets
    val eqCurrentPreset = audioEffectController.currentPreset
    val bassBoostStrength = audioEffectController.bassBoostStrength
    val virtualizerStrength = audioEffectController.virtualizerStrength

    init {
        ProcessLifecycleOwner.get().lifecycle.addObserver(appLifecycleObserver)

        // Surface playback failures instead of letting them stop silently.
        viewModelScope.launch {
            musicPlayer.playbackError
                .filterNotNull()
                .distinctUntilChanged()
                .collect { message -> _uiMessage.tryEmit(message) }
        }

        // Surface scan failures with a recovery prompt.
        viewModelScope.launch {
            scanStatusStore.result
                .filterIsInstance<ScanResult.Error>()
                .distinctUntilChanged()
                .collect { error -> _uiMessage.tryEmit(error.message) }
        }

        // Count a play only once the track has actually been playing for a moment.
        // Writing playCount immediately on every tap would invalidate the songs
        // table — and therefore the paged list — each time, including skips.
        // Track the last-counted song id so each track is counted exactly once
        // per continuous play: pause→resume (incl. audio-focus duck cycles)
        // must not re-count it.
        viewModelScope.launch {
            var lastCountedSongId: Long? = null
            combine(musicPlayer.currentSong, musicPlayer.isPlaying) { song, playing ->
                if (playing) song else null
            }
                .distinctUntilChanged()
                .collectLatest { song ->
                    if (song == null) return@collectLatest
                    if (song.id == lastCountedSongId) return@collectLatest
                    delay(PLAY_COUNT_DELAY_MS)
                    if (song.id == lastCountedSongId) return@collectLatest
                    lastCountedSongId = song.id
                    withContext(Dispatchers.IO) { songDao.incrementPlayCount(song.id) }
                }
        }

        // Position update loop: suspends until playback starts (no idle
        // wake-ups), then drives the seekbar only while playing.
        viewModelScope.launch {
            while (isActive) {
                // Sleep with zero wake-ups until something actually plays.
                musicPlayer.isPlaying.first { it }

                while (musicPlayer.isPlaying.value) {
                    if (isAppInForeground.value) {
                        musicPlayer.updatePosition()
                        delay(POSITION_POLL_FOREGROUND_MS)
                    } else {
                        // Backgrounded mid-playback: stop polling the UI
                        // position; resume on foreground or playback stop.
                        awaitForegroundOrPlaybackStop()
                    }
                }
            }
        }
    }

    private suspend fun awaitForegroundOrPlaybackStop() {
        combine(isAppInForeground, musicPlayer.isPlaying) { foreground, playing ->
            foreground || !playing
        }.first { shouldResumePolling -> shouldResumePolling }
    }

    override fun onCleared() {
        ProcessLifecycleOwner.get().lifecycle.removeObserver(appLifecycleObserver)
        super.onCleared()
    }

    // ── Audio effects ──────────────────────────────────────────────────

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
        Log.d("MainViewModel", "scanMedia() called — scheduling immediate scan")
        mediaScanWorkManager.scheduleImmediateScan()
    }

    // ── Playback ───────────────────────────────────────────────────────

    fun playSong(song: Song) {
        viewModelScope.launch {
            // TODO(perf): this loads the full table (lyrics included) to build
            // the queue. A lyrics-free projection query would cut play latency
            // and memory for large libraries — needs a SongDao method.
            val queue = withContext(Dispatchers.IO) {
                when (_sortOption.value) {
                    SortOption.TITLE -> songDao.getAllSongsSync()
                    SortOption.ARTIST -> songDao.getAllSongsByArtistSync()
                    SortOption.ALBUM -> songDao.getAllSongsByAlbumSync()
                    SortOption.DATE_ADDED -> songDao.getAllSongsByDateAddedSync()
                    SortOption.DURATION -> songDao.getAllSongsByDurationSync()
                    SortOption.PLAY_COUNT -> songDao.getAllSongsByPlayCountSync()
                    SortOption.SIZE -> songDao.getAllSongsBySizeSync()
                    SortOption.DATE_MODIFIED -> songDao.getAllSongsByDateModifiedSync()
                }
            }

            val index = queue.indexOfFirst { it.id == song.id }
            if (index >= 0) {
                musicPlayer.playSongs(queue, index)
            } else {
                musicPlayer.playSong(song)
            }
        }
    }

    /** Plays a track delivered via an ACTION_VIEW ("open with") intent. */
    fun playAudioUri(uri: Uri) {
        viewModelScope.launch {
            val id = runCatching { ContentUris.parseId(uri) }.getOrNull()
            val song = id?.let { songDao.getSongById(it) }
            if (song != null) {
                playSong(song)
                return@launch
            }
            // The activity is exported, so any app can fire VIEW intents at us.
            // Throttle so repeated unknown URIs can't each kick off a library scan.
            val now = SystemClock.elapsedRealtime()
            if (now - lastIntentScanAt < INTENT_SCAN_THROTTLE_MS) {
                _uiMessage.tryEmit("That track isn't in your library")
                return@launch
            }
            lastIntentScanAt = now
            _uiMessage.tryEmit("Couldn't find that track in your library — scanning…")
            scanMedia()
        }
    }

    fun playSongs(songs: List<Song>, startIndex: Int = 0) {
        viewModelScope.launch {
            musicPlayer.playSongs(songs, startIndex)
        }
    }

    fun playFavorites() {
        viewModelScope.launch {
            val favs = withContext(Dispatchers.IO) { songDao.getFavoritesSync() }
            if (favs.isNotEmpty()) {
                musicPlayer.playSongs(favs, 0)
            }
        }
    }

    fun shuffleAndPlay() {
        viewModelScope.launch {
            val shuffled = withContext(Dispatchers.IO) {
                songDao.getAllSongsSync().shuffled()
            }
            if (shuffled.isNotEmpty()) {
                musicPlayer.playSongs(shuffled, 0)
            }
        }
    }

    fun getAlbum(albumId: Long): Flow<Album?> = albumDao.getAlbumById(albumId)

    fun getAlbumSongs(albumId: Long): Flow<List<Song>> = songDao.getSongsByAlbum(albumId)

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

    val sleepTimerRemainingMs = musicPlayer.sleepTimerRemainingMs
    fun startSleepTimer(durationMinutes: Int) = musicPlayer.startSleepTimer(durationMinutes)
    fun cancelSleepTimer() = musicPlayer.cancelSleepTimer()

    fun toggleFavorite(song: Song) {
        viewModelScope.launch {
            songDao.toggleFavorite(song.id)
        }
    }

    fun deleteSong(song: Song) {
        viewModelScope.launch {
            try {
                val uri = song.uri.takeIf { it.isNotBlank() }?.let { Uri.parse(it) }
                    ?: ContentUris.withAppendedId(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        song.id
                    )

                val deleted = withContext(Dispatchers.IO) {
                    when {
                        // API 30+: deletion requires explicit user consent.
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
                            val intent = MediaStore.createDeleteRequest(
                                context.contentResolver,
                                listOf(uri)
                            )
                            _deleteConsentRequest.tryEmit(DeleteConsentRequest(song.id, intent))
                            false // DB row removed only after the UI confirms
                        }
                        else -> try {
                            context.contentResolver.delete(uri, null, null) > 0
                        } catch (e: RecoverableSecurityException) {
                            // API 29: consent arrives via the exception's intent.
                            _deleteConsentRequest.tryEmit(
                                DeleteConsentRequest(song.id, e.userAction.actionIntent)
                            )
                            false
                        }
                    }
                }

                // Only drop the library row if the file is actually gone,
                // otherwise the next scan would re-add it.
                if (deleted) {
                    songDao.deleteSongAndPlaylistEntries(song.id)
                }
            } catch (e: Exception) {
                ErrorHandler.logError("MainViewModel", "Error deleting song id=${song.id}", e)
            }
        }
    }

    /** UI calls this after the user approves the system delete dialog. */
    fun confirmSongDeletion(songId: Long) {
        viewModelScope.launch {
            songDao.deleteSongAndPlaylistEntries(songId)
        }
    }

    fun addToQueue(song: Song) = musicPlayer.addToQueue(song)
    fun addToQueue(songs: List<Song>) = musicPlayer.addToQueue(songs)
    fun clearQueue() = musicPlayer.clearQueue()
    fun skipToIndex(index: Int) = musicPlayer.skipToIndex(index)
    fun removeFromQueue(index: Int) = musicPlayer.removeFromQueue(index)

    // ── Search ─────────────────────────────────────────────────────────

    private fun sanitizeSearchQuery(query: String, scope: SearchScope = SearchScope.ALL): String {
        if (query.isBlank()) return ""
        // Keep letters/digits in any script and turn separators into spaces so
        // "AC/DC" stays two tokens (deleting them produced the unmatchable "acdc").
        val sanitized = query.replace(Regex("[^\\p{L}\\p{N}\\s]"), " ").trim()
        if (sanitized.isBlank()) return ""
        // Term-wise prefix wildcard, column-prefixed when a specific metadata
        // scope is selected so e.g. "artist:beat*" only matches artists.
        val tokens = sanitized.split("\\s+".toRegex()).filter { it.isNotBlank() }
        if (tokens.isEmpty()) return ""
        return when (scope) {
            SearchScope.ALL -> tokens.joinToString(" ") { "$it*" }
            SearchScope.TITLE -> columnMatch("title", tokens)
            SearchScope.ARTIST -> columnMatch("artist", tokens)
            SearchScope.ALBUM -> columnMatch("album", tokens)
            SearchScope.COMPOSER -> columnMatch("composer", tokens)
        }
    }

    private fun columnMatch(column: String, tokens: List<String>): String =
        tokens.joinToString(" ") { "$column:$it*" }

    /**
     * Escapes LIKE wildcards in user input so "%" / "_" / "\" are matched
     * literally (paired with the ESCAPE '\' clause in the DAO queries).
     */
    private fun escapeLike(input: String): String =
        input.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchFilters = MutableStateFlow<SearchFilters?>(null)
    val searchFilters: StateFlow<SearchFilters?> = _searchFilters.asStateFlow()

    // Debounced search results: 300ms debounce + distinct until changed + error handling.
    val searchResults: StateFlow<List<Song>> = combine(
        _searchQuery.debounce(300.milliseconds).distinctUntilChanged(),
        _searchFilters
    ) { query, filters -> query.trim() to (filters ?: SearchFilters()) }
        .flatMapLatest { (query, filters) ->
            val sanitizedQuery = sanitizeSearchQuery(query, filters.scope)
            when {
                query.isBlank() && !filters.hasActiveFilters() -> flowOf(emptyList())
                sanitizedQuery.isNotEmpty() -> songDao.searchSongsWithFiltersFts(
                    sanitizedQuery = sanitizedQuery,
                    artist = filters.artist,
                    album = filters.album,
                    genre = filters.genre,
                    composer = filters.composer,
                    folder = filters.folder,
                    lyrics = filters.lyrics,
                    minDuration = filters.minDuration,
                    maxDuration = filters.maxDuration,
                    minYear = filters.minYear,
                    maxYear = filters.maxYear,
                    favoritesOnly = if (filters.favorites == true) 1 else 0,
                    hiRes = if (filters.hiRes == true) 1 else 0
                ).catch { emit(emptyList()) }.flatMapLatest { fts -> fuzzyAugment(query, fts) }
                else -> songDao.searchSongsWithFilters(
                    query = escapeLike(query),
                    artist = filters.artist,
                    album = filters.album,
                    genre = filters.genre,
                    composer = filters.composer?.let(::escapeLike),
                    folder = filters.folder,
                    lyrics = filters.lyrics?.let(::escapeLike),
                    minDuration = filters.minDuration,
                    maxDuration = filters.maxDuration,
                    minYear = filters.minYear,
                    maxYear = filters.maxYear,
                    favoritesOnly = if (filters.favorites == true) 1 else 0,
                    hiRes = if (filters.hiRes == true) 1 else 0
                ).catch { emit(emptyList()) }
            }
        }
        .logAndFallback(emptyList())
        .stateIn(viewModelScope, shareWhileSubscribed, emptyList())

    /**
     * Appends fuzzy-matched songs (typo tolerance) after FTS results. Runs the
     * expensive distance pass off the main thread and only when FTS was thin.
     */
    private fun fuzzyAugment(raw: String, fts: List<Song>): Flow<List<Song>> = flow {
        if (raw.isBlank() || fts.size >= 50) {
            emit(fts)
            return@flow
        }
        val corpus = songs.value.ifEmpty {
            // Search can be the first screen composed, so the WhileSubscribed
            // `songs` flow hasn't loaded yet — fetch directly instead of no-op'ing.
            withContext(Dispatchers.IO) { songDao.getAllSongsSync() }
        }
        val fuzzy = withContext(Dispatchers.Default) {
            FuzzySearch.searchScored(raw, corpus)
        }
        // Merge on one scale: FTS prefix hits score 1.0, so strong fuzzy matches
        // (which can score 1.0+ with the contiguity boost) interleave with them
        // instead of always being appended underneath. sortedByDescending is stable,
        // so FTS keeps its relative order on ties.
        val ranked = LinkedHashMap<Long, Pair<Song, Float>>(fts.size + fuzzy.size)
        for (song in fts) ranked[song.id] = song to 1.0f
        for ((song, score) in fuzzy) if (!ranked.containsKey(song.id)) ranked[song.id] = song to score
        emit(ranked.values.sortedByDescending { it.second }.map { it.first })
    }

    // Paged search results for large datasets.
    val pagedSearchResults: Flow<PagingData<Song>> = combine(
        _searchQuery.debounce(300.milliseconds).distinctUntilChanged(),
        _searchFilters
    ) { query, filters -> query.trim() to (filters ?: SearchFilters()) }
        .flatMapLatest { (query, filters) ->
            val sanitizedQuery = sanitizeSearchQuery(query, filters.scope)
            if (query.isBlank() && !filters.hasActiveFilters()) {
                flowOf(PagingData.empty())
            } else if (sanitizedQuery.isNotEmpty()) {
                Pager(
                    config = PagingConfig(
                        pageSize = 50,
                        enablePlaceholders = true,
                        initialLoadSize = 100
                    ),
                    pagingSourceFactory = {
                        songDao.searchSongsWithFiltersFtsPaged(
                            sanitizedQuery = sanitizedQuery,
                            artist = filters.artist,
                            album = filters.album,
                            genre = filters.genre,
                            composer = filters.composer,
                            folder = filters.folder,
                            lyrics = filters.lyrics,
                            minDuration = filters.minDuration,
                            maxDuration = filters.maxDuration,
                            minYear = filters.minYear,
                            maxYear = filters.maxYear,
                            favoritesOnly = if (filters.favorites == true) 1 else 0,
                            hiRes = if (filters.hiRes == true) 1 else 0
                        )
                    }
                ).flow
            } else {
                Pager(
                    config = PagingConfig(
                        pageSize = 50,
                        enablePlaceholders = true,
                        initialLoadSize = 100
                    ),
                    pagingSourceFactory = {
                        songDao.searchSongsWithFiltersPaged(
                            query = escapeLike(query),
                            artist = filters.artist,
                            album = filters.album,
                            genre = filters.genre,
                            composer = filters.composer?.let(::escapeLike),
                            folder = filters.folder,
                            lyrics = filters.lyrics?.let(::escapeLike),
                            minDuration = filters.minDuration,
                            maxDuration = filters.maxDuration,
                            minYear = filters.minYear,
                            maxYear = filters.maxYear,
                            favoritesOnly = if (filters.favorites == true) 1 else 0,
                            hiRes = if (filters.hiRes == true) 1 else 0
                        )
                    }
                ).flow
            }
        }
        .cachedIn(viewModelScope)

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    /** Persists a non-empty search so it can appear in the recent-searches list. */
    fun commitSearch(query: String) {
        val trimmed = query.trim()
        if (trimmed.isNotEmpty()) {
            settingsManager.addRecentSearch(trimmed)
            _recentSearches.value = settingsManager.getRecentSearches()
        }
    }

    fun clearRecentSearches() {
        settingsManager.clearRecentSearches()
        _recentSearches.value = emptyList()
    }

    fun updateSearchFilters(filters: SearchFilters?) {
        _searchFilters.value = filters
    }

    private val _recentSearches = MutableStateFlow<List<String>>(
        (settingsManager.getRecentSearches())
    )
    val recentSearches: StateFlow<List<String>> = _recentSearches.asStateFlow()

    // ── Tag editor ─────────────────────────────────────────────────────

    private val _tags = MutableStateFlow<AudioTags?>(null)
    val tags: StateFlow<AudioTags?> = _tags.asStateFlow()

    private var tagLoadToken = 0

    /** Last time an external intent triggered a scan; see [playAudioUri]. */
    private var lastIntentScanAt = 0L

    fun loadTags(songId: Long) {
        val token = ++tagLoadToken
        // Clear first so the editor never shows the previous song's tags.
        _tags.value = null
        viewModelScope.launch(ErrorHandler.coroutineExceptionHandler) {
            val song = songDao.getSongById(songId)
            if (song != null) {
                val result = withContext(Dispatchers.IO) {
                    tagRepository.readTags(song.path)
                }
                // Ignore a stale response if the user opened another song meanwhile.
                if (token == tagLoadToken) {
                    result.onSuccess { _tags.value = it }
                    result.onFailure {
                        ErrorHandler.logError("MainViewModel", "Failed to load tags for song id=${song.id}", it)
                    }
                }
            }
        }
    }

    suspend fun saveTagsAndAwait(songId: Long, newTags: AudioTags): Boolean {
        val song = songDao.getSongById(songId) ?: return false
        val result = withContext(Dispatchers.IO) {
            tagRepository.writeTags(song.path, newTags)
        }
        return result.isSuccess
    }

    fun saveTags(songId: Long, newTags: AudioTags) {
        viewModelScope.launch(ErrorHandler.coroutineExceptionHandler) {
            val song = songDao.getSongById(songId)
            if (song != null) {
                val result = withContext(Dispatchers.IO) {
                    tagRepository.writeTags(song.path, newTags)
                }
                if (result.isSuccess) {
                    // Reflect every editable field back into the library row, not just
                    // title/artist/album — otherwise genre/year edits never surface and
                    // the next scan overwrites the file's new genre from the stale row.
                    val updatedSong = song.copy(
                        title = newTags.title.ifBlank { song.title },
                        artist = newTags.artist.ifBlank { song.artist },
                        album = newTags.album.ifBlank { song.album },
                        genre = newTags.genre.ifBlank { song.genre },
                        composer = newTags.composer.ifBlank { song.composer },
                        year = newTags.year.toIntOrNull() ?: song.year,
                        trackNumber = newTags.trackNumber.substringBefore('/').toIntOrNull() ?: song.trackNumber,
                        lyrics = newTags.lyrics.ifBlank { song.lyrics }
                    )
                    songDao.updateSong(updatedSong)
                } else {
                    ErrorHandler.logError("MainViewModel", "Failed to save tags for song id=${song.id}", result.exceptionOrNull())
                    _uiMessage.tryEmit("Couldn't save tags for ${song.title}")
                }
                // Always clean up temp artwork (success or failure), off the main thread.
                withContext(Dispatchers.IO) { tagRepository.cleanupTempFiles() }
            }
        }
    }

    // ── Theme settings ─────────────────────────────────────────────────

    fun setDarkMode(isDark: Boolean?) = settingsManager.setDarkMode(isDark)
    fun setAmoledBlack(enabled: Boolean) = settingsManager.setAmoledBlack(enabled)
    fun setDynamicColors(enabled: Boolean) = settingsManager.setDynamicColors(enabled)
    fun setPrimaryColor(color: String) = settingsManager.setPrimaryColor(color)
    fun setSecondaryColor(color: String) = settingsManager.setSecondaryColor(color)
    fun setTertiaryColor(color: String) = settingsManager.setTertiaryColor(color)

    // ── Playback settings ──────────────────────────────────────────────

    fun setGaplessPlayback(enabled: Boolean) = settingsManager.setGaplessPlayback(enabled)
    fun setAutoResume(enabled: Boolean) = settingsManager.setAutoResume(enabled)
    fun setSkipSilence(enabled: Boolean) = settingsManager.setSkipSilence(enabled)
    fun setRememberLastPosition(enabled: Boolean) = settingsManager.setRememberLastPosition(enabled)

    // ── Library settings ───────────────────────────────────────────────

    fun setMinTrackDuration(seconds: Int) = settingsManager.setMinTrackDuration(seconds)
    fun setShowDownloadsFolder(enabled: Boolean) = settingsManager.setShowDownloadsFolder(enabled)

    // ── Notification settings ──────────────────────────────────────────

    fun setShowAlbumArt(enabled: Boolean) = settingsManager.setShowAlbumArt(enabled)
    fun setCompactControls(enabled: Boolean) = settingsManager.setCompactControls(enabled)

    // ── Library layout settings ────────────────────────────────────────

    fun setLibraryLayoutMode(mode: String) = settingsManager.setLibraryLayoutMode(mode)
    fun setLibraryGridSize(size: Int) = settingsManager.setLibraryGridSize(size)
    fun setLibraryListDensity(density: String) = settingsManager.setLibraryListDensity(density)



    // ── Playlist management ────────────────────────────────────────────

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

    fun getPlaylist(playlistId: Long): Flow<Playlist?> {
        return playlistDao.getPlaylistByIdFlow(playlistId)
    }

    fun getPlaylistSongs(playlistId: Long): Flow<List<Song>> {
        return playlistDao.getSongsInPlaylist(playlistId)
    }

    fun playPlaylist(songs: List<Song>, startIndex: Int = 0) {
        if (songs.isNotEmpty()) {
            playSongs(songs, startIndex)
        }
    }

    fun removeSongsFromPlaylist(playlistId: Long, songIds: List<Long>) {
        if (songIds.isEmpty()) return
        viewModelScope.launch {
            // One transaction: remove rows + recompute songCount/duration.
            playlistDao.removeSongsFromPlaylistAndRefresh(playlistId, songIds)
        }
    }

    fun addSongsToPlaylist(playlistId: Long, songIds: List<Long>) {
        if (songIds.isEmpty()) return
        viewModelScope.launch {
            // addSongsToPlaylist refreshes songCount/duration inside its transaction.
            playlistDao.addSongsToPlaylist(playlistId, songIds)
        }
    }

    // ── Search filter data sources ─────────────────────────────────────

    val availableArtists: StateFlow<List<String>> = songDao.getAllArtistNames()
        .logAndFallback(emptyList())
        .distinctUntilChanged()
        .stateIn(viewModelScope, shareWhileSubscribed, emptyList())

    val availableAlbums: StateFlow<List<String>> = songDao.getAllAlbumNames()
        .logAndFallback(emptyList())
        .distinctUntilChanged()
        .stateIn(viewModelScope, shareWhileSubscribed, emptyList())

    val availableGenres: StateFlow<List<String>> = songDao.getAllGenres()
        .logAndFallback(emptyList())
        .distinctUntilChanged()
        .stateIn(viewModelScope, shareWhileSubscribed, emptyList())

    val availableComposers: StateFlow<List<String>> = songDao.getAllComposerNames()
        .logAndFallback(emptyList())
        .distinctUntilChanged()
        .stateIn(viewModelScope, shareWhileSubscribed, emptyList())

    val availableFolders: StateFlow<List<String>> = songDao.getAllFolderPaths()
        .logAndFallback(emptyList())
        .distinctUntilChanged()
        .stateIn(viewModelScope, shareWhileSubscribed, emptyList())
}

/** True when the user has configured at least one non-default search filter. */
private fun SearchFilters.hasActiveFilters(): Boolean =
    artist != null || album != null || genre != null || composer != null ||
        folder != null || lyrics != null || minDuration != null || maxDuration != null ||
        minYear != null || maxYear != null || favorites != null || hiRes != null ||
        scope != SearchScope.ALL

/** Logs a query failure and falls back to a default, rather than silently swallowing it. */
private fun <T> Flow<T>.logAndFallback(fallback: T): Flow<T> = catch { e ->
    ErrorHandler.logError("MainViewModel", "Library query failed", e)
    emit(fallback)
}



/** Emitted when deleting a file needs the user's consent (scoped storage, API 29+). */
data class DeleteConsentRequest(
    val songId: Long,
    val intent: PendingIntent
)

enum class SortOption(val label: String) {
    TITLE("Title"),
    ARTIST("Artist"),
    ALBUM("Album"),
    DATE_ADDED("Date Added"),
    DURATION("Duration"),
    PLAY_COUNT("Most Played"),
    SIZE("File Size"),
    DATE_MODIFIED("Date Modified")
}
