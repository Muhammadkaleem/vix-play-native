package com.devbytes.vixplayer.app.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devbytes.vixplayer.app.data.repository.FolderEntry
import com.devbytes.vixplayer.app.data.repository.MediaRepository
import com.devbytes.vixplayer.app.data.repository.VideoFile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface FolderBrowserUiState {
    data object Loading : FolderBrowserUiState
    data class FolderList(val folders: List<FolderEntry>) : FolderBrowserUiState
    data class VideoList(val folderName: String, val videos: List<VideoFile>) : FolderBrowserUiState
    data class Error(val message: String) : FolderBrowserUiState
}

@HiltViewModel
class FolderBrowserViewModel @Inject constructor(
    private val mediaRepository: MediaRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<FolderBrowserUiState>(FolderBrowserUiState.Loading)
    val state: StateFlow<FolderBrowserUiState> = _state.asStateFlow()

    init { loadFolders() }

    fun enterFolder(bucketId: Long, folderName: String) {
        viewModelScope.launch {
            _state.value = FolderBrowserUiState.Loading
            runCatching { mediaRepository.queryVideosInFolder(bucketId) }
                .onSuccess { videos ->
                    _state.value = FolderBrowserUiState.VideoList(folderName, videos)
                }
                .onFailure { e ->
                    _state.value = FolderBrowserUiState.Error(e.message ?: "Unknown error")
                }
        }
    }

    fun navigateUp() { loadFolders() }

    private fun loadFolders() {
        viewModelScope.launch {
            _state.value = FolderBrowserUiState.Loading
            runCatching { mediaRepository.queryFolders() }
                .onSuccess { folders ->
                    _state.value = FolderBrowserUiState.FolderList(folders)
                }
                .onFailure { e ->
                    _state.value = FolderBrowserUiState.Error(e.message ?: "Unknown error")
                }
        }
    }
}
