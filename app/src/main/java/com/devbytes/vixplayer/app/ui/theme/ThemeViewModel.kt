package com.devbytes.vixplayer.app.ui.theme

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devbytes.vixplayer.app.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * Supplies the active [ThemeState] to the Activity so the whole app is wrapped in the
 * chosen theme. Seeds with DARK (the persisted default) so the first frame matches the
 * app's original look before DataStore resolves.
 */
@HiltViewModel
class ThemeViewModel @Inject constructor(
    settings: SettingsRepository,
) : ViewModel() {
    val themeState: StateFlow<ThemeState> = settings.themeState
        .stateIn(viewModelScope, SharingStarted.Eagerly, ThemeState())
}
