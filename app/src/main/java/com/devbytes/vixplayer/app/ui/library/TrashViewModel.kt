package com.devbytes.vixplayer.app.ui.library

import android.content.IntentSender
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devbytes.vixplayer.app.data.repository.DeleteResult
import com.devbytes.vixplayer.app.data.repository.MediaDeleter
import com.devbytes.vixplayer.app.data.repository.MediaRepository
import com.devbytes.vixplayer.app.data.repository.VideoFile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TrashViewModel @Inject constructor(
    private val mediaRepository: MediaRepository,
    private val mediaDeleter: MediaDeleter,
) : ViewModel() {

    sealed interface State {
        data object Loading : State
        data object Empty : State
        data class Loaded(val videos: List<VideoFile>) : State
    }

    private val _state = MutableStateFlow<State>(State.Loading)
    val state: StateFlow<State> = _state.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    fun load() {
        viewModelScope.launch {
            val trashed = runCatching { mediaRepository.queryTrashedVideos() }.getOrDefault(emptyList())
            _state.value = if (trashed.isEmpty()) State.Empty else State.Loaded(trashed)
        }
    }

    /** Restore one item (or all) through the OS batch consent; the screen launches [onNeedsConsent]. */
    fun restore(uris: List<VideoFile>, onNeedsConsent: (IntentSender) -> Unit) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            when (val result = mediaDeleter.restore(uris.map { it.uri })) {
                is DeleteResult.NeedsConsent -> onNeedsConsent(result.intentSender)
                is DeleteResult.Deleted -> load()
                is DeleteResult.Failed -> _message.value = result.message
            }
        }
    }

    fun restoreAll(onNeedsConsent: (IntentSender) -> Unit) {
        val current = (_state.value as? State.Loaded)?.videos ?: return
        restore(current, onNeedsConsent)
    }

    /** Re-read the trash after the consent dialog rather than assuming what survived. */
    fun onRestoreConsentResult(granted: Boolean) {
        val before = (_state.value as? State.Loaded)?.videos?.size ?: 0
        viewModelScope.launch {
            val trashed = runCatching { mediaRepository.queryTrashedVideos() }.getOrDefault(emptyList())
            _state.value = if (trashed.isEmpty()) State.Empty else State.Loaded(trashed)
            if (granted) {
                val restored = (before - trashed.size).coerceAtLeast(0)
                _message.value = when {
                    restored == 0 -> null
                    restored == 1 -> "1 video restored"
                    else -> "$restored videos restored"
                }
            }
        }
    }

    fun consumeMessage() { _message.value = null }
}
