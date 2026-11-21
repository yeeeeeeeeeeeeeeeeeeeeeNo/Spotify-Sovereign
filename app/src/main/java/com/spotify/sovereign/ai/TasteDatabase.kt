package com.spotify.sovereign.ai

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.spotify.sovereign.model.ArtistAffinity
import com.spotify.sovereign.model.CollectionItem
import com.spotify.sovereign.model.TrackItem
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.Calendar

class TasteDatabase(context: Context) : SQLiteOpenHelper(context, "sovereign_taste_brain.db", null, 1) {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE artists (
                name_key TEXT PRIMARY KEY,
                display_name TEXT NOT NULL,
                score REAL NOT NULL DEFAULT 0.0,
                play_count INTEGER NOT NULL DEFAULT 0,
                skip_count INTEGER NOT NULL DEFAULT 0,
                morning_score REAL NOT NULL DEFAULT 0.0,
                afternoon_score REAL NOT NULL DEFAULT 0.0,
                evening_score REAL NOT NULL DEFAULT 0.0,
                night_score REAL NOT NULL DEFAULT 0.0,
                image_url TEXT NOT NULL DEFAULT '',
                last_updated INTEGER NOT NULL DEFAULT 0
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE tracks (
                id TEXT PRIMARY KEY,
                title TEXT NOT NULL,
                artist TEXT NOT NULL,
                album TEXT NOT NULL DEFAULT '',
                thumbnail_url TEXT NOT NULL DEFAULT '',
                duration_sec INTEGER NOT NULL DEFAULT 0,
                explicit INTEGER NOT NULL DEFAULT 0,
                affinity_score REAL NOT NULL DEFAULT 0.0,
                play_count INTEGER NOT NULL DEFAULT 0,
                skip_count INTEGER NOT NULL DEFAULT 0,
                loop_count INTEGER NOT NULL DEFAULT 0,
                liked INTEGER NOT NULL DEFAULT 0,
                lyrics_offset_ms INTEGER NOT NULL DEFAULT 0,
                last_played_ms INTEGER NOT NULL DEFAULT 0
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE transitions (
                from_id TEXT NOT NULL,
                to_id TEXT NOT NULL,
                weight REAL NOT NULL DEFAULT 1.0,
                PRIMARY KEY (from_id, to_id)
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE saved_collections (
                id TEXT PRIMARY KEY,
                payload_json TEXT NOT NULL,
                saved_at INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE meta (
                k TEXT PRIMARY KEY,
                v TEXT NOT NULL
            )
            """.trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit

    // =========================================================================
    // META / ONBOARDING / SETTINGS
    // =========================================================================
    fun getMeta(key: String, default: String = ""): String {
        readableDatabase.rawQuery("SELECT v FROM meta WHERE k = ?", arrayOf(key)).use { c ->
            return if (c.moveToFirst()) c.getString(0) else default
        }
    }

    fun setMeta(key: String, value: String) {
        val cv = ContentValues().apply {
            put("k", key)
            put("v", value)
        }
        writableDatabase.insertWithOnConflict("meta", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun isOnboardingCompleted(): Boolean = getMeta("onboarding_done", "0") == "1"

    fun setOnboardingCompleted(done: Boolean) = setMeta("onboarding_done", if (done) "1" else "0")

    fun getSearchHistory(): List<String> {
        val raw = getMeta("search_history", "[]")
        return runCatching { json.decodeFromString<List<String>>(raw) }.getOrDefault(emptyList())
    }

    fun addSearchHistory(query: String): List<String> {
        val clean = query.trim()
        if (clean.isEmpty()) return getSearchHistory()
        val updated = (listOf(clean) + getSearchHistory().filterNot { it.equals(clean, ignoreCase = true) }).take(14)
        setMeta("search_history", json.encodeToString(updated))
        return updated
    }

    // =========================================================================
    // ARTIST AFFINITY MATRIX (TIME-OF-DAY AWARE)
    // =========================================================================
    fun currentTimeColumn(): String {
        val h = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return when (h) {
            in 6..11 -> "morning_score"
            in 12..16 -> "afternoon_score"
            in 17..21 -> "evening_score"
            else -> "night_score"
        }
    }

    fun currentTimeBucketLabel(): String {
        val h = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return when (h) {
            in 6..11 -> "Morning"
            in 12..16 -> "Afternoon"
            in 17..21 -> "Evening"
            else -> "Late Night"
        }
    }

    fun updateArtistScore(
        artistName: String,
        delta: Double,
        isPlay: Boolean = false,
        isSkip: Boolean = false,
        imageUrl: String = "",
    ) {
        val clean = primaryArtist(artistName)
        if (clean.isBlank()) return
        val key = clean.lowercase()
        val now = System.currentTimeMillis()
        val timeCol = currentTimeColumn()

        writableDatabase.beginTransaction()
        try {
            var curScore = 0.0
            var plays = 0
            var skips = 0
            var curTime = 0.0
            var existingImg = ""
            var exists = false

            writableDatabase.rawQuery(
                "SELECT score, play_count, skip_count, $timeCol, image_url FROM artists WHERE name_key = ?",
                arrayOf(key),
            ).use { c ->
                if (c.moveToFirst()) {
                    exists = true
                    curScore = c.getDouble(0)
                    plays = c.getInt(1)
                    skips = c.getInt(2)
                    curTime = c.getDouble(3)
                    existingImg = c.getString(4)
                }
            }

            val newScore = (curScore + delta).coerceIn(-15.0, 100.0)
            val newTime = (curTime + delta * 0.85).coerceIn(-10.0, 60.0)
            val cv = ContentValues().apply {
                put("name_key", key)
                put("display_name", clean)
                put("score", newScore)
                put("play_count", plays + if (isPlay) 1 else 0)
                put("skip_count", skips + if (isSkip) 1 else 0)
                put(timeCol, newTime)
                put("image_url", imageUrl.ifBlank { existingImg })
                put("last_updated", now)
            }
            if (exists) {
                writableDatabase.update("artists", cv, "name_key = ?", arrayOf(key))
            } else {
                writableDatabase.insert("artists", null, cv)
            }
            writableDatabase.setTransactionSuccessful()
        } finally {
            writableDatabase.endTransaction()
        }
    }

    fun getTopArtists(limit: Int = 12): List<ArtistAffinity> {
        val timeCol = currentTimeColumn()
        val out = mutableListOf<ArtistAffinity>()
        readableDatabase.rawQuery(
            """
            SELECT display_name, score, play_count, skip_count, $timeCol
            FROM artists
            WHERE score > -1.0
            ORDER BY (score + $timeCol * 0.55) DESC
            LIMIT ?
            """.trimIndent(),
            arrayOf(limit.toString()),
        ).use { c ->
            while (c.moveToNext()) {
                out.add(
                    ArtistAffinity(
                        name = c.getString(0),
                        score = c.getDouble(1),
                        plays = c.getInt(2),
                        skips = c.getInt(3),
                        timeBucketScore = c.getDouble(4),
                    )
                )
            }
        }
        return out
    }

    fun getArtistScoreMap(): Map<String, Double> {
        val timeCol = currentTimeColumn()
        val map = HashMap<String, Double>()
        readableDatabase.rawQuery(
            "SELECT name_key, score, $timeCol FROM artists",
            null,
        ).use { c ->
            while (c.moveToNext()) {
                map[c.getString(0)] = c.getDouble(1) + c.getDouble(2) * 0.5
            }
        }
        return map
    }

    fun getBlockedArtistKeys(): Set<String> {
        val out = HashSet<String>()
        readableDatabase.rawQuery(
            "SELECT name_key FROM artists WHERE score <= -2.0 AND skip_count >= 2",
            null,
        ).use { c ->
            while (c.moveToNext()) out.add(c.getString(0))
        }
        return out
    }

    fun getTotalTelemetryCounts(): Pair<Int, Int> {
        var plays = 0
        var skips = 0
        readableDatabase.rawQuery("SELECT SUM(play_count), SUM(skip_count) FROM tracks", null).use { c ->
            if (c.moveToFirst()) {
                plays = c.getInt(0)
                skips = c.getInt(1)
            }
        }
        return plays to skips
    }

    // =========================================================================
    // TRACK TELEMETRY + LIKES + PER-SONG LYRICS SYNC OFFSET
    // =========================================================================
    fun upsertTrackTelemetry(
        track: TrackItem,
        scoreDelta: Double = 0.0,
        incPlay: Boolean = false,
        incSkip: Boolean = false,
        incLoop: Boolean = false,
        setLiked: Boolean? = null,
        touchPlayedNow: Boolean = false,
    ) {
        writableDatabase.beginTransaction()
        try {
            var curScore = 0.0
            var plays = 0
            var skips = 0
            var loops = 0
            var liked = 0
            var lyricsOffset = 0L
            var lastPlayed = 0L
            var exists = false

            writableDatabase.rawQuery(
                "SELECT affinity_score, play_count, skip_count, loop_count, liked, lyrics_offset_ms, last_played_ms FROM tracks WHERE id = ?",
                arrayOf(track.id),
            ).use { c ->
                if (c.moveToFirst()) {
                    exists = true
                    curScore = c.getDouble(0)
                    plays = c.getInt(1)
                    skips = c.getInt(2)
                    loops = c.getInt(3)
                    liked = c.getInt(4)
                    lyricsOffset = c.getLong(5)
                    lastPlayed = c.getLong(6)
                }
            }

            val cv = ContentValues().apply {
                put("id", track.id)
                put("title", track.title)
                put("artist", track.artist)
                put("album", track.album)
                put("thumbnail_url", track.thumbnailUrl)
                put("duration_sec", track.durationSec)
                put("explicit", if (track.explicit) 1 else 0)
                put("affinity_score", (curScore + scoreDelta).coerceIn(-15.0, 100.0))
                put("play_count", plays + if (incPlay) 1 else 0)
                put("skip_count", skips + if (incSkip) 1 else 0)
                put("loop_count", loops + if (incLoop) 1 else 0)
                put("liked", setLiked?.let { if (it) 1 else 0 } ?: liked)
                put("lyrics_offset_ms", lyricsOffset)
                put("last_played_ms", if (touchPlayedNow) System.currentTimeMillis() else lastPlayed)
            }
            if (exists) {
                writableDatabase.update("tracks", cv, "id = ?", arrayOf(track.id))
            } else {
                writableDatabase.insert("tracks", null, cv)
            }
            writableDatabase.setTransactionSuccessful()
        } finally {
            writableDatabase.endTransaction()
        }
    }

    fun getLikedTracks(): List<TrackItem> = queryTracks("WHERE liked = 1 ORDER BY last_played_ms DESC, affinity_score DESC", 200)

    fun getRecentTracks(limit: Int = 30): List<TrackItem> = queryTracks("WHERE last_played_ms > 0 ORDER BY last_played_ms DESC", limit)

    fun getOnRepeatTracks(limit: Int = 18): List<TrackItem> =
        queryTracks("WHERE play_count >= 1 AND affinity_score >= 0.5 ORDER BY (affinity_score + loop_count * 3.0 + play_count * 1.2) DESC", limit)

    fun getAllKnownTrackIds(): Set<String> {
        val out = HashSet<String>()
        readableDatabase.rawQuery("SELECT id FROM tracks WHERE play_count > 0 OR skip_count > 0", null).use { c ->
            while (c.moveToNext()) out.add(c.getString(0))
        }
        return out
    }

    fun getSkippedTrackIds(): Set<String> {
        val out = HashSet<String>()
        readableDatabase.rawQuery("SELECT id FROM tracks WHERE skip_count >= 1 AND affinity_score < 0.0", null).use { c ->
            while (c.moveToNext()) out.add(c.getString(0))
        }
        return out
    }

    fun getLyricsOffsetMs(trackId: String): Long {
        readableDatabase.rawQuery("SELECT lyrics_offset_ms FROM tracks WHERE id = ?", arrayOf(trackId)).use { c ->
            return if (c.moveToFirst()) c.getLong(0) else 0L
        }
    }

    fun setLyricsOffsetMs(track: TrackItem, offsetMs: Long) {
        upsertTrackTelemetry(track)
        val cv = ContentValues().apply { put("lyrics_offset_ms", offsetMs) }
        writableDatabase.update("tracks", cv, "id = ?", arrayOf(track.id))
    }

    // =========================================================================
    // MARKOV VIBE TRANSITIONS
    // =========================================================================
    fun recordTransition(fromId: String, toId: String, delta: Double = 1.0) {
        if (fromId == toId || fromId.isBlank() || toId.isBlank()) return
        writableDatabase.execSQL(
            """
            INSERT INTO transitions (from_id, to_id, weight) VALUES (?, ?, ?)
            ON CONFLICT(from_id, to_id) DO UPDATE SET weight = weight + ?
            """.trimIndent(),
            arrayOf<Any>(fromId, toId, delta, delta),
        )
    }

    fun getMarkovNextTracks(fromId: String, limit: Int = 6): List<TrackItem> {
        val out = mutableListOf<TrackItem>()
        readableDatabase.rawQuery(
            """
            SELECT t.id, t.title, t.artist, t.album, t.thumbnail_url, t.duration_sec, t.explicit
            FROM transitions tr
            JOIN tracks t ON tr.to_id = t.id
            WHERE tr.from_id = ? AND t.affinity_score >= -0.5
            ORDER BY tr.weight DESC
            LIMIT ?
            """.trimIndent(),
            arrayOf(fromId, limit.toString()),
        ).use { c ->
            while (c.moveToNext()) {
                out.add(
                    TrackItem(
                        id = c.getString(0),
                        title = c.getString(1),
                        artist = c.getString(2),
                        album = c.getString(3),
                        thumbnailUrl = c.getString(4),
                        durationSec = c.getInt(5),
                        explicit = c.getInt(6) == 1,
                    )
                )
            }
        }
        return out
    }

    // =========================================================================
    // SAVED COLLECTIONS & IMPORTED PLAYLISTS
    // =========================================================================
    fun getSavedCollections(): List<CollectionItem> {
        val out = mutableListOf<CollectionItem>()
        readableDatabase.rawQuery("SELECT payload_json FROM saved_collections ORDER BY saved_at DESC", null).use { c ->
            while (c.moveToNext()) {
                runCatching { json.decodeFromString<CollectionItem>(c.getString(0)) }.getOrNull()?.let { out.add(it) }
            }
        }
        return out
    }

    fun saveCollection(item: CollectionItem) {
        val cv = ContentValues().apply {
            put("id", item.id)
            put("payload_json", json.encodeToString(item))
            put("saved_at", System.currentTimeMillis())
        }
        writableDatabase.insertWithOnConflict("saved_collections", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun removeCollection(id: String) {
        writableDatabase.delete("saved_collections", "id = ?", arrayOf(id))
    }

    fun resetTasteBrain() {
        writableDatabase.execSQL("DELETE FROM artists")
        writableDatabase.execSQL("DELETE FROM transitions")
        setOnboardingCompleted(false)
    }

    private fun queryTracks(whereAndOrder: String, limit: Int): List<TrackItem> {
        val out = mutableListOf<TrackItem>()
        readableDatabase.rawQuery(
            "SELECT id, title, artist, album, thumbnail_url, duration_sec, explicit FROM tracks $whereAndOrder LIMIT ?",
            arrayOf(limit.toString()),
        ).use { c ->
            while (c.moveToNext()) {
                out.add(
                    TrackItem(
                        id = c.getString(0),
                        title = c.getString(1),
                        artist = c.getString(2),
                        album = c.getString(3),
                        thumbnailUrl = c.getString(4),
                        durationSec = c.getInt(5),
                        explicit = c.getInt(6) == 1,
                    )
                )
            }
        }
        return out
    }

    private fun primaryArtist(raw: String): String {
        return raw.split(",", "&", "•", "feat.", "ft.")
            .firstOrNull()
            ?.replace(Regex("""\s*-\s*Topic$""", RegexOption.IGNORE_CASE), "")
            ?.trim()
            .orEmpty()
    }
}
