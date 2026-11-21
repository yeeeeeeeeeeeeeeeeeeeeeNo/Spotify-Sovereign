package com.spotify.sovereign.ui

import android.app.Application
import android.content.ComponentName
import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.palette.graphics.Palette
import coil3.ImageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.toBitmap
import com.google.common.util.concurrent.MoreExecutors
import com.spotify.sovereign.ai.TasteBrain
import com.spotify.sovereign.ai.TasteDatabase
import com.spotify.sovereign.engine.SovereignAudioEngine
import com.spotify.sovereign.engine.SovereignLyricsEngine
import com.spotify.sovereign.model.AppSettings
import com.spotify.sovereign.model.AppTab
import com.spotify.sovereign.model.AppThemePreset
import com.spotify.sovereign.model.ArtworkStyle
import com.spotify.sovereign.model.AudioFxSettings
import com.spotify.sovereign.model.CollectionItem
import com.spotify.sovereign.model.CustomThemeColors
import com.spotify.sovereign.model.DetailPageData
import com.spotify.sovereign.model.DspEngineStatus
import com.spotify.sovereign.model.HomeFilter
import com.spotify.sovereign.model.HomeShelf
import com.spotify.sovereign.model.LibraryFilter
import com.spotify.sovereign.model.LyricsPayload
import com.spotify.sovereign.model.MusicMoodCategory
import com.spotify.sovereign.model.OnboardingCandidate
import com.spotify.sovereign.model.RepeatMode
import com.spotify.sovereign.model.SearchResults
import com.spotify.sovereign.model.SoundProfile
import com.spotify.sovereign.model.TasteDnaSnapshot
import com.spotify.sovereign.model.TrackColorPalette
import com.spotify.sovereign.model.TrackItem
import com.spotify.sovereign.model.VisualSettings
import com.spotify.sovereign.model.YouTubeAccountProfile
import com.spotify.sovereign.playback.SovereignPlaybackService
import com.spotify.sovereign.ui.theme.SpotifyColors
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import android.webkit.CookieManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class SovereignUiState(
    val showOnboarding: Boolean = true,
    val onboardingCandidates: List<OnboardingCandidate> = emptyList(),
    val onboardingSelected: List<OnboardingCandidate> = emptyList(),
    val onboardingSearchQuery: String = "",
    val onboardingSearchResults: List<OnboardingCandidate> = emptyList(),
    val isLoadingOnboarding: Boolean = false,
    val isImportingPlaylist: Boolean = false,
    val activeTab: AppTab = AppTab.HOME,
    val homeFilter: HomeFilter = HomeFilter.ALL,
    val libraryFilter: LibraryFilter = LibraryFilter.ALL,
    val libraryGridMode: Boolean = false,
    val librarySortAlpha: Boolean = false,
    val isBuildingHome: Boolean = false,
    val quickPicks: List<CollectionItem> = emptyList(),
    val homeShelves: List<HomeShelf> = emptyList(),
    val searchQuery: String = "",
    val isSearching: Boolean = false,
    val searchResults: SearchResults? = null,
    val searchHistory: List<String> = emptyList(),
    val detailPage: DetailPageData? = null,
    val isLoadingDetail: Boolean = false,
    val detailAccentColor: Color = SpotifyColors.DefaultAccent,
    val likedTracks: List<TrackItem> = emptyList(),
    val recentTracks: List<TrackItem> = emptyList(),
    val savedCollections: List<CollectionItem> = emptyList(),
    val currentTrack: TrackItem? = null,
    val queue: List<TrackItem> = emptyList(),
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val shuffleEnabled: Boolean = false,
    val repeatMode: RepeatMode = RepeatMode.OFF,
    val nowPlayingOpen: Boolean = false,
    val fullLyricsOpen: Boolean = false,
    val profileDrawerOpen: Boolean = false,
    val queueSheetOpen: Boolean = false,
    val settingsOpen: Boolean = false,
    val soundFxSheetOpen: Boolean = false,
    val ytLoginOpen: Boolean = false,
    val isSyncingAccount: Boolean = false,
    val ytAccount: YouTubeAccountProfile = YouTubeAccountProfile(),
    val selectedMusicMood: MusicMoodCategory = MusicMoodCategory.FOR_YOU,
    val moodSongsMap: Map<MusicMoodCategory, List<TrackItem>> = emptyMap(),
    val isLoadingMusicMood: Boolean = false,
    val dominantColor: Color = SpotifyColors.DefaultAccent,
    val miniPlayerColor: Color = SpotifyColors.DefaultMiniBg,
    val lyricsCardColor: Color = Color(0xFF333645),
    val trackPalette: TrackColorPalette = TrackColorPalette(),
    val lyricsPayload: LyricsPayload = LyricsPayload(emptyList(), false, "None", 0),
    val isLoadingLyrics: Boolean = false,
    val lyricsOffsetMs: Long = 0L,
    val streamBadge: String = "ULTRA HD",
    val ultraHighAudio: Boolean = true,
    val settings: AppSettings = AppSettings(),
    val dspStatus: DspEngineStatus = DspEngineStatus(),
    val audioCacheBytes: Long = 0L,
    val tasteDna: TasteDnaSnapshot = TasteDnaSnapshot(emptyList(), 0, 0, "Evening", 0),
    val toastMessage: String? = null,
)

class SpotifyViewModel(application: Application) : AndroidViewModel(application) {
    private val appContext = application.applicationContext
    private val db = TasteDatabase(appContext)
    private val engine = SovereignAudioEngine()
    private val lyricsEngine = SovereignLyricsEngine(engine.httpClient)
    private val brain = TasteBrain(db, engine)
    private val imageLoader = ImageLoader(appContext)
    private val settingsJson = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    private fun loadPersistedSettings(): AppSettings {
        val raw = db.getMeta("app_settings_v4", "")
        if (raw.isBlank()) return AppSettings()
        return runCatching { settingsJson.decodeFromString<AppSettings>(raw) }.getOrDefault(AppSettings())
    }

    private fun persistSettings(s: AppSettings) {
        runCatching { db.setMeta("app_settings_v4", settingsJson.encodeToString(s)) }
    }

