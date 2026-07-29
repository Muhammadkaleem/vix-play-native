package com.devbytes.vixplayer.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devbytes.vixplayer.app.data.repository.SettingsRepository
import com.devbytes.vixplayer.app.ui.theme.AccentChoice
import com.devbytes.vixplayer.app.ui.theme.ThemeMode
import com.devbytes.vixplayer.app.ui.theme.ThemeState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ThemeSkinsViewModel @Inject constructor(
    private val settings: SettingsRepository,
) : ViewModel() {

    val themeState: StateFlow<ThemeState> = settings.themeState
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ThemeState())

    fun setMode(mode: ThemeMode) {
        viewModelScope.launch { settings.setThemeMode(mode) }
    }

    fun setAccent(accent: AccentChoice) {
        viewModelScope.launch { settings.setAccent(accent) }
    }
}
