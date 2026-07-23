package com.devbytes.vixplayer.app.player

import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.media.audiofx.Virtualizer
import javax.inject.Inject
import javax.inject.Singleton

/** What the hardware actually supports, queried once rather than assumed. */
data class EqCapabilities(
    val bandCount: Int,
    /** Centre frequency per band, in Hz. */
    val bandFrequencies: List<Int>,
    val minLevelMb: Int,
    val maxLevelMb: Int,
    /** Device-provided preset names, e.g. Normal / Rock / Jazz. */
    val presetNames: List<String>,
    val hasBassBoost: Boolean,
    val hasVirtualizer: Boolean,
    val hasPreamp: Boolean,
)

/**
 * Owns the `android.media.audiofx` effect chain.
 *
 * Effects attach to the session id [PlayerController] assigned to the player, which the
 * app generates itself and keeps for the process. That makes the PRD's "effect must
 * rebind when audio session id changes" edge case **impossible** rather than handled:
 * track changes and queue transitions can't invalidate a session we own.
 *
 * Everything is wrapped: several ROMs restrict `audiofx`, and constructing an effect
 * there throws. A failure leaves [capabilities] null, which the UI renders as an explicit
 * unsupported state instead of inert controls.
 */
@Singleton
class AudioEffects @Inject constructor() {

    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null
    private var loudness: LoudnessEnhancer? = null

    var capabilities: EqCapabilities? = null
        private set

    /** True once [attach] has succeeded; false on hardware that refuses effects. */
    val isSupported: Boolean get() = capabilities != null

    /**
     * Binds the chain to [sessionId]. Safe to call repeatedly; later calls are ignored
     * once bound, since the session never changes.
     */
    fun attach(sessionId: Int) {
        if (equalizer != null || sessionId == 0) return
        runCatching {
            val eq = Equalizer(EFFECT_PRIORITY, sessionId)
            val bands = eq.numberOfBands.toInt()
            val range = eq.bandLevelRange
            capabilities = EqCapabilities(
                bandCount = bands,
                bandFrequencies = (0 until bands).map { eq.getCenterFreq(it.toShort()) / 1000 },
                minLevelMb = range[0].toInt(),
                maxLevelMb = range[1].toInt(),
                presetNames = (0 until eq.numberOfPresets.toInt())
                    .map { eq.getPresetName(it.toShort()) },
                hasBassBoost = false,
                hasVirtualizer = false,
                hasPreamp = false,
            )
            equalizer = eq
        }.onFailure {
            // Restricted ROM, or the session was rejected. Stay unsupported.
            capabilities = null
            return
        }

        // Each optional effect is independent: a device may allow the EQ but refuse
        // virtualizer, so one failure must not disable the rest.
        bassBoost = runCatching { BassBoost(EFFECT_PRIORITY, sessionId) }.getOrNull()
        virtualizer = runCatching { Virtualizer(EFFECT_PRIORITY, sessionId) }.getOrNull()
        loudness = runCatching { LoudnessEnhancer(sessionId) }.getOrNull()
        capabilities = capabilities?.copy(
            hasBassBoost = bassBoost?.strengthSupported == true,
            hasVirtualizer = virtualizer?.strengthSupported == true,
            hasPreamp = loudness != null,
        )
    }

    fun setEnabled(enabled: Boolean) {
        runCatching { equalizer?.enabled = enabled }
        runCatching { bassBoost?.enabled = enabled }
        runCatching { virtualizer?.enabled = enabled }
        runCatching { loudness?.enabled = enabled }
    }

    /** Current level of each band, in millibels. */
    fun bandLevels(): List<Int> {
        val eq = equalizer ?: return emptyList()
        val bands = capabilities?.bandCount ?: return emptyList()
        return runCatching {
            (0 until bands).map { eq.getBandLevel(it.toShort()).toInt() }
        }.getOrDefault(emptyList())
    }

    fun setBandLevel(band: Int, levelMb: Int) {
        runCatching { equalizer?.setBandLevel(band.toShort(), levelMb.toShort()) }
    }

    /** Applies a device preset by index, returning its resulting band levels. */
    fun usePreset(index: Int): List<Int> {
        runCatching { equalizer?.usePreset(index.toShort()) }
        return bandLevels()
    }

    /** Strength is 0..1000 for both effects, per the platform API. */
    fun setBassBoost(strength: Int) {
        runCatching { bassBoost?.setStrength(strength.coerceIn(0, 1000).toShort()) }
    }

    fun setVirtualizer(strength: Int) {
        runCatching { virtualizer?.setStrength(strength.coerceIn(0, 1000).toShort()) }
    }

    /** Preamp gain in millibels; clamped to keep it out of obvious clipping territory. */
    fun setPreamp(gainMb: Int) {
        runCatching { loudness?.setTargetGain(gainMb.coerceIn(0, PREAMP_MAX_MB)) }
    }

    fun release() {
        runCatching { equalizer?.release() }
        runCatching { bassBoost?.release() }
        runCatching { virtualizer?.release() }
        runCatching { loudness?.release() }
        equalizer = null
        bassBoost = null
        virtualizer = null
        loudness = null
        capabilities = null
    }

    private companion object {
        const val EFFECT_PRIORITY = 0
        const val PREAMP_MAX_MB = 1500
    }
}
