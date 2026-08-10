package com.devbytes.vixplayer.app.briefing.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devbytes.vixplayer.app.briefing.Brief
import com.devbytes.vixplayer.app.briefing.BriefingGenerator
import com.devbytes.vixplayer.app.briefing.BriefingRepository
import com.devbytes.vixplayer.app.briefing.CalendarSource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BriefingViewModel @Inject constructor(
    private val generator: BriefingGenerator,
    private val calendar: CalendarSource,
    private val repository: BriefingRepository,
) : ViewModel() {

    sealed interface State {
        data object Loading : State
        data object NeedsPermission : State
        data class Ready(val brief: Brief) : State
    }

    private val _state = MutableStateFlow<State>(State.Loading)
    val state: StateFlow<State> = _state.asStateFlow()

    fun hasCalendarPermission(): Boolean = calendar.hasPermission()

    /** Builds the brief live from the calendar (or reports the missing-permission state). */
    fun load() {
        viewModelScope.launch {
            if (!calendar.hasPermission()) {
                _state.value = State.NeedsPermission
                return@launch
            }
            _state.value = State.Loading
            val zone = repository.current().effectiveZone()
            _state.value = State.Ready(generator.generate(zone))
        }
    }
}
