package com.spotify.sovereign.ai

import com.spotify.sovereign.engine.SovereignAudioEngine
import com.spotify.sovereign.model.CollectionItem
import com.spotify.sovereign.model.HomeShelf
import com.spotify.sovereign.model.OnboardingCandidate
import com.spotify.sovereign.model.TasteDnaSnapshot
import com.spotify.sovereign.model.TrackItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext

/**
 * Sovereign On-Device Behavioral AI Taste & Recommendation Brain.
 * Spies locally on skips (<8s bury), full listens, loops, likes, searches,
 * Markov track-to-track transitions, and time-of-day mood profiles to synthesize
 * Spotify-grade Daily Mixes, Discover Weekly, Time-of-Day Mixes, and Smart Autoplay.
 */
class TasteBrain(
    private val db: TasteDatabase,
    private val engine: SovereignAudioEngine,
) {
    data class GeneratedHome(
        val quickPicks: List<CollectionItem>,
        val shelves: List<HomeShelf>,
        val dna: TasteDnaSnapshot,
    )

    // =========================================================================
    // 1. BEHAVIORAL SPYING HOOKS (ZERO CLOUD, 100% LOCAL)
    // =========================================================================
    fun recordListenOutcome(
        track: TrackItem,
        playedMs: Long,
        totalDurationMs: Long,
        previousTrackId: String?,
        wasLooped: Boolean = false,
    ) {
        val dur = totalDurationMs.coerceAtLeast((track.durationSec * 1000L).coerceAtLeast(30_000L))
        val ratio = playedMs.toDouble() / dur.toDouble()

        when {
            wasLooped -> {
                db.upsertTrackTelemetry(track, scoreDelta = +3.5, incPlay = true, incLoop = true, touchPlayedNow = true)
                db.updateArtistScore(track.artist, delta = +2.2, isPlay = true, imageUrl = track.thumbnailUrl)
            }
            playedMs in 1_200L..8_000L -> {
                // Fast Skip -> ruthless penalty so garbage disappears from recommendations
                db.upsertTrackTelemetry(track, scoreDelta = -2.5, incSkip = true)
                db.updateArtistScore(track.artist, delta = -0.8, isSkip = true)
            }
            ratio < 0.40 && playedMs > 8_000L -> {
                db.upsertTrackTelemetry(track, scoreDelta = -0.6, incSkip = true)
                db.updateArtistScore(track.artist, delta = -0.25, isSkip = true)
            }
            ratio >= 0.72 || playedMs >= 90_000L -> {
                // Full engaged listen -> strong affinity + Markov vibe link
                db.upsertTrackTelemetry(track, scoreDelta = +1.8, incPlay = true, touchPlayedNow = true)
                db.updateArtistScore(track.artist, delta = +1.1, isPlay = true, imageUrl = track.thumbnailUrl)
                if (!previousTrackId.isNullOrBlank()) {
                    db.recordTransition(previousTrackId, track.id, delta = 1.2)
                }
            }
            else -> {
                db.upsertTrackTelemetry(track, scoreDelta = +0.4, incPlay = true, touchPlayedNow = true)
                db.updateArtistScore(track.artist, delta = +0.3, isPlay = true, imageUrl = track.thumbnailUrl)
            }
        }
    }

    fun recordIntentTap(track: TrackItem, previousTrackId: String? = null) {
        db.upsertTrackTelemetry(track, scoreDelta = +1.6, incPlay = true, touchPlayedNow = true)
        db.updateArtistScore(track.artist, delta = +1.0, isPlay = true, imageUrl = track.thumbnailUrl)
        if (!previousTrackId.isNullOrBlank()) {
            db.recordTransition(previousTrackId, track.id, delta = 0.6)
        }
    }

    fun recordLike(track: TrackItem, liked: Boolean) {
        db.upsertTrackTelemetry(
            track = track,
            scoreDelta = if (liked) +4.0 else -1.5,
            setLiked = liked,
        )
        db.updateArtistScore(track.artist, delta = if (liked) +2.5 else -0.6, imageUrl = track.thumbnailUrl)
    }

    fun seedFromOnboarding(selected: List<OnboardingCandidate>) {
        selected.forEachIndexed { idx, cand ->
            val weight = 8.0 - (idx * 0.25).coerceAtMost(3.0)
            db.updateArtistScore(cand.artistName, delta = weight, isPlay = true, imageUrl = cand.imageUrl)
            if (cand.isTrack && !cand.videoId.isNullOrBlank()) {
                val track = TrackItem(
                    id = cand.videoId,
                    title = cand.name,
                    artist = cand.artistName,
                    thumbnailUrl = cand.imageUrl,
                )
                db.upsertTrackTelemetry(track, scoreDelta = 5.0, incPlay = true, setLiked = true, touchPlayedNow = true)
            }
        }
        db.setOnboardingCompleted(true)
    }

    fun getDnaSnapshot(): TasteDnaSnapshot {
        val (plays, skips) = db.getTotalTelemetryCounts()
        return TasteDnaSnapshot(
            topArtists = db.getTopArtists(8),
            totalPlays = plays,
            totalSkipsBlocked = skips,
            currentTimeBucket = db.currentTimeBucketLabel(),
            cachedStreamsCount = engine.cachedStreamCount(),
        )
    }

    // =========================================================================
    // 2. AI PERSONALIZED HOME GENERATOR (REPLACES GENERIC YT MUSIC HOME!)
    // =========================================================================
    suspend fun buildPersonalizedHome(): GeneratedHome = withContext(Dispatchers.IO) {
        val topArtists = db.getTopArtists(10)
        val artistScores = db.getArtistScoreMap()
        val blockedArtists = db.getBlockedArtistKeys()
        val skippedTracks = db.getSkippedTrackIds()
        val knownIds = db.getAllKnownTrackIds()
        val likedTracks = db.getLikedTracks()
        val onRepeat = db.getOnRepeatTracks(18)
        val timeBucket = db.currentTimeBucketLabel()

        val seedArtistNames = topArtists.map { it.name }.ifEmpty {
            listOf("The Weeknd", "Travis Scott", "Frank Ocean", "Kendrick Lamar", "SZA")
        }

        // Parallel Multi-Graph Candidate Harvest (Studio tracks for top artists + Related discovery artists)
        val (artistTrackPools, relatedArtists) = coroutineScope {
            val topTrackJobs = seedArtistNames.take(6).map { name ->
                async { name to runCatching { engine.searchSongsForArtist(name, limit = 8) }.getOrDefault(emptyList()) }
            }
            val relatedArtistJobs = seedArtistNames.take(3).map { name ->
                async { runCatching { engine.fetchRelatedArtistsFromDeezer(name, limit = 5) }.getOrDefault(emptyList()) }
            }
            topTrackJobs.awaitAll().toMap() to relatedArtistJobs.awaitAll().flatten().distinctBy { it.name.lowercase() }
        }

        // Harvest fresh unseen tracks from discovered related artists (for Discover Weekly!)
        val freshDiscoverTracks = coroutineScope {
            relatedArtists
                .filterNot { it.name.lowercase() in blockedArtists }
                .filterNot { rel -> seedArtistNames.any { it.equals(rel.name, ignoreCase = true) } }
                .take(5)
                .map { rel ->
                    async { runCatching { engine.searchSongsForArtist(rel.name, limit = 4) }.getOrDefault(emptyList()) }
                }
                .awaitAll()
                .flatten()
                .filterNot { it.id in skippedTracks }
                .distinctBy { it.id }
        }

        // Ranker Function: ArtistAffinity + TimeOfDay + Novelty - SkipPenalty
        fun rankTracks(tracks: List<TrackItem>, preferNovelty: Boolean): List<TrackItem> {
            return tracks
                .filterNot { it.id in skippedTracks }
                .filterNot { primaryKey(it.artist) in blockedArtists }
                .distinctBy { it.id }
                .sortedByDescending { t ->
                    val aScore = artistScores[primaryKey(t.artist)] ?: 1.0
                    val novelty = if (t.id !in knownIds) (if (preferNovelty) 6.0 else 1.2) else 0.0
                    aScore + novelty
                }
        }

        val allPool = artistTrackPools.values.flatten()

        // Synthesize Custom AI Daily Mixes (Clustered by Artist Neighborhoods)
        val dailyMix1Artists = seedArtistNames.take(2)
        val dailyMix1Tracks = rankTracks(
            dailyMix1Artists.flatMap { artistTrackPools[it].orEmpty() } + freshDiscoverTracks.take(4),
            preferNovelty = false,
        ).take(16)

        val dailyMix2Artists = seedArtistNames.drop(2).take(2).ifEmpty { seedArtistNames.take(2) }
        val dailyMix2Tracks = rankTracks(
            dailyMix2Artists.flatMap { artistTrackPools[it].orEmpty() } + freshDiscoverTracks.drop(4).take(4),
            preferNovelty = false,
        ).take(16)

        val dailyMix3Artists = seedArtistNames.drop(4).take(2).ifEmpty { relatedArtists.take(2).map { it.name } }
        val dailyMix3Tracks = rankTracks(
            dailyMix3Artists.flatMap { artistTrackPools[it].orEmpty() } + freshDiscoverTracks.take(6),
            preferNovelty = true,
        ).take(16)

        val discoverWeeklyTracks = rankTracks(
            freshDiscoverTracks + allPool.filter { it.id !in knownIds },
            preferNovelty = true,
        ).take(18)

        val vibeMixTitle = when (timeBucket) {
            "Late Night" -> "Late Night Drive"
            "Morning" -> "Morning Momentum"
            "Afternoon" -> "Afternoon Focus"
            else -> "Evening Flow"
        }
        val vibeMixTracks = rankTracks(allPool.shuffled().take(10) + freshDiscoverTracks.take(6), preferNovelty = false).take(16)

        val dailyMix1 = CollectionItem(
            id = "ai_daily_mix_1",
            title = "Daily Mix 1",
            subtitle = dailyMix1Artists.joinToString(", ") + " and more",
            thumbnailUrl = dailyMix1Tracks.firstOrNull()?.thumbnailUrl.orEmpty(),
            type = "ai_mix",
            tracks = dailyMix1Tracks,
            accentHex = 0xFF157A6E,
        )
        val dailyMix2 = CollectionItem(
            id = "ai_daily_mix_2",
            title = "Daily Mix 2",
            subtitle = dailyMix2Artists.joinToString(", ") + " and more",
            thumbnailUrl = dailyMix2Tracks.firstOrNull()?.thumbnailUrl.orEmpty(),
            type = "ai_mix",
            tracks = dailyMix2Tracks,
            accentHex = 0xFF8B1E3F,
        )
        val dailyMix3 = CollectionItem(
            id = "ai_daily_mix_3",
            title = "Daily Mix 3",
            subtitle = dailyMix3Artists.joinToString(", ").ifBlank { "Fresh picks for your taste" },
            thumbnailUrl = dailyMix3Tracks.firstOrNull()?.thumbnailUrl.orEmpty(),
            type = "ai_mix",
            tracks = dailyMix3Tracks,
            accentHex = 0xFF6B4226,
        )
        val discoverWeekly = CollectionItem(
            id = "ai_discover_weekly",
            title = "Discover Weekly",
            subtitle = "AI deep cuts you haven't heard yet",
            thumbnailUrl = discoverWeeklyTracks.firstOrNull()?.thumbnailUrl.orEmpty(),
            type = "ai_mix",
            tracks = discoverWeeklyTracks,
            accentHex = 0xFF234788,
        )
        val timeVibeMix = CollectionItem(
            id = "ai_time_vibe",
            title = vibeMixTitle,
            subtitle = "Tuned for your $timeBucket listening DNA",
            thumbnailUrl = vibeMixTracks.firstOrNull()?.thumbnailUrl.orEmpty(),
            type = "ai_mix",
            tracks = vibeMixTracks,
            accentHex = 0xFF53287B,
        )
        val onRepeatMix = CollectionItem(
            id = "ai_on_repeat",
            title = "On Repeat",
            subtitle = "Songs you can't stop replaying",
            thumbnailUrl = (onRepeat.firstOrNull() ?: dailyMix1Tracks.firstOrNull())?.thumbnailUrl.orEmpty(),
            type = "ai_mix",
            tracks = onRepeat.ifEmpty { dailyMix1Tracks.take(10) },
            accentHex = 0xFF1F6E43,
        )
        val likedCard = CollectionItem(
            id = "liked-songs",
            browseId = "LOCAL_LIKED",
            title = "Liked Songs",
            subtitle = "${likedTracks.size} songs",
            type = "liked",
            pinned = true,
            tracks = likedTracks,
            accentHex = 0xFF4B28B5,
        )

        val quickPicks = listOfNotNull(
            likedCard,
            dailyMix1.takeIf { it.tracks.isNotEmpty() },
            timeVibeMix.takeIf { it.tracks.isNotEmpty() },
            discoverWeekly.takeIf { it.tracks.isNotEmpty() },
            dailyMix2.takeIf { it.tracks.isNotEmpty() },
            onRepeatMix.takeIf { it.tracks.isNotEmpty() },
            dailyMix3.takeIf { it.tracks.isNotEmpty() },
            db.getSavedCollections().firstOrNull(),
        ).take(8)

        val shelves = buildList {
            add(
                HomeShelf(
                    id = "made_for_you",
                    title = "Made For You",
                    subtitle = "Synthesized locally by your Taste AI · $timeBucket Mode",
                    items = listOf(dailyMix1, dailyMix2, discoverWeekly, timeVibeMix, dailyMix3, onRepeatMix)
                        .filter { it.tracks.isNotEmpty() },
                )
            )

            if (discoverWeeklyTracks.isNotEmpty()) {
                add(
                    HomeShelf(
                        id = "fresh_cuts",
                        title = "Fresh Picks For You",
                        subtitle = "Unseen tracks matching your radar",
                        items = discoverWeeklyTracks.take(12).map { t ->
                            CollectionItem(
                                id = "trk_${t.id}",
                                title = t.title,
                                subtitle = t.artist,
                                thumbnailUrl = t.thumbnailUrl,
                                type = "song",
                                videoId = t.id,
                                tracks = listOf(t),
                            )
                        },
                        isCompact = true,
                    )
                )
            }

            // Personalized Anchor Artist Shelf
            val anchor = seedArtistNames.firstOrNull()
            val anchorTracks = anchor?.let { artistTrackPools[it] }.orEmpty()
            if (anchor != null && anchorTracks.isNotEmpty()) {
                add(
                    HomeShelf(
                        id = "because_anchor",
                        title = "Because you like $anchor",
                        items = anchorTracks.map { t ->
                            CollectionItem(
                                id = "anc_${t.id}",
                                title = t.title,
                                subtitle = t.artist,
                                thumbnailUrl = t.thumbnailUrl,
                                type = "song",
                                videoId = t.id,
                                tracks = listOf(t),
                            )
                        },
                    )
                )
            }

            // Circular Artists Radar Shelf
            val artistCards = relatedArtists.take(10).map { ra ->
                CollectionItem(
                    id = ra.id,
                    title = ra.name,
                    subtitle = ra.subtitle,
                    thumbnailUrl = ra.imageUrl,
                    type = "artist",
                )
            }
            if (artistCards.isNotEmpty()) {
                add(
                    HomeShelf(
                        id = "artists_radar",
                        title = "Artists on your radar",
                        items = artistCards,
                        isArtistShelf = true,
                    )
                )
            }
        }

        GeneratedHome(
            quickPicks = quickPicks,
            shelves = shelves,
            dna = getDnaSnapshot(),
        )
    }

    // =========================================================================
    // 3. SMART AI AUTOPLAY QUEUE (MARKOV + INNERTUBE RADIO - SKIP FILTERED)
    // =========================================================================
    suspend fun generateSmartAutoplayQueue(seedTrack: TrackItem): List<TrackItem> = withContext(Dispatchers.IO) {
        val markov = db.getMarkovNextTracks(seedTrack.id, limit = 4)
        val radio = runCatching { engine.fetchRelatedTracks(seedTrack.id, limit = 18) }.getOrDefault(emptyList())
        val blockedArtists = db.getBlockedArtistKeys()
        val skippedIds = db.getSkippedTrackIds()
        val artistScores = db.getArtistScoreMap()

        val filteredRadio = radio
            .filterNot { it.id == seedTrack.id || it.id in skippedIds }
            .filterNot { primaryKey(it.artist) in blockedArtists }
            .sortedByDescending { artistScores[primaryKey(it.artist)] ?: 0.0 }

        (markov + filteredRadio).distinctBy { it.id }.take(20)
    }

    private fun primaryKey(raw: String): String {
        return raw.split(",", "&", "•", "feat.", "ft.")
            .firstOrNull()
            ?.replace(Regex("""\s*-\s*Topic$""", RegexOption.IGNORE_CASE), "")
            ?.trim()
            ?.lowercase()
            .orEmpty()
    }
}
