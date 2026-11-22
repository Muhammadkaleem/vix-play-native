package com.devbytes.vixplayer.app.data.subtitles

import android.content.Context
import android.net.Uri
import com.devbytes.vixplayer.app.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.nio.ByteBuffer
import javax.inject.Inject
import javax.inject.Singleton

/** A subtitle saved on the device, ready to attach to the player. */
data class DownloadedSubtitle(
    val file: File,
    val label: String,
    val format: SubtitleFormat,
    /** Downloads OpenSubtitles still allows today, when it said. */
    val remaining: Int?,
)

sealed interface SubtitleSearchResult {
    data class Found(val subtitles: List<OnlineSubtitle>) : SubtitleSearchResult
    data class Failed(val message: String) : SubtitleSearchResult
}

sealed interface SubtitleDownloadResult {
    data class Saved(val subtitle: DownloadedSubtitle) : SubtitleDownloadResult
    data class Failed(val message: String) : SubtitleDownloadResult
}

/**
 * OpenSubtitles.com over plain HTTPS. The API key is the app's, from `opensubtitles.apiKey` in
 * the untracked local.properties; without one the feature isn't offered. Searching is free;
 * downloads count against a daily quota per IP (a handful a day without a user login), so
 * what's downloaded is kept and re-attached rather than fetched again.
 */
@Singleton
class OpenSubtitlesClient @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    val isConfigured: Boolean get() = BuildConfig.OPENSUBTITLES_API_KEY.isNotBlank()

    private val userAgent = "VixPlay v${BuildConfig.VERSION_NAME}"
    private val folder: File get() = File(context.filesDir, "subtitles").apply { mkdirs() }

    /**
     * Searches by [query] and, when the file is local, by its hash too: a hash match is the
     * exact release. Two requests, because combining them narrows to hash matches only.
     */
    suspend fun search(query: SubtitleQuery, languages: List<String>, video: Uri?): SubtitleSearchResult =
        withContext(Dispatchers.IO) {
            if (!isConfigured) return@withContext SubtitleSearchResult.Failed("Online search isn't set up in this build.")
            try {
                val byHash = video?.let { hashOf(it) }?.let { hash ->
                    OpenSubtitles.parseSearch(get(OpenSubtitles.searchUrl(SubtitleQuery(""), languages, hash)))
                        .map { it.copy(hashMatch = true) }
                }.orEmpty()
                val byText = if (query.text.isBlank()) emptyList() else
                    OpenSubtitles.parseSearch(get(OpenSubtitles.searchUrl(query, languages, null)))
                val seen = byHash.map { it.fileId }.toSet()
                SubtitleSearchResult.Found(byHash + byText.filter { it.fileId !in seen })
            } catch (e: ApiException) {
                SubtitleSearchResult.Failed(e.message ?: "OpenSubtitles couldn't search right now.")
            } catch (e: IOException) {
                SubtitleSearchResult.Failed("Can't reach OpenSubtitles. Check your connection.")
            }
        }

    /** Asks for a download link, fetches the file and saves it under the app's files. */
    suspend fun download(subtitle: OnlineSubtitle): SubtitleDownloadResult = withContext(Dispatchers.IO) {
        try {
            val response = JSONObject(post("${OpenSubtitles.BASE_URL}/download", JSONObject().put("file_id", subtitle.fileId).toString()))
            val link = response.optString("link")
            if (link.isBlank()) {
                return@withContext SubtitleDownloadResult.Failed(response.optString("message").ifBlank { "OpenSubtitles didn't send the file." })
            }
            val text = fetch(link)
            val name = response.optString("file_name").ifBlank { subtitle.release }
            val format = OpenSubtitles.formatOf(name, text)
            val file = File(folder, "${subtitle.fileId}.${format.fileExtension}")
            file.writeText(text)
            SubtitleDownloadResult.Saved(
                DownloadedSubtitle(
                    file = file,
                    label = "${subtitle.language.uppercase()} · ${subtitle.release}".take(80),
                    format = format,
                    remaining = if (response.has("remaining")) response.optInt("remaining") else null,
                ),
            )
        } catch (e: ApiException) {
            SubtitleDownloadResult.Failed(e.message ?: "OpenSubtitles couldn't send the file.")
        } catch (e: IOException) {
            SubtitleDownloadResult.Failed("Can't reach OpenSubtitles. Check your connection.")
        }
    }

    /** OpenSubtitles' hash of a local video, or null when it can't be read (a stream, say). */
    private fun hashOf(uri: Uri): String? = runCatching {
        if (uri.scheme != "content" && uri.scheme != "file") return null
        context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
            FileInputStream(pfd.fileDescriptor).channel.use { channel ->
                val size = channel.size()
                if (size < OpenSubtitles.HASH_CHUNK) return null
                fun read(at: Long): ByteArray {
                    val buffer = ByteBuffer.allocate(OpenSubtitles.HASH_CHUNK)
                    channel.position(at)
                    while (buffer.hasRemaining() && channel.read(buffer) > 0) Unit
                    return buffer.array()
                }
                OpenSubtitles.hash(size, read(0), read(size - OpenSubtitles.HASH_CHUNK))
            }
        }
    }.getOrNull()

    private fun get(url: String): String = request(url) { }

    private fun post(url: String, body: String): String = request(url) {
        requestMethod = "POST"
        doOutput = true
        setRequestProperty("Content-Type", "application/json")
        outputStream.use { it.write(body.toByteArray()) }
    }

    private fun request(url: String, configure: HttpURLConnection.() -> Unit): String {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15_000
            readTimeout = 20_000
            setRequestProperty("Api-Key", BuildConfig.OPENSUBTITLES_API_KEY)
            setRequestProperty("User-Agent", userAgent)
            setRequestProperty("Accept", "application/json")
        }
        try {
            connection.configure()
            val code = connection.responseCode
            val text = (if (code in 200..299) connection.inputStream else connection.errorStream)
                ?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (code !in 200..299) throw ApiException(messageFor(code, text))
            return text
        } finally {
            connection.disconnect()
        }
    }

    /** A plain download (the file link), no API headers. */
    private fun fetch(url: String): String {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15_000
            readTimeout = 20_000
            setRequestProperty("User-Agent", userAgent)
        }
        try {
            if (connection.responseCode !in 200..299) throw ApiException("OpenSubtitles couldn't send the file.")
            return connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }

    private fun messageFor(code: Int, body: String): String {
        val apiMessage = runCatching { JSONObject(body).optString("message") }.getOrNull().orEmpty()
        return when (code) {
            401, 403 -> "OpenSubtitles turned the request down (the app's key)."
            406, 429 -> apiMessage.ifBlank { "You've reached today's download limit on OpenSubtitles. Try again tomorrow." }
            in 500..599 -> "OpenSubtitles is having trouble. Try again in a moment."
            else -> apiMessage.ifBlank { "OpenSubtitles couldn't do that (error $code)." }
        }
    }

    private class ApiException(message: String) : IOException(message)
}
