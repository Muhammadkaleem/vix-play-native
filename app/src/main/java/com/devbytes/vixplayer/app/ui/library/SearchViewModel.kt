package com.devbytes.vixplayer.app.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devbytes.vixplayer.app.data.repository.AudioRepository
import com.devbytes.vixplayer.app.data.repository.AudioTrack
import com.devbytes.vixplayer.app.data.repository.FolderEntry
import com.devbytes.vixplayer.app.data.repository.MediaRepository
import com.devbytes.vixplayer.app.data.repository.SettingsRepository
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

/** The four filter tabs over the unified search results. */
enum class SearchCategory(val label: String) { ALL("All"), VIDEOS("Videos"), AUDIO("Audio"), FOLDERS("Folders") }

sealed interface SearchUiState {
    /** Blank query — show recent searches / guidance. */
    data object Idle : SearchUiState
    /** Query typed but the media snapshots haven't loaded yet. */
    data object Loading : SearchUiState
    /** Ranked matches across all three types (the UI filters the view by the active category). */
    data class Results(
        val query: String,
        val videos: List<VideoFile>,
        val audio: List<AudioTrack>,
        val folders: List<FolderEntry>,
    ) : SearchUiState {
        val isEmpty: Boolean get() = videos.isEmpty() && audio.isEmpty() && folders.isEmpty()
    }
}

/**
 * Unified in-memory search over videos, audio and folders. Each media index is loaded once from
 * its existing repository (no re-scan per keystroke), then a debounced query is filtered against
 * all three. Matching is diacritic- and case-insensitive; results are ranked exact → prefix →
 * substring. Well under 200ms for thousands of items without an FTS index.
 */
@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    mediaRepository: MediaRepository,
    audioRepository: AudioRepository,
    private val settings: SettingsRepository,
) : ViewModel() {

    private val videosSnapshot = MutableStateFlow<List<VideoFile>?>(null)
    private val audioSnapshot = MutableStateFlow<List<AudioTrack>?>(null)
    private val foldersSnapshot = MutableStateFlow<List<FolderEntry>?>(null)

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _category = MutableStateFlow(SearchCategory.ALL)
    val category: StateFlow<SearchCategory> = _category.asStateFlow()

    val recentSearches: StateFlow<List<String>> =
        settings.recentSearches.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val loaded = combine(videosSnapshot, audioSnapshot, foldersSnapshot) { v, a, f ->
        if (v != null && a != null && f != null) Triple(v, a, f) else null
    }

    val uiState: StateFlow<SearchUiState> =
        combine(
            loaded,
            // Blank clears instantly; typing settles at 250ms.
            _query.debounce { if (it.isBlank()) 0L else 250L },
        ) { snapshots, q ->
            when {
                q.isBlank() -> SearchUiState.Idle
                snapshots == null -> SearchUiState.Loading
                else -> {
                    val needle = q.normalizeForSearch()
                    val (videos, audio, folders) = snapshots
                    SearchUiState.Results(
                        query = q,
                        videos = rankedMatches(videos, needle) { listOf(it.name) },
                        audio = rankedMatches(audio, needle) { listOf(it.title, it.artist, it.album) },
                        folders = rankedMatches(folders, needle) { listOf(it.name) },
                    )
                }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SearchUiState.Idle)

    init {
        viewModelScope.launch { videosSnapshot.value = runCatching { mediaRepository.queryAllVideos() }.getOrDefault(emptyList()) }
        viewModelScope.launch { audioSnapshot.value = runCatching { audioRepository.queryAllTracks() }.getOrDefault(emptyList()) }
        viewModelScope.launch { foldersSnapshot.value = runCatching { mediaRepository.queryFolders() }.getOrDefault(emptyList()) }
    }

    fun setQuery(value: String) { _query.value = value }
    fun setCategory(value: SearchCategory) { _category.value = value }

    /** Persist the query to recents — call when the user commits (IME search or taps a result). */
    fun commitSearch(value: String = _query.value) {
        viewModelScope.launch { settings.addRecentSearch(value) }
    }

    fun clearRecentSearches() {
        viewModelScope.launch { settings.clearRecentSearches() }
    }
}

/** Keeps matches whose any field contains [needle], ranked exact(0) → prefix(1) → substring(2). */
private inline fun <T> rankedMatches(items: List<T>, needle: String, fields: (T) -> List<String>): List<T> =
    items.mapNotNull { item ->
        val normFields = fields(item).map { it.normalizeForSearch() }
        if (normFields.none { it.contains(needle) }) return@mapNotNull null
        val rank = when {
            normFields.any { it == needle } -> 0
            normFields.any { it.startsWith(needle) } -> 1
            else -> 2
        }
        item to rank
    }.sortedBy { it.second }.map { it.first }

private val combiningMarks = Regex("\\p{Mn}+")

private fun String.normalizeForSearch(): String =
    Normalizer.normalize(this, Normalizer.Form.NFD)
        .replace(combiningMarks, "")
        .lowercase()
