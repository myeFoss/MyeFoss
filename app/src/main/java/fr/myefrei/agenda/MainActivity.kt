package fr.myefrei.agenda

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.View
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.button.MaterialButton
import com.google.android.material.color.DynamicColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
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

class MainActivity : AppCompatActivity() {

    private lateinit var layoutAppScreen: LinearLayout
    private lateinit var layoutLoginScreen: LinearLayout
    private lateinit var layoutLoading: FrameLayout
    private lateinit var loginWebView: WebView
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var rvAgenda: RecyclerView
    private lateinit var tvCurrentWeekLabel: TextView
    private lateinit var tvStudentSubtitle: TextView
    private lateinit var btnPrevWeek: MaterialButton
    private lateinit var btnNextWeek: MaterialButton
    private lateinit var btnRefresh: MaterialButton
    private lateinit var btnLogout: MaterialButton
    private lateinit var btnLogin: MaterialButton

    private lateinit var adapter: AgendaAdapter
    private var currentCalendar: Calendar = Calendar.getInstance(Locale.FRANCE)

    override fun onCreate(savedInstanceState: Bundle?) {
        // Apply Material You Dynamic Colors (Android 12+)
        DynamicColors.applyToActivityIfAvailable(this)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
        setupListeners()
        setupRecyclerView()

        checkSessionAndLoad()
    }

    private fun initViews() {
        layoutAppScreen = findViewById(R.id.layoutAppScreen)
        layoutLoginScreen = findViewById(R.id.layoutLoginScreen)
        layoutLoading = findViewById(R.id.layoutLoading)
        loginWebView = findViewById(R.id.loginWebView)
        swipeRefresh = findViewById(R.id.swipeRefresh)
        rvAgenda = findViewById(R.id.rvAgenda)
        tvCurrentWeekLabel = findViewById(R.id.tvCurrentWeekLabel)
        tvStudentSubtitle = findViewById(R.id.tvStudentSubtitle)
        btnPrevWeek = findViewById(R.id.btnPrevWeek)
        btnNextWeek = findViewById(R.id.btnNextWeek)
        btnRefresh = findViewById(R.id.btnRefresh)
        btnLogout = findViewById(R.id.btnLogout)
        btnLogin = findViewById(R.id.btnLogin)
    }

    private fun setupListeners() {
        btnPrevWeek.setOnClickListener {
            currentCalendar.add(Calendar.DAY_OF_YEAR, -7)
            loadAgendaForCurrentWeek()
        }

        btnNextWeek.setOnClickListener {
            currentCalendar.add(Calendar.DAY_OF_YEAR, 7)
            loadAgendaForCurrentWeek()
        }

        btnRefresh.setOnClickListener {
            loadAgendaForCurrentWeek()
        }

        swipeRefresh.setOnRefreshListener {
            loadAgendaForCurrentWeek(isSwipe = true)
        }

        btnLogin.setOnClickListener {
            startWebSsoLogin()
        }

        btnLogout.setOnClickListener {
            logout()
        }
    }

    private fun setupRecyclerView() {
        adapter = AgendaAdapter()
        rvAgenda.layoutManager = LinearLayoutManager(this)
        rvAgenda.adapter = adapter
    }

