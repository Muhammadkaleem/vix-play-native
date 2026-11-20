package com.devbytes.vixplayer.app.ui.shows

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devbytes.vixplayer.app.data.repository.ShowRepository
import com.devbytes.vixplayer.app.data.repository.ShowWithVideos
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface ShowUiState {
    data object Loading : ShowUiState
    /** The show's files were deleted, renamed or moved out of reach since the link was made. */
    data object Missing : ShowUiState
    data class Loaded(val show: ShowWithVideos) : ShowUiState
}

@HiltViewModel
class ShowViewModel @Inject constructor(
    private val shows: ShowRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<ShowUiState>(ShowUiState.Loading)
    val state: StateFlow<ShowUiState> = _state.asStateFlow()

    /** (Re)reads the show — on entry and on every return from the player, so progress is current. */
    fun load(showKey: String) {
        viewModelScope.launch {
            _state.value = runCatching { shows.show(showKey) }.getOrNull()
                ?.let { ShowUiState.Loaded(it) }
                ?: ShowUiState.Missing
        }
    }
}
