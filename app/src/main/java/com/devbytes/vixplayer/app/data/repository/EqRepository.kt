package com.devbytes.vixplayer.app.data.repository

import android.content.Context
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import com.devbytes.vixplayer.app.data.db.dao.EqDao
import com.devbytes.vixplayer.app.data.db.entity.AudioOutput
import com.devbytes.vixplayer.app.data.db.entity.EqPreset
import com.devbytes.vixplayer.app.data.db.entity.EqProfile
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Equalizer profiles (one per output route) and user-saved presets.
 *
 * The active route is tracked live via [AudioDeviceCallback], so plugging in headphones
 * mid-track swaps to that route's profile — the PRD's per-output application.
 */
@Singleton
class EqRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dao: EqDao,
) {
    private val audioManager =
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private val _output = MutableStateFlow(detectOutput())
    val output: StateFlow<AudioOutput> = _output.asStateFlow()

    init {
        audioManager.registerAudioDeviceCallback(
            object : AudioDeviceCallback() {
                override fun onAudioDevicesAdded(added: Array<out AudioDeviceInfo>?) {
                    _output.value = detectOutput()
                }

                override fun onAudioDevicesRemoved(removed: Array<out AudioDeviceInfo>?) {
                    _output.value = detectOutput()
                }
            },
            Handler(Looper.getMainLooper()),
        )
    }

    /**
     * Bluetooth wins over wired, wired over speaker — the same precedence the platform
     * uses when routing, so the profile matches where sound is actually going.
     */
    private fun detectOutput(): AudioOutput {
        val devices = runCatching {
            audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
        }.getOrNull() ?: return AudioOutput.SPEAKER

        val types = devices.map { it.type }
        return when {
            types.any {
                it == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP || it == AudioDeviceInfo.TYPE_BLUETOOTH_SCO
            } -> AudioOutput.BLUETOOTH

            types.any {
                it == AudioDeviceInfo.TYPE_WIRED_HEADSET ||
                    it == AudioDeviceInfo.TYPE_WIRED_HEADPHONES ||
                    it == AudioDeviceInfo.TYPE_USB_HEADSET
            } -> AudioOutput.HEADSET

            else -> AudioOutput.SPEAKER
        }
    }

    suspend fun profileFor(output: AudioOutput): EqProfile? = dao.getProfile(output.name)

    suspend fun saveProfile(profile: EqProfile) = dao.upsertProfile(profile)

    fun observePresets(): Flow<List<EqPreset>> = dao.observePresets()

    suspend fun savePreset(preset: EqPreset) = dao.insertPreset(preset)

    suspend fun deletePreset(preset: EqPreset) = dao.deletePreset(preset)
}
