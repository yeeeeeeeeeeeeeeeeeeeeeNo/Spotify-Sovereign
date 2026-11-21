package com.spotify.sovereign.model

import kotlinx.serialization.Serializable

@Serializable
data class TrackItem(
    val id: String,
    val title: String,
    val artist: String,
    val album: String = "",
    val thumbnailUrl: String = "",
    val durationSec: Int = 0,
    val explicit: Boolean = false,
)

@Serializable
data class CollectionItem(
    val id: String,
    val browseId: String = "",
    val title: String,
    val subtitle: String,
    val thumbnailUrl: String = "",
    val type: String = "playlist", // "ai_mix", "album", "playlist", "artist", "song", "liked"
    val videoId: String? = null,
    val pinned: Boolean = false,
    val tracks: List<TrackItem> = emptyList(),
    val accentHex: Long = 0xFF1ED760,
)

data class HomeShelf(
    val id: String,
    val title: String,
    val subtitle: String = "",
    val items: List<CollectionItem>,
    val isArtistShelf: Boolean = false,
    val isCompact: Boolean = false,
)

data class DetailPageData(
    val id: String,
    val title: String,
    val subtitle: String,
    val description: String,
    val thumbnailUrl: String,
    val type: String, // "ai_mix", "album", "playlist", "artist", "liked"
    val tracks: List<TrackItem>,
    val accentHex: Long? = null,
)

data class SearchResults(
    val query: String,
    val songs: List<TrackItem> = emptyList(),
    val albums: List<CollectionItem> = emptyList(),
    val artists: List<CollectionItem> = emptyList(),
    val playlists: List<CollectionItem> = emptyList(),
)

data class LyricLine(
    val timeMs: Long,
    val text: String,
)

data class LyricsPayload(
    val lines: List<LyricLine>,
    val isSynced: Boolean,
    val source: String,
    val matchedDurationSec: Int,
)

data class ResolvedStream(
    val videoId: String,
    val url: String,
    val headers: Map<String, String>,
    val codecLabel: String, // e.g., "OPUS 160k", "AAC 256k"
    val bitrateKbps: Int,
    val clientLabel: String,
    val fromCache: Boolean = false,
    val expiresAtMs: Long = System.currentTimeMillis() + 4 * 3600_000L,
)

data class OnboardingCandidate(
    val id: String,
    val name: String,
    val subtitle: String,
    val imageUrl: String,
    val isTrack: Boolean = false,
    val videoId: String? = null,
    val artistName: String = name,
)

data class ArtistAffinity(
    val name: String,
    val score: Double,
    val plays: Int,
    val skips: Int,
    val timeBucketScore: Double,
)

data class TasteDnaSnapshot(
    val topArtists: List<ArtistAffinity>,
    val totalPlays: Int,
    val totalSkipsBlocked: Int,
    val currentTimeBucket: String,
    val cachedStreamsCount: Int,
)

enum class AppTab { HOME, SEARCH, LIBRARY }
enum class HomeFilter(val label: String) { ALL("All"), MUSIC("Music"), DISCOVER("Discover") }
enum class LibraryFilter(val label: String) { ALL("All"), PLAYLISTS("Playlists"), ALBUMS("Albums"), ARTISTS("Artists") }
enum class RepeatMode { OFF, ALL, ONE }
