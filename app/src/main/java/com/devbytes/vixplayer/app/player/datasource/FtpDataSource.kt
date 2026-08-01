package com.devbytes.vixplayer.app.player.datasource

import android.net.Uri
import androidx.media3.common.C
import androidx.media3.datasource.BaseDataSource
import androidx.media3.datasource.DataSpec
import org.apache.commons.net.ftp.FTP
import org.apache.commons.net.ftp.FTPClient
import java.io.InputStream

/**
 * Media3 [androidx.media3.datasource.DataSource] over FTP via Commons-Net. Credentials
 * come from the `ftp://user:pass@host/path` URI. Seeks use the FTP REST offset — each
 * open re-positions the stream, so seeking is supported but coarse.
 */
class FtpDataSource : BaseDataSource(/* isNetwork = */ true) {

    private var ftp: FTPClient? = null
    private var stream: InputStream? = null
    private var uri: Uri? = null
    private var bytesRemaining: Long = C.LENGTH_UNSET.toLong()
    private var opened = false

    override fun open(dataSpec: DataSpec): Long {
        transferInitializing(dataSpec)
        val u = dataSpec.uri
        uri = u

        val host = u.host ?: throw java.io.IOException("Bad FTP URI: no host")
        val port = if (u.port > 0) u.port else 21
        val remote = "/" + u.pathSegments.filter { it.isNotEmpty() }
            .joinToString("/") { Uri.decode(it) }
        val (user, pass) = parseUserInfo(u.userInfo)

        val client = FTPClient()
        ftp = client
        client.connect(host, port)
        if (!client.login(user ?: "anonymous", pass ?: "anonymous@")) {
            throw java.io.IOException("FTP login failed")
        }
        client.enterLocalPassiveMode()
        client.setFileType(FTP.BINARY_FILE_TYPE)

        // Total length: try SIZE so seeking has a known content length.
        val total = runCatching {
            if (client.sendCommand("SIZE", remote) == 213) {
                client.replyStrings.firstOrNull()?.substringAfter("213")?.trim()?.toLongOrNull()
            } else null
        }.getOrNull()

        if (dataSpec.position > 0) client.restartOffset = dataSpec.position
        stream = client.retrieveFileStream(remote)
            ?: throw java.io.IOException("FTP could not open ${dataSpec.uri}")

        bytesRemaining = when {
            dataSpec.length != C.LENGTH_UNSET.toLong() -> dataSpec.length
            total != null -> total - dataSpec.position
            else -> C.LENGTH_UNSET.toLong()
        }

        opened = true
        transferStarted(dataSpec)
        return bytesRemaining
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        if (length == 0) return 0
        val limit =
            if (bytesRemaining == C.LENGTH_UNSET.toLong()) length
            else minOf(length.toLong(), bytesRemaining).toInt()
        if (limit == 0) return C.RESULT_END_OF_INPUT
        val read = stream?.read(buffer, offset, limit) ?: -1
        if (read == -1) return C.RESULT_END_OF_INPUT
        if (bytesRemaining != C.LENGTH_UNSET.toLong()) bytesRemaining -= read
        bytesTransferred(read)
        return read
    }

    override fun getUri(): Uri? = uri

    override fun close() {
        uri = null
        runCatching { stream?.close() }
        runCatching { ftp?.completePendingCommand() }
        runCatching { if (ftp?.isConnected == true) { ftp?.logout(); ftp?.disconnect() } }
        stream = null; ftp = null
        if (opened) {
            opened = false
            transferEnded()
        }
    }

    private fun parseUserInfo(raw: String?): Pair<String?, String?> {
        if (raw.isNullOrEmpty()) return null to null
        val idx = raw.indexOf(':')
        val user = if (idx >= 0) raw.substring(0, idx) else raw
        val pass = if (idx >= 0) raw.substring(idx + 1) else ""
        return Uri.decode(user) to Uri.decode(pass)
    }
}
