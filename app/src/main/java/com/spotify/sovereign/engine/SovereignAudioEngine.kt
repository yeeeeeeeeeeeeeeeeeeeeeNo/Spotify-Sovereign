package com.spotify.sovereign.engine

import android.util.Log
import com.spotify.sovereign.model.CollectionItem
import com.spotify.sovereign.model.DetailPageData
import com.spotify.sovereign.model.OnboardingCandidate
import com.spotify.sovereign.model.ResolvedStream
import com.spotify.sovereign.model.SearchResults
import com.spotify.sovereign.model.TrackItem
import com.spotify.sovereign.model.YouTubeAccountProfile
import java.security.MessageDigest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import okhttp3.ConnectionPool
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.net.URLEncoder
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlin.math.abs

/**
 * 100% Sovereign Zero-Dependency Audio & Discovery Engine.
 * Direct InnerTube multi-client stream resolver + Studio-ATV song matcher +
 * Deezer open discovery graph + Public Spotify Playlist importer.
 * Zero reliance on Metrolist or any third-party extractor library.
 */
class SovereignAudioEngine {
    companion object {
        private const val TAG = "SovereignEngine"
        private val JSON_MEDIA = "application/json; charset=utf-8".toMediaType()
        private const val YT_MUSIC_BASE = "https://music.youtube.com/youtubei/v1"
        private const val YT_WEB_BASE = "https://www.youtube.com/youtubei/v1"
        private const val SONGS_SEARCH_PARAMS = "EgWKAQIIAWoKEAkQBRAKEAMQBA%3D%3D"
    }

    private data class ClientProfile(
        val id: String,
        val clientName: String,
        val clientVersion: String,
        val clientIdHeader: String,
        val userAgent: String,
        val osName: String? = null,
        val osVersion: String? = null,
        val deviceMake: String? = null,
        val deviceModel: String? = null,
        val androidSdkVersion: Int? = null,
        val baseUrl: String = YT_WEB_BASE,
        val thirdPartyEmbedUrl: String? = null,
    )

    private val clientProfiles = listOf(
        ClientProfile(
            id = "VISIONOS",
            clientName = "VISIONOS",
            clientVersion = "0.1",
            clientIdHeader = "101",
            userAgent = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.0 Safari/605.1.15",
            osName = "visionOS",
            osVersion = "1.3.21O771",
            deviceMake = "Apple",
            deviceModel = "RealityDevice14,1",
        ),
        ClientProfile(
            id = "ANDROID_VR",
            clientName = "ANDROID_VR",
            clientVersion = "1.71.26",
            clientIdHeader = "28",
            userAgent = "com.google.android.apps.youtube.vr.oculus/1.71.26 (Linux; U; Android 12L; eureka-user Build/SQ3A.220605.009.A1) gzip",
            osName = "Android",
            osVersion = "12L",
            deviceMake = "Oculus",
            deviceModel = "Quest 3",
            androidSdkVersion = 32,
        ),
        ClientProfile(
            id = "ANDROID_VR_1_65",
            clientName = "ANDROID_VR",
            clientVersion = "1.65.10",
            clientIdHeader = "28",
            userAgent = "com.google.android.apps.youtube.vr.oculus/1.65.10 (Linux; U; Android 12L; eureka-user Build/SQ3A.220605.009.A1) gzip",
            osName = "Android",
            osVersion = "12L",
            deviceMake = "Oculus",
            deviceModel = "Quest 3",
            androidSdkVersion = 32,
        ),
        ClientProfile(
            id = "IOS",
            clientName = "IOS",
            clientVersion = "20.10.4",
            clientIdHeader = "5",
            userAgent = "com.google.ios.youtube/20.10.4 (iPhone16,2; U; CPU iOS 18_3_2 like Mac OS X;)",
            osName = "iPhone",
            osVersion = "18.3.2.22D82",
            deviceMake = "Apple",
            deviceModel = "iPhone16,2",
        ),
        ClientProfile(
            id = "ANDROID_MUSIC",
            clientName = "ANDROID_MUSIC",
            clientVersion = "7.21.50",
            clientIdHeader = "21",
            userAgent = "com.google.android.apps.youtube.music/7.21.50 (Linux; U; Android 14) gzip",
            osName = "Android",
            osVersion = "14",
            androidSdkVersion = 34,
            baseUrl = YT_MUSIC_BASE,
        ),
    )

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectionPool(ConnectionPool(12, 5, TimeUnit.MINUTES))
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    private val streamCache = ConcurrentHashMap<String, ResolvedStream>()
    private val clientHealth = ConcurrentHashMap<String, Int>()
    private val studioMatchCache = ConcurrentHashMap<String, TrackItem>()

    @Volatile
    private var cachedVisitorData: String = ""
    @Volatile
    private var visitorDataFetchedAtMs: Long = 0L

    @Volatile
    private var authCookie: String = ""

    fun setAuthCookie(cookie: String?) {
        authCookie = cookie?.trim().orEmpty()
    }

    fun hasAuthCookie(): Boolean = extractSapisid(authCookie) != null

