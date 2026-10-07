package org.myefoss.app

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

class AppUpdateWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val CHANNEL_ID = "channel_app_updates"
        const val WORK_NAME = "work_app_update_check"
        private const val NOTIFICATION_ID = 2001
        private const val PREFS_KEY_LAST_NOTIFIED_TAG = "last_notified_update_tag"

        fun createNotificationChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val name = "Mises à jour de l'application"
                val descriptionText = "Notifications lors de la disponibilité d'une nouvelle version de MyeFoss"
                val importance = NotificationManager.IMPORTANCE_DEFAULT
                val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                    description = descriptionText
                }
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.createNotificationChannel(channel)
            }
        }

        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val updateCheckRequest = PeriodicWorkRequestBuilder<AppUpdateWorker>(
                1, TimeUnit.HOURS
            )
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                updateCheckRequest
            )
        }
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val release = UpdateManager.checkLatestRelease() ?: return@withContext Result.success()

            val isNewer = UpdateManager.isNewerVersion(BuildConfig.VERSION_NAME, release.tagName)
            if (!isNewer) {
                return@withContext Result.success()
            }

            val prefs = context.getSharedPreferences("myefoss_prefs", Context.MODE_PRIVATE)
            val lastNotifiedTag = prefs.getString(PREFS_KEY_LAST_NOTIFIED_TAG, null)

            // Only notify if we haven't already notified for this exact version tag
            if (lastNotifiedTag == release.tagName) {
                return@withContext Result.success()
            }

            sendUpdateNotification(release.tagName, release.releaseName)
            prefs.edit().putString(PREFS_KEY_LAST_NOTIFIED_TAG, release.tagName).apply()

            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    private fun sendUpdateNotification(tagName: String, releaseName: String) {
        createNotificationChannel(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("EXTRA_SHOW_UPDATE", true)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = "Mise à jour disponible : $tagName"
        val message = if (releaseName.isNotBlank() && releaseName != tagName) {
            "$releaseName disponible. Appuyez pour installer la mise à jour."
        } else {
            "Une nouvelle version ($tagName) est disponible. Appuyez pour mettre à jour."
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, builder.build())
    }
}
