package com.devbytes.vixplayer.app.update

import android.app.Activity
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import com.devbytes.vixplayer.app.analytics.Analytics
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.ActivityResult
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
    private val context = activity.applicationContext
    private val manager = AppUpdateManagerFactory.create(context)

    /** The flow last launched, so [onPromptResult] can attribute the user's answer. */
    private var lastType: Int? = null

    private val listener = InstallStateUpdatedListener { state ->
        if (state.installStatus() == InstallStatus.DOWNLOADED) {
            Analytics.log(context, Analytics.UPDATE_DOWNLOADED)
            onFlexibleDownloaded()
        }
    }

    /** Cold-start check: apply a previously-downloaded update, else start the right flow. */
    fun checkForUpdate() {
        manager.appUpdateInfo.addOnSuccessListener { info ->
            // A Flexible update was downloaded in a prior session but the user skipped the
            // restart — apply it now (skipped ⇒ auto-install on the next launch).
            if (info.installStatus() == InstallStatus.DOWNLOADED) {
                Analytics.log(context, Analytics.UPDATE_INSTALL_STARTED, "trigger" to "next_launch")
                manager.completeUpdate()
                return@addOnSuccessListener
            }
            if (info.updateAvailability() != UpdateAvailability.UPDATE_AVAILABLE) return@addOnSuccessListener

            val stale = info.clientVersionStalenessDays() ?: 0
            val forceImmediate =
                info.updatePriority() >= PRIORITY_IMMEDIATE || stale >= STALE_DAYS_IMMEDIATE

            when {
                forceImmediate && info.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE) ->
                    start(info, AppUpdateType.IMMEDIATE, "launch")
                info.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE) -> {
                    manager.registerListener(listener)
                    start(info, AppUpdateType.FLEXIBLE, "launch")
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
                start(info, AppUpdateType.IMMEDIATE, "resume")
            }
            if (info.installStatus() == InstallStatus.DOWNLOADED) onFlexibleDownloaded()
        }
    }

    /** Installs a downloaded Flexible update (restarts the app) — the snackbar's Restart. */
    fun completeUpdate() {
        Analytics.log(context, Analytics.UPDATE_INSTALL_STARTED, "trigger" to "restart_snackbar")
        manager.completeUpdate()
    }

    /** The Play update dialog closed with [resultCode] (forwarded from MainActivity's launcher). */
    fun onPromptResult(resultCode: Int) {
        val result = when (resultCode) {
            Activity.RESULT_OK -> "accepted"
            Activity.RESULT_CANCELED -> "declined"
            ActivityResult.RESULT_IN_APP_UPDATE_FAILED -> "failed"
            else -> "other_$resultCode"
        }
        Analytics.log(
            context,
            Analytics.UPDATE_PROMPT_RESULT,
            "update_type" to typeName(lastType),
            "result" to result,
        )
    }

    fun unregister() {
        runCatching { manager.unregisterListener(listener) }
    }

    private fun start(info: AppUpdateInfo, type: Int, trigger: String) {
        lastType = type
        Analytics.log(
            context,
            Analytics.UPDATE_PROMPT_SHOWN,
            "update_type" to typeName(type),
            "trigger" to trigger,
            "priority" to info.updatePriority(),
            "staleness_days" to (info.clientVersionStalenessDays() ?: 0),
        )
        runCatching {
            manager.startUpdateFlowForResult(
                info,
                launcher,
                AppUpdateOptions.newBuilder(type).build(),
            )
        }
    }

    private fun typeName(type: Int?) = when (type) {
        AppUpdateType.IMMEDIATE -> "immediate"
        AppUpdateType.FLEXIBLE -> "flexible"
        else -> "unknown"
    }

    private companion object {
        /** Play update-priority (0–5) at or above which we force the Immediate flow. */
        const val PRIORITY_IMMEDIATE = 4
        /** Days on an old version after which we force Immediate even at low priority. */
        const val STALE_DAYS_IMMEDIATE = 30
    }
}
