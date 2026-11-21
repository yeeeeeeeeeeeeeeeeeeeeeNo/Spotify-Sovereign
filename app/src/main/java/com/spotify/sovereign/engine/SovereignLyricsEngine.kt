package com.spotify.sovereign.engine

import com.spotify.sovereign.model.LyricLine
import com.spotify.sovereign.model.LyricsPayload
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder
import kotlin.math.abs

/**
 * Duration-Locked Sovereign Lyrics Engine.
 * Eliminates lyrics drift by matching LRCLIB candidates strictly against the
 * actual ExoPlayer track duration, scoring synced candidates first, and cleaning
 * track/artist noise strings.
 */
class SovereignLyricsEngine(
    private val httpClient: OkHttpClient,
) {
    companion object {
        private val LRC_TAG_REGEX = Regex("""\[(\d{1,2}):(\d{2})(?:\.(\d{1,3}))?]""")
        private val NOISE_REGEX = Regex(
            """\s*[\(\[](?:official|audio|video|lyric|lyrics|hd|4k|hq|remastered|explicit|clean|visualizer|feat\.?|ft\.?).*?[\)\]]""",
            RegexOption.IGNORE_CASE,
        )
    }

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    suspend fun fetchBestLyrics(
        rawTitle: String,
        rawArtist: String,
        actualDurationSec: Int,
    ): LyricsPayload = withContext(Dispatchers.IO) {
        val cleanTitle = cleanTrackTitle(rawTitle)
        val cleanArtist = cleanArtistName(rawArtist)

        // 1. Search LRCLIB with clean title + artist so we can score ALL versions by exact duration!
        val candidates = mutableListOf<JsonObject>()

        if (actualDurationSec > 0) {
            val exactUrl = buildString {
                append("https://lrclib.net/api/get?")
                append("track_name=").append(enc(cleanTitle))
                append("&artist_name=").append(enc(cleanArtist))
                append("&duration=").append(actualDurationSec)
            }
            getJsonElement(exactUrl)?.asObj()?.let { candidates.add(it) }
        }

        val searchUrl = "https://lrclib.net/api/search?track_name=${enc(cleanTitle)}&artist_name=${enc(cleanArtist)}"
        getJsonElement(searchUrl)?.asArr()?.forEach { el ->
            el.asObj()?.let { candidates.add(it) }
        }

        if (candidates.isEmpty()) {
            val broadUrl = "https://lrclib.net/api/search?q=${enc("$cleanTitle $cleanArtist")}"
            getJsonElement(broadUrl)?.asArr()?.forEach { el ->
                el.asObj()?.let { candidates.add(it) }
            }
        }

        if (candidates.isEmpty()) {
            return@withContext LyricsPayload(emptyList(), false, "None", 0)
        }

        // Score every candidate: synced lyrics + tight duration match wins!
        data class ScoredCandidate(
            val obj: JsonObject,
            val synced: Boolean,
            val durationDelta: Int,
            val score: Int,
            val durationSec: Int,
        )

        val scored = candidates.mapNotNull { obj ->
            val syncedStr = obj["syncedLyrics"]?.str().orEmpty()
            val plainStr = obj["plainLyrics"]?.str().orEmpty()
            if (syncedStr.isBlank() && plainStr.isBlank()) return@mapNotNull null

            val isSynced = syncedStr.isNotBlank()
            val candDur = obj["duration"]?.doubleVal()?.toInt() ?: 0
            val durDelta = if (actualDurationSec > 0 && candDur > 0) {
                abs(candDur - actualDurationSec)
            } else {
                5
            }

            // Reject badly mismatched synced tracks (> 16s off means wrong version/live/remix)
            if (isSynced && actualDurationSec > 0 && durDelta > 16) {
                return@mapNotNull null
            }

            val candTrack = obj["trackName"]?.str().orEmpty()
            val candArtist = obj["artistName"]?.str().orEmpty()
            var score = 0
            if (isSynced) score += 1000
            score -= durDelta * 25
            if (candTrack.equals(cleanTitle, ignoreCase = true)) score += 120
            if (candArtist.contains(cleanArtist, ignoreCase = true) || cleanArtist.contains(candArtist, ignoreCase = true)) {
                score += 90
            }
            ScoredCandidate(obj, isSynced, durDelta, score, candDur)
        }

        val winner = scored.maxByOrNull { it.score }
            ?: return@withContext LyricsPayload(emptyList(), false, "None", 0)

        val syncedRaw = winner.obj["syncedLyrics"]?.str()
        if (!syncedRaw.isNullOrBlank()) {
            val parsed = parseSyncedLrc(syncedRaw)
            if (parsed.isNotEmpty()) {
                return@withContext LyricsPayload(
                    lines = parsed,
                    isSynced = true,
                    source = if (winner.durationDelta <= 2) "LRCLIB Exact Sync" else "LRCLIB Sync (±${winner.durationDelta}s)",
                    matchedDurationSec = winner.durationSec,
                )
            }
        }

        val plainRaw = winner.obj["plainLyrics"]?.str().orEmpty()
        val plainLines = plainRaw.lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .mapIndexed { idx, txt -> LyricLine(idx * 4_000L, txt) }

        LyricsPayload(
            lines = plainLines,
            isSynced = false,
            source = "LRCLIB Plain",
            matchedDurationSec = winner.durationSec,
        )
    }

    private fun parseSyncedLrc(raw: String): List<LyricLine> {
        val out = mutableListOf<LyricLine>()
        for (line in raw.lines()) {
            val matches = LRC_TAG_REGEX.findAll(line).toList()
            if (matches.isEmpty()) continue
            val text = line.substring(matches.last().range.last + 1).trim()
            if (text.isEmpty()) continue
            // Supports multi-timestamp LRC lines like [01:12.40][02:24.80]Chorus line
            for (m in matches) {
                val mins = m.groupValues[1].toLongOrNull() ?: 0L
                val secs = m.groupValues[2].toLongOrNull() ?: 0L
                val frac = m.groupValues[3]
                val ms = when (frac.length) {
                    1 -> frac.toLong() * 100L
                    2 -> frac.toLong() * 10L
                    3 -> frac.toLong()
                    else -> 0L
                }
                out.add(LyricLine(mins * 60_000L + secs * 1_000L + ms, text))
            }
        }
        return out.sortedBy { it.timeMs }
    }

    private fun cleanTrackTitle(raw: String): String {
        return raw.replace(NOISE_REGEX, "")
            .replace(Regex("""\s+(?:feat\.?|ft\.?)\s+.*$""", RegexOption.IGNORE_CASE), "")
            .trim()
            .ifBlank { raw.trim() }
    }

    private fun cleanArtistName(raw: String): String {
        return raw.split(",", "&", "•", "feat.", "ft.", " x ", " X ")
            .firstOrNull()
            ?.replace(Regex("""\s*-\s*Topic$""", RegexOption.IGNORE_CASE), "")
            ?.trim()
            ?.ifBlank { raw.trim() }
            ?: raw.trim()
    }

    private fun enc(s: String): String = URLEncoder.encode(s, "UTF-8")

    private fun getJsonElement(url: String): JsonElement? {
        val req = Request.Builder()
            .url(url)
            .header("User-Agent", "SpotifySovereign/2.0 (Android)")
            .build()
        return runCatching {
            httpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@use null
                json.parseToJsonElement(resp.body.string())
            }
        }.getOrNull()
    }

    private fun JsonElement.asObj(): JsonObject? = this as? JsonObject
    private fun JsonElement.asArr(): JsonArray? = this as? JsonArray
    private fun JsonElement.str(): String? = runCatching { this.jsonPrimitive.contentOrNull }.getOrNull()
    private fun JsonElement.doubleVal(): Double? = runCatching { this.jsonPrimitive.doubleOrNull }.getOrNull()
}
