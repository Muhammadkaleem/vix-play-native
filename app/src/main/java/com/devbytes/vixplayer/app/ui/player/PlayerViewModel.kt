package com.devbytes.vixplayer.app.ui.player

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devbytes.vixplayer.app.data.repository.MediaRepository
import com.devbytes.vixplayer.app.data.repository.PlaybackRepository
import com.devbytes.vixplayer.app.data.repository.SettingsRepository
import com.devbytes.vixplayer.app.data.repository.VideoFile
import com.devbytes.vixplayer.app.player.PlayerController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val playbackRepository: PlaybackRepository,
    private val mediaRepository: MediaRepository,
    private val settingsRepository: SettingsRepository,
    private val playerController: PlayerController,
) : ViewModel() {

    private var currentMediaStoreId: Long? = null

    /** The app-scoped player. Owned by [PlayerController], never released by the screen. */
    val player = playerController.player

    /** Mutable subtitle-offset carrier read by the parser during extraction. */
    val subtitleOffset = playerController.subtitleOffset

    /** Opens a video, resetting per-file state. See [PlayerController.prepareFor]. */
    fun prepareFor(uri: String, subtitleOffsetMs: Long) =
        playerController.prepareFor(uri, subtitleOffsetMs)

    /** Whether audio should keep playing once the app is backgrounded. */
    val backgroundPlayback: StateFlow<Boolean> = settingsRepository.backgroundPlayback
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    /** Persisted global playback speed — applied to every video, updated from the player. */
    val playbackSpeed: StateFlow<Float> = settingsRepository.playbackSpeed
        .stateIn(viewModelScope, SharingStarted.Eagerly, 1.0f)

    fun setPlaybackSpeed(speed: Float) {
        viewModelScope.launch { settingsRepository.setPlaybackSpeed(speed) }
    }

    /**
     * Call once per media item. Extracts the MediaStore ID from the URI and returns
     * the last saved position (fast MMKV path), or 0 if none / not a MediaStore URI.
     */
    fun initMedia(uri: String): Long {
        currentMediaStoreId = try {
            val parsed = Uri.parse(uri)
            // MediaStore URIs end with the numeric ID as the last path segment
            parsed.lastPathSegment?.toLong()
        } catch (_: Exception) {
            null
        }
        return currentMediaStoreId?.let { playbackRepository.getPositionFast(it) } ?: 0L
    }

    fun savePositionFast(positionMs: Long) {
        currentMediaStoreId?.let { playbackRepository.savePositionFast(it, positionMs) }
    }

    /**
     * Per-file subtitle offset in ms. Returns 0 for non-MediaStore URIs (external
     * ACTION_VIEW intents have no id) — the offset still works, it just isn't remembered.
     */
    fun getSubtitleOffsetMs(): Long =
        currentMediaStoreId?.let { playbackRepository.getSubtitleOffsetFast(it) } ?: 0L

    fun saveSubtitleOffsetMs(offsetMs: Long) {
        currentMediaStoreId?.let { playbackRepository.saveSubtitleOffsetFast(it, offsetMs) }
    }

    fun persistPosition(positionMs: Long) {
        val id = currentMediaStoreId ?: return
        viewModelScope.launch {
            playbackRepository.persistPosition(id, positionMs)
        }
    }

    /** File metadata (name/path/size/duration) for the File Info sheet, or null if not a MediaStore item. */
    suspend fun loadFileInfo(): VideoFile? =
        currentMediaStoreId?.let { mediaRepository.queryVideoById(it) }

    /** The next video in the current item's folder, or null if there's no next (last file / non-MediaStore). */
    suspend fun nextInFolder(): VideoFile? =
        currentMediaStoreId?.let { mediaRepository.nextInFolder(it) }
}
