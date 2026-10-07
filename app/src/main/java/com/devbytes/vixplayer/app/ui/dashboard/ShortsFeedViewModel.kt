package com.devbytes.vixplayer.app.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devbytes.vixplayer.app.data.repository.MediaRepository
import com.devbytes.vixplayer.app.data.repository.VideoFile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Backs the TikTok-style Shorts feed — the full list of portrait videos. */
@HiltViewModel
class ShortsFeedViewModel @Inject constructor(
    private val media: MediaRepository,
) : ViewModel() {

    private val _shorts = MutableStateFlow<List<VideoFile>?>(null) // null = loading
    val shorts: StateFlow<List<VideoFile>?> = _shorts.asStateFlow()

    init {
        viewModelScope.launch {
            _shorts.value = runCatching { media.queryShorts() }.getOrDefault(emptyList())
        }
    }
}
