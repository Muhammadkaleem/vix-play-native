package com.devbytes.vixplayer.app

import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.IntentCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.devbytes.vixplayer.app.ads.AdsController
import com.devbytes.vixplayer.app.briefing.BriefingNotifier
import com.devbytes.vixplayer.app.data.repository.SettingsRepository
import com.devbytes.vixplayer.app.notifications.PulseEvaluator
import com.devbytes.vixplayer.app.notifications.PulseNotifier
import com.devbytes.vixplayer.app.notifications.PulseScheduler
import com.devbytes.vixplayer.app.player.PipController
import com.devbytes.vixplayer.app.update.AppUpdater
import com.devbytes.vixplayer.app.ui.VixPlayNavGraph
import com.devbytes.vixplayer.app.ui.splash.SplashViewModel
import com.devbytes.vixplayer.app.ui.theme.ThemeViewModel
import com.devbytes.vixplayer.app.ui.theme.VixPlayTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val splashViewModel: SplashViewModel by viewModels()
    private val themeViewModel: ThemeViewModel by viewModels()

    @Inject lateinit var adsController: AdsController
    @Inject lateinit var pulseScheduler: PulseScheduler
    @Inject lateinit var pulseEvaluator: PulseEvaluator
    @Inject lateinit var settingsRepository: SettingsRepository

    /** PlayerScreen registers its PiP contract here; the OS hooks below call it. */
    val pipController = PipController()

    /**
     * Media handed to us by another app via ACTION_VIEW / ACTION_SEND. A one-shot the nav
     * graph consumes to jump straight into the player, bypassing the splash permission gate
     * — the launching app already granted read access through the intent. Cleared once the
     * graph has routed it so a rotation or process return doesn't replay the launch.
     */
    val pendingExternalMedia = mutableStateOf<List<Uri>>(emptyList())

    /**
     * A resume-nudge notification tap carries the video URI here. Distinct from
     * [pendingExternalMedia]: this lands on the player with the library beneath it (Back →
     * library), whereas an external open makes the player the root (Back → caller).
     */
    val pendingPulsePlayUri = mutableStateOf<String?>(null)

    /** A Daily Briefing notification tap asks to open today's brief with the library beneath. */
    val pendingOpenBriefing = mutableStateOf(false)

    // ── In-app updates (Google Play) ──────────────────────────────────────────
    /** Launcher for the Play update consent/progress UI (registered on the Activity). */
    private val updateLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult(),
    ) { result ->
        // Analytics only — onResume reconciles in-progress / downloaded state.
        if (::appUpdater.isInitialized) appUpdater.onPromptResult(result.resultCode)
    }

    /** Flipped true when a Flexible update has downloaded; drives the restart Snackbar. */
    private val updateReady = mutableStateOf(false)

    private lateinit var appUpdater: AppUpdater

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen().setKeepOnScreenCondition { !splashViewModel.checkDone.value }
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        pendingExternalMedia.value = extractMedia(intent)
        pendingPulsePlayUri.value = intent?.getStringExtra(PulseNotifier.EXTRA_PLAY_URI)
        pendingOpenBriefing.value = intent?.getBooleanExtra(BriefingNotifier.EXTRA_OPEN_BRIEFING, false) == true
        // Crash breadcrumb: external-open sessions exercise a different (intent-granted URI)
        // code path than in-app browsing.
        if (pendingExternalMedia.value.isNotEmpty()) {
            FirebaseCrashlytics.getInstance().setCustomKey("external_open", true)
        }
        // Record app usage (kills the idle nudge condition, acknowledges the library) and
        // (re)schedule the Pulse job if enabled.
        lifecycleScope.launch {
            // Pulse bookkeeping must never take down app startup.
            runCatching {
                pulseEvaluator.onAppOpened()
                if (settingsRepository.pulseSettings.first().master) pulseScheduler.schedule()
            }
        }
        // Gather EEA consent (UMP) then initialize ads; the banner stays hidden until this
        // resolves, so no ad loads before consent.
        adsController.ensureConsentAndInit(this)
        // Check for a Play update: Flexible in the background by default, escalating to the
        // Immediate blocking flow for high-priority / very-stale releases. No-op off-Play.
        appUpdater = AppUpdater(this, updateLauncher) { updateReady.value = true }
        appUpdater.checkForUpdate()
        setContent {
            val themeState by themeViewModel.themeState.collectAsState()
            VixPlayTheme(mode = themeState.mode, accent = themeState.accent) {
                VixPlayNavGraph(
                    pendingExternalMedia = pendingExternalMedia.value,
                    onExternalMediaConsumed = { pendingExternalMedia.value = emptyList() },
                    pulsePlayUri = pendingPulsePlayUri.value,
                    onPulsePlayConsumed = { pendingPulsePlayUri.value = null },
                    openBriefing = pendingOpenBriefing.value,
                    onBriefingConsumed = { pendingOpenBriefing.value = false },
                    updateReady = updateReady.value,
                    onInstallUpdate = { appUpdater.completeUpdate() },
                )
            }
        }
    }

    override fun onStart() {
        super.onStart()
        // Session counting for the interstitial frequency rules.
        adsController.onAppForeground()
    }

    override fun onStop() {
        adsController.onAppBackground()
        super.onStop()
    }

    override fun onResume() {
        super.onResume()
        // Resume an interrupted Immediate update; re-surface a finished Flexible download.
        if (::appUpdater.isInitialized) appUpdater.onResume()
    }

    override fun onDestroy() {
        if (::appUpdater.isInitialized) appUpdater.unregister()
        super.onDestroy()
    }

    // A file opened (or a Pulse tap) while the app is already running arrives here.
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val media = extractMedia(intent)
        if (media.isNotEmpty()) pendingExternalMedia.value = media
        intent.getStringExtra(PulseNotifier.EXTRA_PLAY_URI)?.let { pendingPulsePlayUri.value = it }
        if (intent.getBooleanExtra(BriefingNotifier.EXTRA_OPEN_BRIEFING, false)) pendingOpenBriefing.value = true
    }

    /** Pulls playable URIs out of a VIEW / SEND / SEND_MULTIPLE intent (empty otherwise). */
    private fun extractMedia(intent: Intent?): List<Uri> = when (intent?.action) {
        Intent.ACTION_VIEW -> intent.data?.let { listOf(it) } ?: emptyList()
        Intent.ACTION_SEND ->
            (IntentCompat.getParcelableExtra(intent, Intent.EXTRA_STREAM, Uri::class.java)
                ?: intent.clipData?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.uri)
                ?.let { listOf(it) } ?: emptyList()
        Intent.ACTION_SEND_MULTIPLE ->
            IntentCompat.getParcelableArrayListExtra(intent, Intent.EXTRA_STREAM, Uri::class.java)
                ?: emptyList()
        else -> emptyList()
    }

    // Home / recents while a video is actively playing → seamless PiP. The decision
    // (is this a playing video on the player route?) is owned by PlayerScreen.
    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && pipController.canEnterPip()) {
            pipController.buildParams()?.let { enterPictureInPictureMode(it) }
        }
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration,
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        pipController.onModeChanged(isInPictureInPictureMode)
    }
}
