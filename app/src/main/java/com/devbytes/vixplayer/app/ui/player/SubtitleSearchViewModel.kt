package com.devbytes.vixplayer.app.ui.player

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devbytes.vixplayer.app.data.subtitles.DownloadedSubtitle
import com.devbytes.vixplayer.app.data.subtitles.OnlineSubtitle
import com.devbytes.vixplayer.app.data.subtitles.OpenSubtitlesClient
import com.devbytes.vixplayer.app.data.subtitles.SubtitleDownloadResult
import com.devbytes.vixplayer.app.data.subtitles.SubtitleQuery
import com.devbytes.vixplayer.app.data.subtitles.SubtitleSearchResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

/** A language the search can be limited to; [code] null means every language. */
data class SubtitleLanguage(val code: String?, val label: String)

data class SubtitleSearchState(
    val text: String = "",
    /** The episode the file is, when it is one; the search narrows to it while [episodeOnly]. */
    val season: Int? = null,
    val episode: Int? = null,
    val episodeOnly: Boolean = true,
    val year: Int? = null,
    val languages: List<SubtitleLanguage> = emptyList(),
    val language: SubtitleLanguage? = null,
    val loading: Boolean = false,
    val results: List<OnlineSubtitle>? = null,
    val error: String? = null,
    /** The file being downloaded, while one is. */
    val downloadingId: Long? = null,
)

@HiltViewModel
class SubtitleSearchViewModel @Inject constructor(
    private val client: OpenSubtitlesClient,
) : ViewModel() {

    private val _state = MutableStateFlow(SubtitleSearchState())
    val state: StateFlow<SubtitleSearchState> = _state.asStateFlow()

    private var video: Uri? = null
    private var startedFor: Uri? = null
    private var searchJob: Job? = null

    /** Fills in what the file looks like and searches straight away, once per video. */
    fun start(query: SubtitleQuery, video: Uri?) {
        if (startedFor == video && _state.value.results != null) return
        startedFor = video
        this.video = video
        val languages = languageChoices()
        _state.value = SubtitleSearchState(
            text = query.text,
            season = query.season,
            episode = query.episode,
            year = query.year,
            languages = languages,
            language = languages.first(),
        )
        search()
    }

    fun setText(text: String) = _state.update { it.copy(text = text) }

    fun setEpisodeOnly(on: Boolean) {
        _state.update { it.copy(episodeOnly = on) }
        search()
    }

    fun setLanguage(language: SubtitleLanguage) {
        _state.update { it.copy(language = language) }
        search()
    }

    fun search() {
        val s = _state.value
        searchJob?.cancel()
        _state.update { it.copy(loading = true, error = null) }
        searchJob = viewModelScope.launch {
            val query = SubtitleQuery(
                text = s.text,
                season = s.season.takeIf { s.episodeOnly },
                episode = s.episode.takeIf { s.episodeOnly },
                year = s.year.takeIf { s.season == null },
            )
            val languages = listOfNotNull(s.language?.code)
            when (val result = client.search(query, languages, video)) {
                is SubtitleSearchResult.Found -> _state.update { it.copy(loading = false, results = result.subtitles) }
                is SubtitleSearchResult.Failed -> _state.update { it.copy(loading = false, error = result.message) }
            }
        }
    }

    /** Downloads [subtitle]; [onSaved] gets the file once it's on the device. */
    fun download(subtitle: OnlineSubtitle, onSaved: (DownloadedSubtitle) -> Unit, onFailed: (String) -> Unit) {
        if (_state.value.downloadingId != null) return
        _state.update { it.copy(downloadingId = subtitle.fileId) }
        viewModelScope.launch {
            val result = client.download(subtitle)
            _state.update { it.copy(downloadingId = null) }
            when (result) {
                is SubtitleDownloadResult.Saved -> onSaved(result.subtitle)
                is SubtitleDownloadResult.Failed -> onFailed(result.message)
            }
        }
    }

    /** The phone's language first, then English, then the most-subtitled languages, then all. */
    private fun languageChoices(): List<SubtitleLanguage> {
        val device = Locale.getDefault()
        val deviceCode = when (device.language) {
            "pt" -> if (device.country == "PT") "pt-pt" else "pt-br"
            "zh" -> if (device.country in setOf("TW", "HK")) "zh-tw" else "zh-cn"
            else -> device.language
        }
        val codes = (listOf(deviceCode, "en") + COMMON_LANGUAGES).distinct()
        return codes.map { SubtitleLanguage(it, labelFor(it)) } + SubtitleLanguage(null, "All languages")
    }

    private fun labelFor(code: String): String {
        val locale = Locale.forLanguageTag(code)
        val name = locale.getDisplayLanguage(Locale.getDefault()).ifBlank { code }
        val region = locale.getDisplayCountry(Locale.getDefault())
        return name.replaceFirstChar { it.titlecase(Locale.getDefault()) } +
            if (code.contains('-') && region.isNotBlank()) " ($region)" else ""
    }

    private companion object {
        val COMMON_LANGUAGES = listOf(
            "es", "fr", "de", "pt-br", "it", "ar", "hi", "ur", "id", "tr", "ru", "zh-cn", "ja", "ko", "nl",
            "pl", "fa", "bn", "vi", "th",
        )
    }
}