    private fun loadPersistedYouTubeAccount(): YouTubeAccountProfile {
        val cookie = db.getMeta("yt_account_cookie", "")
        if (cookie.isNotBlank()) {
            engine.setAuthCookie(cookie)
        }
        val raw = db.getMeta("yt_account_profile", "")
        if (raw.isBlank()) return YouTubeAccountProfile(signedIn = engine.hasAuthCookie())
        return runCatching { settingsJson.decodeFromString<YouTubeAccountProfile>(raw) }
            .getOrDefault(YouTubeAccountProfile(signedIn = engine.hasAuthCookie()))
    }

    private val _uiState = MutableStateFlow(
        SovereignUiState(
            showOnboarding = !db.isOnboardingCompleted(),
            likedTracks = db.getLikedTracks(),
            recentTracks = db.getRecentTracks(),
            savedCollections = db.getSavedCollections(),
            searchHistory = db.getSearchHistory(),
            ultraHighAudio = db.getMeta("ultra_high", "1") == "1",
            settings = loadPersistedSettings(),
            ytAccount = loadPersistedYouTubeAccount(),
            tasteDna = brain.getDnaSnapshot(),
        )
    )
    val uiState: StateFlow<SovereignUiState> = _uiState.asStateFlow()

    private var mediaController: MediaController? = null
    private var searchJob: Job? = null
    private var onboardingSearchJob: Job? = null
    private var playJob: Job? = null
    private var preBufferJob: Job? = null
    private var lyricsJob: Job? = null
    private var toastJob: Job? = null

    private var previousTrackIdForMarkov: String? = null
    private var activeTrackStartElapsedMs: Long = 0L
    private var lyricsMatchedDurationSec: Int = 0

    private val starterOnboardingArtists = listOf(
        "The Weeknd", "Travis Scott", "Frank Ocean", "Kendrick Lamar",
        "SZA", "Drake", "Playboi Carti", "Daft Punk",
        "Billie Eilish", "Metro Boomin", "Radiohead", "Tame Impala",
        "Bad Bunny", "Kanye West", "Fred again..", "Tyler, The Creator",
        "Lana Del Rey", "21 Savage", "Arctic Monkeys", "Don Toliver",
    )

    init {
        bindPlaybackService()
        startHighPrecisionProgressLoop()
        if (_uiState.value.showOnboarding) {
            loadStarterOnboardingGrid()
        } else {
            refreshPersonalizedHome()
        }
    }

