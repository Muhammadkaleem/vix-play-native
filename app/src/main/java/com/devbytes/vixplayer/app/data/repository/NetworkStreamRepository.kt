package com.devbytes.vixplayer.app.data.repository

import android.net.Uri
import com.devbytes.vixplayer.app.data.db.dao.NetworkStreamDao
import com.devbytes.vixplayer.app.data.db.entity.NetworkStream
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NetworkStreamRepository @Inject constructor(
    private val dao: NetworkStreamDao,
) {
    fun observeRecents(): Flow<List<NetworkStream>> = dao.observeRecents()

    /** Records a just-played URL at the top of the recents, trimming the tail. */
    suspend fun record(url: String) {
        dao.upsert(
            NetworkStream(
                url = url,
                label = deriveLabel(url),
                lastPlayedAt = System.currentTimeMillis(),
            )
        )
        dao.trim(MAX_RECENTS)
    }

    suspend fun remove(stream: NetworkStream) = dao.delete(stream)

    private companion object {
        const val MAX_RECENTS = 50
    }
}

/**
 * A readable one-liner for the recents list: "host · filename" when both resolve, else
 * whatever part is available, falling back to the raw URL. Credentials embedded in the
 * URL (`user:pass@host`) are never shown — [Uri.getHost] omits them.
 */
internal fun deriveLabel(url: String): String {
    val uri = runCatching { Uri.parse(url) }.getOrNull()
    val host = uri?.host
    val lastSegment = uri?.lastPathSegment?.takeIf { it.isNotBlank() }
    return when {
        host != null && lastSegment != null -> "$host · $lastSegment"
        host != null -> host
        lastSegment != null -> lastSegment
        else -> url
    }
}
