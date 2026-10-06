package org.myefoss.app

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

object UpdateManager {

    private const val GITHUB_REPO = "myeFoss/MyeFoss"
    private const val RELEASES_API = "https://api.github.com/repos/$GITHUB_REPO/releases?per_page=1"

    data class ReleaseInfo(
        val tagName: String,
        val releaseName: String,
        val body: String,
        val downloadUrl: String
    )

    suspend fun checkLatestRelease(): ReleaseInfo? = withContext(Dispatchers.IO) {
        try {
            val url = URL(RELEASES_API)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.setRequestProperty("Accept", "application/vnd.github.v3+json")
            conn.setRequestProperty("User-Agent", "MyeFoss-App")
            conn.connectTimeout = 8000
            conn.readTimeout = 8000

            if (conn.responseCode != 200) return@withContext null

            val jsonStr = conn.inputStream.bufferedReader().use { it.readText() }
            val releasesArray = JSONArray(jsonStr)
            if (releasesArray.length() == 0) return@withContext null
            val json = releasesArray.getJSONObject(0)

            val tagName = json.optString("tag_name", "")
            val name = json.optString("name", tagName)
            val body = json.optString("body", "")

            val assets: JSONArray = json.optJSONArray("assets") ?: return@withContext null
            var downloadUrl: String? = null

            // 1. Release APK: MyeFoss.apk or *-release.apk
            for (i in 0 until assets.length()) {
                val asset = assets.getJSONObject(i)
                val assetName = asset.optString("name", "")
                val urlStr = asset.optString("browser_download_url", "")
                if (assetName.equals("MyeFoss.apk", ignoreCase = true) || assetName.endsWith("-release.apk")) {
                    downloadUrl = urlStr
                    break
                }
            }

            // 2. Fallback to any APK
            if (downloadUrl == null) {
                for (i in 0 until assets.length()) {
                    val asset = assets.getJSONObject(i)
                    val assetName = asset.optString("name", "")
                    val urlStr = asset.optString("browser_download_url", "")
                    if (assetName.endsWith(".apk")) {
                        downloadUrl = urlStr
                        break
                    }
                }
            }

            if (downloadUrl != null && tagName.isNotEmpty()) {
                ReleaseInfo(tagName, name, body, downloadUrl)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Compares version strings like "0.9.0-rc1" or "0.9.0" vs tag "v0.9.1" or "v0.9.0-rc2".
     * Returns true if remote is strictly newer than current.
     */
    fun isNewerVersion(currentVersion: String, remoteTag: String): Boolean {
        try {
            val cleanCurrent = currentVersion.trim().removePrefix("v").removePrefix("V")
            val cleanRemote = remoteTag.trim().removePrefix("v").removePrefix("V")

            if (cleanCurrent == cleanRemote) return false

            val currentParts = cleanCurrent.split("-")
            val remoteParts = cleanRemote.split("-")

            val currentBaseNums = currentParts[0].split(".").mapNotNull { it.toIntOrNull() }
            val remoteBaseNums = remoteParts[0].split(".").mapNotNull { it.toIntOrNull() }

            val maxLen = maxOf(currentBaseNums.size, remoteBaseNums.size)
            for (i in 0 until maxLen) {
                val cNum = currentBaseNums.getOrElse(i) { 0 }
                val rNum = remoteBaseNums.getOrElse(i) { 0 }
                if (rNum > cNum) return true
                if (rNum < cNum) return false
            }

            // If base numbers match (e.g. 0.9.0 vs 0.9.0-rc2 or 0.9.0-rc1 vs 0.9.0):
            // Final release (no -suffix) > release candidates (-rcX)
            val currentSuffix = currentParts.getOrNull(1)
            val remoteSuffix = remoteParts.getOrNull(1)

            if (currentSuffix != null && remoteSuffix == null) {
                // Remote is stable release, current is RC -> newer
                return true
            }
            if (currentSuffix == null && remoteSuffix != null) {
                // Current is stable release, remote is RC -> current is newer
                return false
            }
            if (currentSuffix != null && remoteSuffix != null) {
                // Compare RC numbers (e.g. rc1 vs rc2)
                return remoteSuffix > currentSuffix
            }

            return false
        } catch (e: Exception) {
            return false
        }
    }

    fun showUpdateDialog(
        context: Context,
        release: ReleaseInfo,
        onDownloadRequested: () -> Unit
    ) {
        val summary = if (release.body.isNotBlank()) {
            val lines = release.body.lines().take(5).joinToString("\n")
            "\n\n$lines"
        } else ""

        MaterialAlertDialogBuilder(context)
            .setTitle("Mise à jour disponible : ${release.tagName}")
            .setMessage("Une nouvelle version de MyeFoss est disponible.$summary\n\nSouhaitez-vous télécharger et installer la mise à jour ?")
            .setPositiveButton("Mettre à jour") { _, _ ->
                onDownloadRequested()
                downloadAndInstall(context, release.downloadUrl, release.tagName)
            }
            .setNegativeButton("Plus tard", null)
            .show()
    }

    private fun downloadAndInstall(context: Context, downloadUrl: String, tagName: String) {
        try {
            val fileName = "myefoss_${tagName.replace("/", "_")}.apk"
            val destinationFile = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), fileName)
            if (destinationFile.exists()) {
                destinationFile.delete()
            }

            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val request = DownloadManager.Request(Uri.parse(downloadUrl))
                .setTitle("Téléchargement de MyeFoss $tagName")
                .setDescription("Mise à jour de l'application...")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationUri(Uri.fromFile(destinationFile))
                .setMimeType("application/vnd.android.package-archive")

            val downloadId = downloadManager.enqueue(request)

            val onCompleteReceiver = object : BroadcastReceiver() {
                override fun onReceive(recvContext: Context, intent: Intent) {
                    val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L)
                    if (id == downloadId) {
                        try {
                            recvContext.unregisterReceiver(this)
                        } catch (e: Exception) {
                            // Ignored
                        }
                        installApk(recvContext, destinationFile)
                    }
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(
                    onCompleteReceiver,
                    IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
                    Context.RECEIVER_NOT_EXPORTED
                )
            } else {
                context.registerReceiver(
                    onCompleteReceiver,
                    IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
                )
            }
        } catch (e: Exception) {
            // Fallback to opening browser directly if DownloadManager fails
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(downloadUrl)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(browserIntent)
        }
    }

    fun installApk(context: Context, apkFile: File) {
        if (!apkFile.exists()) return

        val apkUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            apkFile
        )

        val installIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(apkUri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        context.startActivity(installIntent)
    }
}
