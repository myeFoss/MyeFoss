package fr.myefrei.agenda

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.color.DynamicColors
import com.google.android.material.navigation.NavigationView
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

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationDrawer: NavigationView
    private lateinit var bottomNavigation: BottomNavigationView
    private lateinit var layoutAppScreen: LinearLayout
    private lateinit var layoutLoginScreen: LinearLayout
    private lateinit var layoutLoading: FrameLayout
    private lateinit var loginWebView: WebView

    // Toolbar views
    private lateinit var btnMenuDrawer: MaterialButton
    private lateinit var tvToolbarTitle: TextView
    private lateinit var tvStudentSubtitle: TextView
    private lateinit var btnRefresh: MaterialButton

    // Tab containers
    private lateinit var tabContainerPlanning: LinearLayout
    private lateinit var tabContainerScolarity: ScrollView

    // Planning views
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var rvAgenda: RecyclerView
    private lateinit var tvCurrentWeekLabel: TextView
    private lateinit var btnPrevWeek: MaterialButton
    private lateinit var btnNextWeek: MaterialButton

    // Scolarity views
    private lateinit var cardGrades: MaterialCardView
    private lateinit var cardAbsences: MaterialCardView
    private lateinit var cardLxp: MaterialCardView

    // Login
    private lateinit var btnLogin: MaterialButton

    private lateinit var adapter: AgendaAdapter
    private var currentCalendar: Calendar = Calendar.getInstance(Locale.FRANCE)
    private var lastLoadedSections: List<DaySection> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        // Material You Dynamic Colors (Android 12+)
        DynamicColors.applyToActivityIfAvailable(this)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
        setupListeners()
        setupRecyclerView()

        checkSessionAndLoad()
    }

    private fun initViews() {
        drawerLayout = findViewById(R.id.drawerLayout)
        navigationDrawer = findViewById(R.id.navigationDrawer)
        bottomNavigation = findViewById(R.id.bottomNavigation)
        layoutAppScreen = findViewById(R.id.layoutAppScreen)
        layoutLoginScreen = findViewById(R.id.layoutLoginScreen)
        layoutLoading = findViewById(R.id.layoutLoading)
        loginWebView = findViewById(R.id.loginWebView)

        btnMenuDrawer = findViewById(R.id.btnMenuDrawer)
        tvToolbarTitle = findViewById(R.id.tvToolbarTitle)
        tvStudentSubtitle = findViewById(R.id.tvStudentSubtitle)
        btnRefresh = findViewById(R.id.btnRefresh)

        tabContainerPlanning = findViewById(R.id.tabContainerPlanning)
        tabContainerScolarity = findViewById(R.id.tabContainerScolarity)

        swipeRefresh = findViewById(R.id.swipeRefresh)
        rvAgenda = findViewById(R.id.rvAgenda)
        tvCurrentWeekLabel = findViewById(R.id.tvCurrentWeekLabel)
        btnPrevWeek = findViewById(R.id.btnPrevWeek)
        btnNextWeek = findViewById(R.id.btnNextWeek)

        cardGrades = findViewById(R.id.cardGrades)
        cardAbsences = findViewById(R.id.cardAbsences)
        cardLxp = findViewById(R.id.cardLxp)

        btnLogin = findViewById(R.id.btnLogin)
    }

    private fun setupListeners() {
        // Drawer toggle
        btnMenuDrawer.setOnClickListener {
            if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                drawerLayout.closeDrawer(GravityCompat.START)
            } else {
                drawerLayout.openDrawer(GravityCompat.START)
            }
        }

        // Drawer navigation items
        navigationDrawer.setNavigationItemSelectedListener { item ->
            drawerLayout.closeDrawer(GravityCompat.START)
            when (item.itemId) {
                R.id.drawer_planning -> {
                    bottomNavigation.selectedItemId = R.id.nav_planning
                    true
                }
                R.id.drawer_grades -> {
                    bottomNavigation.selectedItemId = R.id.nav_scolarity
                    showScolarityFeature("Notes & Résultats")
                    true
                }
                R.id.drawer_absences -> {
                    bottomNavigation.selectedItemId = R.id.nav_scolarity
                    showScolarityFeature("Absences")
                    true
                }
                R.id.drawer_lxp -> {
                    bottomNavigation.selectedItemId = R.id.nav_scolarity
                    showScolarityFeature("LXP / E-learning")
                    true
                }
                R.id.drawer_logout -> {
                    logout()
                    true
                }
                else -> false
            }
        }

        // Bottom Navigation Bar tabs
        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_planning -> {
                    tabContainerPlanning.visibility = View.VISIBLE
                    tabContainerScolarity.visibility = View.GONE
                    tvToolbarTitle.text = "Planning"
                    btnRefresh.visibility = View.VISIBLE
                    true
                }
                R.id.nav_scolarity -> {
                    tabContainerPlanning.visibility = View.GONE
                    tabContainerScolarity.visibility = View.VISIBLE
                    tvToolbarTitle.text = "Scolarité"
                    btnRefresh.visibility = View.GONE
                    true
                }
                else -> false
            }
        }

        // Week navigation
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

        // Scolarity card clicks
        cardGrades.setOnClickListener { showScolarityFeature("Notes & Résultats") }
        cardAbsences.setOnClickListener { showScolarityFeature("Suivi des Absences") }
        cardLxp.setOnClickListener { showScolarityFeature("LXP / E-learning") }

        btnLogin.setOnClickListener {
            startWebSsoLogin()
        }
    }

    private fun showScolarityFeature(title: String) {
        Toast.makeText(this, "$title sera bientôt disponible !", Toast.LENGTH_SHORT).show()
    }

    private fun setupRecyclerView() {
        adapter = AgendaAdapter { clickedCourse ->
            val sheet = CourseDetailsBottomSheet.newInstance(clickedCourse)
            sheet.show(supportFragmentManager, "course_details")
        }
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

                lastLoadedSections = daySections
                adapter.submitList(daySections)

                // Scroll to today's section automatically
                val todayIndex = daySections.indexOfFirst { it.isToday }
                if (todayIndex >= 0) {
                    rvAgenda.post {
                        (rvAgenda.layoutManager as? LinearLayoutManager)?.scrollToPositionWithOffset(todayIndex, 0)
                    }
                }

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
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
            return
        }
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
