package com.devbytes.vixplayer.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devbytes.vixplayer.app.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlaybackDefaultsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    val backgroundPlayback: StateFlow<Boolean> = settingsRepository.backgroundPlayback
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    fun setBackgroundPlayback(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setBackgroundPlayback(enabled) }
    }
}
