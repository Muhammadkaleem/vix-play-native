package com.devbytes.vixplayer.app.update

import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability

/**
 * Google Play In-App Updates, hybrid policy: a background **Flexible** update by default,
 * escalating to the blocking **Immediate** flow for high-priority or very-stale releases.
 *
 * Owned by [MainActivity] — the result launcher must be registered on the Activity. Call
 * [checkForUpdate] from `onCreate`, [onResume] from `onResume`, [unregister] from `onDestroy`.
 *
 * **Only functional when the app was installed by Play** (internal testing track / internal
 * app sharing); on a sideloaded or emulator build `appUpdateInfo` resolves to
 * `UPDATE_NOT_AVAILABLE`, so every path below is a safe no-op there.
 */
class AppUpdater(
    activity: ComponentActivity,
    private val launcher: ActivityResultLauncher<IntentSenderRequest>,
    /** Fired when a Flexible update has finished downloading and is ready to install. */
    private val onFlexibleDownloaded: () -> Unit,
) {
    private val manager = AppUpdateManagerFactory.create(activity.applicationContext)

    private val listener = InstallStateUpdatedListener { state ->
        if (state.installStatus() == InstallStatus.DOWNLOADED) onFlexibleDownloaded()
    }

    /** Cold-start check: apply a previously-downloaded update, else start the right flow. */
    fun checkForUpdate() {
        manager.appUpdateInfo.addOnSuccessListener { info ->
            // A Flexible update was downloaded in a prior session but the user skipped the
            // restart — apply it now (skipped ⇒ auto-install on the next launch).
            if (info.installStatus() == InstallStatus.DOWNLOADED) {
                manager.completeUpdate()
                return@addOnSuccessListener
            }
            if (info.updateAvailability() != UpdateAvailability.UPDATE_AVAILABLE) return@addOnSuccessListener

            val stale = info.clientVersionStalenessDays() ?: 0
            val forceImmediate =
                info.updatePriority() >= PRIORITY_IMMEDIATE || stale >= STALE_DAYS_IMMEDIATE

            when {
                forceImmediate && info.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE) ->
                    start(info, AppUpdateType.IMMEDIATE)
                info.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE) -> {
                    manager.registerListener(listener)
                    start(info, AppUpdateType.FLEXIBLE)
                }
            }
        }
    }

    /** Foreground: resume an interrupted Immediate update; surface a finished Flexible one. */
    fun onResume() {
        manager.appUpdateInfo.addOnSuccessListener { info ->
            if (info.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS &&
                info.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)
            ) {
                start(info, AppUpdateType.IMMEDIATE)
            }
            if (info.installStatus() == InstallStatus.DOWNLOADED) onFlexibleDownloaded()
        }
    }

    /** Installs a downloaded Flexible update (restarts the app). */
    fun completeUpdate() {
        manager.completeUpdate()
    }

    fun unregister() {
        runCatching { manager.unregisterListener(listener) }
    }

    private fun start(info: AppUpdateInfo, type: Int) {
        runCatching {
            manager.startUpdateFlowForResult(
                info,
                launcher,
                AppUpdateOptions.newBuilder(type).build(),
            )
        }
    }

    private companion object {
        /** Play update-priority (0–5) at or above which we force the Immediate flow. */
        const val PRIORITY_IMMEDIATE = 4
        /** Days on an old version after which we force Immediate even at low priority. */
        const val STALE_DAYS_IMMEDIATE = 30
    }
}
