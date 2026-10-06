package fr.myefrei.agenda

import android.annotation.SuppressLint
import android.net.Uri
import android.os.Bundle
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        webView = WebView(this)
        setContentView(webView)

        // Enable cookies and third-party cookies
        val cookieManager = android.webkit.CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)
        cookieManager.setAcceptThirdPartyCookies(webView, true)

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            allowFileAccess = true
            allowContentAccess = true
            mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            
            // Spoof standard Chrome Mobile User Agent to bypass Keycloak/SSO webview blocks
            userAgentString = "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
            
            // Enable CORS for local file:/// assets fetching remote APIs
            allowUniversalAccessFromFileURLs = true
            allowFileAccessFromFileURLs = true
        }

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val url = request?.url?.toString() ?: return false

                // Intercept the Web SSO gateway redirect once authenticated on myefrei.fr
                if (url.startsWith("https://www.myefrei.fr/home") || 
                    url.startsWith("https://www.myefrei.fr/dashboard") ||
                    url.startsWith("https://www.myefrei.fr/planning") ||
                    (url.startsWith("https://www.myefrei.fr") && url.contains("logged_in=1"))) {
                    android.util.Log.e("WebViewConsole", "Web login succeeded! Redirecting to index.html with web session")
                    webView.loadUrl("file:///android_asset/index.html?web_auth=1")
                    return true
                }

                // If redirected to root of myefrei after SSO callback
                if (url == "https://www.myefrei.fr/" || url == "https://www.myefrei.fr") {
                    val cookieManager = android.webkit.CookieManager.getInstance()
                    val cookies = cookieManager.getCookie("https://www.myefrei.fr") ?: ""
                    if (cookies.contains("myefrei.sid")) {
                        android.util.Log.e("WebViewConsole", "myefrei.sid cookie detected after login! Loading index.html")
                        webView.loadUrl("file:///android_asset/index.html?web_auth=1")
                        return true
                    }
                }

                // Let all other URLs (Keycloak SSO login page, Microsoft AD, etc.) load in WebView
                return false
            }
        }

        webView.webChromeClient = object : android.webkit.WebChromeClient() {
            override fun onConsoleMessage(consoleMessage: android.webkit.ConsoleMessage?): Boolean {
                if (consoleMessage != null) {
                    android.util.Log.e("WebViewConsole", "${consoleMessage.message()} -- From line ${consoleMessage.lineNumber()} of ${consoleMessage.sourceId()}")
                }
                return true
            }
        }

        webView.addJavascriptInterface(WebAppInterface(), "Android")

        webView.loadUrl("file:///android_asset/index.html")
    }

    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }

    inner class WebAppInterface {

        // Merge cookies from both Efrei domains so auth.sid (Keycloak) is sent alongside myefrei.sid
        private fun getCookiesForUrl(urlStr: String): String? {
            val cookieManager = android.webkit.CookieManager.getInstance()
            val directCookies = cookieManager.getCookie(urlStr) ?: ""
            val wwwCookies = cookieManager.getCookie("https://www.myefrei.fr") ?: ""
            val authCookies = cookieManager.getCookie("https://auth.myefrei.fr") ?: ""

            // Merge all cookie strings, deduplicating by key
            val cookieMap = mutableMapOf<String, String>()
            for (cookieStr in listOf(authCookies, wwwCookies, directCookies)) {
                if (cookieStr.isNotBlank()) {
                    cookieStr.split(";").forEach { part ->
                        val trimmed = part.trim()
                        val eqIdx = trimmed.indexOf('=')
                        if (eqIdx > 0) {
                            val key = trimmed.substring(0, eqIdx).trim()
                            cookieMap[key] = trimmed
                        }
                    }
                }
            }

            val merged = cookieMap.values.joinToString("; ")
            android.util.Log.e("WebViewConsole", "Merged cookies for $urlStr: $merged")
            return merged.ifBlank { null }
        }

        // Authenticated GET request forwarding session cookies from WebView CookieManager
        @android.webkit.JavascriptInterface
        fun getSecure(urlStr: String): String {
            return try {
                val url = java.net.URL(urlStr)
                val conn = url.openConnection() as java.net.HttpURLConnection
                conn.requestMethod = "GET"
                conn.connectTimeout = 15000
                conn.readTimeout = 15000
                conn.setRequestProperty("Accept", "application/json")
                conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36")

                val cookies = getCookiesForUrl(urlStr)
                if (!cookies.isNullOrEmpty()) {
                    conn.setRequestProperty("Cookie", cookies)
                    android.util.Log.e("WebViewConsole", "getSecure forwarding cookies for $urlStr: $cookies")
                } else {
                    android.util.Log.e("WebViewConsole", "getSecure: no cookies found for $urlStr")
                }

                val status = conn.responseCode
                val stream = if (status >= 400) conn.errorStream else conn.inputStream
                val responseText = stream?.bufferedReader()?.use { it.readText() } ?: ""

                android.util.Log.e("WebViewConsole", "getSecure $urlStr -> $status")

                org.json.JSONObject().apply {
                    put("status", status)
                    put("body", responseText)
                }.toString()
            } catch (e: Exception) {
                android.util.Log.e("WebViewConsole", "getSecure error: ${e.message}")
                org.json.JSONObject().apply {
                    put("status", 500)
                    put("error", e.message)
                }.toString()
            }
        }

        // Authenticated POST request forwarding session cookies from WebView CookieManager
        @android.webkit.JavascriptInterface
        fun postSecure(urlStr: String, contentType: String, bodyStr: String): String {
            return try {
                val url = java.net.URL(urlStr)
                val conn = url.openConnection() as java.net.HttpURLConnection
                conn.requestMethod = "POST"
                conn.doOutput = true
                conn.connectTimeout = 10000
                conn.readTimeout = 10000
                conn.setRequestProperty("Content-Type", contentType)
                conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36")

                val cookies = getCookiesForUrl(urlStr)
                if (!cookies.isNullOrEmpty()) {
                    conn.setRequestProperty("Cookie", cookies)
                    android.util.Log.e("WebViewConsole", "postSecure forwarding cookies: $cookies")
                }

                conn.outputStream.use { os ->
                    val input = bodyStr.toByteArray(Charsets.UTF_8)
                    os.write(input, 0, input.size)
                }

                val status = conn.responseCode
                val stream = if (status >= 400) conn.errorStream else conn.inputStream
                val responseText = stream?.bufferedReader()?.use { it.readText() } ?: ""

                org.json.JSONObject().apply {
                    put("status", status)
                    put("body", responseText)
                }.toString()
            } catch (e: Exception) {
                org.json.JSONObject().apply {
                    put("status", 500)
                    put("error", e.message)
                }.toString()
            }
        }
    }
}
