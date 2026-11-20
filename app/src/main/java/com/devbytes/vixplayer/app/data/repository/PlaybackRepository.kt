package com.devbytes.vixplayer.app.data.repository

import com.devbytes.vixplayer.app.data.db.dao.PlaybackPositionDao
import com.devbytes.vixplayer.app.data.db.entity.PlaybackPosition
import com.devbytes.vixplayer.app.player.IntroWindow
import com.tencent.mmkv.MMKV
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlaybackRepository @Inject constructor(
    private val dao: PlaybackPositionDao,
) {
    private val mmkv = MMKV.defaultMMKV()

    fun savePositionFast(mediaStoreId: Long, positionMs: Long) {
        mmkv.encode("pos_$mediaStoreId", positionMs)
    }

    fun getPositionFast(mediaStoreId: Long): Long =
        mmkv.decodeLong("pos_$mediaStoreId", 0L)

    /**
     * Per-file subtitle timing offset in milliseconds (negative = subtitles earlier).
     * MMKV only — nothing ever lists offsets, so this needs no Room mirror. Deliberately
     * left untouched by [clearAll]: clearing watch history shouldn't discard subtitle tuning.
     */
    fun saveSubtitleOffsetFast(mediaStoreId: Long, offsetMs: Long) {
        mmkv.encode("suboff_$mediaStoreId", offsetMs)
    }

    fun getSubtitleOffsetFast(mediaStoreId: Long): Long =
        mmkv.decodeLong("suboff_$mediaStoreId", 0L)

    /**
     * One episode's own intro window, overriding its show's (a cold open that runs longer,
     * say). Per file like the subtitle offset, so it lives in MMKV beside it.
     */
    fun saveIntroOverride(mediaStoreId: Long, window: IntroWindow?) {
        if (window == null) {
            mmkv.removeValuesForKeys(arrayOf("intro_start_$mediaStoreId", "intro_end_$mediaStoreId"))
        } else {
            mmkv.encode("intro_start_$mediaStoreId", window.startMs)
            mmkv.encode("intro_end_$mediaStoreId", window.endMs)
        }
    }

    fun getIntroOverride(mediaStoreId: Long): IntroWindow? {
        if (!mmkv.containsKey("intro_end_$mediaStoreId")) return null
        return IntroWindow(mmkv.decodeLong("intro_start_$mediaStoreId", 0L), mmkv.decodeLong("intro_end_$mediaStoreId", 0L))
    }

    suspend fun persistPosition(mediaStoreId: Long, positionMs: Long) {
        dao.upsert(PlaybackPosition(mediaStoreId, positionMs, System.currentTimeMillis()))
    }

    /**
     * Stamps "last played = now" at playback start/resume **without** disturbing the saved
     * position — so the floating resume button's "most recent" is correct the instant a video
     * begins (not only when it's exited). Preserves the existing position (Room, else MMKV,
     * else 0 for a brand-new play).
     */
    suspend fun markPlayed(mediaStoreId: Long) {
        val position = dao.getPosition(mediaStoreId)?.positionMs
            ?: mmkv.decodeLong("pos_$mediaStoreId", 0L)
        dao.upsert(PlaybackPosition(mediaStoreId, position, System.currentTimeMillis()))
    }

    suspend fun getPosition(mediaStoreId: Long): Long =
        dao.getPosition(mediaStoreId)?.positionMs
            ?: mmkv.decodeLong("pos_$mediaStoreId", 0L)

    suspend fun getRecentPositions(limit: Int = 20) = dao.getRecentPositions(limit)

    suspend fun clearPosition(mediaStoreId: Long) {
        mmkv.remove("pos_$mediaStoreId")
        dao.delete(mediaStoreId)
    }

    /**
     * Clears all resume history from both stores. Removes each `pos_` key from
     * the MMKV fast cache before dropping the Room table so positions can't
     * resurrect via [getPosition]'s MMKV fallback.
     */
    suspend fun clearAll() {
        dao.getAll().forEach { mmkv.remove("pos_${it.mediaStoreId}") }
        dao.clearAll()
    }
}
