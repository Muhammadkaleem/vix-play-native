package com.devbytes.vixplayer.app.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * What was last picked while watching a show, applied to its other episodes. Keyed by the
 * normalised show name (`EpisodeInfo.showKey`), so it follows the show across season folders.
 */
@Entity(tableName = "show_preference")
data class ShowPreference(
    @PrimaryKey val showKey: String,
    /** Language tag of the audio track last chosen; null = no choice made. */
    val audioLanguage: String?,
    /** [SUBTITLES_ON], [SUBTITLES_OFF], or null = no choice made. */
    val subtitleMode: String?,
    /** Language tag of the subtitle track last chosen, when [subtitleMode] is on. */
    val subtitleLanguage: String?,
    val updatedAt: Long,
) {
    companion object {
        const val SUBTITLES_ON = "on"
        const val SUBTITLES_OFF = "off"
    }
}
