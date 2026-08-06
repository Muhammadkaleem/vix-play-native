package com.devbytes.vixplayer.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devbytes.vixplayer.app.data.repository.SettingsRepository
import com.devbytes.vixplayer.app.notifications.PulseNotifier
import com.devbytes.vixplayer.app.notifications.PulseScheduler
import com.devbytes.vixplayer.app.notifications.PulseSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val settings: SettingsRepository,
    private val scheduler: PulseScheduler,
    private val notifier: PulseNotifier,
) : ViewModel() {

    val pulseSettings: StateFlow<PulseSettings> = settings.pulseSettings
        .stateIn(viewModelScope, SharingStarted.Eagerly, PulseSettings())

    /** System-level notification permission/channel state — drives the "off in settings" hint. */
    fun systemNotificationsEnabled(): Boolean = notifier.notificationsEnabled()

    /** Turning the master on (re)schedules the daily job; off cancels it. */
    fun setMaster(enabled: Boolean) {
        viewModelScope.launch {
            settings.setPulseMaster(enabled)
            if (enabled) scheduler.schedule() else scheduler.cancel()
        }
    }

    fun setResume(enabled: Boolean) = viewModelScope.launch { settings.setPulseResume(enabled) }
    fun setNewVideos(enabled: Boolean) = viewModelScope.launch { settings.setPulseNewVideos(enabled) }
    fun setDigest(enabled: Boolean) = viewModelScope.launch { settings.setPulseDigest(enabled) }
}
