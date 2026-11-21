package com.spotify.sovereign.playback

import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionParameters
import androidx.media3.common.util.UnstableApi
import com.spotify.sovereign.model.AudioFxSettings
import com.spotify.sovereign.model.DspEngineStatus
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.BaseDataSource
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.HttpDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.CacheWriter
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import okhttp3.ConnectionPool
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.util.concurrent.TimeUnit
import kotlin.math.min

/**
 * Sovereign Bounded-Chunk OkHttp DataSource.
 * YouTube's googlevideo CDN rejects unbounded HTTP requests (bytes=0- or bytes=N-)
 * with HTTP 403 on IOS/VR audio streams, while returning HTTP 206 instantaneously
 * for strictly bounded Range chunks (e.g. bytes=0-524287, bytes=524288-1048575).
 *
 * This DataSource transparently slices EVERY ExoPlayer read & seek into seamless
 * 512 KB bounded HTTP/2 range chunks — eliminating 0:00 stalls and 0:29 cutoffs forever.
 */
@OptIn(UnstableApi::class)
class BoundedChunkOkHttpDataSource(
    private val client: OkHttpClient,
    private val headersProvider: () -> Map<String, String>,
    private val chunkSizeBytes: Long = 512L * 1024L,
) : BaseDataSource(/* isNetwork = */ true) {

    private var dataSpec: DataSpec? = null
    private var currentUri: Uri? = null
    private var activeResponse: Response? = null
    private var activeStream: InputStream? = null

    private var currentPosition: Long = 0L
    private var requestEndExclusive: Long = C.LENGTH_UNSET.toLong()
    private var totalContentLength: Long = C.LENGTH_UNSET.toLong()
    private var currentChunkRemaining: Long = 0L
    private var opened = false

    override fun open(dataSpec: DataSpec): Long {
        this.dataSpec = dataSpec
        this.currentUri = dataSpec.uri
        this.currentPosition = dataSpec.position
        this.requestEndExclusive = if (dataSpec.length != C.LENGTH_UNSET.toLong()) {
            dataSpec.position + dataSpec.length
        } else {
            C.LENGTH_UNSET.toLong()
        }
        this.totalContentLength = C.LENGTH_UNSET.toLong()
        this.currentChunkRemaining = 0L

        transferInitializing(dataSpec)
        openNextChunk(dataSpec)
        opened = true
        transferStarted(dataSpec)

        return when {
            dataSpec.length != C.LENGTH_UNSET.toLong() -> dataSpec.length
            totalContentLength != C.LENGTH_UNSET.toLong() -> (totalContentLength - currentPosition).coerceAtLeast(0L)
            else -> C.LENGTH_UNSET.toLong()
        }
    }

    private fun openNextChunk(spec: DataSpec) {
        closeActiveChunk()

        if (totalContentLength != C.LENGTH_UNSET.toLong() && currentPosition >= totalContentLength) {
            currentChunkRemaining = 0L
            return
        }
        if (requestEndExclusive != C.LENGTH_UNSET.toLong() && currentPosition >= requestEndExclusive) {
            currentChunkRemaining = 0L
            return
        }

        var endInclusive = currentPosition + chunkSizeBytes - 1L
        if (requestEndExclusive != C.LENGTH_UNSET.toLong()) {
            endInclusive = min(endInclusive, requestEndExclusive - 1L)
        }
        if (totalContentLength != C.LENGTH_UNSET.toLong()) {
            endInclusive = min(endInclusive, totalContentLength - 1L)
        }

        val reqBuilder = Request.Builder()
            .url(spec.uri.toString())
            .header("Range", "bytes=$currentPosition-$endInclusive")

        headersProvider().forEach { (k, v) ->
            if (k.isNotBlank() && !k.equals("Range", ignoreCase = true)) {
                reqBuilder.header(k, v)
            }
        }

        val resp = try {
            client.newCall(reqBuilder.build()).execute()
        } catch (e: IOException) {
            throw HttpDataSource.HttpDataSourceException(
                e,
                spec,
                androidx.media3.common.PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
                HttpDataSource.HttpDataSourceException.TYPE_OPEN,
            )
        }

        val code = resp.code
        if (code != 200 && code != 206) {
            val headersMap = resp.headers.toMultimap()
            resp.close()
            throw HttpDataSource.InvalidResponseCodeException(
                code,
                resp.message,
                null,
                headersMap,
                spec,
                ByteArray(0),
            )
        }

        // Parse total length from Content-Range: bytes start-end/TOTAL
        val contentRange = resp.header("Content-Range")
        if (!contentRange.isNullOrBlank()) {
            val slashIdx = contentRange.lastIndexOf('/')
            if (slashIdx > 0) {
                val totalStr = contentRange.substring(slashIdx + 1).trim()
                totalStr.toLongOrNull()?.let { totalContentLength = it }
            }
        }

        val bodyLen = resp.body.contentLength()
        if (code == 200 && totalContentLength == C.LENGTH_UNSET.toLong() && bodyLen > 0L) {
            totalContentLength = bodyLen
        }

        activeResponse = resp
        activeStream = resp.body.byteStream()
        currentChunkRemaining = if (bodyLen > 0L) bodyLen else (endInclusive - currentPosition + 1L)
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        if (length == 0) return 0
        val spec = dataSpec ?: return C.RESULT_END_OF_INPUT

        while (true) {
            if (totalContentLength != C.LENGTH_UNSET.toLong() && currentPosition >= totalContentLength) {
                return C.RESULT_END_OF_INPUT
            }
            if (requestEndExclusive != C.LENGTH_UNSET.toLong() && currentPosition >= requestEndExclusive) {
                return C.RESULT_END_OF_INPUT
            }

            if (activeStream == null || currentChunkRemaining <= 0L) {
                openNextChunk(spec)
                if (activeStream == null || currentChunkRemaining <= 0L) {
                    return C.RESULT_END_OF_INPUT
                }
            }

            val toRead = min(length.toLong(), currentChunkRemaining).toInt()
            val bytesRead = activeStream?.read(buffer, offset, toRead) ?: -1
            if (bytesRead == -1) {
                closeActiveChunk()
                // If we reached EOF before total length, open the next bounded chunk!
                if (totalContentLength == C.LENGTH_UNSET.toLong() || currentPosition < totalContentLength) {
                    if (requestEndExclusive == C.LENGTH_UNSET.toLong() || currentPosition < requestEndExclusive) {
                        openNextChunk(spec)
                        if (activeStream != null && currentChunkRemaining > 0L) {
                            continue
                        }
                    }
                }
                return C.RESULT_END_OF_INPUT
            }

            currentPosition += bytesRead
            currentChunkRemaining -= bytesRead
            bytesTransferred(bytesRead)
            return bytesRead
        }
    }

    override fun getUri(): Uri? = currentUri

    override fun close() {
        closeActiveChunk()
        dataSpec = null
        currentUri = null
        if (opened) {
            opened = false
            transferEnded()
        }
    }

    private fun closeActiveChunk() {
        runCatching { activeStream?.close() }
        runCatching { activeResponse?.close() }
        activeStream = null
        activeResponse = null
        currentChunkRemaining = 0L
    }
}

