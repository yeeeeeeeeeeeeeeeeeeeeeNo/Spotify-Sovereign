package com.spotify.sovereign.engine

import android.util.Log
import com.spotify.sovereign.model.LyricsPayload
import com.spotify.sovereign.model.ResolvedStream
import com.spotify.sovereign.model.TrackItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.Inet4Address
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketTimeoutException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.min

data class CastTvDevice(
    val name: String,
    val ip: String,
    val port: Int = 9470,
)

data class CastTvStatus(
    val active: Boolean = false,
    val playing: Boolean = false,
    val preparing: Boolean = false,
    val finished: Boolean = false,
    val trackId: String = "",
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
)

/**
 * Sovereign Cast Sender + Local Bounded-Chunk HTTP Audio Relay + Bidirectional TV Remote Callback Listener.
 *
 * - UDP 9472: Instant <50ms discovery of Mono TV on LAN (zero GMS/mDNS!)
 * - HTTP 9471: Streams 512KB bounded CDN chunks to TV's native MediaPlayer -> zero 30s freeze, instant seeking!
 * - TCP 9473: Receives instant TV Remote callbacks (NEXT / PREV / ENDED / STOP) from Xiaomi TV!
 */
class SovereignCastSender(
    private val httpClient: OkHttpClient,
    private val onRemoteEvent: (String) -> Unit,
) {
    companion object {
        private const val TAG = "SovereignCastSender"
        const val PROXY_PORT = 9471
        const val UDP_DISCOVERY_PORT = 9472
        const val CTRL_CALLBACK_PORT = 9473
        private const val CHUNK_SIZE = 512L * 1024L
    }

    private val json = Json { ignoreUnknownKeys = true }
    private val activeStreams = ConcurrentHashMap<String, ResolvedStream>()
    private val totalLengthCache = ConcurrentHashMap<String, Long>()
    private val serversStarted = AtomicBoolean(false)

    private var proxyServer: ServerSocket? = null
    private var ctrlServer: ServerSocket? = null

    fun ensureServersRunning() {
        if (!serversStarted.compareAndSet(false, true)) return
        Thread(::runHttpAudioRelay, "SovCastProxy").apply { isDaemon = true }.start()
        Thread(::runReverseControlServer, "SovCastCtrl").apply { isDaemon = true }.start()
    }

    fun shutdown() {
        serversStarted.set(false)
        runCatching { proxyServer?.close() }
        runCatching { ctrlServer?.close() }
    }

    fun getLocalWifiIpv4(): String? {
        return runCatching {
            val ifaces = NetworkInterface.getNetworkInterfaces() ?: return@runCatching null
            var fallback: String? = null
            for (ni in ifaces) {
                if (!ni.isUp || ni.isLoopback) continue
                for (addr in ni.inetAddresses) {
                    if (addr is Inet4Address && !addr.isLoopbackAddress) {
                        val ip = addr.hostAddress ?: continue
                        if (ni.name.startsWith("wlan") || ip.startsWith("192.168.") || ip.startsWith("10.")) {
                            return@runCatching ip
                        }
                        if (fallback == null) fallback = ip
                    }
                }
            }
            fallback
        }.getOrNull()
    }

    suspend fun discoverTvDevices(timeoutMs: Int = 900): List<CastTvDevice> = withContext(Dispatchers.IO) {
        val found = LinkedHashMap<String, CastTvDevice>()
        runCatching {
            DatagramSocket().use { sock ->
                sock.broadcast = true
                sock.soTimeout = 250
                val ping = "SOV_CAST_PING".toByteArray(Charsets.UTF_8)
                val targets = mutableListOf(InetAddress.getByName("255.255.255.255"))
                getLocalWifiIpv4()?.let { myIp ->
                    val prefix = myIp.substringBeforeLast('.', "")
                    if (prefix.isNotEmpty()) {
                        runCatching { targets.add(InetAddress.getByName("$prefix.255")) }
                    }
                }
                for (t in targets) {
                    runCatching {
                        sock.send(DatagramPacket(ping, ping.size, t, UDP_DISCOVERY_PORT))
                    }
                }
                val deadline = System.currentTimeMillis() + timeoutMs
                val buf = ByteArray(256)
                while (System.currentTimeMillis() < deadline) {
                    try {
                        val pkt = DatagramPacket(buf, buf.size)
                        sock.receive(pkt)
                        val msg = String(pkt.data, 0, pkt.length, Charsets.UTF_8).trim()
                        if (msg.startsWith("SOV_CAST_PONG|")) {
                            val parts = msg.split("|")
                            val name = parts.getOrNull(1)?.ifBlank { "Xiaomi TV" } ?: "Xiaomi TV"
                            val port = parts.getOrNull(2)?.toIntOrNull() ?: 9470
                            val ip = pkt.address?.hostAddress ?: continue
                            found[ip] = CastTvDevice(name = name, ip = ip, port = port)
                        }
                    } catch (_: SocketTimeoutException) {
                        // keep listening until deadline
                    }
                }
            }
        }
        found.values.toList()
    }

    suspend fun probeTvAtIp(ip: String, port: Int = 9470): CastTvDevice? = withContext(Dispatchers.IO) {
        val clean = ip.trim()
        if (clean.isEmpty()) return@withContext null
        val resp = sendJsonCommand(CastTvDevice("Xiaomi TV", clean, port), buildJsonObject { put("cmd", "STATUS") })
        if (resp != null) CastTvDevice("Xiaomi TV ($clean)", clean, port) else null
    }

    suspend fun playOnTv(
        device: CastTvDevice,
        track: TrackItem,
        stream: ResolvedStream,
        positionMs: Long,
        accentColorInt: Int,
        lyricsPayload: LyricsPayload,
        lyricsOffsetMs: Long,
    ): Boolean = withContext(Dispatchers.IO) {
        ensureServersRunning()
        val myIp = getLocalWifiIpv4() ?: return@withContext false
        activeStreams[track.id] = stream
        // Pre-probe exact content length once so TV MediaPlayer gets instant Content-Length & seeking!
        val totalLen = probeContentLength(track.id, stream)
        if (totalLen > 0L) totalLengthCache[track.id] = totalLen

        val proxyUrl = "http://$myIp:$PROXY_PORT/audio?id=${track.id}&t=${System.currentTimeMillis()}"
        val payload = buildJsonObject {
            put("cmd", "PLAY")
            put("streamUrl", proxyUrl)
            put("trackId", track.id)
            put("title", track.title)
            put("artist", track.artist)
            put("album", track.album)
            put("artUrl", track.thumbnailUrl)
            put("positionMs", positionMs.coerceAtLeast(0L))
            put("durationMs", (track.durationSec * 1000L).coerceAtLeast(0L))
            put("accentColor", accentColorInt)
            put("lyricsOffsetMs", lyricsOffsetMs)
            put("phoneIp", myIp)
            put("ctrlPort", CTRL_CALLBACK_PORT)
            put("lyrics", buildJsonArray {
                if (lyricsPayload.isSynced) {
                    lyricsPayload.lines.forEach { l ->
                        add(buildJsonObject {
                            put("t", l.timeMs)
                            put("s", l.text)
                        })
                    }
                }
            })
        }
        sendJsonCommand(device, payload) != null
    }

    suspend fun syncLyricsToTv(device: CastTvDevice, lyricsPayload: LyricsPayload, lyricsOffsetMs: Long) = withContext(Dispatchers.IO) {
        if (!lyricsPayload.isSynced || lyricsPayload.lines.isEmpty()) return@withContext
        val payload = buildJsonObject {
            put("cmd", "LYRICS")
            put("lyricsOffsetMs", lyricsOffsetMs)
            put("lyrics", buildJsonArray {
                lyricsPayload.lines.forEach { l ->
                    add(buildJsonObject {
                        put("t", l.timeMs)
                        put("s", l.text)
                    })
                }
            })
        }
        sendJsonCommand(device, payload)
    }

    suspend fun pauseOnTv(device: CastTvDevice) = sendSimple(device, "PAUSE")
    suspend fun resumeOnTv(device: CastTvDevice) = sendSimple(device, "RESUME")
    suspend fun stopOnTv(device: CastTvDevice) = sendSimple(device, "STOP")

    suspend fun seekOnTv(device: CastTvDevice, positionMs: Long) = withContext(Dispatchers.IO) {
        sendJsonCommand(device, buildJsonObject {
            put("cmd", "SEEK")
            put("positionMs", positionMs.coerceAtLeast(0L))
        })
    }

    suspend fun syncOffsetToTv(device: CastTvDevice, offsetMs: Long) = withContext(Dispatchers.IO) {
        sendJsonCommand(device, buildJsonObject {
            put("cmd", "OFFSET")
            put("lyricsOffsetMs", offsetMs)
        })
    }

    suspend fun adjustTvVolume(device: CastTvDevice, delta: Int) = withContext(Dispatchers.IO) {
        sendJsonCommand(device, buildJsonObject {
            put("cmd", "VOLUME")
            put("delta", delta)
        })
    }

    suspend fun pollTvStatus(device: CastTvDevice): CastTvStatus? = withContext(Dispatchers.IO) {
        val obj = sendJsonCommand(device, buildJsonObject { put("cmd", "STATUS") }) ?: return@withContext null
        CastTvStatus(
            active = obj["active"]?.jsonPrimitive?.booleanOrNull ?: false,
            playing = obj["playing"]?.jsonPrimitive?.booleanOrNull ?: false,
            preparing = obj["preparing"]?.jsonPrimitive?.booleanOrNull ?: false,
            finished = obj["finished"]?.jsonPrimitive?.booleanOrNull ?: false,
            trackId = obj["trackId"]?.jsonPrimitive?.contentOrNull.orEmpty(),
            positionMs = obj["positionMs"]?.jsonPrimitive?.longOrNull ?: 0L,
            durationMs = obj["durationMs"]?.jsonPrimitive?.longOrNull ?: 0L,
        )
    }

    private suspend fun sendSimple(device: CastTvDevice, cmd: String) = withContext(Dispatchers.IO) {
        sendJsonCommand(device, buildJsonObject { put("cmd", cmd) })
    }

    private fun sendJsonCommand(device: CastTvDevice, body: JsonObject): JsonObject? {
        return runCatching {
            Socket().use { s ->
                s.connect(InetSocketAddress(device.ip, device.port), 2200)
                s.soTimeout = 3000
                val out = s.getOutputStream()
                out.write((body.toString() + "\n").toByteArray(Charsets.UTF_8))
                out.flush()
                val br = BufferedReader(InputStreamReader(s.getInputStream(), Charsets.UTF_8))
                val line = br.readLine() ?: return@use null
                json.parseToJsonElement(line).jsonObject
            }
        }.getOrNull()
    }

    // =========================================================================
    // REVERSE CALLBACK SERVER (TCP 9473: Receives TV Remote NEXT/PREV/ENDED)
    // =========================================================================
    private fun runReverseControlServer() {
        runCatching {
            val ss = ServerSocket().apply {
                reuseAddress = true
                bind(InetSocketAddress(CTRL_CALLBACK_PORT))
            }
            ctrlServer = ss
            while (serversStarted.get()) {
                val client = ss.accept()
                Thread {
                    runCatching {
                        client.use { s ->
                            s.soTimeout = 2000
                            val line = BufferedReader(InputStreamReader(s.getInputStream(), Charsets.UTF_8)).readLine() ?: return@use
                            val cmd = json.parseToJsonElement(line).jsonObject["cmd"]?.jsonPrimitive?.contentOrNull
                            if (!cmd.isNullOrBlank()) onRemoteEvent(cmd)
                        }
                    }
                }.start()
            }
        }
    }

    // =========================================================================
    // BOUNDED-CHUNK HTTP AUDIO RELAY (TCP 9471: Streams Zero-Freeze Audio to TV!)
    // =========================================================================
    private fun probeContentLength(videoId: String, stream: ResolvedStream): Long {
        totalLengthCache[videoId]?.let { if (it > 0L) return it }
        val reqBuilder = Request.Builder()
            .url(stream.url)
            .header("Range", "bytes=0-1023")
        stream.headers.forEach { (k, v) ->
            if (!k.equals("Range", ignoreCase = true)) reqBuilder.header(k, v)
        }
        return runCatching {
            httpClient.newCall(reqBuilder.build()).execute().use { resp ->
                val cr = resp.header("Content-Range")
                val slash = cr?.lastIndexOf('/') ?: -1
                if (slash > 0) cr!!.substring(slash + 1).trim().toLongOrNull() ?: 0L
                else resp.body.contentLength().coerceAtLeast(0L)
            }
        }.getOrDefault(0L)
    }

    private fun runHttpAudioRelay() {
        runCatching {
            val ss = ServerSocket().apply {
                reuseAddress = true
                bind(InetSocketAddress(PROXY_PORT))
            }
            proxyServer = ss
            while (serversStarted.get()) {
                val client = ss.accept()
                Thread { handleProxyClient(client) }.apply { isDaemon = true }.start()
            }
        }
    }

    private fun handleProxyClient(client: Socket) {
        runCatching {
            client.use { sock ->
                sock.soTimeout = 15_000
                val reader = BufferedReader(InputStreamReader(sock.getInputStream(), Charsets.US_ASCII))
                val reqLine = reader.readLine() ?: return@use
                val path = reqLine.split(" ").getOrNull(1) ?: return@use
                var rangeHeader: String? = null
                while (true) {
                    val h = reader.readLine() ?: break
                    if (h.isEmpty()) break
                    if (h.startsWith("Range:", ignoreCase = true)) {
                        rangeHeader = h.substringAfter(':').trim()
                    }
                }

                val vid = path.substringAfter("id=", "").substringBefore('&')
                val stream = activeStreams[vid]
                val os = sock.getOutputStream()
                if (stream == null) {
                    os.write("HTTP/1.1 404 Not Found\r\nContent-Length: 0\r\n\r\n".toByteArray())
                    os.flush()
                    return@use
                }

                val totalLen = probeContentLength(vid, stream)
                val contentType = when {
                    stream.codecLabel.contains("AAC", ignoreCase = true) -> "audio/mp4"
                    stream.codecLabel.contains("OPUS", ignoreCase = true) -> "audio/webm"
                    else -> "audio/webm"
                }

                var startByte = 0L
                var endByte = if (totalLen > 0L) totalLen - 1L else -1L
                val isRange = !rangeHeader.isNullOrBlank() && rangeHeader.startsWith("bytes=")
                if (isRange) {
                    val spec = rangeHeader!!.substringAfter("bytes=").substringBefore(',')
                    val parts = spec.split("-")
                    startByte = parts.getOrNull(0)?.toLongOrNull() ?: 0L
                    val reqEnd = parts.getOrNull(1)?.toLongOrNull()
                    if (reqEnd != null && reqEnd >= startByte) {
                        endByte = if (totalLen > 0L) min(reqEnd, totalLen - 1L) else reqEnd
                    }
                }

                val respLen = if (endByte >= startByte && totalLen > 0L) (endByte - startByte + 1L) else -1L
                val headers = buildString {
                    if (isRange && totalLen > 0L) {
                        append("HTTP/1.1 206 Partial Content\r\n")
                        append("Content-Range: bytes $startByte-$endByte/$totalLen\r\n")
                    } else {
                        append("HTTP/1.1 200 OK\r\n")
                    }
                    append("Content-Type: $contentType\r\n")
                    append("Accept-Ranges: bytes\r\n")
                    if (respLen > 0L) append("Content-Length: $respLen\r\n")
                    append("Connection: close\r\n\r\n")
                }
                os.write(headers.toByteArray(Charsets.US_ASCII))
                os.flush()

                // Stream seamlessly in strictly bounded 512 KB HTTP/2 range chunks!
                var pos = startByte
                val targetEnd = endByte
                val buf = ByteArray(16384)
                while (true) {
                    if (targetEnd >= 0L && pos > targetEnd) break
                    var chunkEnd = pos + CHUNK_SIZE - 1L
                    if (targetEnd >= 0L) chunkEnd = min(chunkEnd, targetEnd)

                    val reqBuilder = Request.Builder()
                        .url(stream.url)
                        .header("Range", "bytes=$pos-$chunkEnd")
                    stream.headers.forEach { (k, v) ->
                        if (!k.equals("Range", ignoreCase = true)) reqBuilder.header(k, v)
                    }

                    val streamedBytes = httpClient.newCall(reqBuilder.build()).execute().use { resp ->
                        if (resp.code != 200 && resp.code != 206) return@use -1L
                        var written = 0L
                        val ins = resp.body.byteStream()
                        while (true) {
                            val n = ins.read(buf)
                            if (n <= 0) break
                            os.write(buf, 0, n)
                            written += n
                        }
                        os.flush()
                        written
                    }
                    if (streamedBytes <= 0L) break
                    pos += streamedBytes
                }
            }
        }.onFailure {
            Log.d(TAG, "Proxy client closed: ${it.message}")
        }
    }
}
