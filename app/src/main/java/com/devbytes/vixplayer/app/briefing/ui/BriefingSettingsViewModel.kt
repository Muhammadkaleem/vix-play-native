package com.devbytes.vixplayer.app.briefing.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devbytes.vixplayer.app.briefing.BriefingConfig
import com.devbytes.vixplayer.app.briefing.BriefingNotifier
import com.devbytes.vixplayer.app.briefing.BriefingRepository
import com.devbytes.vixplayer.app.briefing.BriefingScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BriefingSettingsViewModel @Inject constructor(
    private val repository: BriefingRepository,
    private val scheduler: BriefingScheduler,
    private val notifier: BriefingNotifier,
) : ViewModel() {

    val config: StateFlow<BriefingConfig> = repository.config
        .stateIn(viewModelScope, SharingStarted.Eagerly, BriefingConfig())

    fun canDeliverExactly(): Boolean = scheduler.canDeliverExactly()
    fun systemNotificationsEnabled(): Boolean = notifier.notificationsEnabled()

    /** Pause/resume without discarding the approved sample — resume never needs re-setup. */
    fun setEnabled(enabled: Boolean) = viewModelScope.launch {
        repository.update { it.copy(enabled = enabled) }
        scheduler.schedule(repository.current())
    }

    fun setLockScreenDetail(enabled: Boolean) = viewModelScope.launch {
        repository.update { it.copy(lockScreenDetail = enabled) }
    }

    /** Full off: cancel the alarm and clear approval so nothing can fire until re-setup. */
    fun turnOff() = viewModelScope.launch {
        scheduler.cancel()
        repository.update { BriefingConfig() }
    }
}
