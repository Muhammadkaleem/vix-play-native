package com.devbytes.vixplayer.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devbytes.vixplayer.app.data.repository.PrivateItem
import com.devbytes.vixplayer.app.data.repository.PrivateVault
import com.devbytes.vixplayer.app.data.repository.UnlockResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class VaultMode { SETUP, SHOW_RECOVERY, LOCKED, RECOVERY, UNLOCKED }

data class PrivateUiState(
    val mode: VaultMode = VaultMode.LOCKED,
    val items: List<PrivateItem> = emptyList(),
    val error: String? = null,
    /** Shown once, immediately after PIN setup. */
    val recoveryCode: String? = null,
)

@HiltViewModel
class PrivateFolderViewModel @Inject constructor(
    private val vault: PrivateVault,
) : ViewModel() {

    private val _state = MutableStateFlow(PrivateUiState())
    val state: StateFlow<PrivateUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            _state.value = _state.value.copy(
                mode = if (vault.hasPin()) VaultMode.LOCKED else VaultMode.SETUP,
            )
        }
    }

    /** Set the first PIN (or a replacement after recovery). Shows the recovery code once. */
    fun submitNewPin(pin: String, confirm: String) {
        if (pin.length < 4) {
            _state.value = _state.value.copy(error = "PIN must be at least 4 digits")
            return
        }
        if (pin != confirm) {
            _state.value = _state.value.copy(error = "PINs don't match")
            return
        }
        viewModelScope.launch {
            val recovery = vault.setPin(pin)
            _state.value = _state.value.copy(
                mode = VaultMode.SHOW_RECOVERY,
                recoveryCode = recovery,
                error = null,
            )
        }
    }

    /** Dismiss the recovery-code screen and enter the unlocked vault. */
    fun acknowledgeRecovery() {
        _state.value = _state.value.copy(recoveryCode = null)
        loadUnlocked()
    }

    fun unlock(pin: String) {
        viewModelScope.launch {
            when (vault.verifyPin(pin)) {
                UnlockResult.OK -> loadUnlocked()
                UnlockResult.WRONG -> _state.value = _state.value.copy(error = "Wrong PIN")
                UnlockResult.NO_PIN -> _state.value = _state.value.copy(mode = VaultMode.SETUP)
            }
        }
    }

    fun startRecovery() {
        _state.value = _state.value.copy(mode = VaultMode.RECOVERY, error = null)
    }

    fun cancelRecovery() {
        _state.value = _state.value.copy(mode = VaultMode.LOCKED, error = null)
    }

    /** A correct recovery code lets the user set a fresh PIN. */
    fun submitRecovery(code: String) {
        viewModelScope.launch {
            when (vault.verifyRecovery(code)) {
                UnlockResult.OK -> _state.value =
                    _state.value.copy(mode = VaultMode.SETUP, error = null)
                else -> _state.value = _state.value.copy(error = "Wrong recovery code")
            }
        }
    }

    fun unhide(item: PrivateItem) {
        viewModelScope.launch {
            vault.unhide(item)
            loadUnlocked()
        }
    }

    fun delete(item: PrivateItem) {
        viewModelScope.launch {
            vault.delete(item)
            loadUnlocked()
        }
    }

    /** Re-lock when leaving the screen, so returning requires the PIN again. */
    fun lock() {
        _state.value = _state.value.copy(mode = VaultMode.LOCKED, items = emptyList())
    }

    fun clearError() {
        if (_state.value.error != null) _state.value = _state.value.copy(error = null)
    }

    private fun loadUnlocked() {
        viewModelScope.launch {
            _state.value = _state.value.copy(
                mode = VaultMode.UNLOCKED,
                items = vault.list(),
                error = null,
            )
        }
    }
}
