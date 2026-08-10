package com.devbytes.vixplayer.app.briefing.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devbytes.vixplayer.app.briefing.Brief
import com.devbytes.vixplayer.app.briefing.BriefingConfig
import com.devbytes.vixplayer.app.briefing.BriefingGenerator
import com.devbytes.vixplayer.app.briefing.BriefingRepository
import com.devbytes.vixplayer.app.briefing.BriefingScheduler
import com.devbytes.vixplayer.app.briefing.CalendarSource
import com.devbytes.vixplayer.app.briefing.ZoneMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.ZoneId
import javax.inject.Inject

@HiltViewModel
class SetupViewModel @Inject constructor(
    private val repository: BriefingRepository,
    private val scheduler: BriefingScheduler,
    private val calendar: CalendarSource,
    private val generator: BriefingGenerator,
) : ViewModel() {

    enum class Step { INTENT, SOURCES, SCHEDULE, CONNECT, SAMPLE, CONFIRM }

    data class UiState(
        val step: Step = Step.INTENT,
        val deliveryMinute: Int = 8 * 60,
        val daysMask: Int = BriefingConfig.MON_TO_FRI,
        val zoneMode: ZoneMode = ZoneMode.FOLLOW_DEVICE,
        val zoneId: String = ZoneId.systemDefault().id,
        val calendarConnected: Boolean = false,
        val sampleLoading: Boolean = false,
        val sampleBrief: Brief? = null,
        val canDeliverExactly: Boolean = true,
    )

    private val _ui = MutableStateFlow(
        UiState(calendarConnected = calendar.hasPermission(), canDeliverExactly = scheduler.canDeliverExactly()),
    )
    val ui: StateFlow<UiState> = _ui.asStateFlow()

    fun setTime(minuteOfDay: Int) = _ui.update { it.copy(deliveryMinute = minuteOfDay.coerceIn(0, 24 * 60 - 1)) }

    fun toggleDay(day: DayOfWeek) = _ui.update {
        val bit = 1 shl (day.value - 1)
        it.copy(daysMask = it.daysMask xor bit)
    }

    fun setZoneMode(mode: ZoneMode) = _ui.update { it.copy(zoneMode = mode) }

    fun refreshConnection() = _ui.update {
        it.copy(calendarConnected = calendar.hasPermission(), canDeliverExactly = scheduler.canDeliverExactly())
    }

    fun back() = _ui.update { it.copy(step = Step.entries[(it.step.ordinal - 1).coerceAtLeast(0)]) }

    fun next() {
        val cur = _ui.value.step
        val nextStep = Step.entries[(cur.ordinal + 1).coerceAtMost(Step.entries.lastIndex)]
        _ui.update { it.copy(step = nextStep) }
        if (nextStep == Step.SAMPLE) generateSample()
    }

    private fun generateSample() {
        viewModelScope.launch {
            _ui.update { it.copy(sampleLoading = true) }
            val s = _ui.value
            val zone = if (s.zoneMode == ZoneMode.FIXED_ZONE) {
                runCatching { ZoneId.of(s.zoneId) }.getOrDefault(ZoneId.systemDefault())
            } else ZoneId.systemDefault()
            val brief = if (calendar.hasPermission()) generator.generate(zone) else null
            _ui.update { it.copy(sampleBrief = brief, sampleLoading = false) }
        }
    }

    /** The go-live gate: only here is [BriefingConfig.sampleApprovedAt] set and the alarm scheduled. */
    fun goLive(onDone: () -> Unit) {
        viewModelScope.launch {
            val s = _ui.value
            repository.update {
                it.copy(
                    enabled = true,
                    deliveryMinuteOfDay = s.deliveryMinute,
                    daysMask = if (s.daysMask == 0) BriefingConfig.MON_TO_FRI else s.daysMask,
                    zoneMode = s.zoneMode,
                    fixedZoneId = if (s.zoneMode == ZoneMode.FIXED_ZONE) s.zoneId else null,
                    sampleApprovedAt = System.currentTimeMillis(),
                )
            }
            scheduler.schedule(repository.current())
            onDone()
        }
    }
}
