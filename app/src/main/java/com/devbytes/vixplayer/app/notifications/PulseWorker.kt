package com.devbytes.vixplayer.app.notifications

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Runs ~daily. Bails immediately if notifications are system-disabled (no state advances),
 * otherwise asks [PulseEvaluator] for at most one notification and posts it. All guards,
 * precedence, cap, and bookkeeping live in the evaluator.
 */
@HiltWorker
class PulseWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val evaluator: PulseEvaluator,
    private val notifier: PulseNotifier,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        if (!notifier.notificationsEnabled()) return Result.success()
        evaluator.evaluate()?.let { notifier.post(it) }
        return Result.success()
    }
}
