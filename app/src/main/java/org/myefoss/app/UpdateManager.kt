package org.myefoss.app

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.text.method.LinkMovementMethod
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
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
     * Fetches commits between current version and remote tag via GitHub Compare API,
     * or falls back to recent commits if compare returns 404.
     * Returns Markdown formatted list of commit titles.
     */
    suspend fun fetchChangelogMarkdown(currentVersion: String, remoteTag: String): String = withContext(Dispatchers.IO) {
        val cleanCurrent = currentVersion.trim().removePrefix("v").removePrefix("V")
        val cleanRemote = remoteTag.trim().removePrefix("v").removePrefix("V")

        // Try GitHub compare API with and without 'v' prefix
        val compareUrls = listOf(
            "https://api.github.com/repos/$GITHUB_REPO/compare/v$cleanCurrent...v$cleanRemote",
            "https://api.github.com/repos/$GITHUB_REPO/compare/$cleanCurrent...$cleanRemote"
        )

        for (compareUrl in compareUrls) {
            try {
                val conn = URL(compareUrl).openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.setRequestProperty("Accept", "application/vnd.github.v3+json")
                conn.setRequestProperty("User-Agent", "MyeFoss-App")
                conn.connectTimeout = 8000
                conn.readTimeout = 8000

                if (conn.responseCode == 200) {
                    val text = conn.inputStream.bufferedReader().use { it.readText() }
                    val json = JSONObject(text)
                    val commitsArr = json.optJSONArray("commits")
                    if (commitsArr != null && commitsArr.length() > 0) {
                        return@withContext formatCommitsToMarkdown(commitsArr)
                    }
                }
            } catch (e: Exception) {
                // Try next
            }
        }

        // Fallback: list latest commits
        try {
            val commitsUrl = "https://api.github.com/repos/$GITHUB_REPO/commits?per_page=15"
            val conn = URL(commitsUrl).openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.setRequestProperty("Accept", "application/vnd.github.v3+json")
            conn.setRequestProperty("User-Agent", "MyeFoss-App")
            conn.connectTimeout = 8000
            conn.readTimeout = 8000

            if (conn.responseCode == 200) {
                val text = conn.inputStream.bufferedReader().use { it.readText() }
                val commitsArr = JSONArray(text)
                if (commitsArr.length() > 0) {
                    return@withContext formatCommitsToMarkdown(commitsArr)
                }
            }
        } catch (e: Exception) {
            // Ignored
        }

        ""
    }

    private fun formatCommitsToMarkdown(commitsArray: JSONArray): String {
        val sb = StringBuilder()
        val seen = mutableSetOf<String>()

        for (i in 0 until commitsArray.length()) {
            val item = commitsArray.optJSONObject(i) ?: continue
            val commitObj = item.optJSONObject("commit") ?: item
            val rawMessage = commitObj.optString("message", "")
            val title = rawMessage.lines().firstOrNull()?.trim() ?: ""
            if (title.isBlank() || seen.contains(title)) continue
            seen.add(title)

            // Parse linux kernel style "subsystem: summary" into "- **subsystem**: summary"
            val colonIdx = title.indexOf(':')
            if (colonIdx > 0 && colonIdx < 30) {
                val tag = title.substring(0, colonIdx).trim()
                val desc = title.substring(colonIdx + 1).trim()
                sb.append("- **").append(tag).append("**: ").append(desc).append("\n")
            } else {
                sb.append("- ").append(title).append("\n")
            }
        }
        return sb.toString().trim()
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
                return true
            }
            if (currentSuffix == null && remoteSuffix != null) {
                return false
            }
            if (currentSuffix != null && remoteSuffix != null) {
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
        changelogMarkdown: String = "",
        onDownloadRequested: () -> Unit = {}
    ) {
        val markdownContent = buildString {
            append("Une nouvelle version de **MyeFoss** (`${release.tagName}`) est disponible.\n\n")

            if (changelogMarkdown.isNotBlank()) {
                append("### Changements récents\n")
                append(changelogMarkdown)
                append("\n\n")
            } else if (release.body.isNotBlank()) {
                append("### Notes de version\n")
                append(release.body)
                append("\n\n")
            }

            append("Souhaitez-vous télécharger et installer la mise à jour ?")
        }

        // Create a scrollable view with markdown support
        val density = context.resources.displayMetrics.density
        val paddingPx = (20 * density).toInt()

        val scrollView = ScrollView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(paddingPx, (8 * density).toInt(), paddingPx, (8 * density).toInt())
        }

        val tvBody = TextView(context).apply {
            movementMethod = LinkMovementMethod.getInstance()
            textSize = 14f
            text = MarkdownUtils.markdownToSpanned(markdownContent)
            val typedValue = android.util.TypedValue()
            if (context.theme.resolveAttribute(com.google.android.material.R.attr.colorOnSurface, typedValue, true)) {
                setTextColor(typedValue.data)
            }
        }

        container.addView(tvBody)
        scrollView.addView(container)

        MaterialAlertDialogBuilder(context)
            .setTitle("Mise à jour disponible : ${release.tagName}")
            .setView(scrollView)
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
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (!downloadsDir.exists()) {
                downloadsDir.mkdirs()
            }
            val destinationFile = File(downloadsDir, fileName)
            if (destinationFile.exists()) {
                destinationFile.delete()
            }

            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val request = DownloadManager.Request(Uri.parse(downloadUrl))
                .setTitle("Téléchargement de MyeFoss $tagName")
                .setDescription("Mise à jour de l'application...")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
                .setMimeType("application/vnd.android.package-archive")

            val downloadId = downloadManager.enqueue(request)
            Toast.makeText(context, "Téléchargement de la mise à jour lancé...", Toast.LENGTH_SHORT).show()

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
            try {
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(downloadUrl)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(browserIntent)
            } catch (err: Exception) {
                Toast.makeText(context, "Erreur lors du téléchargement : ${err.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    fun installApk(context: Context, apkFile: File) {
        if (!apkFile.exists()) {
            Toast.makeText(context, "Fichier d'installation introuvable", Toast.LENGTH_SHORT).show()
            return
        }

        try {
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
        } catch (e: Exception) {
            Toast.makeText(context, "Erreur lors du lancement de l'installation : ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}
