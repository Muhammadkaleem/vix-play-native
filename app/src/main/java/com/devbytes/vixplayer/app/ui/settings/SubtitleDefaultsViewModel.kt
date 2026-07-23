package com.devbytes.vixplayer.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devbytes.vixplayer.app.data.repository.SettingsRepository
import com.devbytes.vixplayer.app.data.repository.SubtitleStyle
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SubtitleDefaultsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    val style: StateFlow<SubtitleStyle> = settingsRepository.subtitleStyle
        .stateIn(viewModelScope, SharingStarted.Eagerly, SubtitleStyle.DEFAULT)

    fun setStyle(style: SubtitleStyle) {
        viewModelScope.launch { settingsRepository.setSubtitleStyle(style) }
    }
}
