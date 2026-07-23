package com.devbytes.vixplayer.app.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devbytes.vixplayer.app.data.repository.MediaRepository
import com.devbytes.vixplayer.app.data.repository.VideoFile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.Normalizer
import javax.inject.Inject

sealed interface SearchUiState {
    /** Blank query — guidance prompt. */
    data object Idle : SearchUiState
    /** Query typed but the video snapshot hasn't loaded yet. */
    data object Loading : SearchUiState
    data class Results(val query: String, val videos: List<VideoFile>) : SearchUiState
    data class NoResults(val query: String) : SearchUiState
}

/**
 * In-memory search over the MediaStore video snapshot. Loads [queryAllVideos]
 * once, then filters a debounced query against it — well under the PRD's 200ms
 * target for thousands of items without a Room FTS index (deferred; see the
 * PRD's "very large library" caveat). Matching is diacritic- and case-
 * insensitive (NFD-normalize → strip combining marks → lowercase).
 */
@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    mediaRepository: MediaRepository,
) : ViewModel() {

    private val snapshot = MutableStateFlow<List<VideoFile>?>(null)
    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    val uiState: StateFlow<SearchUiState> =
        combine(
            snapshot,
            // Blank clears instantly (0ms); typing settles at 250ms per the PRD.
            _query.debounce { if (it.isBlank()) 0L else 250L },
        ) { videos, q ->
            when {
                q.isBlank() -> SearchUiState.Idle
                videos == null -> SearchUiState.Loading
                else -> {
                    val needle = q.normalizeForSearch()
                    val matches = videos.filter { it.name.normalizeForSearch().contains(needle) }
                    if (matches.isEmpty()) SearchUiState.NoResults(q) else SearchUiState.Results(q, matches)
                }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SearchUiState.Idle)

    init {
        viewModelScope.launch { snapshot.value = mediaRepository.queryAllVideos() }
    }

    fun setQuery(value: String) { _query.value = value }
}

private val combiningMarks = Regex("\\p{Mn}+")

private fun String.normalizeForSearch(): String =
    Normalizer.normalize(this, Normalizer.Form.NFD)
        .replace(combiningMarks, "")
        .lowercase()