    private fun bindPlaybackService() {
        runCatching { appContext.startService(Intent(appContext, SovereignPlaybackService::class.java)) }
        val token = SessionToken(appContext, ComponentName(appContext, SovereignPlaybackService::class.java))
        val future = MediaController.Builder(appContext, token).buildAsync()
        future.addListener({
            runCatching {
                val controller = future.get()
                mediaController = controller
                // Push persisted Dolby Atmos & Studio Mastering DSP settings immediately when service connects
                SovereignPlaybackService.activeService?.let { svc ->
                    val st = svc.applyAudioFx(_uiState.value.settings.audioFx)
                    _uiState.update { it.copy(dspStatus = st, audioCacheBytes = svc.getCacheSizeBytes()) }
                }
                controller.addListener(object : Player.Listener {
                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        _uiState.update { it.copy(isPlaying = isPlaying) }
                    }

                    override fun onPlaybackStateChanged(playbackState: Int) {
                        _uiState.update { it.copy(isBuffering = playbackState == Player.STATE_BUFFERING) }
                        if (playbackState == Player.STATE_READY) {
                            recheckLyricsWithExactDuration()
                        }
                        if (playbackState == Player.STATE_ENDED) {
                            onTrackFinishedNaturally()
                        }
                    }

                    override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                        recoverFromStreamError()
                    }
                })
            }
        }, MoreExecutors.directExecutor())
    }

    private var lastErrorRecoveryAtMs = 0L

    private fun recoverFromStreamError() {
        val now = System.currentTimeMillis()
        if (now - lastErrorRecoveryAtMs < 4_000L) return
        lastErrorRecoveryAtMs = now
        val track = _uiState.value.currentTrack ?: return
        val resumeMs = _uiState.value.positionMs.coerceAtLeast(0L)
        viewModelScope.launch {
            runCatching {
                val fresh = engine.resolveAudioStream(track.id, preferUltraHigh = _uiState.value.ultraHighAudio, forceRefresh = true)
                withContext(Dispatchers.Main) {
                    SovereignPlaybackService.activeService?.playStream(
                        url = fresh.url,
                        headers = fresh.headers,
                        title = track.title,
                        artist = track.artist,
                        artworkUri = track.thumbnailUrl.ifBlank { null },
                        mediaId = track.id,
                        startPositionMs = resumeMs,
                    )
                }
            }
        }
    }

    // 100ms high-precision ticker for ultra-smooth Apple-Music-grade synced lyrics
    private fun startHighPrecisionProgressLoop() {
        viewModelScope.launch {
            while (isActive) {
                val player = SovereignPlaybackService.activeService?.player ?: mediaController
                if (player != null) {
                    val pos = player.currentPosition.coerceAtLeast(0L)
                    val realDur = player.duration.takeIf { it > 0L }
                        ?: ((_uiState.value.currentTrack?.durationSec ?: 0) * 1000L)
                    _uiState.update { st ->
                        if (st.positionMs != pos || st.durationMs != realDur || st.isPlaying != player.isPlaying) {
                            st.copy(positionMs = pos, durationMs = realDur, isPlaying = player.isPlaying)
                        } else st
                    }
                }
                delay(120L)
            }
        }
    }

    // =========================================================================
    // 1. SMART PICK-10 ONBOARDING (DYNAMIC EXPANDING ARTISTS + SPOTIFY PASTE)
    // =========================================================================
    private fun loadStarterOnboardingGrid() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingOnboarding = true) }
            val loaded = withContext(Dispatchers.IO) {
                coroutineScope {
                    starterOnboardingArtists.map { name ->
                        async {
                            engine.searchDeezerArtists(name, limit = 1).firstOrNull()
                                ?: OnboardingCandidate(
                                    id = "seed_${name.lowercase()}",
                                    name = name,
                                    subtitle = "Artist",
                                    imageUrl = "",
                                    artistName = name,
                                )
                        }
                    }.awaitAll()
                }
            }
            _uiState.update { it.copy(onboardingCandidates = loaded, isLoadingOnboarding = false) }
        }
    }

    fun onOnboardingSearchChange(query: String) {
        _uiState.update { it.copy(onboardingSearchQuery = query) }
        onboardingSearchJob?.cancel()
        if (query.isBlank()) {
            _uiState.update { it.copy(onboardingSearchResults = emptyList()) }
            return
        }
        onboardingSearchJob = viewModelScope.launch {
            delay(300L)
            val (artists, songs) = withContext(Dispatchers.IO) {
                coroutineScope {
                    val a = async { engine.searchDeezerArtists(query, limit = 5) }
                    val s = async { engine.searchAll(query).songs.take(5) }
                    a.await() to s.await()
                }
            }
            val songCands = songs.map { t ->
                OnboardingCandidate(
                    id = "trk_${t.id}",
                    name = t.title,
                    subtitle = "Song · ${t.artist}",
                    imageUrl = t.thumbnailUrl,
                    isTrack = true,
                    videoId = t.id,
                    artistName = t.artist,
                )
            }
            _uiState.update { it.copy(onboardingSearchResults = artists + songCands) }
        }
    }

    fun toggleOnboardingPick(candidate: OnboardingCandidate) {
        val current = _uiState.value.onboardingSelected
        val exists = current.any { it.id == candidate.id || it.name.equals(candidate.name, ignoreCase = true) }
        if (exists) {
            _uiState.update { st ->
                st.copy(onboardingSelected = st.onboardingSelected.filterNot { it.id == candidate.id || it.name.equals(candidate.name, ignoreCase = true) })
            }
            return
        }
        val updated = (current + candidate).take(20)
        _uiState.update { it.copy(onboardingSelected = updated) }

        // Spotify-style dynamic expansion: spawn similar artists live into the grid!
        viewModelScope.launch {
            val related = engine.fetchRelatedArtistsFromDeezer(candidate.artistName, limit = 4)
            if (related.isNotEmpty()) {
                _uiState.update { st ->
                    val existingNames = st.onboardingCandidates.map { it.name.lowercase() }.toSet()
                    val fresh = related.filterNot { it.name.lowercase() in existingNames }
                    st.copy(onboardingCandidates = st.onboardingCandidates + fresh)
                }
            }
        }
    }

    fun finishOnboarding() {
        val picks = _uiState.value.onboardingSelected
        if (picks.size < 3) {
            showToast("Pick at least 3 artists or songs to train your AI")
            return
        }
        brain.seedFromOnboarding(picks)
        _uiState.update {
            it.copy(
                showOnboarding = false,
                likedTracks = db.getLikedTracks(),
                tasteDna = brain.getDnaSnapshot(),
            )
        }
        refreshPersonalizedHome()
    }

    fun reopenOnboardingToRetrain() {
        _uiState.update { it.copy(profileDrawerOpen = false, showOnboarding = true, onboardingSelected = emptyList()) }
        if (_uiState.value.onboardingCandidates.isEmpty()) {
            loadStarterOnboardingGrid()
        }
    }

    fun importPublicSpotifyPlaylist(url: String, fromOnboarding: Boolean = false) {
        if (url.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isImportingPlaylist = true) }
            try {
                val (playlistName, rawTracks) = engine.importSpotifyPlaylistUrl(url)
                if (rawTracks.isEmpty()) error("No tracks found in that playlist")

                // Resolve first 18 tracks to studio audio tracks in parallel!
                val resolvedTracks = withContext(Dispatchers.IO) {
                    coroutineScope {
                        rawTracks.take(18).map { (title, artist) ->
                            async { engine.searchStudioSong(title, artist) }
                        }.awaitAll().filterNotNull()
                    }
                }

                // Feed every imported track straight into our Local Taste Brain!
                resolvedTracks.forEach { t ->
                    db.upsertTrackTelemetry(t, scoreDelta = 3.0, setLiked = false)
                    db.updateArtistScore(t.artist, delta = 3.5, isPlay = true, imageUrl = t.thumbnailUrl)
                }

                val col = CollectionItem(
                    id = "sp_imp_${System.currentTimeMillis()}",
                    title = playlistName,
                    subtitle = "Imported · ${resolvedTracks.size} studio tracks",
                    thumbnailUrl = resolvedTracks.firstOrNull()?.thumbnailUrl.orEmpty(),
                    type = "playlist",
                    tracks = resolvedTracks,
                    accentHex = 0xFF1DB954,
                )
                db.saveCollection(col)
                db.setOnboardingCompleted(true)

                _uiState.update {
                    it.copy(
                        isImportingPlaylist = false,
                        showOnboarding = if (fromOnboarding) false else it.showOnboarding,
                        savedCollections = db.getSavedCollections(),
                        tasteDna = brain.getDnaSnapshot(),
                    )
                }
                showToast("Imported \"$playlistName\" (${resolvedTracks.size} tracks) into AI Brain!")
                refreshPersonalizedHome()
            } catch (e: Exception) {
                _uiState.update { it.copy(isImportingPlaylist = false) }
                showToast(e.message ?: "Import failed — ensure playlist is Public")
            }
        }
    }

    // =========================================================================
    // 2. PERSONALIZED AI HOME FEED
    // =========================================================================
    fun refreshPersonalizedHome() {
        viewModelScope.launch {
            _uiState.update { it.copy(isBuildingHome = true) }
            runCatching { brain.buildPersonalizedHome() }
                .onSuccess { home ->
                    _uiState.update {
                        it.copy(
                            isBuildingHome = false,
                            quickPicks = home.quickPicks,
                            homeShelves = home.shelves,
                            tasteDna = home.dna,
                        )
                    }
                }
                .onFailure { err ->
                    _uiState.update { it.copy(isBuildingHome = false) }
                    showToast("Feed refresh failed: ${err.message}")
                }
        }
    }

    // =========================================================================
    // 3. SEARCH & NAVIGATION
    // =========================================================================
    fun selectTab(tab: AppTab) = _uiState.update { it.copy(activeTab = tab, detailPage = null) }
    fun setHomeFilter(f: HomeFilter) = _uiState.update { it.copy(homeFilter = f) }
    fun setLibraryFilter(f: LibraryFilter) = _uiState.update { it.copy(libraryFilter = f) }
    fun toggleLibraryGrid() = _uiState.update { it.copy(libraryGridMode = !it.libraryGridMode) }
    fun toggleLibrarySort() = _uiState.update { it.copy(librarySortAlpha = !it.librarySortAlpha) }

    fun onSearchQueryChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        searchJob?.cancel()
        if (query.isBlank()) {
            _uiState.update { it.copy(searchResults = null, isSearching = false) }
            return
        }
        searchJob = viewModelScope.launch {
            delay(340L)
            executeSearch(query)
        }
    }

    fun executeSearch(query: String) {
        val clean = query.trim()
        if (clean.isEmpty()) return
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            val hist = db.addSearchHistory(clean)
            _uiState.update { it.copy(searchQuery = clean, isSearching = true, searchHistory = hist) }
            runCatching { engine.searchAll(clean) }
                .onSuccess { res -> _uiState.update { it.copy(isSearching = false, searchResults = res) } }
                .onFailure { _uiState.update { it.copy(isSearching = false) }; showToast("Search error") }
        }
    }

    fun clearSearch() {
        searchJob?.cancel()
        _uiState.update { it.copy(searchQuery = "", searchResults = null, isSearching = false) }
    }

    fun openCollection(item: CollectionItem) {
        if (item.type == "liked" || item.browseId == "LOCAL_LIKED") {
            val liked = db.getLikedTracks()
            _uiState.update {
                it.copy(
                    detailAccentColor = Color(0xFF4B28B5),
                    detailPage = DetailPageData(
                        id = "LOCAL_LIKED",
                        title = "Liked Songs",
                        subtitle = "Playlist · ${liked.size} songs",
                        description = "Every track here anchors your on-device AI taste model.",
                        thumbnailUrl = "",
                        type = "liked",
                        tracks = liked,
                    ),
                )
            }
            return
        }

        if (item.type == "song" && !item.videoId.isNullOrBlank()) {
            val track = item.tracks.firstOrNull() ?: TrackItem(
                id = item.videoId,
                title = item.title,
                artist = item.subtitle,
                thumbnailUrl = item.thumbnailUrl,
            )
            playTrack(track)
            return
        }

        if (item.tracks.isNotEmpty()) {
            _uiState.update {
                it.copy(
                    detailAccentColor = Color(item.accentHex),
                    detailPage = DetailPageData(
                        id = item.id,
                        title = item.title,
                        subtitle = item.subtitle,
                        description = if (item.type == "ai_mix") "Dynamically generated locally by your Sovereign Taste AI." else "",
                        thumbnailUrl = item.thumbnailUrl,
                        type = item.type,
                        tracks = item.tracks,
                        accentHex = item.accentHex,
                    ),
                )
            }
            viewModelScope.launch {
                extractPaletteColors(item.thumbnailUrl)?.let { p ->
                    _uiState.update { it.copy(detailAccentColor = p.first) }
                }
            }
            return
        }

        if (item.type == "artist" && item.browseId.isBlank()) {
            viewModelScope.launch {
                _uiState.update {
                    it.copy(
                        isLoadingDetail = true,
                        detailPage = DetailPageData(item.id, item.title, "Artist Radio", "", item.thumbnailUrl, "artist", emptyList()),
                    )
                }
                val tracks = engine.searchSongsForArtist(item.title, limit = 15)
                _uiState.update {
                    it.copy(
                        isLoadingDetail = false,
                        detailPage = DetailPageData(item.id, item.title, "Top Studio Tracks", "Curated via Sovereign Engine", item.thumbnailUrl, "artist", tracks),
                    )
                }
                extractPaletteColors(item.thumbnailUrl)?.let { p ->
                    _uiState.update { it.copy(detailAccentColor = p.first) }
                }
            }
            return
        }

        if (item.browseId.isBlank()) {
            executeSearch(item.title)
            selectTab(AppTab.SEARCH)
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoadingDetail = true,
                    detailPage = DetailPageData(item.browseId, item.title, item.subtitle, "", item.thumbnailUrl, item.type, emptyList()),
                )
            }
            extractPaletteColors(item.thumbnailUrl)?.let { p ->
                _uiState.update { it.copy(detailAccentColor = p.first) }
            }
            runCatching {
                engine.browseCollection(item.browseId, item.title, item.subtitle, item.thumbnailUrl, item.type)
            }.onSuccess { detail ->
                _uiState.update { it.copy(isLoadingDetail = false, detailPage = detail) }
            }.onFailure {
                _uiState.update { it.copy(isLoadingDetail = false) }
                showToast("Unable to open collection")
            }
        }
    }

    fun closeDetail() = _uiState.update { it.copy(detailPage = null, isLoadingDetail = false) }

    fun toggleSaveDetail() {
        val page = _uiState.value.detailPage ?: return
        if (page.type == "liked") return
        val saved = _uiState.value.savedCollections
        val exists = saved.any { it.id == page.id }
        if (exists) {
            db.removeCollection(page.id)
        } else {
            db.saveCollection(
                CollectionItem(
                    id = page.id,
                    browseId = if (page.type == "ai_mix") "" else page.id,
                    title = page.title,
                    subtitle = page.subtitle,
                    thumbnailUrl = page.thumbnailUrl,
                    type = page.type,
                    tracks = page.tracks,
                    accentHex = page.accentHex ?: 0xFF1ED760,
                )
            )
        }
        _uiState.update { it.copy(savedCollections = db.getSavedCollections()) }
        showToast(if (exists) "Removed from Library" else "Saved to Your Library")
    }

    // =========================================================================
    // 4. ULTRA-FAST PLAYBACK + PREDICTIVE PRE-BUFFERING + BEHAVIORAL SPYING
    // =========================================================================
    fun playTrack(track: TrackItem, customQueue: List<TrackItem>? = null, fromUserIntent: Boolean = true) {
        // Spy on the PREVIOUS track's listen duration before switching (catches <8s fast skips!)
        val prev = _uiState.value.currentTrack
        if (prev != null && prev.id != track.id) {
            val listenedMs = _uiState.value.positionMs.coerceAtLeast(
                if (activeTrackStartElapsedMs > 0L) (System.currentTimeMillis() - activeTrackStartElapsedMs) else 0L
            )
            brain.recordListenOutcome(
                track = prev,
                playedMs = listenedMs,
                totalDurationMs = _uiState.value.durationMs,
                previousTrackId = previousTrackIdForMarkov,
            )
            previousTrackIdForMarkov = prev.id
        } else if (prev != null && prev.id == track.id && _uiState.value.positionMs > 25_000L) {
            // Instant manual replay -> obsession signal!
            brain.recordListenOutcome(prev, _uiState.value.durationMs, _uiState.value.durationMs, previousTrackIdForMarkov, wasLooped = true)
        }

        if (fromUserIntent) {
            brain.recordIntentTap(track, previousTrackIdForMarkov)
        }

        val newQueue = when {
            customQueue != null && customQueue.isNotEmpty() -> customQueue
            _uiState.value.queue.any { it.id == track.id } -> _uiState.value.queue
            else -> listOf(track) + _uiState.value.queue.filterNot { it.id == track.id }
        }

        activeTrackStartElapsedMs = System.currentTimeMillis()
        lyricsMatchedDurationSec = track.durationSec
        val savedOffset = db.getLyricsOffsetMs(track.id)

        _uiState.update {
            it.copy(
                currentTrack = track,
                queue = newQueue,
                recentTracks = db.getRecentTracks(),
                isBuffering = true,
                positionMs = 0L,
                durationMs = track.durationSec * 1000L,
                lyricsPayload = LyricsPayload(emptyList(), false, "Loading", 0),
                lyricsOffsetMs = savedOffset,
                tasteDna = brain.getDnaSnapshot(),
            )
        }

        // Dynamic OLED + 4-Color Ambient Aurora Palette extraction
        viewModelScope.launch {
            extractFullPalette(track.thumbnailUrl)?.let { (dom, mini, lyr, aurora) ->
                _uiState.update {
                    it.copy(
                        dominantColor = dom,
                        miniPlayerColor = mini,
                        lyricsCardColor = lyr,
                        trackPalette = aurora,
                    )
                }
            }
        }

        // Fetch duration-locked synced lyrics immediately
        fetchLyricsForTrack(track, track.durationSec)

        // Smart AI Autoplay Queue Expansion when queue is short
        if (newQueue.size <= 3) {
            viewModelScope.launch {
                val smartNext = brain.generateSmartAutoplayQueue(track)
                if (smartNext.isNotEmpty()) {
                    _uiState.update { st ->
                        val merged = (st.queue + smartNext).distinctBy { it.id }
                        st.copy(queue = merged)
                    }
                    triggerPredictivePreBuffer()
                }
            }
        }

        // Resolve via OUR Sovereign Multi-Client Engine & stream immediately
        playJob?.cancel()
        playJob = viewModelScope.launch {
            try {
                val stream = engine.resolveAudioStream(track.id, preferUltraHigh = _uiState.value.ultraHighAudio)
                var svc = SovereignPlaybackService.activeService
                var tries = 0
                while (svc == null && tries < 20) {
                    runCatching { appContext.startService(Intent(appContext, SovereignPlaybackService::class.java)) }
                    delay(80L)
                    svc = SovereignPlaybackService.activeService
                    tries++
                }
                withContext(Dispatchers.Main) {
                    svc?.let { activeSvc ->
                        activeSvc.playStream(
                            url = stream.url,
                            headers = stream.headers,
                            title = track.title,
                            artist = track.artist,
                            artworkUri = track.thumbnailUrl.ifBlank { null },
                            mediaId = track.id,
                        )
                        val dspSt = activeSvc.applyAudioFx(_uiState.value.settings.audioFx)
                        _uiState.update { it.copy(dspStatus = dspSt, audioCacheBytes = activeSvc.getCacheSizeBytes()) }
                    }
                }
                val badge = if (stream.fromCache) "${stream.codecLabel} · INSTANT" else stream.codecLabel
                _uiState.update {
                    it.copy(
                        streamBadge = badge,
                        isBuffering = false,
                        tasteDna = brain.getDnaSnapshot(),
                    )
                }
                // Kick off predictive pre-buffering for Track N+1 right away!
                triggerPredictivePreBuffer()
            } catch (e: Exception) {
                _uiState.update { it.copy(isBuffering = false, isPlaying = false) }
                showToast("Stream error: ${e.message?.take(55) ?: "retrying"}")
            }
        }
    }

    /**
     * Predictive Next-Track Pre-Buffering: resolves & caches the opening 384 KB
     * of the next queued song into Media3 SimpleCache in the background!
     */
    private fun triggerPredictivePreBuffer() {
        preBufferJob?.cancel()
        preBufferJob = viewModelScope.launch(Dispatchers.IO) {
            val st = _uiState.value
            val cur = st.currentTrack ?: return@launch
            val idx = st.queue.indexOfFirst { it.id == cur.id }
            if (idx < 0 || st.queue.size <= 1) return@launch
            val nextTrack = st.queue[(idx + 1).mod(st.queue.size)]
            val stream = engine.preResolveStream(nextTrack.id) ?: return@launch
            SovereignPlaybackService.activeService?.preBufferNextTrack(stream.url, stream.headers, nextTrack.id)
            // Also pre-resolve N+2 metadata URL
            if (st.queue.size > 2) {
                val next2 = st.queue[(idx + 2).mod(st.queue.size)]
                engine.preResolveStream(next2.id)
            }
            _uiState.update { it.copy(tasteDna = brain.getDnaSnapshot()) }
        }
    }

    private fun fetchLyricsForTrack(track: TrackItem, durationSec: Int) {
        lyricsJob?.cancel()
        lyricsJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoadingLyrics = true) }
            val payload = lyricsEngine.fetchBestLyrics(track.title, track.artist, durationSec)
            _uiState.update { it.copy(lyricsPayload = payload, isLoadingLyrics = false) }
        }
    }

    private fun recheckLyricsWithExactDuration() {
        val track = _uiState.value.currentTrack ?: return
        val player = SovereignPlaybackService.activeService?.player ?: return
        val realSec = (player.duration / 1000L).toInt()
        if (realSec > 10 && kotlin.math.abs(realSec - lyricsMatchedDurationSec) >= 4) {
            lyricsMatchedDurationSec = realSec
            fetchLyricsForTrack(track, realSec)
        }
    }

    fun adjustLyricsOffset(deltaMs: Long) {
        val track = _uiState.value.currentTrack ?: return
        val next = (_uiState.value.lyricsOffsetMs + deltaMs).coerceIn(-15_000L, 15_000L)
        db.setLyricsOffsetMs(track, next)
        _uiState.update { it.copy(lyricsOffsetMs = next) }
    }

    fun resetLyricsOffset() {
        val track = _uiState.value.currentTrack ?: return
        db.setLyricsOffsetMs(track, 0L)
        _uiState.update { it.copy(lyricsOffsetMs = 0L) }
    }

    fun togglePlayPause() {
        val player = SovereignPlaybackService.activeService?.player ?: mediaController ?: return
        if (player.isPlaying) {
            player.pause()
        } else if (player.mediaItemCount == 0 && _uiState.value.currentTrack != null) {
            playTrack(_uiState.value.currentTrack!!, fromUserIntent = false)
        } else {
            player.play()
        }
    }

    fun seekTo(ms: Long) {
        val player = SovereignPlaybackService.activeService?.player ?: mediaController ?: return
        player.seekTo(ms.coerceAtLeast(0L))
        _uiState.update { it.copy(positionMs = ms.coerceAtLeast(0L)) }
    }

    fun skipNext() {
        val st = _uiState.value
        val cur = st.currentTrack ?: return
        if (st.queue.isEmpty()) return
        val idx = st.queue.indexOfFirst { it.id == cur.id }
        val nextTrack = if (st.shuffleEnabled && st.queue.size > 1) {
            st.queue.filter { it.id != cur.id }.random()
        } else {
            st.queue[(idx + 1).mod(st.queue.size)]
        }
        playTrack(nextTrack, st.queue, fromUserIntent = false)
    }

    fun skipPrevious() {
        val st = _uiState.value
        if (st.positionMs > 3_500L) {
            seekTo(0L)
            return
        }
        val cur = st.currentTrack ?: return
        if (st.queue.isEmpty()) return
        val idx = st.queue.indexOfFirst { it.id == cur.id }
        val prevIdx = if (idx <= 0) st.queue.lastIndex else idx - 1
        playTrack(st.queue[prevIdx], st.queue, fromUserIntent = false)
    }

    private fun onTrackFinishedNaturally() {
        val st = _uiState.value
        val cur = st.currentTrack ?: return
        brain.recordListenOutcome(
            track = cur,
            playedMs = st.durationMs,
            totalDurationMs = st.durationMs,
            previousTrackId = previousTrackIdForMarkov,
            wasLooped = st.repeatMode == RepeatMode.ONE,
        )
        previousTrackIdForMarkov = cur.id
        when (st.repeatMode) {
            RepeatMode.ONE -> {
                seekTo(0L)
                SovereignPlaybackService.activeService?.player?.play()
            }
            RepeatMode.ALL, RepeatMode.OFF -> skipNext()
        }
    }

    fun toggleShuffle() {
        val next = !_uiState.value.shuffleEnabled
        _uiState.update { it.copy(shuffleEnabled = next) }
        showToast(if (next) "Shuffle on" else "Shuffle off")
    }

    fun cycleRepeat() {
        val next = when (_uiState.value.repeatMode) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }
        _uiState.update { it.copy(repeatMode = next) }
        showToast(when (next) { RepeatMode.OFF -> "Repeat off"; RepeatMode.ALL -> "Repeat all"; RepeatMode.ONE -> "Repeat one" })
    }

    fun toggleLikeTrack(track: TrackItem) {
        val isLiked = _uiState.value.likedTracks.any { it.id == track.id }
        val newLiked = !isLiked
        brain.recordLike(track, liked = newLiked)
        val updatedLiked = db.getLikedTracks()
        _uiState.update { st ->
            val updatedDetail = if (st.detailPage?.type == "liked") {
                st.detailPage.copy(subtitle = "Playlist · ${updatedLiked.size} songs", tracks = updatedLiked)
            } else st.detailPage
            st.copy(
                likedTracks = updatedLiked,
                detailPage = updatedDetail,
                tasteDna = brain.getDnaSnapshot(),
            )
        }
        // 2-Way Sync with signed-in YouTube Music account!
        if (_uiState.value.ytAccount.signedIn) {
            viewModelScope.launch(Dispatchers.IO) {
                engine.syncTrackLikeToYouTube(track.id, newLiked)
            }
        }
        showToast(if (isLiked) "Removed from Liked Songs" else "Added to Liked Songs · Synced")
    }

    fun toggleUltraHighAudio() {
        val next = !_uiState.value.ultraHighAudio
        db.setMeta("ultra_high", if (next) "1" else "0")
        _uiState.update { it.copy(ultraHighAudio = next) }
        showToast(if (next) "Audio: Ultra-High Opus / AAC" else "Audio: Balanced Data Saver")
    }

    fun openNowPlaying() = _uiState.update { it.copy(nowPlayingOpen = true) }
    fun closeNowPlaying() = _uiState.update { it.copy(nowPlayingOpen = false, fullLyricsOpen = false, soundFxSheetOpen = false) }
    fun openFullLyrics() = _uiState.update { it.copy(fullLyricsOpen = true) }
    fun closeFullLyrics() = _uiState.update { it.copy(fullLyricsOpen = false) }
    fun setProfileDrawer(open: Boolean) = _uiState.update { it.copy(profileDrawerOpen = open, tasteDna = brain.getDnaSnapshot()) }
    fun setQueueSheet(open: Boolean) = _uiState.update { it.copy(queueSheetOpen = open) }
    fun setSoundFxSheet(open: Boolean) {
        val svc = SovereignPlaybackService.activeService
        _uiState.update {
            it.copy(
                soundFxSheetOpen = open,
                dspStatus = svc?.getDspStatus() ?: it.dspStatus,
            )
        }
    }

    fun openSettings() {
        val svc = SovereignPlaybackService.activeService
        _uiState.update {
            it.copy(
                settingsOpen = true,
                profileDrawerOpen = false,
                soundFxSheetOpen = false,
                dspStatus = svc?.getDspStatus() ?: it.dspStatus,
                audioCacheBytes = svc?.getCacheSizeBytes() ?: it.audioCacheBytes,
                tasteDna = brain.getDnaSnapshot(),
            )
        }
    }

    fun closeSettings() = _uiState.update { it.copy(settingsOpen = false) }

    // =========================================================================
    // 5. SOVEREIGN SOUND ENGINE (DOLBY ATMOS + 10-BAND DSP) & VISUAL STUDIO
    // =========================================================================
    fun updateAudioFx(updated: AudioFxSettings) {
        val nextSettings = _uiState.value.settings.copy(audioFx = updated)
        persistSettings(nextSettings)
        val dspSt = SovereignPlaybackService.activeService?.applyAudioFx(updated) ?: _uiState.value.dspStatus
        _uiState.update { it.copy(settings = nextSettings, dspStatus = dspSt) }
    }

    fun selectSoundProfile(profile: SoundProfile) {
        val cur = _uiState.value.settings.audioFx
        val updated = cur.copy(
            masterEnabled = profile != SoundProfile.FLAT_REFERENCE,
            profile = profile,
            atmosSpatialEnabled = profile.atmosSpatial,
            spatialWidth = profile.spatialWidth,
            bassBoost = profile.bassBoost,
            clarityExciter = profile.clarityExciter,
            multibandLimiter = profile.multibandComp,
            inputGainDb = profile.inputGainDb,
            customBandsDb = if (profile == SoundProfile.CUSTOM) cur.customBandsDb else profile.bandsDb,
        )
        updateAudioFx(updated)
        showToast("Sound Profile: ${profile.title}")
    }

    fun updateCustomEqBand(bandIndex: Int, gainDb: Float) {
        val cur = _uiState.value.settings.audioFx
        val mutable = cur.effectiveBandsDb().toMutableList()
        if (bandIndex in mutable.indices) {
            mutable[bandIndex] = gainDb.coerceIn(-10f, 10f)
        }
        updateAudioFx(
            cur.copy(
                masterEnabled = true,
                profile = SoundProfile.CUSTOM,
                customBandsDb = mutable,
            )
        )
    }

    fun resetCustomEq() {
        val cur = _uiState.value.settings.audioFx
        updateAudioFx(
            cur.copy(
                masterEnabled = true,
                profile = SoundProfile.CUSTOM,
                customBandsDb = List(10) { 0f },
            )
        )
        showToast("10-Band EQ reset to flat")
    }

    fun updateVisualSettings(updated: VisualSettings) {
        val nextSettings = _uiState.value.settings.copy(visual = updated)
        persistSettings(nextSettings)
        _uiState.update { it.copy(settings = nextSettings) }
    }

    fun cycleArtworkStyle() {
        val cur = _uiState.value.settings.visual
        val styles = ArtworkStyle.entries
        val nextStyle = styles[(styles.indexOf(cur.artworkStyle) + 1) % styles.size]
        updateVisualSettings(cur.copy(artworkStyle = nextStyle))
        showToast("Artwork Stage: ${nextStyle.title}")
    }

    fun clearAudioCache() {
        viewModelScope.launch(Dispatchers.IO) {
            val freed = SovereignPlaybackService.activeService?.clearAudioCache() ?: 0L
            val after = SovereignPlaybackService.activeService?.getCacheSizeBytes() ?: 0L
            _uiState.update { it.copy(audioCacheBytes = after) }
            val mb = String.format(java.util.Locale.US, "%.1f MB", freed / (1024.0 * 1024.0))
            showToast("Cleared $mb of cached audio")
        }
    }

    // =========================================================================
    // 6. YOUTUBE MUSIC ACCOUNT AUTH + 2-WAY LIBRARY/LIKES/PLAYLISTS SYNC
    // =========================================================================
    fun openYouTubeLogin() {
        _uiState.update { it.copy(ytLoginOpen = true, profileDrawerOpen = false) }
    }

    fun closeYouTubeLogin() {
        _uiState.update { it.copy(ytLoginOpen = false) }
    }

    fun onYouTubeCookiesCaptured(cookie: String) {
        db.setMeta("yt_account_cookie", cookie)
        engine.setAuthCookie(cookie)
        _uiState.update { it.copy(ytLoginOpen = false) }
        syncYouTubeAccountNow(showSuccessToast = true)
    }

    fun syncYouTubeAccountNow(showSuccessToast: Boolean = true) {
        if (!engine.hasAuthCookie()) {
            openYouTubeLogin()
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSyncingAccount = true) }
            runCatching {
                val (profile, liked, playlists) = withContext(Dispatchers.IO) {
                    coroutineScope {
                        val p = async { engine.fetchYouTubeAccountProfile() }
                        val l = async { engine.fetchYouTubeLikedSongs(limit = 60) }
                        val pl = async { engine.fetchYouTubePlaylists() }
                        Triple(p.await(), l.await(), pl.await())
                    }
                }
                val finalProfile = profile ?: YouTubeAccountProfile(
                    name = "YouTube Music",
                    handle = "Connected",
                    signedIn = true,
                )
                db.setMeta("yt_account_profile", settingsJson.encodeToString(finalProfile))

                // Merge YouTube Liked Songs straight into local Liked Songs & Taste Brain
                liked.forEach { t ->
                    db.upsertTrackTelemetry(t, scoreDelta = 2.5, setLiked = true)
                    db.updateArtistScore(t.artist, delta = 2.0, imageUrl = t.thumbnailUrl)
                }
                // Merge YouTube Playlists into Your Library
                playlists.forEach { col ->
                    db.saveCollection(col)
                }

                _uiState.update {
                    it.copy(
                        isSyncingAccount = false,
                        ytAccount = finalProfile,
                        likedTracks = db.getLikedTracks(),
                        savedCollections = db.getSavedCollections(),
                        tasteDna = brain.getDnaSnapshot(),
                    )
                }
                if (showSuccessToast) {
                    showToast("Synced ${finalProfile.name}: ${liked.size} likes · ${playlists.size} playlists")
                }
            }.onFailure { err ->
                _uiState.update { it.copy(isSyncingAccount = false) }
                showToast("Account sync error: ${err.message?.take(48) ?: "try again"}")
            }
        }
    }

    fun signOutYouTubeAccount() {
        db.setMeta("yt_account_cookie", "")
        db.setMeta("yt_account_profile", "")
        engine.setAuthCookie(null)
        runCatching {
            CookieManager.getInstance().removeAllCookies(null)
            CookieManager.getInstance().flush()
        }
        _uiState.update { it.copy(ytAccount = YouTubeAccountProfile(signedIn = false)) }
        showToast("Signed out of YouTube Music")
    }

    // =========================================================================
    // 7. HOME "MUSIC" TAB — PURE SONGS BY MOOD & CATEGORY
    // =========================================================================
    fun selectMusicMood(mood: MusicMoodCategory) {
        _uiState.update { it.copy(selectedMusicMood = mood) }
        if (mood == MusicMoodCategory.FOR_YOU || _uiState.value.moodSongsMap.containsKey(mood)) {
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMusicMood = true) }
            val tracks = runCatching { engine.searchSongsByMood(mood.query, limit = 20) }.getOrDefault(emptyList())
            _uiState.update { st ->
                st.copy(
                    isLoadingMusicMood = false,
                    moodSongsMap = st.moodSongsMap + (mood to tracks),
                )
            }
        }
    }

    fun showToast(msg: String) {
        toastJob?.cancel()
        toastJob = viewModelScope.launch {
            _uiState.update { it.copy(toastMessage = msg) }
            delay(2200L)
            _uiState.update { it.copy(toastMessage = null) }
        }
    }

    private data class ExtractedColors(
        val dominant: Color,
        val mini: Color,
        val lyrics: Color,
        val aurora: TrackColorPalette,
    )

    /**
     * Bulletproof Album-Art Color Extractor:
     * - Scans up to 24 real swatches ranked by saturation + population (ZERO ugly green fallback!)
     * - Detects pure black / monochrome / grayscale covers and outputs sleek Platinum Silver-White (#E6E8F0)
     * - Boosts HSV luminance so Progress Bar, Switches, and Active UI ALWAYS pop vividly on every song!
     */
    private suspend fun extractFullPalette(url: String): ExtractedColors? = withContext(Dispatchers.IO) {
        if (url.isBlank()) return@withContext null
        runCatching {
            val req = ImageRequest.Builder(appContext).data(url).build()
            val result = imageLoader.execute(req) as? SuccessResult ?: return@runCatching null
            val bmp: Bitmap = result.image.toBitmap().copy(Bitmap.Config.ARGB_8888, false)
            val palette = Palette.from(bmp).maximumColorCount(24).generate()

            val hsv = FloatArray(3)
            data class ScoredSwatch(val rgb: Int, val hue: Float, val sat: Float, val value: Float, val pop: Int)

            val allSwatches = palette.swatches.map { sw ->
                android.graphics.Color.colorToHSV(sw.rgb, hsv)
                ScoredSwatch(sw.rgb, hsv[0], hsv[1], hsv[2], sw.population)
            }

            val colorfulSwatches = allSwatches.filter { it.sat >= 0.16f && it.value >= 0.12f }
            val isMonochrome = colorfulSwatches.isEmpty()

            if (isMonochrome) {
                // Sleek Monochrome / Pure-Black Cover Palette (Zero ugly green fallback!)
                val platinum = Color(0xFFE6E8F0)
                val charcoalVibrant = Color(0xFF3E4250)
                val obsidianDark = Color(0xFF191B22)
                val slateMuted = Color(0xFF282B36)
                return@runCatching ExtractedColors(
                    dominant = Color(0xFF282B34),
                    mini = Color(0xFF181A20),
                    lyrics = Color(0xFF2D303A),
                    aurora = TrackColorPalette(
                        vibrant = charcoalVibrant,
                        darkVibrant = obsidianDark,
                        muted = slateMuted,
                        highlightAccent = platinum,
                        subtleBgTint = Color(0xFF08090C),
                        isMonochrome = true,
                    ),
                )
            }

            // Pick best vivid accent swatch (prefers rich saturation + meaningful pixel area)
            val primary = colorfulSwatches.maxByOrNull { sw ->
                val satWeight = sw.sat * sw.sat
                val brightWeight = if (sw.value > 0.25f) 1.2f else 0.7f
                satWeight * brightWeight * kotlin.math.ln((sw.pop + 10).toDouble()).toFloat()
            } ?: colorfulSwatches.first()

            // Pick a secondary complementary/distinct hue swatch if present
            val secondary = colorfulSwatches
                .filter { sw ->
                    val dh = kotlin.math.abs(sw.hue - primary.hue)
                    val hueDist = kotlin.math.min(dh, 360f - dh)
                    hueDist >= 28f
                }
                .maxByOrNull { it.sat * kotlin.math.ln((it.pop + 5).toDouble()).toFloat() }
                ?: primary

            // Build vivid readable Accent (high brightness V=0.84..0.98 so progress bar & pills POP!)
            val accentRgb = android.graphics.Color.HSVToColor(
                floatArrayOf(
                    primary.hue,
                    primary.sat.coerceIn(0.42f, 0.88f),
                    primary.value.coerceIn(0.84f, 0.98f),
                )
            )
            // Build rich Aurora Vibrant Orb
            val vibrantRgb = android.graphics.Color.HSVToColor(
                floatArrayOf(
                    primary.hue,
                    primary.sat.coerceIn(0.45f, 0.90f),
                    primary.value.coerceIn(0.48f, 0.78f),
                )
            )
            // Build deep Velvet Dark Orb
            val darkRgb = android.graphics.Color.HSVToColor(
                floatArrayOf(
                    primary.hue,
                    primary.sat.coerceIn(0.40f, 0.85f),
                    0.24f,
                )
            )
            // Build complementary Muted Orb
            val mutedRgb = android.graphics.Color.HSVToColor(
                floatArrayOf(
                    secondary.hue,
                    secondary.sat.coerceIn(0.32f, 0.72f),
                    0.34f,
                )
            )
            // Build ultra-deep Subtle Background Tint for Match Current Song theme
            val bgTintRgb = android.graphics.Color.HSVToColor(
                floatArrayOf(
                    primary.hue,
                    primary.sat.coerceIn(0.25f, 0.55f),
                    0.075f,
                )
            )

            val rawVibrant = Color(vibrantRgb)
            val dominant = Color(
                (rawVibrant.red * 0.65f).coerceIn(0.06f, 0.72f),
                (rawVibrant.green * 0.65f).coerceIn(0.06f, 0.72f),
                (rawVibrant.blue * 0.65f).coerceIn(0.06f, 0.72f),
                1f,
            )
            val mini = Color(
                (rawVibrant.red * 0.36f).coerceIn(0.05f, 0.42f),
                (rawVibrant.green * 0.36f).coerceIn(0.05f, 0.42f),
                (rawVibrant.blue * 0.36f).coerceIn(0.05f, 0.42f),
                1f,
            )
            val lyrics = Color(
                (rawVibrant.red * 0.74f).coerceIn(0.12f, 0.76f),
                (rawVibrant.green * 0.74f).coerceIn(0.12f, 0.76f),
                (rawVibrant.blue * 0.74f).coerceIn(0.12f, 0.76f),
                1f,
            )

            ExtractedColors(
                dominant = dominant,
                mini = mini,
                lyrics = lyrics,
                aurora = TrackColorPalette(
                    vibrant = rawVibrant,
                    darkVibrant = Color(darkRgb),
                    muted = Color(mutedRgb),
                    highlightAccent = Color(accentRgb),
                    subtleBgTint = Color(bgTintRgb),
                    isMonochrome = false,
                ),
            )
        }.getOrNull()
    }

    private suspend fun extractPaletteColors(url: String): Triple<Color, Color, Color>? {
        val full = extractFullPalette(url) ?: return null
        return Triple(full.dominant, full.mini, full.lyrics)
    }

    override fun onCleared() {
        mediaController?.release()
        super.onCleared()
    }
}
