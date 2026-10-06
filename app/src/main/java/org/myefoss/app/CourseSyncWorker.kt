package org.myefoss.app

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.webkit.CookieManager
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class CourseSyncWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val CHANNEL_ID = "channel_course_updates"
        const val WORK_NAME = "work_course_sync"

        fun createNotificationChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val name = "Changements de cours"
                val descriptionText = "Notifications lors des modifications de salle, d'horaire ou d'annulation"
                val importance = NotificationManager.IMPORTANCE_DEFAULT
                val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                    description = descriptionText
                }
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.createNotificationChannel(channel)
            }
        }
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            // Check if notifications are enabled by user
            val prefs = context.getSharedPreferences("myefoss_prefs", Context.MODE_PRIVATE)
            val isEnabled = prefs.getBoolean("notify_course_changes", false)
            if (!isEnabled) {
                return@withContext Result.success()
            }

            // Sync upcoming 14 days
            val cal = Calendar.getInstance(Locale.FRANCE)
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            val startDate = cal.time

            cal.add(Calendar.DAY_OF_YEAR, 14)
            cal.set(Calendar.HOUR_OF_DAY, 23)
            cal.set(Calendar.MINUTE, 59)
            val endDate = cal.time

            val existingCourses = OfflineCacheManager.loadCourses(context).associateBy { it.id }
            val freshCourses = fetchPlanning(startDate, endDate)

            if (freshCourses.isNotEmpty()) {
                // Compare with cached courses for changes
                val timeFormat = SimpleDateFormat("HH:mm", Locale.FRANCE)
                val dateFormat = SimpleDateFormat("EEEE d MMMM", Locale.FRANCE)

                for (newCourse in freshCourses) {
                    val oldCourse = existingCourses[newCourse.id] ?: continue

                    val oldLoc = oldCourse.locations.firstOrNull()?.formatDisplay()
                    val newLoc = newCourse.locations.firstOrNull()?.formatDisplay()

                    val oldTime = oldCourse.startDate?.let { timeFormat.format(it) }
                    val newTime = newCourse.startDate?.let { timeFormat.format(it) }

                    var changeReason: String? = null

                    if (oldLoc != null && newLoc != null && oldLoc != newLoc) {
                        changeReason = "Changement de salle : $newLoc (au lieu de $oldLoc)"
                    } else if (oldTime != null && newTime != null && oldTime != newTime) {
                        changeReason = "Changement d'horaire : début à $newTime (au lieu de $oldTime)"
                    }

                    if (changeReason != null) {
                        val dateLabel = newCourse.startDate?.let { dateFormat.format(it) } ?: ""
                        sendChangeNotification(
                            title = "Modification : ${newCourse.name}",
                            message = if (dateLabel.isNotBlank()) "$dateLabel • $changeReason" else changeReason,
                            notificationId = newCourse.id.hashCode()
                        )
                    }
                }

                // Update cache with fresh data
                OfflineCacheManager.saveCourses(context, freshCourses)
            }

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }

    private fun sendChangeNotification(title: String, message: String, notificationId: Int) {
        createNotificationChannel(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(notificationId, builder.build())
    }

    private fun fetchPlanning(startDate: Date, endDate: Date): List<CourseEvent> {
        val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val startStr = URLEncoder.encode(isoFormat.format(startDate), "UTF-8")
        val endStr = URLEncoder.encode(isoFormat.format(endDate), "UTF-8")
        val urlStr = "https://www.myefrei.fr/api/rest/student/planning?startDate=$startStr&endDate=$endStr"

        val cookieManager = CookieManager.getInstance()
        val directCookies = cookieManager.getCookie(urlStr) ?: ""
        val wwwCookies = cookieManager.getCookie("https://www.myefrei.fr") ?: ""
        val authCookies = cookieManager.getCookie("https://auth.myefrei.fr") ?: ""

        val cookieMap = mutableMapOf<String, String>()
        for (cookieStr in listOf(authCookies, wwwCookies, directCookies)) {
            if (cookieStr.isNotBlank()) {
                cookieStr.split(";").forEach { part ->
                    val trimmed = part.trim()
                    val eqIdx = trimmed.indexOf('=')
                    if (eqIdx > 0) {
                        cookieMap[trimmed.substring(0, eqIdx).trim()] = trimmed
                    }
                }
            }
        }
        val mergedCookies = cookieMap.values.joinToString("; ")

        val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 15000
            readTimeout = 15000
            setRequestProperty("Accept", "application/json, text/plain, */*")
            setRequestProperty("Accept-Language", "fr-FR,fr;q=0.9,en-US;q=0.8,en;q=0.7")
            setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 14; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36")
            setRequestProperty("Referer", "https://www.myefrei.fr/portal/student/planning")
            setRequestProperty("Origin", "https://www.myefrei.fr")
            setRequestProperty("Sec-Fetch-Dest", "empty")
            setRequestProperty("Sec-Fetch-Mode", "cors")
            setRequestProperty("Sec-Fetch-Site", "same-origin")
            if (mergedCookies.isNotBlank()) {
                setRequestProperty("Cookie", mergedCookies)
            }
        }

        val code = conn.responseCode
        if (code in 200..299) {
            val response = conn.inputStream.bufferedReader().use { it.readText() }
            val jsonArray = JSONArray(response)
            val list = mutableListOf<CourseEvent>()
            for (i in 0 until jsonArray.length()) {
                list.add(CourseEvent.fromJson(jsonArray.getJSONObject(i)))
            }
            return list
        }
        return emptyList()
    }
}
