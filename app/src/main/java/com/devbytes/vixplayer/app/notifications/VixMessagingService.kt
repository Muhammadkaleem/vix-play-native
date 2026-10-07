package com.devbytes.vixplayer.app.notifications

import android.app.PendingIntent
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.devbytes.vixplayer.app.MainActivity
import com.devbytes.vixplayer.app.R
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

/**
 * Receives Firebase Cloud Messaging pushes.
 *
 * Background **notification** messages are rendered by the system directly (using the
 * `default_notification_*` manifest meta-data), so this only needs to handle the cases the
 * system doesn't: foreground messages and data-only messages. A `uri` data field deep-links
 * into the player, reusing the same [PulseNotifier.EXTRA_PLAY_URI] path as Library Pulse.
 */
class VixMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        // No backend to register the token with yet; Console/topic sends still reach the
        // device. When a backend exists, upload `token` here.
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val title = message.notification?.title
            ?: message.data["title"]
            ?: getString(R.string.app_name)
        val body = message.notification?.body ?: message.data["body"].orEmpty()
        val playUri = message.data["uri"]

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            if (!playUri.isNullOrBlank()) putExtra(PulseNotifier.EXTRA_PLAY_URI, playUri)
        }
        val pending = PendingIntent.getActivity(
            this,
            System.currentTimeMillis().toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_play)
            .setColor(getColor(R.color.dark_accent))
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pending)
            .build()

        // POST_NOTIFICATIONS is requested at splash; if it's not granted, notify is a no-op.
        if (NotificationManagerCompat.from(this).areNotificationsEnabled()) {
            NotificationManagerCompat.from(this).notify(System.currentTimeMillis().toInt(), notification)
        }
    }

    companion object {
        /** Push channel id — created in VixPlayApp and named by the manifest default. */
        const val CHANNEL_ID = "push"
    }
}
