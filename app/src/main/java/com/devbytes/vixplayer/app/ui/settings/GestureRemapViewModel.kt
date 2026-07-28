package com.devbytes.vixplayer.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devbytes.vixplayer.app.data.repository.SettingsRepository
import com.devbytes.vixplayer.app.player.gesture.GestureBindings
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GestureRemapViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    val bindings: StateFlow<GestureBindings> = settingsRepository.gestureBindings
        .stateIn(viewModelScope, SharingStarted.Eagerly, GestureBindings.DEFAULT)

    /** Persist a full binding set. Each edit rewrites the whole record — the set is tiny. */
    private fun update(bindings: GestureBindings) {
        viewModelScope.launch { settingsRepository.setGestureBindings(bindings) }
    }

    fun setLeftDrag(action: com.devbytes.vixplayer.app.player.gesture.DragAction) =
        update(bindings.value.copy(leftDrag = action))

    fun setRightDrag(action: com.devbytes.vixplayer.app.player.gesture.DragAction) =
        update(bindings.value.copy(rightDrag = action))

    fun setLeftTap(action: com.devbytes.vixplayer.app.player.gesture.TapAction) =
        update(bindings.value.copy(leftTap = action))

    fun setRightTap(action: com.devbytes.vixplayer.app.player.gesture.TapAction) =
        update(bindings.value.copy(rightTap = action))

    fun resetToDefaults() = update(GestureBindings.DEFAULT)
}
