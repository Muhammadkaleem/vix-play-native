package com.devbytes.vixplayer.app.ui.audio

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devbytes.vixplayer.app.data.db.entity.AudioOutput
import com.devbytes.vixplayer.app.data.db.entity.EqPreset
import com.devbytes.vixplayer.app.data.db.entity.EqProfile
import com.devbytes.vixplayer.app.data.repository.EqRepository
import com.devbytes.vixplayer.app.player.AudioEffects
import com.devbytes.vixplayer.app.player.EqCapabilities
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Live equalizer state for the screen. Null [capabilities] means unsupported hardware. */
data class EqUiState(
    val supported: Boolean = false,
    val enabled: Boolean = false,
    val capabilities: EqCapabilities? = null,
    val bandLevels: List<Int> = emptyList(),
    val presetName: String = CUSTOM_PRESET,
    val bassBoost: Int = 0,
    val virtualizer: Int = 0,
    val preampMb: Int = 0,
)

const val CUSTOM_PRESET = "Custom"

@HiltViewModel
class EqualizerViewModel @Inject constructor(
    private val effects: AudioEffects,
    private val repository: EqRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(EqUiState())
    val state: StateFlow<EqUiState> = _state.asStateFlow()

    val output: StateFlow<AudioOutput> = repository.output

    val presets: StateFlow<List<EqPreset>> = repository.observePresets()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch {
            // Reload whenever the route changes, so headphones get their own curve.
            repository.output.collect { route -> loadFor(route) }
        }
    }

    private suspend fun loadFor(route: AudioOutput) {
        val caps = effects.capabilities
        if (caps == null) {
            _state.value = EqUiState(supported = false)
            return
        }
        val saved = repository.profileFor(route)
        val levels = saved?.bandLevels?.takeIf { it.size == caps.bandCount }
            ?: List(caps.bandCount) { 0 }

        levels.forEachIndexed { index, level -> effects.setBandLevel(index, level) }
        effects.setBassBoost(saved?.bassBoost ?: 0)
        effects.setVirtualizer(saved?.virtualizer ?: 0)
        effects.setPreamp(saved?.preampMb ?: 0)
        effects.setEnabled(saved?.enabled ?: false)

        _state.value = EqUiState(
            supported = true,
            enabled = saved?.enabled ?: false,
            capabilities = caps,
            bandLevels = levels,
            presetName = saved?.presetName ?: CUSTOM_PRESET,
            bassBoost = saved?.bassBoost ?: 0,
            virtualizer = saved?.virtualizer ?: 0,
            preampMb = saved?.preampMb ?: 0,
        )
    }

    fun setEnabled(enabled: Boolean) {
        effects.setEnabled(enabled)
        update { it.copy(enabled = enabled) }
    }

    fun setBand(index: Int, levelMb: Int) {
        effects.setBandLevel(index, levelMb)
        update { current ->
            val levels = current.bandLevels.toMutableList().also { it[index] = levelMb }
            // Touching a band means this is no longer the named preset.
            current.copy(bandLevels = levels, presetName = CUSTOM_PRESET)
        }
    }

    /** Applies one of the device's built-in presets. */
    fun applyDevicePreset(index: Int, name: String) {
        val levels = effects.usePreset(index)
        update { it.copy(bandLevels = levels, presetName = name) }
    }

    /** Applies a user-saved preset, restoring every effect it captured. */
    fun applySavedPreset(preset: EqPreset) {
        preset.bandLevels.forEachIndexed { index, level -> effects.setBandLevel(index, level) }
        effects.setBassBoost(preset.bassBoost)
        effects.setVirtualizer(preset.virtualizer)
        effects.setPreamp(preset.preampMb)
        update {
            it.copy(
                bandLevels = preset.bandLevels,
                bassBoost = preset.bassBoost,
                virtualizer = preset.virtualizer,
                preampMb = preset.preampMb,
                presetName = preset.name,
            )
        }
    }

    fun setBassBoost(strength: Int) {
        effects.setBassBoost(strength)
        update { it.copy(bassBoost = strength) }
    }

    fun setVirtualizer(strength: Int) {
        effects.setVirtualizer(strength)
        update { it.copy(virtualizer = strength) }
    }

    fun setPreamp(gainMb: Int) {
        effects.setPreamp(gainMb)
        update { it.copy(preampMb = gainMb) }
    }

    fun savePreset(name: String) {
        val s = _state.value
        viewModelScope.launch {
            repository.savePreset(
                EqPreset(
                    name = name,
                    bandLevels = s.bandLevels,
                    bassBoost = s.bassBoost,
                    virtualizer = s.virtualizer,
                    preampMb = s.preampMb,
                )
            )
        }
    }

    fun deletePreset(preset: EqPreset) {
        viewModelScope.launch { repository.deletePreset(preset) }
    }

    /** Applies the change to state and persists it against the current route. */
    private fun update(transform: (EqUiState) -> EqUiState) {
        val next = transform(_state.value)
        _state.value = next
        if (!next.supported) return
        viewModelScope.launch {
            repository.saveProfile(
                EqProfile(
                    output = repository.output.value.name,
                    enabled = next.enabled,
                    bandLevels = next.bandLevels,
                    presetName = next.presetName,
                    bassBoost = next.bassBoost,
                    virtualizer = next.virtualizer,
                    preampMb = next.preampMb,
                )
            )
        }
    }
}
