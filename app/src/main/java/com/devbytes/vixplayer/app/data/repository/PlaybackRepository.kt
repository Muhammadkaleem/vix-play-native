package com.devbytes.vixplayer.app.data.repository

import com.devbytes.vixplayer.app.data.db.dao.PlaybackPositionDao
import com.devbytes.vixplayer.app.data.db.entity.PlaybackPosition
import com.devbytes.vixplayer.app.player.CreditsWindow
import com.devbytes.vixplayer.app.player.DetectedCredits
import com.devbytes.vixplayer.app.player.DetectedIntro
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

    /** What intro detection found in this file; null = not scanned yet. */
    fun getDetectedIntro(mediaStoreId: Long): DetectedIntro? =
        when (val value = mmkv.decodeString("$DETECTED_KEY$mediaStoreId")) {
            null -> null
            "none" -> DetectedIntro.None
            "rejected" -> DetectedIntro.Rejected
            else -> value.split(',').mapNotNull { it.toLongOrNull() }.takeIf { it.size == 2 }
                ?.let { DetectedIntro.Found(IntroWindow(it[0], it[1])) }
        }

    /** Saves a detection result, but never over the user's rejection of an earlier one. */
    fun saveDetectedIntro(mediaStoreId: Long, result: DetectedIntro) {
        if (result !is DetectedIntro.Rejected && getDetectedIntro(mediaStoreId) == DetectedIntro.Rejected) return
        mmkv.encode(
            "$DETECTED_KEY$mediaStoreId",
            when (result) {
                is DetectedIntro.Found -> "${result.window.startMs},${result.window.endMs}"
                DetectedIntro.None -> "none"
                DetectedIntro.Rejected -> "rejected"
            },
        )
    }

    /** The episodes a show's season was last scanned with — a new one means scan again. */
    fun scannedSeason(showKey: String, season: Int): String? = mmkv.decodeString("$SCANNED_KEY$showKey|$season")

    fun markSeasonScanned(showKey: String, season: Int, episodes: String) {
        mmkv.encode("$SCANNED_KEY$showKey|$season", episodes)
    }

    /** Where this file's end credits were found; null = not scanned yet. */
    fun getDetectedCredits(mediaStoreId: Long): DetectedCredits? =
        when (val value = mmkv.decodeString("$CREDITS_KEY$mediaStoreId")) {
            null -> null
            "none" -> DetectedCredits.None
            "rejected" -> DetectedCredits.Rejected
            else -> value.split(',').mapNotNull { it.toLongOrNull() }.takeIf { it.size == 2 }
                ?.let { DetectedCredits.Found(CreditsWindow(it[0], it[1])) }
        }

    /** Saves a detection result, but never over the user's rejection of an earlier one. */
    fun saveDetectedCredits(mediaStoreId: Long, result: DetectedCredits) {
        if (result !is DetectedCredits.Rejected && getDetectedCredits(mediaStoreId) == DetectedCredits.Rejected) return
        mmkv.encode(
            "$CREDITS_KEY$mediaStoreId",
            when (result) {
                is DetectedCredits.Found -> "${result.window.startMs},${result.window.endMs}"
                DetectedCredits.None -> "none"
                DetectedCredits.Rejected -> "rejected"
            },
        )
    }

    /** Where one episode's credits start, marked by hand for it alone; null to clear. */
    fun saveCreditsOverride(mediaStoreId: Long, startMs: Long?) {
        if (startMs == null) mmkv.remove("credits_start_$mediaStoreId") else mmkv.encode("credits_start_$mediaStoreId", startMs)
    }

    fun getCreditsOverride(mediaStoreId: Long): Long? =
        if (mmkv.containsKey("credits_start_$mediaStoreId")) mmkv.decodeLong("credits_start_$mediaStoreId", 0L) else null

    /**
     * A show's credits, marked by hand, as time before the end of each episode — per show
     * like the season scan markers, so MMKV rather than a Room column. Null to clear.
     */
    fun saveShowCredits(showKey: String, fromEndMs: Long?) {
        if (fromEndMs == null) mmkv.remove("creditsshow_$showKey") else mmkv.encode("creditsshow_$showKey", fromEndMs)
    }

    fun getShowCredits(showKey: String): Long? =
        if (mmkv.containsKey("creditsshow_$showKey")) mmkv.decodeLong("creditsshow_$showKey", 0L) else null

    /** Like [scannedSeason], for the credits — kept apart so seasons scanned for intros before get a credits-only pass. */
    fun creditsScannedSeason(showKey: String, season: Int): String? = mmkv.decodeString("$CREDITS_SCANNED_KEY$showKey|$season")

    fun markCreditsScanned(showKey: String, season: Int, episodes: String) {
        mmkv.encode("$CREDITS_SCANNED_KEY$showKey|$season", episodes)
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

    private companion object {
        /** Versioned, so a better detector can rescan everything by bumping it. */
        const val DETECTED_KEY = "introauto_v2_"
        const val SCANNED_KEY = "introscan_v2_"
        const val CREDITS_KEY = "creditsauto_v1_"
        const val CREDITS_SCANNED_KEY = "creditsscan_v1_"
    }
}
