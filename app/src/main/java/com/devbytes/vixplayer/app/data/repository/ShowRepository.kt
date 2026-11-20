package com.devbytes.vixplayer.app.data.repository

import com.devbytes.vixplayer.app.data.db.dao.ShowPreferenceDao
import com.devbytes.vixplayer.app.data.db.entity.ShowPreference
import com.devbytes.vixplayer.app.data.series.EpisodeFile
import com.devbytes.vixplayer.app.data.series.Show
import com.devbytes.vixplayer.app.data.series.ShowEpisode
import com.devbytes.vixplayer.app.data.series.ShowIndex
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/** A show plus the [VideoFile] behind each episode, for playing and thumbnails. */
data class ShowWithVideos(val show: Show, private val videos: Map<Long, VideoFile>) {
    fun videoOf(episode: ShowEpisode): VideoFile? = videos[episode.file.id]
}

/**
 * Shows built from the library on demand (see [ShowIndex]) — nothing about a show is stored
 * except what the viewer picked while watching it ([ShowPreference]).
 */
@Singleton
class ShowRepository @Inject constructor(
    private val media: MediaRepository,
    private val playback: PlaybackRepository,
    private val preferences: ShowPreferenceDao,
) {

    /**
     * Every show in the library, most recently watched first. Pass [library] when the caller
     * has already loaded all videos, to skip a second MediaStore query.
     */
    suspend fun shows(library: List<VideoFile>? = null): List<ShowWithVideos> {
        val videos = library ?: media.queryAllVideos()
        // Positions come from the player's fast store, which every pause, seek and finish
        // writes; Room's copy is only stamped at start and exit, so it lags an autoplayed episode.
        val lastPlayed = playback.getRecentPositions(10_000).associate { it.mediaStoreId to it.lastPlayedAt }
        return withContext(Dispatchers.Default) {
            val byId = videos.associateBy { it.mediaStoreId }
            ShowIndex.build(
                videos.map { video ->
                    EpisodeFile(
                        id = video.mediaStoreId,
                        name = video.name,
                        path = video.path,
                        durationMs = video.durationMs,
                        positionMs = playback.getPositionFast(video.mediaStoreId),
                        lastPlayedAt = lastPlayed[video.mediaStoreId] ?: 0L,
                    )
                },
            ).map { show ->
                ShowWithVideos(show, show.episodes.mapNotNull { e -> byId[e.file.id]?.let { e.file.id to it } }.toMap())
            }
        }
    }

    suspend fun show(showKey: String): ShowWithVideos? = shows().firstOrNull { it.show.key == showKey }

    suspend fun preference(showKey: String): ShowPreference? = preferences.get(showKey)

    suspend fun rememberAudio(showKey: String, language: String) =
        preferences.upsert(current(showKey).copy(audioLanguage = language, updatedAt = System.currentTimeMillis()))

    suspend fun rememberSubtitles(showKey: String, on: Boolean, language: String?) =
        preferences.upsert(
            current(showKey).copy(
                subtitleMode = if (on) ShowPreference.SUBTITLES_ON else ShowPreference.SUBTITLES_OFF,
                subtitleLanguage = if (on) language else null,
                updatedAt = System.currentTimeMillis(),
            ),
        )

    private suspend fun current(showKey: String) =
        preferences.get(showKey) ?: ShowPreference(showKey, null, null, null, 0L)
}