/**
 * Sovereign Background Audio Engine with Bounded-Chunk HTTP/2 Streaming,
 * 512 MB Disk SimpleCache, and Predictive Next-Track Pre-Buffering.
 */
@OptIn(UnstableApi::class)
class SovereignPlaybackService : MediaSessionService() {
    companion object {
        private const val TAG = "SovereignPlayback"
        private const val MAX_CACHE_BYTES = 512L * 1024L * 1024L // 512 MB audio cache
        private const val PREBUFFER_BYTES = 512L * 1024L // First 512 KB pre-warmed on disk

        @Volatile
        var activeService: SovereignPlaybackService? = null
            private set

        @Volatile
        private var sharedCache: SimpleCache? = null
    }

    private var mediaSession: MediaSession? = null
    private lateinit var cacheDataSourceFactory: CacheDataSource.Factory
    lateinit var player: ExoPlayer
        private set

    val dspEngine: SovereignDspEngine = SovereignDspEngine()

    @Volatile
    private var pendingAudioFxSettings: AudioFxSettings = AudioFxSettings()

    @Volatile
    private var activeHeaders: Map<String, String> = mapOf(
        "User-Agent" to "com.google.ios.youtube/20.10.4 (iPhone16,2; U; CPU iOS 18_3_2 like Mac OS X;)",
        "Accept" to "*/*",
    )

