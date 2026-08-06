package com.devbytes.vixplayer.app.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.devbytes.vixplayer.app.MainActivity
import com.devbytes.vixplayer.app.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** Owns the `pulse` channel and turns a resolved [PulseNotification] into a posted notification. */
@Singleton
class PulseNotifier @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    /** Whether the app may post at all (system-level toggle / channel not blocked). */
    fun notificationsEnabled(): Boolean =
        NotificationManagerCompat.from(context).areNotificationsEnabled()

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java)
        if (manager.getNotificationChannel(CHANNEL_ID) == null) {
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Library updates", NotificationManager.IMPORTANCE_DEFAULT).apply {
                    description = "Reminders to resume, new videos, and your weekly recap."
                },
            )
        }
    }

    fun post(notification: PulseNotification) {
        if (!notificationsEnabled()) return
        ensureChannel()

        // Content tap → MainActivity. A play URI deep-links into the player (auto-resumes);
        // otherwise it opens the library home. FROM_PULSE tells the graph to keep home beneath.
        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_FROM_PULSE, true)
            notification.playUri?.let { putExtra(EXTRA_PLAY_URI, it) }
        }
        val contentPi = PendingIntent.getActivity(
            context, notification.trigger.ordinal, contentIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        // "Turn off" → flips the master toggle off and cancels the schedule.
        val offIntent = Intent(context, PulseActionReceiver::class.java).apply { action = ACTION_TURN_OFF }
        val offPi = PendingIntent.getBroadcast(
            context, 0, offIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        val built = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_play)
            .setContentTitle(notification.title)
            .setContentText(notification.text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(notification.text))
            .setContentIntent(contentPi)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .addAction(0, "Turn off", offPi)
            .build()

        // One live Pulse notification at a time — a new one replaces the old.
        runCatching { NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, built) }
    }

    companion object {
        const val CHANNEL_ID = "pulse"
        const val NOTIFICATION_ID = 4201
        const val ACTION_TURN_OFF = "com.devbytes.vixplayer.app.PULSE_TURN_OFF"
        const val EXTRA_PLAY_URI = "com.devbytes.vixplayer.app.EXTRA_PLAY_URI"
        const val EXTRA_FROM_PULSE = "com.devbytes.vixplayer.app.EXTRA_FROM_PULSE"
    }
}
