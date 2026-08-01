package com.devbytes.vixplayer.app.player.datasource

import android.net.Uri
import androidx.media3.common.C
import androidx.media3.datasource.BaseDataSource
import androidx.media3.datasource.DataSpec
import com.hierynomus.msdtyp.AccessMask
import com.hierynomus.mssmb2.SMB2CreateDisposition
import com.hierynomus.mssmb2.SMB2ShareAccess
import com.hierynomus.smbj.SMBClient
import com.hierynomus.smbj.auth.AuthenticationContext
import com.hierynomus.smbj.connection.Connection
import com.hierynomus.smbj.session.Session
import com.hierynomus.smbj.share.DiskShare
import com.hierynomus.smbj.share.File
import java.util.EnumSet

/**
 * Media3 [androidx.media3.datasource.DataSource] over SMB via SMBJ. Credentials are read
 * from the `smb://user:pass@host/share/path` URI (built in memory at play time). Supports
 * random-access reads, so seeking works natively.
 */
class SmbDataSource : BaseDataSource(/* isNetwork = */ true) {

    private var client: SMBClient? = null
    private var connection: Connection? = null
    private var session: Session? = null
    private var share: DiskShare? = null
    private var file: File? = null

    private var uri: Uri? = null
    private var position: Long = 0
    private var bytesRemaining: Long = 0
    private var opened = false

    override fun open(dataSpec: DataSpec): Long {
        transferInitializing(dataSpec)
        val u = dataSpec.uri
        uri = u

        val host = u.host ?: throw java.io.IOException("Bad SMB URI: no host")
        val port = if (u.port > 0) u.port else SMBClient.DEFAULT_PORT
        val segments = u.pathSegments.filter { it.isNotEmpty() }
        if (segments.isEmpty()) throw java.io.IOException("Bad SMB URI: no share")
        val shareName = Uri.decode(segments.first())
        val filePath = segments.drop(1).joinToString("\\") { Uri.decode(it) }

        val (user, pass, domain) = parseUserInfo(u.userInfo)

        val c = SMBClient()
        client = c
        val conn = c.connect(host, port).also { connection = it }
        val auth =
            if (user == null) AuthenticationContext.guest()
            else AuthenticationContext(user, pass.orEmpty().toCharArray(), domain.orEmpty())
        val sess = conn.authenticate(auth).also { session = it }
        val ds = (sess.connectShare(shareName) as DiskShare).also { share = it }

        val f = ds.openFile(
            filePath,
            EnumSet.of(AccessMask.GENERIC_READ),
            null,
            SMB2ShareAccess.ALL,
            SMB2CreateDisposition.FILE_OPEN,
            null,
        ).also { file = it }

        val size = f.fileInformation.standardInformation.endOfFile
        position = dataSpec.position
        bytesRemaining =
            if (dataSpec.length != C.LENGTH_UNSET.toLong()) dataSpec.length
            else (size - position)

        opened = true
        transferStarted(dataSpec)
        return bytesRemaining
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        if (length == 0) return 0
        if (bytesRemaining == 0L) return C.RESULT_END_OF_INPUT
        val toRead = minOf(length.toLong(), bytesRemaining).toInt()
        val read = file?.read(buffer, position, offset, toRead) ?: -1
        if (read == -1) return C.RESULT_END_OF_INPUT
        position += read
        bytesRemaining -= read
        bytesTransferred(read)
        return read
    }

    override fun getUri(): Uri? = uri

    override fun close() {
        uri = null
        runCatching { file?.close() }
        runCatching { share?.close() }
        runCatching { session?.close() }
        runCatching { connection?.close() }
        runCatching { client?.close() }
        file = null; share = null; session = null; connection = null; client = null
        if (opened) {
            opened = false
            transferEnded()
        }
    }

    /** Splits a raw `user:pass` userInfo, URL-decoding each part. null user = guest. */
    private fun parseUserInfo(raw: String?): Triple<String?, String?, String?> {
        if (raw.isNullOrEmpty()) return Triple(null, null, null)
        val idx = raw.indexOf(':')
        val user = if (idx >= 0) raw.substring(0, idx) else raw
        val pass = if (idx >= 0) raw.substring(idx + 1) else ""
        return Triple(Uri.decode(user), Uri.decode(pass), "")
    }
}