    private val streamHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectionPool(ConnectionPool(10, 5, TimeUnit.MINUTES))
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    override fun onCreate() {
        super.onCreate()
        val cacheDir = File(cacheDir, "sovereign_audio_cache_v2").apply { mkdirs() }
        val cache = sharedCache ?: SimpleCache(
            cacheDir,
            LeastRecentlyUsedCacheEvictor(MAX_CACHE_BYTES),
            StandaloneDatabaseProvider(this),
        ).also { sharedCache = it }

        val boundedUpstreamFactory = DataSource.Factory {
            BoundedChunkOkHttpDataSource(
                client = streamHttpClient,
                headersProvider = { activeHeaders },
                chunkSizeBytes = 512L * 1024L,
            )
        }

        cacheDataSourceFactory = CacheDataSource.Factory()
            .setCache(cache)
            .setUpstreamDataSourceFactory(boundedUpstreamFactory)
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)

        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                15_000, // minBufferMs
                50_000, // maxBufferMs
                500,    // instant start!
                1_200,
            )
            .build()

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()

        player = ExoPlayer.Builder(this)
            .setMediaSourceFactory(DefaultMediaSourceFactory(cacheDataSourceFactory))
            .setLoadControl(loadControl)
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .build()

        // Ensure PCM AudioTrack routing so DynamicsProcessing / Dolby Atmos 3D DSP ALWAYS processes every sample
        player.trackSelectionParameters = player.trackSelectionParameters
            .buildUpon()
            .setAudioOffloadPreferences(
                TrackSelectionParameters.AudioOffloadPreferences.Builder()
                    .setAudioOffloadMode(TrackSelectionParameters.AudioOffloadPreferences.AUDIO_OFFLOAD_MODE_DISABLED)
                    .build()
            )
            .build()

        // Attach Studio DSP Engine immediately & re-attach whenever Android assigns a new AudioSessionId
        if (player.audioSessionId != C.AUDIO_SESSION_ID_UNSET && player.audioSessionId > 0) {
            dspEngine.attach(player.audioSessionId, pendingAudioFxSettings)
        }
        player.addListener(object : Player.Listener {
            override fun onAudioSessionIdChanged(audioSessionId: Int) {
                if (audioSessionId != C.AUDIO_SESSION_ID_UNSET && audioSessionId > 0) {
                    dspEngine.attach(audioSessionId, pendingAudioFxSettings)
                }
            }
        })

        mediaSession = MediaSession.Builder(this, player).build()
        activeService = this
    }

    fun applyAudioFx(settings: AudioFxSettings): DspEngineStatus {
        pendingAudioFxSettings = settings
        val sid = runCatching { player.audioSessionId }.getOrDefault(0)
        if (sid != C.AUDIO_SESSION_ID_UNSET && sid > 0) {
            dspEngine.attach(sid, settings)
        } else {
            dspEngine.apply(settings)
        }
        return dspEngine.status
    }

    fun getDspStatus(): DspEngineStatus = dspEngine.status

    fun getCacheSizeBytes(): Long = runCatching { sharedCache?.cacheSpace ?: 0L }.getOrDefault(0L)

    fun clearAudioCache(): Long {
        val before = getCacheSizeBytes()
        runCatching {
            val cache = sharedCache ?: return 0L
            cache.keys.toList().forEach { key ->
                runCatching { cache.removeResource(key) }
            }
        }
        return before
    }

    fun playStream(
        url: String,
        headers: Map<String, String>,
        title: String,
        artist: String,
        artworkUri: String?,
        mediaId: String,
        startPositionMs: Long = 0L,
    ) {
        activeHeaders = headers.filterKeys { it.isNotBlank() }

        val metaBuilder = MediaMetadata.Builder()
            .setTitle(title)
            .setArtist(artist)
        if (!artworkUri.isNullOrBlank()) {
            metaBuilder.setArtworkUri(Uri.parse(artworkUri))
        }

        val item = MediaItem.Builder()
            .setMediaId(mediaId)
            .setCustomCacheKey("sov3_$mediaId")
            .setUri(url)
            .setMediaMetadata(metaBuilder.build())
            .build()

        player.setMediaItem(item, startPositionMs)
        player.prepare()
        player.playWhenReady = true
    }

    fun preBufferNextTrack(url: String, headers: Map<String, String>, mediaId: String) {
        runCatching {
            val cache = sharedCache ?: return
            val key = "sov3_$mediaId"
            if (cache.getCachedBytes(key, 0L, PREBUFFER_BYTES) >= PREBUFFER_BYTES / 2) {
                return
            }
            val cleanHeaders = headers.filterKeys { it.isNotBlank() }
            val dedicatedUpstream = DataSource.Factory {
                BoundedChunkOkHttpDataSource(
                    client = streamHttpClient,
                    headersProvider = { cleanHeaders },
                    chunkSizeBytes = PREBUFFER_BYTES,
                )
            }
            val preCacheDs = CacheDataSource.Factory()
                .setCache(cache)
                .setUpstreamDataSourceFactory(dedicatedUpstream)
                .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
                .createDataSourceForDownloading()

            val spec = DataSpec.Builder()
                .setUri(url)
                .setKey(key)
                .setPosition(0L)
                .setLength(PREBUFFER_BYTES)
                .build()

            CacheWriter(preCacheDs, spec, null, null).cache()
            Log.d(TAG, "Pre-buffered $mediaId into SimpleCache ($PREBUFFER_BYTES bytes)")
        }.onFailure {
            Log.d(TAG, "Pre-buffer skipped for $mediaId: ${it.message}")
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onTaskRemoved(rootIntent: Intent?) {
        val p = mediaSession?.player
        if (p == null || !p.playWhenReady || p.mediaItemCount == 0) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        activeService = null
        dspEngine.release()
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }
}
