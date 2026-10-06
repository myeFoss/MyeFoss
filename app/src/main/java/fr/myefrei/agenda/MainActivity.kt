package fr.myefrei.agenda

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.ImageView
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

    // Planning header & calendar views
    private lateinit var btnHeaderTitleWrapper: LinearLayout
    private lateinit var tvCurrentPeriodLabel: TextView
    private lateinit var ivExpandIcon: ImageView
    private lateinit var btnToday: MaterialButton
    private lateinit var btnPrevPeriod: MaterialButton
    private lateinit var btnNextPeriod: MaterialButton

    // Calendar containers
    private lateinit var layoutWeekStrip: LinearLayout
    private lateinit var layoutMonthContainer: LinearLayout
    private lateinit var layoutMonthHeaderDays: LinearLayout
    private lateinit var layoutMonthGridRows: LinearLayout

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
    private var isMonthExpanded: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        DynamicColors.applyToActivityIfAvailable(this)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
        setupListeners()
        setupRecyclerView()

        // Load offline cache immediately on launch
        allCachedCourses = OfflineCacheManager.loadCourses(this)
        if (allCachedCourses.isNotEmpty()) {
            displayPlanning(allCachedCourses)
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

        btnHeaderTitleWrapper = findViewById(R.id.btnHeaderTitleWrapper)
        tvCurrentPeriodLabel = findViewById(R.id.tvCurrentPeriodLabel)
        ivExpandIcon = findViewById(R.id.ivExpandIcon)
        btnToday = findViewById(R.id.btnToday)
        btnPrevPeriod = findViewById(R.id.btnPrevPeriod)
        btnNextPeriod = findViewById(R.id.btnNextPeriod)

        layoutWeekStrip = findViewById(R.id.layoutWeekStrip)
        layoutMonthContainer = findViewById(R.id.layoutMonthContainer)
        layoutMonthHeaderDays = findViewById(R.id.layoutMonthHeaderDays)
        layoutMonthGridRows = findViewById(R.id.layoutMonthGridRows)

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

        // Toggle Month Expand/Collapse on header title click
        btnHeaderTitleWrapper.setOnClickListener {
            isMonthExpanded = !isMonthExpanded
            updateExpandState()
        }

        // Return to Today button
        btnToday.setOnClickListener {
            currentWeekCal = Calendar.getInstance(Locale.FRANCE)
            selectedDayCal = Calendar.getInstance(Locale.FRANCE)
            loadAgendaForCurrentWeek()
        }

        // Period navigation (< and >)
        btnPrevPeriod.setOnClickListener {
            if (isMonthExpanded) {
                currentWeekCal.add(Calendar.MONTH, -1)
            } else {
                currentWeekCal.add(Calendar.DAY_OF_YEAR, -7)
            }
            loadAgendaForCurrentWeek()
        }

        btnNextPeriod.setOnClickListener {
            if (isMonthExpanded) {
                currentWeekCal.add(Calendar.MONTH, 1)
            } else {
                currentWeekCal.add(Calendar.DAY_OF_YEAR, 7)
            }
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

    private fun updateExpandState() {
        if (isMonthExpanded) {
            ivExpandIcon.setImageResource(R.drawable.ic_expand_less)
            layoutWeekStrip.visibility = View.GONE
            layoutMonthContainer.visibility = View.VISIBLE
            renderMonthGrid(allCachedCourses)
        } else {
            ivExpandIcon.setImageResource(R.drawable.ic_expand_more)
            layoutWeekStrip.visibility = View.VISIBLE
            layoutMonthContainer.visibility = View.GONE
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
        val (startOfWeek, endOfWeek, _) = getWeekBoundaries(currentWeekCal)

        val monthFormat = SimpleDateFormat("MMMM yyyy", Locale.FRANCE)
        tvCurrentPeriodLabel.text = monthFormat.format(currentWeekCal.time).replaceFirstChar { it.uppercase() }

        // Step 1: Render immediately from cache
        displayPlanning(allCachedCourses)

        if (!isSwipe && allCachedCourses.isEmpty()) {
            layoutLoading.visibility = View.VISIBLE
        }

        // Step 2: Fetch fresh data from network (fetch whole month range so month view also has data)
        lifecycleScope.launch {
            try {
                val calMonth = Calendar.getInstance(Locale.FRANCE).apply {
                    time = currentWeekCal.time
                    set(Calendar.DAY_OF_MONTH, 1)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val startMonth = calMonth.time
                val maxDay = calMonth.getActualMaximum(Calendar.DAY_OF_MONTH)
                calMonth.set(Calendar.DAY_OF_MONTH, maxDay)
                calMonth.set(Calendar.HOUR_OF_DAY, 23)
                calMonth.set(Calendar.MINUTE, 59)
                calMonth.set(Calendar.SECOND, 59)
                calMonth.set(Calendar.MILLISECOND, 999)
                val endMonth = calMonth.time

                val freshCourses = withContext(Dispatchers.IO) {
                    fetchPlanningFromApi(startMonth, endMonth)
                }

                if (freshCourses.isNotEmpty()) {
                    OfflineCacheManager.saveCourses(this@MainActivity, freshCourses)
                    allCachedCourses = OfflineCacheManager.loadCourses(this@MainActivity)
                    displayPlanning(allCachedCourses)
                }

            } catch (e: Exception) {
                if (e.message?.contains("401") == true || e.message?.contains("403") == true) {
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

    private fun startSilentReauth() {
        loginWebView.settings.javaScriptEnabled = true
        loginWebView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
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

    // Display only the selected day (or next upcoming day if empty) for a clean, non-overloaded home screen
    private fun displayPlanning(courses: List<CourseEvent>) {
        val (_, _, weekDays) = getWeekBoundaries(currentWeekCal)
        val cal = Calendar.getInstance(Locale.FRANCE)
        val todayCal = Calendar.getInstance(Locale.FRANCE)
        val dayLabelFormat = SimpleDateFormat("EEEE d MMMM", Locale.FRANCE)

        // All week sections for strip dots
        val allWeekSections = weekDays.map { dayDate ->
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

        renderWeekStrip(weekDays, allWeekSections)
        if (isMonthExpanded) {
            renderMonthGrid(courses)
        }

        // Show focused day in list: selected day + optionally next day with courses if current has none
        val selectedDate = selectedDayCal.time
        val selectedSection = allWeekSections.firstOrNull {
            val dCal = Calendar.getInstance(Locale.FRANCE).apply { time = it.date }
            dCal.get(Calendar.DAY_OF_YEAR) == selectedDayCal.get(Calendar.DAY_OF_YEAR) &&
                    dCal.get(Calendar.YEAR) == selectedDayCal.get(Calendar.YEAR)
        }

        val visibleSections = mutableListOf<DaySection>()
        if (selectedSection != null) {
            visibleSections.add(selectedSection)
            // If selected day has no courses, also show the next day that has courses so user sees upcoming classes
            if (selectedSection.courses.isEmpty()) {
                val nextUpcoming = allWeekSections.firstOrNull {
                    it.date.after(selectedDate) && it.courses.isNotEmpty()
                }
                if (nextUpcoming != null) {
                    visibleSections.add(nextUpcoming)
                }
            }
        } else {
            visibleSections.addAll(allWeekSections)
        }

        adapter.submitList(visibleSections)
    }

    // Google Calendar style 7-day horizontal strip
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

            val hasCourses = sections.getOrNull(index)?.courses?.isNotEmpty() == true
            viewIndicator.visibility = if (hasCourses) View.VISIBLE else View.INVISIBLE

            if (isSelected) {
                chipView.setBackgroundResource(R.drawable.bg_day_chip)
                chipView.isSelected = true
                tvDayName.setTextColor(ContextCompat.getColor(this, R.color.md_theme_light_onPrimary))
                tvDayNumber.setTextColor(ContextCompat.getColor(this, R.color.md_theme_light_onPrimary))
            } else if (isToday) {
                chipView.setBackgroundResource(R.drawable.bg_day_chip)
                chipView.isActivated = true
                tvDayName.setTextColor(ContextCompat.getColor(this, R.color.md_theme_light_onPrimaryContainer))
                tvDayNumber.setTextColor(ContextCompat.getColor(this, R.color.md_theme_light_onPrimaryContainer))
            } else if (hasCourses) {
                // Surbrillance pour les jours avec cours
                chipView.setBackgroundResource(R.drawable.bg_day_has_courses)
                chipView.isSelected = false
                chipView.isActivated = false
                tvDayName.setTextColor(ContextCompat.getColor(this, R.color.md_theme_light_onSecondaryContainer))
                tvDayNumber.setTextColor(ContextCompat.getColor(this, R.color.md_theme_light_onSecondaryContainer))
            } else {
                chipView.setBackgroundResource(R.drawable.bg_day_chip)
                chipView.isSelected = false
                chipView.isActivated = false
                tvDayName.setTextColor(ContextCompat.getColor(this, R.color.md_theme_light_onSurfaceVariant))
                tvDayNumber.setTextColor(ContextCompat.getColor(this, R.color.md_theme_light_onSurface))
            }

            chipView.setOnClickListener {
                selectedDayCal.time = date
                displayPlanning(allCachedCourses)
            }

            layoutWeekStrip.addView(chipView)
        }
    }

    // Full Month Grid View when expanded
    private fun renderMonthGrid(courses: List<CourseEvent>) {
        layoutMonthHeaderDays.removeAllViews()
        layoutMonthGridRows.removeAllViews()

        val dayNames = listOf("L", "M", "M", "J", "V", "S", "D")
        dayNames.forEach { name ->
            val tv = TextView(this).apply {
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                gravity = Gravity.CENTER
                text = name
                textSize = 12f
                setTextColor(ContextCompat.getColor(this@MainActivity, R.color.md_theme_light_onSurfaceVariant))
            }
            layoutMonthHeaderDays.addView(tv)
        }

        val cal = Calendar.getInstance(Locale.FRANCE).apply {
            time = currentWeekCal.time
            set(Calendar.DAY_OF_MONTH, 1)
        }
        val firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        val offset = if (firstDayOfWeek == Calendar.SUNDAY) 6 else firstDayOfWeek - Calendar.MONDAY
        val maxDays = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

        val todayCal = Calendar.getInstance(Locale.FRANCE)
        val totalCells = ((offset + maxDays + 6) / 7) * 7

        var dayCounter = 1
        var currentRow: LinearLayout? = null

        for (cell in 0 until totalCells) {
            if (cell % 7 == 0) {
                currentRow = LinearLayout(this).apply {
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                    orientation = LinearLayout.HORIZONTAL
                    setPadding(0, 4, 0, 4)
                }
                layoutMonthGridRows.addView(currentRow)
            }

            val cellContainer = FrameLayout(this).apply {
                layoutParams = LinearLayout.LayoutParams(0, 110, 1f)
            }

            if (cell >= offset && dayCounter <= maxDays) {
                val thisDay = dayCounter
                val cellCal = Calendar.getInstance(Locale.FRANCE).apply {
                    time = cal.time
                    set(Calendar.DAY_OF_MONTH, thisDay)
                }

                val isToday = cellCal.get(Calendar.YEAR) == todayCal.get(Calendar.YEAR) &&
                        cellCal.get(Calendar.DAY_OF_YEAR) == todayCal.get(Calendar.DAY_OF_YEAR)

                val isSelectedDay = cellCal.get(Calendar.YEAR) == selectedDayCal.get(Calendar.YEAR) &&
                        cellCal.get(Calendar.DAY_OF_YEAR) == selectedDayCal.get(Calendar.DAY_OF_YEAR)

                val hasCourses = courses.any { course ->
                    course.startDate?.let {
                        val cCal = Calendar.getInstance(Locale.FRANCE).apply { time = it }
                        cCal.get(Calendar.YEAR) == cellCal.get(Calendar.YEAR) &&
                                cCal.get(Calendar.DAY_OF_YEAR) == cellCal.get(Calendar.DAY_OF_YEAR)
                    } ?: false
                }

                val dayBtn = TextView(this).apply {
                    layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
                    gravity = Gravity.CENTER
                    text = thisDay.toString()
                    textSize = 13f

                    if (isSelectedDay) {
                        setBackgroundResource(R.drawable.bg_day_chip)
                        isSelected = true
                        setTextColor(ContextCompat.getColor(this@MainActivity, R.color.md_theme_light_onPrimary))
                    } else if (isToday) {
                        setBackgroundResource(R.drawable.bg_day_chip)
                        isActivated = true
                        setTextColor(ContextCompat.getColor(this@MainActivity, R.color.md_theme_light_onPrimaryContainer))
                    } else if (hasCourses) {
                        setBackgroundResource(R.drawable.bg_day_has_courses)
                        setTextColor(ContextCompat.getColor(this@MainActivity, R.color.md_theme_light_onSecondaryContainer))
                        setTypeface(typeface, android.graphics.Typeface.BOLD)
                    } else {
                        setTextColor(ContextCompat.getColor(this@MainActivity, R.color.md_theme_light_onSurface))
                    }

                    setOnClickListener {
                        selectedDayCal.time = cellCal.time
                        currentWeekCal.time = cellCal.time
                        isMonthExpanded = false
                        updateExpandState()
                        displayPlanning(allCachedCourses)
                    }
                }

                cellContainer.addView(dayBtn)

                if (hasCourses) {
                    val dot = View(this).apply {
                        val size = 10
                        layoutParams = FrameLayout.LayoutParams(size, size).apply {
                            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
                            bottomMargin = 8
                        }
                        setBackgroundResource(R.drawable.dot_today)
                    }
                    cellContainer.addView(dot)
                }

                dayCounter++
            }

            currentRow?.addView(cellContainer)
        }
    }

    private fun spToFloat(): Float = 13f

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