    private fun checkSessionAndLoad() {
        val cookieManager = CookieManager.getInstance()
        val cookies = cookieManager.getCookie("https://www.myefrei.fr") ?: ""
        if (cookies.contains("myefrei.sid")) {
            showScreen(Screen.APP)
            loadAgendaForCurrentWeek()
        } else {
            showScreen(Screen.LOGIN)
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun startWebSsoLogin() {
        showScreen(Screen.WEBVIEW)
        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)
        cookieManager.setAcceptThirdPartyCookies(loginWebView, true)

        loginWebView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            userAgentString = "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
        }

        loginWebView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val url = request?.url?.toString() ?: return false

                if (url.startsWith("https://www.myefrei.fr/home") ||
                    url.startsWith("https://www.myefrei.fr/dashboard") ||
                    url.startsWith("https://www.myefrei.fr/portal") ||
                    url == "https://www.myefrei.fr/" || url == "https://www.myefrei.fr") {

                    val cm = CookieManager.getInstance()
                    val c = cm.getCookie("https://www.myefrei.fr") ?: ""
                    if (c.contains("myefrei.sid")) {
                        loginWebView.visibility = View.GONE
                        showScreen(Screen.APP)
                        loadAgendaForCurrentWeek()
                        return true
                    }
                }
                return false
            }
        }

        val authUrl = "https://www.myefrei.fr/auth/efrei?redirectPath=" + URLEncoder.encode("portal/student/planning", "UTF-8")
        loginWebView.loadUrl(authUrl)
    }

    private fun loadAgendaForCurrentWeek(isSwipe: Boolean = false) {
        val (startOfWeek, endOfWeek, weekDays) = getWeekBoundaries(currentCalendar)

        val weekHeaderFormat = SimpleDateFormat("d MMMM yyyy", Locale.FRANCE)
        tvCurrentWeekLabel.text = "Semaine du ${weekHeaderFormat.format(startOfWeek)}"

        if (!isSwipe) layoutLoading.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                val courses = withContext(Dispatchers.IO) {
                    fetchPlanningFromApi(startOfWeek, endOfWeek)
                }

                // Group courses by day
                val cal = Calendar.getInstance(Locale.FRANCE)
                val todayCal = Calendar.getInstance(Locale.FRANCE)
                val dayLabelFormat = SimpleDateFormat("EEEE d MMMM", Locale.FRANCE)

                val daySections = weekDays.map { dayDate ->
                    cal.time = dayDate
                    val dayCourses = courses.filter { course ->
                        course.startDate?.let {
                            val cCal = Calendar.getInstance(Locale.FRANCE).apply { time = it }
                            cCal.get(Calendar.YEAR) == cal.get(Calendar.YEAR) &&
                                    cCal.get(Calendar.DAY_OF_YEAR) == cal.get(Calendar.DAY_OF_YEAR)
                        } ?: false
                    }.sortedBy { it.startDate ?: Date(0) }

                    val isToday = cal.get(Calendar.YEAR) == todayCal.get(Calendar.YEAR) &&
                            cal.get(Calendar.DAY_OF_YEAR) == todayCal.get(Calendar.DAY_OF_YEAR)

                    val label = dayLabelFormat.format(dayDate).replaceFirstChar { it.uppercase() }
                    DaySection(dayDate, label, isToday, dayCourses)
                }

                adapter.submitList(daySections)

            } catch (e: Exception) {
                if (e.message?.contains("401") == true || e.message?.contains("403") == true) {
                    Toast.makeText(this@MainActivity, "Session expirée, veuillez vous reconnecter", Toast.LENGTH_SHORT).show()
                    logout()
                } else {
                    Toast.makeText(this@MainActivity, "Erreur de chargement: ${e.message}", Toast.LENGTH_LONG).show()
                }
            } finally {
                layoutLoading.visibility = View.GONE
                swipeRefresh.isRefreshing = false
            }
        }
    }

    private fun fetchPlanningFromApi(startDate: Date, endDate: Date): List<CourseEvent> {
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
            setRequestProperty("Accept", "application/json")
            setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36")
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
        } else {
            val error = conn.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
            throw Exception("HTTP $code: $error")
        }
    }

    private fun getWeekBoundaries(referenceCal: Calendar): Triple<Date, Date, List<Date>> {
        val cal = Calendar.getInstance(Locale.FRANCE).apply {
            time = referenceCal.time
            firstDayOfWeek = Calendar.MONDAY
            val dayOfWeek = get(Calendar.DAY_OF_WEEK)
            val diff = if (dayOfWeek == Calendar.SUNDAY) -6 else Calendar.MONDAY - dayOfWeek
            add(Calendar.DAY_OF_MONTH, diff)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val weekDays = mutableListOf<Date>()
        val start = cal.time

        for (i in 0 until 7) {
            weekDays.add(cal.time)
            cal.add(Calendar.DAY_OF_MONTH, 1)
        }

        cal.add(Calendar.DAY_OF_MONTH, -1)
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        val end = cal.time

        return Triple(start, end, weekDays)
    }

    private fun logout() {
        CookieManager.getInstance().removeAllCookies(null)
        CookieManager.getInstance().flush()
        adapter.submitList(emptyList())
        showScreen(Screen.LOGIN)
    }

    private fun showScreen(screen: Screen) {
        when (screen) {
            Screen.APP -> {
                layoutAppScreen.visibility = View.VISIBLE
                layoutLoginScreen.visibility = View.GONE
                loginWebView.visibility = View.GONE
            }
            Screen.LOGIN -> {
                layoutAppScreen.visibility = View.GONE
                layoutLoginScreen.visibility = View.VISIBLE
                loginWebView.visibility = View.GONE
            }
            Screen.WEBVIEW -> {
                layoutAppScreen.visibility = View.GONE
                layoutLoginScreen.visibility = View.GONE
                loginWebView.visibility = View.VISIBLE
            }
        }
    }

    override fun onBackPressed() {
        if (loginWebView.visibility == View.VISIBLE) {
            if (loginWebView.canGoBack()) {
                loginWebView.goBack()
            } else {
                showScreen(Screen.LOGIN)
            }
        } else {
            super.onBackPressed()
        }
    }

    enum class Screen {
        APP, LOGIN, WEBVIEW
    }
}
