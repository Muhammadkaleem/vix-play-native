package com.devbytes.vixplayer.app.briefing

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

/** The exact alarm fires here; hand off to an expedited worker for the actual generation. */
class BriefingAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val request = OneTimeWorkRequestBuilder<BriefingWorker>()
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .build()
        WorkManager.getInstance(context)
            .enqueueUniqueWork("briefing_generate", ExistingWorkPolicy.REPLACE, request)
    }
}

/**
 * Re-registers the next fire after a reboot or a time/zone change (the timezone-follow
 * behavior). The alarm doesn't survive these on its own.
 */
@AndroidEntryPoint
class BriefingBootReceiver : BroadcastReceiver() {
    @Inject lateinit var scheduler: BriefingScheduler
    @Inject lateinit var repository: BriefingRepository

    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                scheduler.schedule(repository.current())
            } finally {
                pending.finish()
            }
        }
    }
}
