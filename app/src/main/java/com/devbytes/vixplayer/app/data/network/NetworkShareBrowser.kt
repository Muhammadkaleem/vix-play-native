package com.devbytes.vixplayer.app.data.network

import android.net.Uri
import com.devbytes.vixplayer.app.data.db.entity.NetworkServer
import com.devbytes.vixplayer.app.data.db.entity.ShareProtocol
import com.hierynomus.msdtyp.AccessMask
import com.hierynomus.msfscc.FileAttributes
import com.hierynomus.mssmb2.SMBApiException
import com.hierynomus.smbj.SMBClient
import com.hierynomus.smbj.auth.AuthenticationContext
import com.hierynomus.smbj.share.DiskShare
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.commons.net.ftp.FTP
import org.apache.commons.net.ftp.FTPClient
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.inject.Inject
import javax.inject.Singleton

/** One row in a share listing. [playUri] is set only for playable video files. */
data class ShareEntry(val name: String, val isDirectory: Boolean, val playUri: String?)

/** Recoverable browse failures mapped to user-facing messages. */
sealed class ShareException(message: String) : Exception(message) {
    class AuthFailed : ShareException("Wrong username or password.")
    class Unreachable : ShareException("Can't reach the server. Check the address and your network.")
    class SmbV1Unsupported :
        ShareException("This server only supports SMBv1, which isn't supported.")
    class Unknown(message: String) : ShareException(message)
}

private val VIDEO_EXTENSIONS = setOf(
    "mp4", "mkv", "avi", "mov", "webm", "flv", "wmv", "m4v", "3gp", "ts", "mpg", "mpeg", "m2ts",
)

@Singleton
class NetworkShareBrowser @Inject constructor() {

    /** Lists directories + playable videos at [path] within [server]. Blocking → IO. */
    suspend fun list(server: NetworkServer, password: String?, path: String): List<ShareEntry> =
        withContext(Dispatchers.IO) {
            when (server.shareProtocol) {
                ShareProtocol.SMB -> listSmb(server, password, path)
                ShareProtocol.FTP -> listFtp(server, password, path)
            }
        }

    private fun listSmb(server: NetworkServer, password: String?, path: String): List<ShareEntry> {
        val port = if (server.port > 0) server.port else SMBClient.DEFAULT_PORT
        val client = SMBClient()
        try {
            client.connect(server.host, port).use { conn ->
                val auth =
                    if (server.isGuest) AuthenticationContext.guest()
                    else AuthenticationContext(
                        server.username, password.orEmpty().toCharArray(), server.domain,
                    )
                val session = conn.authenticate(auth)
                (session.connectShare(server.share) as DiskShare).use { share ->
                    val rel = path.trim('/').replace('/', '\\')
                    return share.list(rel).mapNotNull { info ->
                        val name = info.fileName
                        if (name == "." || name == "..") return@mapNotNull null
                        val isDir =
                            (info.fileAttributes and FileAttributes.FILE_ATTRIBUTE_DIRECTORY.value) != 0L
                        when {
                            isDir -> ShareEntry(name, true, null)
                            isVideo(name) ->
                                ShareEntry(name, false, buildSmbUri(server, password, joinPath(path, name)))
                            else -> null
                        }
                    }.sortedWith(entryOrder)
                }
            }
        } catch (e: SMBApiException) {
            throw ShareException.AuthFailed()
        } catch (e: UnknownHostException) {
            throw ShareException.Unreachable()
        } catch (e: SocketTimeoutException) {
            throw ShareException.Unreachable()
        } catch (e: IOException) {
            // SMBJ throws a generic transport error when a server offers only SMBv1.
            val msg = e.message.orEmpty().lowercase()
            if ("dialect" in msg || "smb1" in msg || "negotiat" in msg) throw ShareException.SmbV1Unsupported()
            throw ShareException.Unreachable()
        } finally {
            runCatching { client.close() }
        }
    }

    private fun listFtp(server: NetworkServer, password: String?, path: String): List<ShareEntry> {
        val port = if (server.port > 0) server.port else 21
        val ftp = FTPClient()
        try {
            ftp.connect(server.host, port)
            val user = if (server.isGuest) "anonymous" else server.username
            val pass = if (server.isGuest) "anonymous@" else password.orEmpty()
            if (!ftp.login(user, pass)) throw ShareException.AuthFailed()
            ftp.enterLocalPassiveMode()
            ftp.setFileType(FTP.BINARY_FILE_TYPE)
            val dir = "/" + path.trim('/')
            ftp.changeWorkingDirectory(dir)
            return ftp.listFiles().mapNotNull { f ->
                val name = f.name
                if (name == "." || name == "..") return@mapNotNull null
                when {
                    f.isDirectory -> ShareEntry(name, true, null)
                    f.isFile && isVideo(name) ->
                        ShareEntry(name, false, buildFtpUri(server, password, joinPath(path, name)))
                    else -> null
                }
            }.sortedWith(entryOrder)
        } catch (e: UnknownHostException) {
            throw ShareException.Unreachable()
        } catch (e: SocketTimeoutException) {
            throw ShareException.Unreachable()
        } catch (e: ShareException) {
            throw e
        } catch (e: IOException) {
            throw ShareException.Unreachable()
        } finally {
            runCatching { if (ftp.isConnected) { ftp.logout(); ftp.disconnect() } }
        }
    }

    private fun isVideo(name: String) =
        name.substringAfterLast('.', "").lowercase() in VIDEO_EXTENSIONS

    companion object {
        /** Directories first, then case-insensitive by name. */
        private val entryOrder = compareByDescending<ShareEntry> { it.isDirectory }
            .thenBy(String.CASE_INSENSITIVE_ORDER) { it.name }

        private fun joinPath(base: String, name: String): String =
            if (base.trim('/').isEmpty()) name else "${base.trim('/')}/$name"

        /** smb://[enc(user):enc(pass)@]host[:port]/share/path — creds embedded in memory only. */
        fun buildSmbUri(server: NetworkServer, password: String?, relPath: String): String =
            buildUri("smb", server, password, "${server.share}/$relPath")

        /** ftp://[enc(user):enc(pass)@]host[:port]/path */
        fun buildFtpUri(server: NetworkServer, password: String?, relPath: String): String =
            buildUri("ftp", server, password, relPath)

        private fun buildUri(
            scheme: String,
            server: NetworkServer,
            password: String?,
            pathPart: String,
        ): String {
            val userInfo = when {
                server.isGuest && scheme == "ftp" -> "anonymous:anonymous%40@"
                server.isGuest -> "" // SMB guest: no userinfo
                else -> "${Uri.encode(server.username)}:${Uri.encode(password.orEmpty())}@"
            }
            val portPart = if (server.port > 0) ":${server.port}" else ""
            val cleanPath = pathPart.split("/").filter { it.isNotEmpty() }
                .joinToString("/") { Uri.encode(it) }
            return "$scheme://$userInfo${server.host}$portPart/$cleanPath"
        }
    }
}
