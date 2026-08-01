package com.devbytes.vixplayer.app.player.datasource

import android.content.Context
import android.net.Uri
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.TransferListener

/**
 * Routes by URI scheme: `smb://` → [SmbDataSource], `ftp://` → [FtpDataSource], everything
 * else (file/content/http/https/…) → Media3's [DefaultDataSource]. Wired into
 * `PlayerController`'s media-source factory so network shares play on the same path as
 * local and HTTP media.
 */
class NetworkDataSource(private val context: Context) : DataSource {

    private val transferListeners = ArrayList<TransferListener>()
    private var delegate: DataSource? = null

    override fun addTransferListener(transferListener: TransferListener) {
        transferListeners.add(transferListener)
    }

    override fun open(dataSpec: DataSpec): Long {
        val d = when (dataSpec.uri.scheme?.lowercase()) {
            "smb" -> SmbDataSource()
            "ftp" -> FtpDataSource()
            else -> DefaultDataSource.Factory(context).createDataSource()
        }
        transferListeners.forEach { d.addTransferListener(it) }
        delegate = d
        return d.open(dataSpec)
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int =
        delegate!!.read(buffer, offset, length)

    override fun getUri(): Uri? = delegate?.uri

    override fun getResponseHeaders(): Map<String, List<String>> =
        delegate?.responseHeaders ?: emptyMap()

    override fun close() {
        try {
            delegate?.close()
        } finally {
            delegate = null
        }
    }

    class Factory(private val context: Context) : DataSource.Factory {
        override fun createDataSource(): DataSource = NetworkDataSource(context)
    }
}
