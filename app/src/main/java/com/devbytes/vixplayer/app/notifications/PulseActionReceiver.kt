package com.devbytes.vixplayer.app.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import com.devbytes.vixplayer.app.data.repository.SettingsRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Handles the notification's "Turn off" action: flips the master toggle and cancels work. */
@AndroidEntryPoint
class PulseActionReceiver : BroadcastReceiver() {

    @Inject lateinit var settings: SettingsRepository
    @Inject lateinit var scheduler: PulseScheduler

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != PulseNotifier.ACTION_TURN_OFF) return
        // Dismiss the notification the user acted on right away.
        NotificationManagerCompat.from(context).cancel(PulseNotifier.NOTIFICATION_ID)
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                settings.setPulseMaster(false)
                scheduler.cancel()
            } finally {
                pending.finish()
            }
        }
    }
}
