package org.myefoss.app

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.progressindicator.LinearProgressIndicator

class LxpWebDialogFragment : DialogFragment() {

    private var targetUrl: String = ""
    private var actionTitle: String = "LXP"

    private lateinit var toolbar: MaterialToolbar
    private lateinit var progressBar: LinearProgressIndicator
    private lateinit var webView: WebView
    private lateinit var btnOpenExternal: MaterialButton

    companion object {
        private const val ARG_URL = "arg_url"
        private const val ARG_TITLE = "arg_title"

        fun newInstance(url: String, title: String): LxpWebDialogFragment {
            val fragment = LxpWebDialogFragment()
            val args = Bundle().apply {
                putString(ARG_URL, url)
                putString(ARG_TITLE, title)
            }
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.Theme_MyeFoss)
        targetUrl = arguments?.getString(ARG_URL) ?: "https://www.myefrei.fr/portal/student/lxp/catalog/"
        actionTitle = arguments?.getString(ARG_TITLE) ?: "LXP"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_lxp_web_view, container, false)
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        toolbar = view.findViewById(R.id.lxpWebToolbar)
        progressBar = view.findViewById(R.id.lxpWebProgressBar)
        webView = view.findViewById(R.id.lxpWebView)
        btnOpenExternal = view.findViewById(R.id.btnLxpWebOpenExternal)

        toolbar.title = actionTitle
        toolbar.setNavigationOnClickListener { dismiss() }

        btnOpenExternal.setOnClickListener {
            val current = webView.url ?: targetUrl
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(current)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Impossible d'ouvrir le navigateur externe", Toast.LENGTH_SHORT).show()
            }
        }

        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)
        cookieManager.setAcceptThirdPartyCookies(webView, true)
        cookieManager.flush()

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            useWideViewPort = true
            loadWithOverviewMode = true
            userAgentString = "Mozilla/5.0 (Linux; Android 14; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                super.onProgressChanged(view, newProgress)
                if (newProgress < 100) {
                    progressBar.visibility = View.VISIBLE
                    progressBar.isIndeterminate = false
                    progressBar.progress = newProgress
                } else {
                    progressBar.visibility = View.GONE
                }
            }

            override fun onReceivedTitle(view: WebView?, title: String?) {
                super.onReceivedTitle(view, title)
                if (!title.isNullOrBlank() && !title.startsWith("http")) {
                    toolbar.subtitle = title
                }
            }
        }

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val url = request?.url?.toString() ?: return false
                if (url.startsWith("http://") || url.startsWith("https://")) {
                    return false
                }
                // Handle tel:, mailto:, intent: etc.
                return try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    startActivity(intent)
                    true
                } catch (e: Exception) {
                    true
                }
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                progressBar.visibility = View.VISIBLE
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                progressBar.visibility = View.GONE
            }
        }

        // Normalize URL
        val resolvedUrl = if (targetUrl.startsWith("http://") || targetUrl.startsWith("https://")) {
            targetUrl
        } else {
            "https://www.myefrei.fr" + (if (targetUrl.startsWith("/")) "" else "/") + targetUrl
        }

        webView.loadUrl(resolvedUrl)
    }

    override fun onDestroyView() {
        try {
            webView.stopLoading()
            webView.destroy()
        } catch (e: Exception) {}
        super.onDestroyView()
    }
}