    private fun extractSapisid(cookie: String): String? {
        if (cookie.isBlank()) return null
        val map = cookie.split(";").mapNotNull { part ->
            val idx = part.indexOf('=')
            if (idx <= 0) null else part.substring(0, idx).trim() to part.substring(idx + 1).trim()
        }.toMap()
        return map["SAPISID"] ?: map["__Secure-3PAPISID"] ?: map["__Secure-1PAPISID"]
    }

    private fun buildSapisidAuthHeader(origin: String = "https://music.youtube.com"): String? {
        val sapisid = extractSapisid(authCookie) ?: return null
        val ts = System.currentTimeMillis() / 1000L
        val raw = "$ts $sapisid $origin"
        val digest = MessageDigest.getInstance("SHA-1").digest(raw.toByteArray(Charsets.UTF_8))
        val hex = digest.joinToString("") { "%02x".format(it) }
        return "SAPISIDHASH ${ts}_$hex"
    }

    private fun ensureVisitorData(forceRefresh: Boolean = false): String {
        val now = System.currentTimeMillis()
        if (!forceRefresh && cachedVisitorData.isNotBlank() && now - visitorDataFetchedAtMs < 20 * 60_000L) {
            return cachedVisitorData
        }
        val payload = buildJsonObject {
            putJsonObject("context") {
                putJsonObject("client") {
                    put("clientName", "WEB_REMIX")
                    put("clientVersion", "1.20260401.01.00")
                    put("hl", localeHl)
                    put("gl", localeGl)
                }
            }
        }.toString()
        val req = Request.Builder()
            .url("$YT_MUSIC_BASE/visitor_id?prettyPrint=false")
            .post(payload.toRequestBody(JSON_MEDIA))
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/131.0.0.0 Safari/537.36")
            .header("X-YouTube-Client-Name", "67")
            .header("Origin", "https://music.youtube.com")
            .build()
        val fresh = runCatching {
            httpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@use ""
                val root = json.parseToJsonElement(resp.body.string()).asObj()
                root?.get("responseContext")?.asObj()?.get("visitorData")?.str().orEmpty()
            }
        }.getOrDefault("")
        if (fresh.isNotBlank()) {
            cachedVisitorData = fresh
            visitorDataFetchedAtMs = now
        }
        return cachedVisitorData
    }

    private val localeHl: String = Locale.getDefault().language.ifBlank { "en" }
    private val localeGl: String = Locale.getDefault().country.ifBlank { "US" }

    fun cachedStreamCount(): Int = streamCache.size

    fun getCachedStream(videoId: String): ResolvedStream? {
        val hit = streamCache[videoId] ?: return null
        if (System.currentTimeMillis() >= hit.expiresAtMs - 120_000L) {
            streamCache.remove(videoId)
            return null
        }
        return hit.copy(fromCache = true)
    }

    // =========================================================================
    // 1. ULTRA-HIGH AUDIO STREAM RESOLUTION (OUR OWN MULTI-CLIENT ENGINE)
    // =========================================================================
    suspend fun resolveAudioStream(
        videoId: String,
        preferUltraHigh: Boolean = true,
        forceRefresh: Boolean = false,
    ): ResolvedStream = withContext(Dispatchers.IO) {
        if (!forceRefresh) {
            getCachedStream(videoId)?.let { return@withContext it }
        }

        val orderedClients = clientProfiles.sortedByDescending { clientHealth[it.id] ?: 0 }
        var lastError: String? = null

        for (profile in orderedClients) {
            try {
                val stream = fetchStreamFromProfile(videoId, profile, preferUltraHigh)
                if (stream != null) {
                    clientHealth[profile.id] = ((clientHealth[profile.id] ?: 0) + 10).coerceAtMost(50)
                    streamCache[videoId] = stream
                    return@withContext stream
                }
            } catch (e: Exception) {
                lastError = "${profile.id}: ${e.message}"
                Log.w(TAG, "Profile ${profile.id} failed for $videoId: ${e.message}")
                clientHealth[profile.id] = ((clientHealth[profile.id] ?: 0) - 15).coerceAtLeast(-100)
            }
        }
        error("All sovereign clients failed for $videoId (${lastError ?: "unavailable"})")
    }

    suspend fun preResolveStream(videoId: String): ResolvedStream? = withContext(Dispatchers.IO) {
        runCatching { resolveAudioStream(videoId, preferUltraHigh = true, forceRefresh = false) }.getOrNull()
    }

    private fun fetchStreamFromProfile(
        videoId: String,
        profile: ClientProfile,
        preferUltraHigh: Boolean,
    ): ResolvedStream? {
        val visitorData = ensureVisitorData()
        val bodyJson = buildJsonObject {
            putJsonObject("context") {
                putJsonObject("client") {
                    put("clientName", profile.clientName)
                    put("clientVersion", profile.clientVersion)
                    put("hl", localeHl)
                    put("gl", localeGl)
                    if (visitorData.isNotBlank()) put("visitorData", visitorData)
                    profile.osName?.let { put("osName", it) }
                    profile.osVersion?.let { put("osVersion", it) }
                    profile.deviceMake?.let { put("deviceMake", it) }
                    profile.deviceModel?.let { put("deviceModel", it) }
                    profile.androidSdkVersion?.let { put("androidSdkVersion", it) }
                }
                profile.thirdPartyEmbedUrl?.let { embedUrl ->
                    putJsonObject("thirdParty") {
                        put("embedUrl", embedUrl)
                    }
                }
            }
            put("videoId", videoId)
            put("contentCheckOk", true)
            put("racyCheckOk", true)
        }.toString()

        val reqBuilder = Request.Builder()
            .url("${profile.baseUrl}/player?prettyPrint=false")
            .post(bodyJson.toRequestBody(JSON_MEDIA))
            .header("User-Agent", profile.userAgent)
            .header("X-YouTube-Client-Name", profile.clientIdHeader)
            .header("X-YouTube-Client-Version", profile.clientVersion)
            .header("Origin", if (profile.baseUrl == YT_MUSIC_BASE) "https://music.youtube.com" else "https://www.youtube.com")
        if (visitorData.isNotBlank()) {
            reqBuilder.header("X-Goog-Visitor-Id", visitorData)
        }

        val respBody = httpClient.newCall(reqBuilder.build()).execute().use { resp ->
            if (!resp.isSuccessful) error("HTTP ${resp.code}")
            resp.body.string()
        }

        val root = json.parseToJsonElement(respBody).asObj() ?: return null
        val playability = root["playabilityStatus"]?.asObj()?.get("status")?.str()
        if (playability != "OK") {
            val reason = root["playabilityStatus"]?.asObj()?.get("reason")?.str() ?: playability
            if (playability == "LOGIN_REQUIRED" || playability == "UNPLAYABLE") {
                ensureVisitorData(forceRefresh = true)
            }
            error("Playability $reason")
        }

        val streamingData = root["streamingData"]?.asObj() ?: return null
        val adaptive = streamingData["adaptiveFormats"]?.asArr().orEmpty()
        val legacy = streamingData["formats"]?.asArr().orEmpty()

        data class FormatPick(
            val url: String,
            val itag: Int,
            val mimeType: String,
            val bitrate: Int,
            val contentLength: Long,
            val isOpus: Boolean,
        )

        val audioFormats = (adaptive + legacy).mapNotNull { el ->
            val obj = el.asObj() ?: return@mapNotNull null
            val url = obj["url"]?.str() ?: return@mapNotNull null
            val mime = obj["mimeType"]?.str() ?: ""
            if (!mime.startsWith("audio/")) return@mapNotNull null
            val itag = obj["itag"]?.intVal() ?: 0
            val bitrate = obj["bitrate"]?.intVal() ?: obj["averageBitrate"]?.intVal() ?: 0
            val contentLen = obj["contentLength"]?.str()?.toLongOrNull() ?: 0L
            val isOpus = mime.contains("opus", ignoreCase = true) || itag == 251 || itag == 774
            FormatPick(url, itag, mime, bitrate, contentLen, isOpus)
        }

        if (audioFormats.isEmpty()) return null

        val best = if (preferUltraHigh) {
            audioFormats.maxWithOrNull(
                compareBy<FormatPick> { it.bitrate }
                    .thenBy { if (it.isOpus) 1 else 0 }
            )
        } else {
            audioFormats.minByOrNull { abs(it.bitrate - 128_000) }
        } ?: return null

        // Smart Past-1MB CDN Probe: catches Google's 1MB (30s) tokenless wall immediately!
        val probeStart = if (best.contentLength > 1_600_000L) 1_500_000L else 0L
        val probeEnd = probeStart + 1023L
        val probeReq = Request.Builder()
            .url(best.url)
            .header("User-Agent", profile.userAgent)
            .header("Range", "bytes=$probeStart-$probeEnd")
            .build()
        val probeOk = runCatching {
            httpClient.newCall(probeReq).execute().use { r -> r.code == 200 || r.code == 206 }
        }.getOrDefault(false)
        if (!probeOk) {
            error("CDN rejected past-1MB probe for ${profile.id} itag=${best.itag}")
        }

        val kbps = (best.bitrate / 1000).coerceAtLeast(64)
        val codecTag = when {
            best.isOpus -> "OPUS ${kbps}k"
            best.mimeType.contains("mp4", ignoreCase = true) -> "AAC ${kbps}k"
            else -> "AUDIO ${kbps}k"
        }

        return ResolvedStream(
            videoId = videoId,
            url = best.url,
            headers = mapOf(
                "User-Agent" to profile.userAgent,
                "Accept" to "*/*",
            ),
            codecLabel = codecTag,
            bitrateKbps = kbps,
            clientLabel = profile.id,
            fromCache = false,
            expiresAtMs = System.currentTimeMillis() + 4 * 3600_000L,
        )
    }

    // =========================================================================
    // 2. DIRECT YOUTUBE MUSIC SEARCH (EXACT + ALL VERSIONS + SIMILAR VIBES!)
    // =========================================================================
    private val variantNoiseRegex = Regex(
        """\b(slowed|ultra\s*slowed|super\s*slowed|sped\s*up|speed\s*up|reverb|and\s*reverb|\+\s*reverb|nightcore|remix|8d|bass\s*boosted|instrumental|acoustic|live|audio|lyrics)\b""",
        RegexOption.IGNORE_CASE,
    )

    suspend fun searchAll(query: String): SearchResults = withContext(Dispatchers.IO) {
        val cleanQuery = query.trim()
        val baseTitle = cleanQuery.replace(variantNoiseRegex, " ").replace(Regex("""\s+"""), " ").trim()
        val hasVariantModifier = baseTitle.isNotBlank() && !baseTitle.equals(cleanQuery, ignoreCase = true)

        val (root, directSongs, variantSongs) = coroutineScope {
            val mainJob = async {
                postWebRemix(
                    endpoint = "search",
                    extra = buildJsonObject { put("query", cleanQuery) },
                )
            }
            val songsJob = async {
                searchSongsByMood(cleanQuery, limit = 18)
            }
            val variantJob = async {
                if (hasVariantModifier) {
                    val v1 = async { searchSongsByMood("$baseTitle slowed sped up", limit = 14) }
                    val v2 = async { searchSongsByMood(baseTitle, limit = 10) }
                    (v1.await() + v2.await()).distinctBy { it.id }
                } else {
                    emptyList()
                }
            }
            Triple(mainJob.await(), songsJob.await(), variantJob.await())
        }

        val songs = mutableListOf<TrackItem>()
        val albums = mutableListOf<CollectionItem>()
        val artists = mutableListOf<CollectionItem>()
        val playlists = mutableListOf<CollectionItem>()

        if (root != null) {
            findFirstObject(root, "musicCardShelfRenderer")?.let { card ->
                val title = runText(card["title"])
                val sub = runText(card["subtitle"]) ?: ""
                val thumb = upgradeThumb(extractThumb(card["thumbnail"]))
                val nav = card["title"]?.asObj()?.get("runs")?.asArr()?.firstOrNull()?.asObj()
                    ?.get("navigationEndpoint")?.asObj()
                val vid = nav?.get("watchEndpoint")?.asObj()?.get("videoId")?.str()
                val bid = nav?.get("browseEndpoint")?.asObj()?.get("browseId")?.str()
                if (!title.isNullOrBlank()) {
                    if (vid != null) {
                        songs.add(
                            TrackItem(
                                id = vid,
                                title = title,
                                artist = sub.substringAfter("•").trim().ifBlank { sub },
                                thumbnailUrl = thumb,
                            )
                        )
                    } else if (bid != null) {
                        val col = CollectionItem(
                            id = bid,
                            browseId = bid,
                            title = title,
                            subtitle = sub,
                            thumbnailUrl = thumb,
                            type = when {
                                bid.startsWith("UC") -> "artist"
                                bid.startsWith("MPRE") || bid.startsWith("OLAK") -> "album"
                                else -> "playlist"
                            },
                        )
                        when (col.type) {
                            "artist" -> artists.add(col)
                            "album" -> albums.add(col)
                            else -> playlists.add(col)
                        }
                    }
                }
            }

            for (shelf in findAllObjects(root, "musicShelfRenderer")) {
                val shelfTitle = runText(shelf["title"])?.lowercase().orEmpty()
                val contents = shelf["contents"]?.asArr() ?: continue
                for (el in contents) {
                    val item = el.asObj()?.get("musicResponsiveListItemRenderer")?.asObj() ?: continue
                    val track = parseResponsiveTrack(item)
                    if (track != null && (shelfTitle.contains("song") || shelfTitle.isEmpty())) {
                        songs.add(track)
                        continue
                    }
                    val col = parseResponsiveCollection(item, shelfTitle)
                    if (col != null) {
                        when (col.type) {
                            "artist" -> artists.add(col)
                            "album" -> albums.add(col)
                            "playlist" -> playlists.add(col)
                            else -> if (track != null) songs.add(track)
                        }
                    } else if (track != null) {
                        songs.add(track)
                    }
                }
            }
        }

        // Exact matches first -> dedicated Songs tab hits -> all slowed/ultra-slowed/sped-up/original variants!
        val mergedSongs = (songs + directSongs + variantSongs).distinctBy { it.id }

        // Pull similar vibe tracks from the top matched song's YouTube Music radio graph!
        val topId = mergedSongs.firstOrNull()?.id
        val related = if (!topId.isNullOrBlank()) {
            val seenIds = mergedSongs.map { it.id }.toSet()
            runCatching { fetchRelatedTracks(topId, limit = 16) }
                .getOrDefault(emptyList())
                .filterNot { it.id in seenIds }
                .distinctBy { it.id }
        } else {
            emptyList()
        }

        SearchResults(
            query = cleanQuery,
            songs = mergedSongs,
            relatedSongs = related,
            albums = albums.distinctBy { it.browseId },
            artists = artists.distinctBy { it.browseId },
            playlists = playlists.distinctBy { it.browseId },
        )
    }

    /**
     * Studio-track-only resolver: guarantees official ATV studio audio tracks
     * (no music video intros -> accurate lyrics + highest Opus studio quality).
     */
    suspend fun searchStudioSong(
        title: String,
        artist: String,
        expectedDurationSec: Int = 0,
    ): TrackItem? = withContext(Dispatchers.IO) {
        val cacheKey = "${title.lowercase()}|${artist.lowercase()}"
        studioMatchCache[cacheKey]?.let { return@withContext it }

        val query = "$title $artist".trim()
        val root = postWebRemix(
            endpoint = "search",
            extra = buildJsonObject {
                put("query", query)
                put("params", SONGS_SEARCH_PARAMS)
            },
        ) ?: return@withContext null

        val candidates = findAllObjects(root, "musicResponsiveListItemRenderer")
            .mapNotNull { parseResponsiveTrack(it) }
            .distinctBy { it.id }

        if (candidates.isEmpty()) return@withContext null

        val best = if (expectedDurationSec > 0) {
            candidates.minByOrNull { c ->
                val durPenalty = if (c.durationSec > 0) abs(c.durationSec - expectedDurationSec) else 12
                val titlePenalty = if (c.title.contains(title, ignoreCase = true) || title.contains(c.title, ignoreCase = true)) 0 else 15
                durPenalty + titlePenalty
            }
        } else {
            candidates.firstOrNull {
                it.title.contains(title, ignoreCase = true) || it.artist.contains(artist, ignoreCase = true)
            } ?: candidates.first()
        }

        if (best != null) {
            studioMatchCache[cacheKey] = best
        }
        best
    }

    suspend fun searchSongsForArtist(artistName: String, limit: Int = 10): List<TrackItem> = withContext(Dispatchers.IO) {
        val root = postWebRemix(
            endpoint = "search",
            extra = buildJsonObject {
                put("query", artistName)
                put("params", SONGS_SEARCH_PARAMS)
            },
        ) ?: return@withContext emptyList()

        findAllObjects(root, "musicResponsiveListItemRenderer")
            .mapNotNull { parseResponsiveTrack(it) }
            .distinctBy { it.id }
            .take(limit)
    }

    // =========================================================================
    // 3. ALBUM / PLAYLIST / ARTIST BROWSE + RELATED RADIO (/next)
    // =========================================================================
    suspend fun browseCollection(
        browseId: String,
        fallbackTitle: String,
        fallbackSubtitle: String,
        fallbackThumb: String,
        fallbackType: String,
    ): DetailPageData = withContext(Dispatchers.IO) {
        val cleanId = if (browseId.startsWith("PL") || browseId.startsWith("RD") || browseId.startsWith("OLAK")) {
            "VL$browseId"
        } else {
            browseId
        }
        val root = postWebRemix(
            endpoint = "browse",
            extra = buildJsonObject { put("browseId", cleanId) },
        ) ?: return@withContext DetailPageData(browseId, fallbackTitle, fallbackSubtitle, "", fallbackThumb, fallbackType, emptyList())

        val header = findFirstObject(root, "musicResponsiveHeaderRenderer")
            ?: findFirstObject(root, "musicDetailHeaderRenderer")
            ?: findFirstObject(root, "musicImmersiveHeaderRenderer")
            ?: findFirstObject(root, "musicVisualHeaderRenderer")

        val title = runText(header?.get("title")) ?: fallbackTitle
        val subtitle = runText(header?.get("subtitle"))
            ?: runText(header?.get("secondSubtitle"))
            ?: fallbackSubtitle
        val desc = runText(header?.get("description")) ?: ""
        val thumb = upgradeThumb(extractThumb(header?.get("thumbnail"))).ifBlank { fallbackThumb }

        val tracks = findAllObjects(root, "musicResponsiveListItemRenderer")
            .mapNotNull { parseResponsiveTrack(it, defaultAlbum = title, defaultThumb = thumb, defaultArtist = subtitle) }
            .distinctBy { it.id }

        DetailPageData(
            id = browseId,
            title = title.ifBlank { "Playlist" },
            subtitle = subtitle,
            description = desc,
            thumbnailUrl = thumb,
            type = fallbackType,
            tracks = tracks,
        )
    }

    suspend fun fetchRelatedTracks(videoId: String, limit: Int = 18): List<TrackItem> = withContext(Dispatchers.IO) {
        val root = postWebRemix(
            endpoint = "next",
            extra = buildJsonObject {
                put("videoId", videoId)
                put("playlistId", "RDAMVM$videoId")
                put("isAudioOnly", true)
            },
        ) ?: return@withContext emptyList()

        findAllObjects(root, "playlistPanelVideoRenderer").mapNotNull { panel ->
            val vid = panel["videoId"]?.str() ?: return@mapNotNull null
            val title = runText(panel["title"]) ?: return@mapNotNull null
            val byline = runText(panel["longBylineText"]) ?: runText(panel["shortBylineText"]) ?: "Music"
            val artist = byline.split("•").firstOrNull()?.trim().orEmpty().ifBlank { byline }
            val durStr = runText(panel["lengthText"]) ?: "0:00"
            val thumb = upgradeThumb(extractThumb(panel["thumbnail"]))
            TrackItem(
                id = vid,
                title = title,
                artist = artist,
                thumbnailUrl = thumb,
                durationSec = parseDurationToSec(durStr),
            )
        }.distinctBy { it.id }.take(limit)
    }

    // =========================================================================
    // 4. DEEZER OPEN DISCOVERY GRAPH (HD ARTIST PHOTOS + DEEP RELATED GRAPH)
    // =========================================================================
    suspend fun searchDeezerArtists(query: String, limit: Int = 8): List<OnboardingCandidate> = withContext(Dispatchers.IO) {
        val enc = URLEncoder.encode(query, "UTF-8")
        val obj = getJson("https://api.deezer.com/search/artist?q=$enc&limit=$limit") ?: return@withContext emptyList()
        obj["data"]?.asArr().orEmpty().mapNotNull { el ->
            val a = el.asObj() ?: return@mapNotNull null
            val id = a["id"]?.str() ?: return@mapNotNull null
            val name = a["name"]?.str() ?: return@mapNotNull null
            val pic = a["picture_medium"]?.str() ?: a["picture_big"]?.str() ?: ""
            OnboardingCandidate(
                id = "dz_ar_$id",
                name = name,
                subtitle = "Artist",
                imageUrl = pic,
                isTrack = false,
                artistName = name,
            )
        }
    }

    suspend fun fetchRelatedArtistsFromDeezer(artistName: String, limit: Int = 6): List<OnboardingCandidate> = withContext(Dispatchers.IO) {
        val enc = URLEncoder.encode(artistName, "UTF-8")
        val searchObj = getJson("https://api.deezer.com/search/artist?q=$enc&limit=1") ?: return@withContext emptyList()
        val artistId = searchObj["data"]?.asArr()?.firstOrNull()?.asObj()?.get("id")?.str() ?: return@withContext emptyList()
        val relObj = getJson("https://api.deezer.com/artist/$artistId/related?limit=$limit") ?: return@withContext emptyList()
        relObj["data"]?.asArr().orEmpty().mapNotNull { el ->
            val a = el.asObj() ?: return@mapNotNull null
            val id = a["id"]?.str() ?: return@mapNotNull null
            val name = a["name"]?.str() ?: return@mapNotNull null
            val pic = a["picture_medium"]?.str() ?: a["picture_big"]?.str() ?: ""
            OnboardingCandidate(
                id = "dz_ar_$id",
                name = name,
                subtitle = "Similar to $artistName",
                imageUrl = pic,
                isTrack = false,
                artistName = name,
            )
        }
    }

    // =========================================================================
    // 5. 1-TAP PUBLIC SPOTIFY PLAYLIST LINK IMPORTER (ZERO LOGIN NEEDED!)
    // =========================================================================
    suspend fun importSpotifyPlaylistUrl(url: String): Pair<String, List<Pair<String, String>>> = withContext(Dispatchers.IO) {
        val match = Regex("""playlist/([a-zA-Z0-9]+)""").find(url)
            ?: error("Paste a valid Spotify playlist link (open.spotify.com/playlist/...)")
        val playlistId = match.groupValues[1]
        val req = Request.Builder()
            .url("https://open.spotify.com/embed/playlist/$playlistId")
            .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/128.0 Mobile Safari/537.36")
            .build()

        val html = httpClient.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) error("Playlist not accessible (make sure it's Public)")
            resp.body.string()
        }

        val scriptMatch = Regex("""<script id="__NEXT_DATA__" type="application/json">(.*?)</script>""").find(html)
            ?: error("Couldn't parse Spotify embed metadata")
        val root = json.parseToJsonElement(scriptMatch.groupValues[1]).asObj()
            ?: error("Invalid playlist JSON")

        val entity = findFirstObject(root, "entity")
        val title = entity?.get("name")?.str() ?: "Imported Spotify Playlist"
        val trackList = entity?.get("trackList")?.asArr().orEmpty()
        val pairs = trackList.mapNotNull { el ->
            val o = el.asObj() ?: return@mapNotNull null
            val t = o["title"]?.str()?.trim() ?: return@mapNotNull null
            val a = o["subtitle"]?.str()?.trim()?.replace("\u00A0", " ") ?: ""
            if (t.isNotBlank()) t to a else null
        }
        title to pairs
    }

    // =========================================================================
    // 6. YOUTUBE MUSIC ACCOUNT SYNC (SAPISIDHASH AUTH — PROFILE / LIKES / PLAYLISTS)
    // =========================================================================
    suspend fun fetchYouTubeAccountProfile(): YouTubeAccountProfile? = withContext(Dispatchers.IO) {
        if (!hasAuthCookie()) return@withContext null
        val root = postWebRemix(
            endpoint = "account/account_menu",
            extra = buildJsonObject { put("deviceTheme", "DEVICE_THEME_SUPPORTED") },
            authenticated = true,
        ) ?: return@withContext null

        val header = findFirstObject(root, "activeAccountHeaderRenderer") ?: return@withContext null
        val name = runText(header["accountName"]) ?: "YouTube Music"
        val handle = runText(header["channelHandle"]) ?: runText(header["email"]) ?: ""
        val avatar = upgradeThumb(extractThumb(header["accountPhoto"]))
        YouTubeAccountProfile(
            name = name,
            handle = handle,
            avatarUrl = avatar,
            signedIn = true,
        )
    }

    suspend fun fetchYouTubeLikedSongs(limit: Int = 60): List<TrackItem> = withContext(Dispatchers.IO) {
        if (!hasAuthCookie()) return@withContext emptyList()
        val root = postWebRemix(
            endpoint = "browse",
            extra = buildJsonObject { put("browseId", "VLLM") },
            authenticated = true,
        ) ?: return@withContext emptyList()

        findAllObjects(root, "musicResponsiveListItemRenderer")
            .mapNotNull { parseResponsiveTrack(it) }
            .distinctBy { it.id }
            .take(limit)
    }

    suspend fun fetchYouTubePlaylists(): List<CollectionItem> = withContext(Dispatchers.IO) {
        if (!hasAuthCookie()) return@withContext emptyList()
        val root = postWebRemix(
            endpoint = "browse",
            extra = buildJsonObject { put("browseId", "FEmusic_liked_playlists") },
            authenticated = true,
        ) ?: return@withContext emptyList()

        val out = mutableListOf<CollectionItem>()
        for (card in findAllObjects(root, "musicTwoRowItemRenderer")) {
            val title = runText(card["title"]) ?: continue
            val subtitle = runText(card["subtitle"]) ?: "YouTube Music Playlist"
            val thumb = upgradeThumb(extractThumb(card["thumbnailRenderer"]))
            val browseId = card["navigationEndpoint"]?.asObj()?.get("browseEndpoint")?.asObj()?.get("browseId")?.str()
                ?: continue
            if (browseId == "VLLM" || browseId == "VLSE") continue
            out.add(
                CollectionItem(
                    id = "ytpl_$browseId",
                    browseId = browseId,
                    title = title,
                    subtitle = subtitle,
                    thumbnailUrl = thumb,
                    type = "playlist",
                    accentHex = 0xFF7AA2F7,
                )
            )
        }
        out.distinctBy { it.browseId }
    }

    suspend fun syncTrackLikeToYouTube(videoId: String, liked: Boolean): Boolean = withContext(Dispatchers.IO) {
        if (!hasAuthCookie()) return@withContext false
        val ep = if (liked) "like/like" else "like/removelike"
        val res = postWebRemix(
            endpoint = ep,
            extra = buildJsonObject {
                putJsonObject("target") { put("videoId", videoId) }
            },
            authenticated = true,
        )
        res != null
    }

    suspend fun searchSongsByMood(query: String, limit: Int = 18): List<TrackItem> = withContext(Dispatchers.IO) {
        val root = postWebRemix(
            endpoint = "search",
            extra = buildJsonObject {
                put("query", query)
                put("params", SONGS_SEARCH_PARAMS)
            },
        ) ?: return@withContext emptyList()

        findAllObjects(root, "musicResponsiveListItemRenderer")
            .mapNotNull { parseResponsiveTrack(it) }
            .distinctBy { it.id }
            .take(limit)
    }

    // =========================================================================
    // INTERNAL HTTP + JSON UTILITIES
    // =========================================================================
    private fun postWebRemix(
        endpoint: String,
        extra: JsonObject,
        authenticated: Boolean = false,
    ): JsonObject? {
        val payload = buildJsonObject {
            putJsonObject("context") {
                putJsonObject("client") {
                    put("clientName", "WEB_REMIX")
                    put("clientVersion", "1.20260401.01.00")
                    put("hl", localeHl)
                    put("gl", localeGl)
                }
            }
            extra.forEach { (k, v) -> put(k, v) }
        }.toString()

        val reqBuilder = Request.Builder()
            .url("$YT_MUSIC_BASE/$endpoint?prettyPrint=false")
            .post(payload.toRequestBody(JSON_MEDIA))
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/131.0.0.0 Safari/537.36")
            .header("Origin", "https://music.youtube.com")
            .header("Referer", "https://music.youtube.com/")
            .header("X-YouTube-Client-Name", "67")
            .header("X-YouTube-Client-Version", "1.20260401.01.00")

        if (authenticated || hasAuthCookie()) {
            val authHdr = buildSapisidAuthHeader("https://music.youtube.com")
            if (authHdr != null && authCookie.isNotBlank()) {
                reqBuilder
                    .header("Cookie", authCookie)
                    .header("Authorization", authHdr)
                    .header("X-Origin", "https://music.youtube.com")
                    .header("X-Goog-AuthUser", "0")
            }
        }

        return runCatching {
            httpClient.newCall(reqBuilder.build()).execute().use { resp ->
                if (!resp.isSuccessful) return@use null
                json.parseToJsonElement(resp.body.string()).asObj()
            }
        }.getOrNull()
    }

    private fun getJson(url: String): JsonObject? {
        val req = Request.Builder()
            .url(url)
            .header("User-Agent", "SpotifySovereign/2.0")
            .build()
        return runCatching {
            httpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@use null
                json.parseToJsonElement(resp.body.string()).asObj()
            }
        }.getOrNull()
    }

    private fun parseResponsiveCollection(obj: JsonObject, shelfHint: String): CollectionItem? {
        val flex = obj["flexColumns"]?.asArr() ?: return null
        val title = flexColumnText(flex.getOrNull(0)) ?: return null
        val subtitle = flexColumnText(flex.getOrNull(1)) ?: ""
        val thumb = upgradeThumb(extractThumb(obj["thumbnail"]))
        val browseId = obj["navigationEndpoint"]?.asObj()?.get("browseEndpoint")?.asObj()?.get("browseId")?.str()
            ?: return null
        val type = when {
            shelfHint.contains("artist") || browseId.startsWith("UC") -> "artist"
            shelfHint.contains("album") || browseId.startsWith("MPRE") || browseId.startsWith("OLAK") -> "album"
            else -> "playlist"
        }
        return CollectionItem(
            id = browseId,
            browseId = browseId,
            title = title,
            subtitle = subtitle,
            thumbnailUrl = thumb,
            type = type,
        )
    }

    private fun parseResponsiveTrack(
        obj: JsonObject,
        defaultAlbum: String = "",
        defaultThumb: String = "",
        defaultArtist: String = "",
    ): TrackItem? {
        val videoId = obj["playlistItemData"]?.asObj()?.get("videoId")?.str()
            ?: findFirstObject(obj, "watchEndpoint")?.get("videoId")?.str()
            ?: return null

        val flex = obj["flexColumns"]?.asArr() ?: return null
        val title = flexColumnText(flex.getOrNull(0)) ?: return null
        val secondLine = flexColumnText(flex.getOrNull(1)) ?: ""
        val parts = secondLine.split("•").map { it.trim() }.filter { it.isNotEmpty() }

        var durationSec = 0
        val fixedText = flexColumnText(obj["fixedColumns"]?.asArr()?.firstOrNull())
        if (!fixedText.isNullOrBlank()) durationSec = parseDurationToSec(fixedText)

        val cleaned = parts.filterNot { p ->
            if (p.matches(Regex("""\d+:\d{2}(:\d{2})?"""))) {
                if (durationSec == 0) durationSec = parseDurationToSec(p)
                true
            } else {
                p.equals("Song", ignoreCase = true) ||
                    p.equals("Video", ignoreCase = true) ||
                    p.contains("plays", ignoreCase = true) ||
                    p.contains("views", ignoreCase = true)
            }
        }

        val artist = cleaned.firstOrNull()?.takeIf { it.isNotBlank() }
            ?: defaultArtist.split("•").firstOrNull()?.trim()?.takeIf { it.isNotBlank() }
            ?: "Artist"
        val album = cleaned.getOrNull(1) ?: defaultAlbum
        val thumb = upgradeThumb(extractThumb(obj["thumbnail"])).ifBlank { defaultThumb }
        val explicit = obj["badges"]?.asArr()?.any {
            it.toString().contains("MUSIC_EXPLICIT_BADGE", ignoreCase = true)
        } == true

        return TrackItem(
            id = videoId,
            title = title,
            artist = artist,
            album = album,
            thumbnailUrl = thumb,
            durationSec = durationSec,
            explicit = explicit,
        )
    }

    private fun flexColumnText(col: JsonElement?): String? {
        val o = col?.asObj() ?: return null
        val r = o["musicResponsiveListItemFlexColumnRenderer"]?.asObj()
            ?: o["musicResponsiveListItemFixedColumnRenderer"]?.asObj()
            ?: return null
        return runText(r["text"])
    }

    private fun runText(el: JsonElement?): String? {
        val obj = el?.asObj() ?: return null
        obj["simpleText"]?.str()?.let { return it }
        val runs = obj["runs"]?.asArr() ?: return null
        return runs.mapNotNull { it.asObj()?.get("text")?.str() }.joinToString("").ifBlank { null }
    }

    private fun extractThumb(el: JsonElement?): String {
        if (el == null) return ""
        val arr = findAllThumbnailArrays(el).firstOrNull()
        val url = arr?.lastOrNull()?.asObj()?.get("url")?.str() ?: ""
        return if (url.startsWith("//")) "https:$url" else url
    }

    private fun findAllThumbnailArrays(el: JsonElement): List<JsonArray> {
        val out = mutableListOf<JsonArray>()
        when (el) {
            is JsonObject -> {
                el["thumbnails"]?.asArr()?.let { out.add(it) }
                el.values.forEach { out.addAll(findAllThumbnailArrays(it)) }
            }
            is JsonArray -> el.forEach { out.addAll(findAllThumbnailArrays(it)) }
            else -> Unit
        }
        return out
    }

    private fun upgradeThumb(url: String): String {
        if (url.isBlank()) return ""
        return url.replace(Regex("""=w\d+-h\d+[^&]*"""), "=w1080-h1080-l95-rj")
    }

    private fun parseDurationToSec(raw: String): Int {
        val parts = raw.trim().split(":").mapNotNull { it.toIntOrNull() }
        return when (parts.size) {
            2 -> parts[0] * 60 + parts[1]
            3 -> parts[0] * 3600 + parts[1] * 60 + parts[2]
            else -> 0
        }
    }

    private fun findAllObjects(el: JsonElement, key: String): List<JsonObject> {
        val res = mutableListOf<JsonObject>()
        when (el) {
            is JsonObject -> {
                el[key]?.asObj()?.let { res.add(it) }
                el.values.forEach { res.addAll(findAllObjects(it, key)) }
            }
            is JsonArray -> el.forEach { res.addAll(findAllObjects(it, key)) }
            else -> Unit
        }
        return res
    }

    private fun findFirstObject(el: JsonElement, key: String): JsonObject? {
        when (el) {
            is JsonObject -> {
                el[key]?.asObj()?.let { return it }
                for (v in el.values) {
                    findFirstObject(v, key)?.let { return it }
                }
            }
            is JsonArray -> {
                for (v in el) {
                    findFirstObject(v, key)?.let { return it }
                }
            }
            else -> Unit
        }
        return null
    }

    private fun JsonElement.asObj(): JsonObject? = this as? JsonObject
    private fun JsonElement.asArr(): JsonArray? = this as? JsonArray
    private fun JsonElement.str(): String? = runCatching { this.jsonPrimitive.contentOrNull }.getOrNull()
    private fun JsonElement.intVal(): Int? = runCatching { this.jsonPrimitive.intOrNull }.getOrNull()
}
