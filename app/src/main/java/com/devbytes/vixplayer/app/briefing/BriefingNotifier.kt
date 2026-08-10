package com.devbytes.vixplayer.app.briefing

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

/** Owns the `briefing` channel and posts the morning notification. */
@Singleton
class BriefingNotifier @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun notificationsEnabled(): Boolean =
        NotificationManagerCompat.from(context).areNotificationsEnabled()

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java)
        if (manager.getNotificationChannel(CHANNEL_ID) == null) {
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Daily briefing", NotificationManager.IMPORTANCE_DEFAULT).apply {
                    description = "Your morning brief, at the time you choose."
                },
            )
        }
    }

    fun post(brief: Brief, lockScreenDetail: Boolean) {
        if (!notificationsEnabled()) return
        ensureChannel()

        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_OPEN_BRIEFING, true)
        }
        val tapPi = PendingIntent.getActivity(
            context, 0, tapIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        val built = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_timer)
            .setContentTitle("Your morning brief")
            .setContentText(brief.daySummary)
            .setStyle(NotificationCompat.BigTextStyle().bigText(brief.daySummary))
            .setContentIntent(tapPi)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            // Redact the day-shape detail on the lock screen unless the user opted in.
            .setVisibility(
                if (lockScreenDetail) NotificationCompat.VISIBILITY_PUBLIC
                else NotificationCompat.VISIBILITY_PRIVATE,
            )
            .build()

        runCatching { NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, built) }
    }

    companion object {
        const val CHANNEL_ID = "briefing"
        const val NOTIFICATION_ID = 4301
        const val EXTRA_OPEN_BRIEFING = "com.devbytes.vixplayer.app.EXTRA_OPEN_BRIEFING"
    }
}
