package com.devbytes.vixplayer.app.data.repository

import com.devbytes.vixplayer.app.data.db.dao.PlaylistDao
import com.devbytes.vixplayer.app.data.db.entity.Playlist
import com.devbytes.vixplayer.app.data.db.entity.PlaylistItem
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * A playlist row resolved against the current library.
 *
 * [available] is false when the source file is gone. Such items are shown flagged and
 * skipped on playback rather than silently dropped — the PRD is explicit that missing
 * items must not disappear without notice.
 */
data class PlaylistRow(
    val item: PlaylistItem,
    val available: Boolean,
    val albumArtUri: String?,
)

@Singleton
class PlaylistRepository @Inject constructor(
    private val dao: PlaylistDao,
    private val audioRepository: AudioRepository,
) {
    fun observePlaylists(): Flow<List<Playlist>> = dao.observePlaylists()

    fun observeItems(playlistId: Long): Flow<List<PlaylistItem>> = dao.observeItems(playlistId)

    suspend fun itemCount(playlistId: Long): Int = dao.itemCount(playlistId)

    suspend fun get(id: Long): Playlist? = dao.getPlaylist(id)

    suspend fun create(name: String): Long =
        dao.insertPlaylist(Playlist(name = name, createdAt = System.currentTimeMillis()))

    suspend fun rename(playlist: Playlist, name: String) =
        dao.updatePlaylist(playlist.copy(name = name))

    suspend fun delete(playlist: Playlist) = dao.deletePlaylist(playlist)

    /** Appends to the end of the playlist, capturing the title/artist for later display. */
    suspend fun addTrack(playlistId: Long, track: AudioTrack) {
        val next = dao.maxPosition(playlistId) + 1
        dao.insertItem(
            PlaylistItem(
                playlistId = playlistId,
                mediaStoreId = track.mediaStoreId,
                uri = track.uri.toString(),
                title = track.title,
                artist = track.artist,
                position = next,
            )
        )
    }

    suspend fun removeItem(item: PlaylistItem) = dao.deleteItem(item)

    /** Persists a whole new ordering; called once on drop, not per swap. */
    suspend fun reorder(items: List<PlaylistItem>) = dao.reorder(items)

    /**
     * Resolves stored items against the live library. Anything whose `mediaStoreId` no
     * longer exists is returned with `available = false` so the UI can flag it.
     */
    suspend fun rowsFor(playlistId: Long): List<PlaylistRow> {
        val items = dao.itemsFor(playlistId)
        if (items.isEmpty()) return emptyList()
        val library = audioRepository.queryAllTracks().associateBy { it.mediaStoreId }
        return items.map { item ->
            val track = library[item.mediaStoreId]
            PlaylistRow(
                item = item,
                available = track != null,
                albumArtUri = track?.albumArtUri?.toString(),
            )
        }
    }
}
