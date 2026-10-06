package fr.myefrei.agenda

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
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
import androidx.core.content.ContextCompat
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
    private lateinit var tvCurrentPeriodLabel: TextView
    private lateinit var btnToday: MaterialButton
    private lateinit var btnPrevPeriod: MaterialButton
    private lateinit var btnNextPeriod: MaterialButton
    private lateinit var layoutWeekStrip: LinearLayout
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var rvAgenda: RecyclerView

    // Scolarity views
    private lateinit var cardGrades: MaterialCardView
    private lateinit var cardAbsences: MaterialCardView
    private lateinit var cardLxp: MaterialCardView

    // Login
    private lateinit var btnLogin: MaterialButton

    private lateinit var adapter: AgendaAdapter
    private var currentWeekCal: Calendar = Calendar.getInstance(Locale.FRANCE)
    private var selectedDayCal: Calendar = Calendar.getInstance(Locale.FRANCE)
    private var allCachedCourses: List<CourseEvent> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        // Material You Dynamic Colors (Android 12+)
        DynamicColors.applyToActivityIfAvailable(this)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
        setupListeners()
        setupRecyclerView()

        // Load offline cache immediately on launch
        allCachedCourses = OfflineCacheManager.loadCourses(this)
        if (allCachedCourses.isNotEmpty()) {
            displayWeekFromCourses(allCachedCourses)
        }

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

        tvCurrentPeriodLabel = findViewById(R.id.tvCurrentPeriodLabel)
        btnToday = findViewById(R.id.btnToday)
        btnPrevPeriod = findViewById(R.id.btnPrevPeriod)
        btnNextPeriod = findViewById(R.id.btnNextPeriod)
        layoutWeekStrip = findViewById(R.id.layoutWeekStrip)
        swipeRefresh = findViewById(R.id.swipeRefresh)
        rvAgenda = findViewById(R.id.rvAgenda)

        cardGrades = findViewById(R.id.cardGrades)
        cardAbsences = findViewById(R.id.cardAbsences)
        cardLxp = findViewById(R.id.cardLxp)

        btnLogin = findViewById(R.id.btnLogin)
    }

    private fun setupListeners() {
        btnMenuDrawer.setOnClickListener {
            if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                drawerLayout.closeDrawer(GravityCompat.START)
            } else {
                drawerLayout.openDrawer(GravityCompat.START)
            }
        }

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

        // Return to Today button
        btnToday.setOnClickListener {
            currentWeekCal = Calendar.getInstance(Locale.FRANCE)
            selectedDayCal = Calendar.getInstance(Locale.FRANCE)
            loadAgendaForCurrentWeek()
        }

        // Week navigation (< and >)
        btnPrevPeriod.setOnClickListener {
            currentWeekCal.add(Calendar.DAY_OF_YEAR, -7)
            loadAgendaForCurrentWeek()
        }

        btnNextPeriod.setOnClickListener {
            currentWeekCal.add(Calendar.DAY_OF_YEAR, 7)
            loadAgendaForCurrentWeek()
        }

        btnRefresh.setOnClickListener {
            loadAgendaForCurrentWeek()
        }

        swipeRefresh.setOnRefreshListener {
            loadAgendaForCurrentWeek(isSwipe = true)
        }

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
        if (cookies.contains("myefrei.sid") || allCachedCourses.isNotEmpty()) {
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
        val (startOfWeek, endOfWeek, weekDays) = getWeekBoundaries(currentWeekCal)

        // Month / Year title on top (e.g. "Octobre 2026")
        val monthFormat = SimpleDateFormat("MMMM yyyy", Locale.FRANCE)
        tvCurrentPeriodLabel.text = monthFormat.format(startOfWeek).replaceFirstChar { it.uppercase() }

        // Step 1: Immediately render from local memory / offline cache
        displayWeekFromCourses(allCachedCourses)

        if (!isSwipe && allCachedCourses.isEmpty()) {
            layoutLoading.visibility = View.VISIBLE
        }

        // Step 2: Fetch fresh data from network in background
        lifecycleScope.launch {
            try {
                val freshCourses = withContext(Dispatchers.IO) {
                    fetchPlanningFromApi(startOfWeek, endOfWeek)
                }

                if (freshCourses.isNotEmpty()) {
                    // Update cache on disk
                    OfflineCacheManager.saveCourses(this@MainActivity, freshCourses)
                    allCachedCourses = OfflineCacheManager.loadCourses(this@MainActivity)
                    displayWeekFromCourses(allCachedCourses)
                }

            } catch (e: Exception) {
                // If offline, user still sees their cached schedule smoothly
                if (e.message?.contains("401") == true || e.message?.contains("403") == true) {
                    Toast.makeText(this@MainActivity, "Session expirée, reconnexion...", Toast.LENGTH_SHORT).show()
                    startSilentReauth()
                } else if (allCachedCourses.isEmpty()) {
                    Toast.makeText(this@MainActivity, "Hors-ligne ou indisponible", Toast.LENGTH_SHORT).show()
                }
            } finally {
                layoutLoading.visibility = View.GONE
                swipeRefresh.isRefreshing = false
            }
        }
    }

    // Attempt transparent session reload if expired
    private fun startSilentReauth() {
        loginWebView.settings.javaScriptEnabled = true
        loginWebView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val url = request?.url?.toString() ?: return false
                val cm = CookieManager.getInstance()
                val c = cm.getCookie("https://www.myefrei.fr") ?: ""
                if (c.contains("myefrei.sid")) {
                    loadAgendaForCurrentWeek()
                    return true
                }
                return false
            }
        }
        loginWebView.loadUrl("https://www.myefrei.fr/auth/efrei?redirectPath=" + URLEncoder.encode("portal/student/planning", "UTF-8"))
    }

    private fun displayWeekFromCourses(courses: List<CourseEvent>) {
        val (_, _, weekDays) = getWeekBoundaries(currentWeekCal)
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
        renderWeekStrip(weekDays, daySections)

        // Scroll to selected/today section
        val targetIndex = daySections.indexOfFirst {
            val dCal = Calendar.getInstance(Locale.FRANCE).apply { time = it.date }
            dCal.get(Calendar.DAY_OF_YEAR) == selectedDayCal.get(Calendar.DAY_OF_YEAR) &&
                    dCal.get(Calendar.YEAR) == selectedDayCal.get(Calendar.YEAR)
        }.takeIf { it >= 0 } ?: daySections.indexOfFirst { it.isToday }.takeIf { it >= 0 } ?: 0

        rvAgenda.post {
            (rvAgenda.layoutManager as? LinearLayoutManager)?.scrollToPositionWithOffset(targetIndex, 0)
        }
    }

    // Render Google Calendar style horizontal 7-day strip
    private fun renderWeekStrip(weekDays: List<Date>, sections: List<DaySection>) {
        layoutWeekStrip.removeAllViews()
        val inflater = LayoutInflater.from(this)
        val dayNames = listOf("LUN", "MAR", "MER", "JEU", "VEN", "SAM", "DIM")
        val todayCal = Calendar.getInstance(Locale.FRANCE)
        val cal = Calendar.getInstance(Locale.FRANCE)

        weekDays.forEachIndexed { index, date ->
            cal.time = date
            val chipView = inflater.inflate(R.layout.item_week_day_chip, layoutWeekStrip, false)
            val tvDayName: TextView = chipView.findViewById(R.id.tvDayName)
            val tvDayNumber: TextView = chipView.findViewById(R.id.tvDayNumber)
            val viewIndicator: View = chipView.findViewById(R.id.viewCourseIndicator)

            tvDayName.text = dayNames.getOrElse(index) { "" }
            tvDayNumber.text = cal.get(Calendar.DAY_OF_MONTH).toString()

            val isToday = cal.get(Calendar.YEAR) == todayCal.get(Calendar.YEAR) &&
                    cal.get(Calendar.DAY_OF_YEAR) == todayCal.get(Calendar.DAY_OF_YEAR)

            val isSelected = cal.get(Calendar.YEAR) == selectedDayCal.get(Calendar.YEAR) &&
                    cal.get(Calendar.DAY_OF_YEAR) == selectedDayCal.get(Calendar.DAY_OF_YEAR)

            // Has courses dot indicator
            val hasCourses = sections.getOrNull(index)?.courses?.isNotEmpty() == true
            viewIndicator.visibility = if (hasCourses) View.VISIBLE else View.INVISIBLE

            if (isSelected) {
                chipView.isSelected = true
                tvDayName.setTextColor(ContextCompat.getColor(this, R.color.md_theme_light_onPrimary))
                tvDayNumber.setTextColor(ContextCompat.getColor(this, R.color.md_theme_light_onPrimary))
            } else if (isToday) {
                chipView.isActivated = true
                tvDayName.setTextColor(ContextCompat.getColor(this, R.color.md_theme_light_onPrimaryContainer))
                tvDayNumber.setTextColor(ContextCompat.getColor(this, R.color.md_theme_light_onPrimaryContainer))
            }

            chipView.setOnClickListener {
                selectedDayCal.time = date
                renderWeekStrip(weekDays, sections)
                // Scroll to this day's section in RecyclerView
                (rvAgenda.layoutManager as? LinearLayoutManager)?.scrollToPositionWithOffset(index, 0)
            }

            layoutWeekStrip.addView(chipView)
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
