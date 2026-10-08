package org.myefoss.app

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat

object SessionNotificationManager {

    const val CHANNEL_ID = "channel_session_alerts"
    private const val NOTIFICATION_ID = 3001
    private const val PREFS_KEY_LAST_NOTIFIED_TIME = "last_notified_session_expired"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Expiration de session"
            val descriptionText = "Notifications lors de la déconnexion ou de l'expiration du compte"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun notifySessionExpired(context: Context) {
        val prefs = context.getSharedPreferences("myefoss_prefs", Context.MODE_PRIVATE)
        val isEnabled = prefs.getBoolean("notify_session_expired", true)
        if (!isEnabled) return

        // Throttle to avoid repeated notifications (e.g. at most once every 30 minutes)
        val lastNotified = prefs.getLong(PREFS_KEY_LAST_NOTIFIED_TIME, 0L)
        val now = System.currentTimeMillis()
        if (now - lastNotified < 30 * 60 * 1000L) {
            return
        }
        prefs.edit().putLong(PREFS_KEY_LAST_NOTIFIED_TIME, now).apply()

        createNotificationChannel(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("EXTRA_SHOW_REAUTH", true)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = "Session MyeFoss expirée"
        val message = "Votre session MyEfrei est déconnectée. Appuyez ici pour vous reconnecter."

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_security)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, builder.build())
    }

    fun cancelNotification(context: Context) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(NOTIFICATION_ID)
    }
}
