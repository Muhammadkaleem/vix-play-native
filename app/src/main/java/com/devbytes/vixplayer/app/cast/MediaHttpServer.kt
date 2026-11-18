package com.devbytes.vixplayer.app.cast

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.media3.common.C
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSourceInputStream
import androidx.media3.datasource.DataSpec
import com.devbytes.vixplayer.app.player.datasource.NetworkDataSource
import fi.iki.elonen.NanoHTTPD
import java.io.IOException
import java.net.Inet4Address
import java.net.NetworkInterface
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Minimal HTTP server that lets a Chromecast fetch media the phone can read but the receiver
 * can't reach: `content://` (MediaStore), `file://` (private vault), `smb://` / `ftp://`.
 *
 * Every served URI goes through [NetworkDataSource], the same path the local player uses, so
 * there is one way to open media in the app — and SMB credentials never leave the phone, the
 * receiver only ever sees `http://<phone-ip>:<port>/media/<token>`.
 *
 * Only URIs explicitly [register]ed are served, under unguessable tokens, and the server runs
 * only while a cast session exists ([start]/[stop]) — it is not a general file share.
 *
 * HTTP Range is honoured because that is how the receiver seeks; the length comes from the
 * data source's `open()` so `Content-Range` is exact.
 */
class MediaHttpServer(private val context: Context) {

    private data class Entry(val uri: Uri, val mime: String)

    private val entries = ConcurrentHashMap<String, Entry>()
    private var server: Server? = null

    val isRunning: Boolean get() = server?.isAlive == true

    /** Port the server is listening on, or null when stopped. */
    val port: Int? get() = server?.listeningPort?.takeIf { isRunning }

    @Synchronized
    fun start(): Boolean {
        if (isRunning) return true
        return runCatching {
            // Port 0 = let the OS pick a free ephemeral port.
            Server(0).also {
                it.start(NanoHTTPD.SOCKET_READ_TIMEOUT, /* daemon = */ true)
                server = it
            }
            true
        }.onFailure { Log.w(TAG, "Could not start media server", it) }.getOrDefault(false)
    }

    @Synchronized
    fun stop() {
        server?.stop()
        server = null
        entries.clear()
    }

    /**
     * Makes [uri] fetchable and returns its LAN URL, or null when the server isn't running or
     * the phone has no LAN address (no Wi-Fi) — in which case casting a local file can't work.
     */
    fun register(uri: Uri, mime: String): String? {
        val port = port ?: return null
        val host = lanAddress() ?: return null
        val token = UUID.randomUUID().toString().replace("-", "")
        entries[token] = Entry(uri, mime)
        return "http://$host:$port/media/$token"
    }

    fun unregisterAll() = entries.clear()

    private inner class Server(port: Int) : NanoHTTPD(port) {
        override fun serve(session: IHTTPSession): Response {
            val token = session.uri.removePrefix("/media/").takeIf { session.uri.startsWith("/media/") }
            val entry = token?.let { entries[it] }
                ?: return newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "Not found")

            val factory = NetworkDataSource.Factory(context)
            // First open: learn the total length (needed for Content-Range and for the
            // receiver's duration/seek maths).
            val total = runCatching {
                factory.createDataSource().let { ds ->
                    val len = ds.open(DataSpec(entry.uri))
                    ds.close()
                    len
                }
            }.getOrElse { return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, MIME_PLAINTEXT, "Unreadable") }

            val range = session.headers["range"]?.let { parseRange(it, total) }
            val start = range?.first ?: 0L
            val end = range?.second ?: (if (total != C.LENGTH_UNSET.toLong()) total - 1 else -1L)
            val length = if (end >= 0) end - start + 1 else C.LENGTH_UNSET.toLong()

            val source: DataSource = factory.createDataSource()
            val stream = DataSourceInputStream(
                source,
                DataSpec.Builder().setUri(entry.uri).setPosition(start).setLength(length).build(),
            )

            val status = if (range != null) Response.Status.PARTIAL_CONTENT else Response.Status.OK
            val response = if (length != C.LENGTH_UNSET.toLong()) {
                newFixedLengthResponse(status, entry.mime, stream, length)
            } else {
                newChunkedResponse(status, entry.mime, stream)
            }
            response.addHeader("Accept-Ranges", "bytes")
            if (range != null && total != C.LENGTH_UNSET.toLong()) {
                response.addHeader("Content-Range", "bytes $start-$end/$total")
            }
            return response
        }
    }

    companion object {
        private const val TAG = "MediaHttpServer"

        /** `bytes=START-END` / `bytes=START-` → inclusive (start, end), clamped to [total]. */
        fun parseRange(header: String, total: Long): Pair<Long, Long>? {
            val spec = header.trim().removePrefix("bytes=")
            if (spec == header.trim() || spec.contains(',')) return null // unsupported form
            val start = spec.substringBefore('-').toLongOrNull() ?: return null
            val endRaw = spec.substringAfter('-').toLongOrNull()
            val last = if (total == C.LENGTH_UNSET.toLong()) (endRaw ?: return null) else total - 1
            val end = (endRaw ?: last).coerceAtMost(last)
            return if (start in 0..end) start to end else null
        }

        /** The phone's IPv4 LAN address (Wi-Fi preferred), or null when offline. */
        fun lanAddress(): String? = runCatching {
            NetworkInterface.getNetworkInterfaces().toList()
                .filter { it.isUp && !it.isLoopback }
                .sortedBy { if (it.name.startsWith("wlan")) 0 else 1 }
                .flatMap { it.inetAddresses.toList() }
                .firstOrNull { it is Inet4Address && it.isSiteLocalAddress }
                ?.hostAddress
        }.getOrNull()
    }
}
