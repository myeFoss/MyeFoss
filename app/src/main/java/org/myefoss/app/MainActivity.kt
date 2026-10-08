package org.myefoss.app

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
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
import org.json.JSONObject
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
    private var tvDrawerUserName: TextView? = null
    private var tvDrawerUserEmail: TextView? = null

    // Toolbar views
    private lateinit var btnMenuDrawer: MaterialButton
    private lateinit var tvToolbarTitle: TextView
    private lateinit var tvStudentSubtitle: TextView
    private lateinit var btnRefresh: MaterialButton

    // Tab containers
    private lateinit var tabContainerPlanning: LinearLayout
    private lateinit var tabContainerScolarity: ScrollView
    private lateinit var tabContainerSettings: ScrollView
    private lateinit var tabContainerGrades: LinearLayout
    private lateinit var tabContainerAbsences: LinearLayout
    private lateinit var tabContainerCampus: LinearLayout
    private lateinit var tabContainerLxp: LinearLayout

    // Campus views
    private lateinit var layoutCampusList: LinearLayout
    private lateinit var swipeRefreshCampus: SwipeRefreshLayout

    // LXP views
    private lateinit var layoutLxpList: LinearLayout
    private lateinit var swipeRefreshLxp: SwipeRefreshLayout
    private lateinit var layoutLxpLoading: LinearLayout
    private lateinit var layoutLxpEmpty: LinearLayout
    private lateinit var tvLxpEmptyMessage: TextView
    private lateinit var btnHeaderOpenCatalog: MaterialButton
    private lateinit var btnOpenLxpCatalog: MaterialButton
    private lateinit var btnRetryLxp: MaterialButton

    // Grades views
    private lateinit var btnBackFromGrades: MaterialButton
    private lateinit var layoutGradesList: LinearLayout

    // Absences views
    private lateinit var btnBackFromAbsences: MaterialButton
    private lateinit var layoutAbsencesList: LinearLayout
    private lateinit var layoutAbsencesEmptyState: LinearLayout
    private lateinit var tvAbsencesTotalHours: TextView
    private lateinit var tvAbsencesSummaryLabel: TextView
    private lateinit var tvAbsencesJustifiedCount: TextView
    private lateinit var tvAbsencesUnjustifiedCount: TextView
    private lateinit var spinnerAbsencesSchoolYear: android.widget.Spinner
    private var isAbsencesYearSpinnerInitialized: Boolean = false

    // Settings views
    private lateinit var rgThemeMode: android.widget.RadioGroup
    private lateinit var rbThemeSystem: android.widget.RadioButton
    private lateinit var rbThemeLight: android.widget.RadioButton
    private lateinit var rbThemeDark: android.widget.RadioButton

    private lateinit var rgPalette: android.widget.RadioGroup
    private lateinit var rbPaletteDefault: android.widget.RadioButton
    private lateinit var rbPaletteEmerald: android.widget.RadioButton
    private lateinit var rbPalettePurple: android.widget.RadioButton
    private lateinit var rbPaletteAmber: android.widget.RadioButton
    private lateinit var rbPaletteMonet: android.widget.RadioButton
    private lateinit var dividerMonet: View
    private lateinit var switchCourseNotifications: com.google.android.material.materialswitch.MaterialSwitch
    private lateinit var switchSessionNotifications: com.google.android.material.materialswitch.MaterialSwitch
    private lateinit var textAppVersion: TextView
    private lateinit var btnCheckUpdates: MaterialButton

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
    private lateinit var btnRefreshGrades: MaterialButton
    private lateinit var btnRefreshAbsences: MaterialButton
    private lateinit var swipeRefreshGrades: SwipeRefreshLayout
    private lateinit var swipeRefreshAbsences: SwipeRefreshLayout

    // Scolarity views
    private lateinit var cardGrades: MaterialCardView
    private lateinit var cardAbsences: MaterialCardView
    private lateinit var cardLxp: MaterialCardView
    private lateinit var cardCampus: MaterialCardView

    // Login
    private lateinit var btnLogin: MaterialButton
    private lateinit var bannerSessionExpired: MaterialCardView
    private lateinit var btnBannerReconnect: MaterialButton

    private lateinit var adapter: AgendaAdapter
    private var currentWeekCal: Calendar = Calendar.getInstance(Locale.FRANCE)
    private var selectedDayCal: Calendar = Calendar.getInstance(Locale.FRANCE)
    private var allCachedCourses: List<CourseEvent> = emptyList()
    private var isMonthExpanded: Boolean = false
    private var lastFetchedMonthKey: String = ""
    private lateinit var spinnerSchoolYear: android.widget.Spinner
    private var isYearSpinnerInitialized: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        val prefs = getSharedPreferences("myefoss_prefs", MODE_PRIVATE)
        val savedThemeMode = prefs.getInt("theme_mode", androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        val savedPalette = prefs.getString("theme_palette", "default") ?: "default"

        androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(savedThemeMode)

        // Apply selected palette theme
        when (savedPalette) {
            "monet" -> {
                if (DynamicColors.isDynamicColorAvailable()) {
                    DynamicColors.applyIfAvailable(this)
                } else {
                    setTheme(R.style.Theme_MyeFoss)
                }
            }
            "emerald" -> setTheme(R.style.Theme_MyeFoss_Emerald)
            "purple" -> setTheme(R.style.Theme_MyeFoss_Purple)
            "amber" -> setTheme(R.style.Theme_MyeFoss_Amber)
            else -> setTheme(R.style.Theme_MyeFoss)
        }

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
        cachedStudentPeriods = OfflineCacheManager.loadStudentPeriods(this)
        val cachedProfile = OfflineCacheManager.loadStudentProfile(this)
        if (cachedProfile != null) {
            displayUserProfile(cachedProfile)
        }

        // Restore active screen after theme recreation
        val lastScreen = prefs.getString("last_active_screen", "planning")
        when (lastScreen) {
            "settings" -> openSettingsScreen()
            "scolarity" -> showScolarityTab()
            "campus" -> showCampusTab()
            "grades" -> openGradesScreen()
            "absences" -> openAbsencesScreen()
            else -> showPlanningTab()
        }

        checkSessionAndLoad()

        AppUpdateWorker.createNotificationChannel(this)
        AppUpdateWorker.schedule(this)

        handleUpdateIntent(intent)
        checkForUpdatesSilently()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent?.let { handleUpdateIntent(it) }
    }

    private fun handleUpdateIntent(intent: Intent) {
        if (intent.getBooleanExtra("EXTRA_SHOW_REAUTH", false)) {
            intent.removeExtra("EXTRA_SHOW_REAUTH")
            startSilentReauth()
            return
        }
        if (intent.getBooleanExtra("EXTRA_SHOW_UPDATE", false)) {
            intent.removeExtra("EXTRA_SHOW_UPDATE")
            lifecycleScope.launch {
                val release = UpdateManager.checkLatestRelease()
                if (release != null && UpdateManager.isNewerVersion(BuildConfig.VERSION_NAME, release.tagName)) {
                    val changelog = UpdateManager.fetchChangelogMarkdown(BuildConfig.VERSION_NAME, release.tagName)
                    UpdateManager.showUpdateDialog(this@MainActivity, release, changelog) {}
                }
            }
        }
    }

    private fun checkForUpdatesSilently() {
        lifecycleScope.launch {
            val release = UpdateManager.checkLatestRelease()
            if (release != null && UpdateManager.isNewerVersion(BuildConfig.VERSION_NAME, release.tagName)) {
                val changelog = UpdateManager.fetchChangelogMarkdown(BuildConfig.VERSION_NAME, release.tagName)
                UpdateManager.showUpdateDialog(this@MainActivity, release, changelog) {}
            }
        }
    }

    private fun initViews() {
        drawerLayout = findViewById(R.id.drawerLayout)
        navigationDrawer = findViewById(R.id.navigationDrawer)
        val headerView = navigationDrawer.getHeaderView(0)
        if (headerView != null) {
            tvDrawerUserName = headerView.findViewById(R.id.tvDrawerUserName)
            tvDrawerUserEmail = headerView.findViewById(R.id.tvDrawerUserEmail)
        }
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
        tabContainerSettings = findViewById(R.id.tabContainerSettings)
        tabContainerGrades = findViewById(R.id.tabContainerGrades)
        tabContainerAbsences = findViewById(R.id.tabContainerAbsences)
        tabContainerCampus = findViewById(R.id.tabContainerCampus)
        tabContainerLxp = findViewById(R.id.tabContainerLxp)

        layoutCampusList = findViewById(R.id.layoutCampusList)
        swipeRefreshCampus = findViewById(R.id.swipeRefreshCampus)

        layoutLxpList = findViewById(R.id.layoutLxpList)
        swipeRefreshLxp = findViewById(R.id.swipeRefreshLxp)
        layoutLxpLoading = findViewById(R.id.layoutLxpLoading)
        layoutLxpEmpty = findViewById(R.id.layoutLxpEmpty)
        tvLxpEmptyMessage = findViewById(R.id.tvLxpEmptyMessage)
        btnHeaderOpenCatalog = findViewById(R.id.btnHeaderOpenCatalog)
        btnOpenLxpCatalog = findViewById(R.id.btnOpenLxpCatalog)
        btnRetryLxp = findViewById(R.id.btnRetryLxp)

        btnBackFromGrades = findViewById(R.id.btnBackFromGrades)
        btnRefreshGrades = findViewById(R.id.btnRefreshGrades)
        swipeRefreshGrades = findViewById(R.id.swipeRefreshGrades)
        layoutGradesList = findViewById(R.id.layoutGradesList)
        layoutGradesEmptyState = findViewById(R.id.layoutGradesEmptyState)
        tvGeneralAverage = findViewById(R.id.tvGeneralAverage)
        tvGradesSemesterLabel = findViewById(R.id.tvGradesSemesterLabel)
        spinnerSchoolYear = findViewById(R.id.spinnerSchoolYear)

        btnBackFromAbsences = findViewById(R.id.btnBackFromAbsences)
        btnRefreshAbsences = findViewById(R.id.btnRefreshAbsences)
        swipeRefreshAbsences = findViewById(R.id.swipeRefreshAbsences)
        layoutAbsencesList = findViewById(R.id.layoutAbsencesList)
        layoutAbsencesEmptyState = findViewById(R.id.layoutAbsencesEmptyState)
        tvAbsencesTotalHours = findViewById(R.id.tvAbsencesTotalHours)
        tvAbsencesSummaryLabel = findViewById(R.id.tvAbsencesSummaryLabel)
        tvAbsencesJustifiedCount = findViewById(R.id.tvAbsencesJustifiedCount)
        tvAbsencesUnjustifiedCount = findViewById(R.id.tvAbsencesUnjustifiedCount)
        spinnerAbsencesSchoolYear = findViewById(R.id.spinnerAbsencesSchoolYear)

        rgThemeMode = findViewById(R.id.rgThemeMode)
        rbThemeSystem = findViewById(R.id.rbThemeSystem)
        rbThemeLight = findViewById(R.id.rbThemeLight)
        rbThemeDark = findViewById(R.id.rbThemeDark)

        rgPalette = findViewById(R.id.rgPalette)
        rbPaletteDefault = findViewById(R.id.rbPaletteDefault)
        rbPaletteEmerald = findViewById(R.id.rbPaletteEmerald)
        rbPalettePurple = findViewById(R.id.rbPalettePurple)
        rbPaletteAmber = findViewById(R.id.rbPaletteAmber)
        rbPaletteMonet = findViewById(R.id.rbPaletteMonet)
        dividerMonet = findViewById(R.id.dividerMonet)
        switchCourseNotifications = findViewById(R.id.switchCourseNotifications)
        switchSessionNotifications = findViewById(R.id.switchSessionNotifications)
        textAppVersion = findViewById(R.id.textAppVersion)
        btnCheckUpdates = findViewById(R.id.btnCheckUpdates)

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
        cardCampus = findViewById(R.id.cardCampus)

        btnLogin = findViewById(R.id.btnLogin)
        bannerSessionExpired = findViewById(R.id.bannerSessionExpired)
        btnBannerReconnect = findViewById(R.id.btnBannerReconnect)
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
                    openGradesScreen()
                    true
                }
                R.id.drawer_absences -> {
                    openAbsencesScreen()
                    true
                }
                R.id.drawer_lxp -> {
                    openLxpScreen()
                    true
                }
                R.id.drawer_campus -> {
                    bottomNavigation.selectedItemId = R.id.nav_campus
                    true
                }
                R.id.drawer_settings -> {
                    openSettingsScreen()
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
                    showPlanningTab()
                    true
                }
                R.id.nav_scolarity -> {
                    showScolarityTab()
                    true
                }
                R.id.nav_campus -> {
                    showCampusTab()
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

        cardGrades.setOnClickListener { openGradesScreen() }
        cardAbsences.setOnClickListener { openAbsencesScreen() }
        cardLxp.setOnClickListener { openLxpScreen() }
        cardCampus.setOnClickListener { showCampusTab() }

        btnBannerReconnect.setOnClickListener {
            startWebSsoLogin()
        }

        btnBackFromGrades.setOnClickListener {
            showScolarityTab()
        }

        btnRefreshGrades.setOnClickListener {
            loadStudentGrades(currentSelectedYear.ifBlank { getCurrentAcademicYear() }, isSwipe = true)
        }

        swipeRefreshGrades.setOnRefreshListener {
            loadStudentGrades(currentSelectedYear.ifBlank { getCurrentAcademicYear() }, isSwipe = true)
        }

        btnBackFromAbsences.setOnClickListener {
            showScolarityTab()
        }

        btnRefreshAbsences.setOnClickListener {
            loadStudentAbsences(currentSelectedAbsencesYear.ifBlank { getCurrentAcademicYear() }, isSwipe = true)
        }

        swipeRefreshAbsences.setOnRefreshListener {
            loadStudentAbsences(currentSelectedAbsencesYear.ifBlank { getCurrentAcademicYear() }, isSwipe = true)
        }

        swipeRefreshCampus.setOnRefreshListener {
            loadCampusesList(isSwipe = true)
        }

        swipeRefreshLxp.setOnRefreshListener {
            loadLxpActions(isSwipe = true)
        }

        btnHeaderOpenCatalog.setOnClickListener {
            openLxpWebCatalog()
        }

        btnOpenLxpCatalog.setOnClickListener {
            openLxpWebCatalog()
        }

        btnRetryLxp.setOnClickListener {
            loadLxpActions(isSwipe = false)
        }

        setupThemeSettings()

        btnLogin.setOnClickListener {
            startWebSsoLogin()
        }
    }

    private fun showPlanningTab() {
        getSharedPreferences("myefoss_prefs", MODE_PRIVATE).edit().putString("last_active_screen", "planning").apply()
        tabContainerPlanning.visibility = View.VISIBLE
        tabContainerScolarity.visibility = View.GONE
        tabContainerSettings.visibility = View.GONE
        tabContainerGrades.visibility = View.GONE
        tabContainerAbsences.visibility = View.GONE
        tabContainerCampus.visibility = View.GONE
        tabContainerLxp.visibility = View.GONE
        tvToolbarTitle.text = "Planning"
        btnRefresh.visibility = View.VISIBLE
        bottomNavigation.menu.findItem(R.id.nav_planning)?.isChecked = true
        navigationDrawer.setCheckedItem(R.id.drawer_planning)
    }

    private fun showScolarityTab() {
        getSharedPreferences("myefoss_prefs", MODE_PRIVATE).edit().putString("last_active_screen", "scolarity").apply()
        tabContainerPlanning.visibility = View.GONE
        tabContainerScolarity.visibility = View.VISIBLE
        tabContainerSettings.visibility = View.GONE
        tabContainerGrades.visibility = View.GONE
        tabContainerAbsences.visibility = View.GONE
        tabContainerCampus.visibility = View.GONE
        tabContainerLxp.visibility = View.GONE
        tvToolbarTitle.text = "Scolarité"
        btnRefresh.visibility = View.GONE
        bottomNavigation.menu.findItem(R.id.nav_scolarity)?.isChecked = true
        // Clear drawer check or leave unselected for sub-items
        navigationDrawer.checkedItem?.isChecked = false
    }

    private fun showCampusTab() {
        getSharedPreferences("myefoss_prefs", MODE_PRIVATE).edit().putString("last_active_screen", "campus").apply()
        tabContainerPlanning.visibility = View.GONE
        tabContainerScolarity.visibility = View.GONE
        tabContainerSettings.visibility = View.GONE
        tabContainerGrades.visibility = View.GONE
        tabContainerAbsences.visibility = View.GONE
        tabContainerCampus.visibility = View.VISIBLE
        tabContainerLxp.visibility = View.GONE
        tvToolbarTitle.text = "Campus"
        btnRefresh.visibility = View.GONE
        bottomNavigation.menu.findItem(R.id.nav_campus)?.isChecked = true
        navigationDrawer.setCheckedItem(R.id.drawer_campus)

        loadCampusesList()
    }

    private fun openLxpScreen() {
        getSharedPreferences("myefoss_prefs", MODE_PRIVATE).edit().putString("last_active_screen", "lxp").apply()
        tabContainerPlanning.visibility = View.GONE
        tabContainerScolarity.visibility = View.GONE
        tabContainerSettings.visibility = View.GONE
        tabContainerGrades.visibility = View.GONE
        tabContainerAbsences.visibility = View.GONE
        tabContainerCampus.visibility = View.GONE
        tabContainerLxp.visibility = View.VISIBLE
        tvToolbarTitle.text = "LXP / E-learning"
        btnRefresh.visibility = View.GONE
        navigationDrawer.setCheckedItem(R.id.drawer_lxp)

        loadLxpActions()
    }

    private fun openSettingsScreen() {
        getSharedPreferences("myefoss_prefs", MODE_PRIVATE).edit().putString("last_active_screen", "settings").apply()
        tabContainerPlanning.visibility = View.GONE
        tabContainerScolarity.visibility = View.GONE
        tabContainerGrades.visibility = View.GONE
        tabContainerAbsences.visibility = View.GONE
        tabContainerCampus.visibility = View.GONE
        tabContainerLxp.visibility = View.GONE
        tabContainerSettings.visibility = View.VISIBLE
        tvToolbarTitle.text = "Paramètres"
        btnRefresh.visibility = View.GONE
        navigationDrawer.setCheckedItem(R.id.drawer_settings)
    }

    private fun openGradesScreen() {
        getSharedPreferences("myefoss_prefs", MODE_PRIVATE).edit().putString("last_active_screen", "grades").apply()
        tabContainerPlanning.visibility = View.GONE
        tabContainerScolarity.visibility = View.GONE
        tabContainerSettings.visibility = View.GONE
        tabContainerAbsences.visibility = View.GONE
        tabContainerCampus.visibility = View.GONE
        tabContainerLxp.visibility = View.GONE
        tabContainerGrades.visibility = View.VISIBLE
        tvToolbarTitle.text = "Notes & Résultats"
        btnRefresh.visibility = View.GONE
        navigationDrawer.setCheckedItem(R.id.drawer_grades)

        loadStudentGrades()
    }

    private fun openAbsencesScreen() {
        getSharedPreferences("myefoss_prefs", MODE_PRIVATE).edit().putString("last_active_screen", "absences").apply()
        tabContainerPlanning.visibility = View.GONE
        tabContainerScolarity.visibility = View.GONE
        tabContainerSettings.visibility = View.GONE
        tabContainerGrades.visibility = View.GONE
        tabContainerCampus.visibility = View.GONE
        tabContainerLxp.visibility = View.GONE
        tabContainerAbsences.visibility = View.VISIBLE
        tvToolbarTitle.text = "Suivi des Absences"
        btnRefresh.visibility = View.GONE
        navigationDrawer.setCheckedItem(R.id.drawer_absences)

        loadStudentAbsences()
    }

    private fun setupThemeSettings() {
        val prefs = getSharedPreferences("myefoss_prefs", MODE_PRIVATE)
        val currentThemeMode = prefs.getInt("theme_mode", androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        val currentPalette = prefs.getString("theme_palette", "default") ?: "default"

        // 1. Initial State for Theme Mode
        when (currentThemeMode) {
            androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO -> rbThemeLight.isChecked = true
            androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES -> rbThemeDark.isChecked = true
            else -> rbThemeSystem.isChecked = true
        }

        // 2. Initial State for Color Palette
        val isMonetAvailable = DynamicColors.isDynamicColorAvailable()
        if (!isMonetAvailable) {
            rbPaletteMonet.visibility = View.GONE
            dividerMonet.visibility = View.GONE
        } else {
            rbPaletteMonet.visibility = View.VISIBLE
            dividerMonet.visibility = View.VISIBLE
        }

        when (currentPalette) {
            "monet" -> if (isMonetAvailable) rbPaletteMonet.isChecked = true else rbPaletteDefault.isChecked = true
            "emerald" -> rbPaletteEmerald.isChecked = true
            "purple" -> rbPalettePurple.isChecked = true
            "amber" -> rbPaletteAmber.isChecked = true
            else -> rbPaletteDefault.isChecked = true
        }

        // Theme Mode Change Listener
        rgThemeMode.setOnCheckedChangeListener { _, checkedId ->
            val targetMode = when (checkedId) {
                R.id.rbThemeLight -> androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO
                R.id.rbThemeDark -> androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES
                else -> androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            }

            prefs.edit()
                .putInt("theme_mode", targetMode)
                .putString("last_active_screen", "settings")
                .apply()

            androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(targetMode)
        }

        // Palette Change Listener
        rgPalette.setOnCheckedChangeListener { _, checkedId ->
            val targetPalette = when (checkedId) {
                R.id.rbPaletteMonet -> "monet"
                R.id.rbPaletteEmerald -> "emerald"
                R.id.rbPalettePurple -> "purple"
                R.id.rbPaletteAmber -> "amber"
                else -> "default"
            }

            if (targetPalette != currentPalette) {
                prefs.edit()
                    .putString("theme_palette", targetPalette)
                    .putString("last_active_screen", "settings")
                    .apply()

                recreate()
            }
        }

        // 3. Notifications: Course Changes & Session Expiration
        val isNotifEnabled = prefs.getBoolean("notify_course_changes", false)
        switchCourseNotifications.isChecked = isNotifEnabled

        val isSessionNotifEnabled = prefs.getBoolean("notify_session_expired", true)
        switchSessionNotifications.isChecked = isSessionNotifEnabled

        fun checkNotificationPermissionIfNeeded() {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 101)
                }
            }
        }

        switchCourseNotifications.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("notify_course_changes", isChecked).apply()

            if (isChecked) {
                checkNotificationPermissionIfNeeded()
                CourseSyncWorker.createNotificationChannel(this)
                scheduleCourseSyncWorker()
                Toast.makeText(this, "Notifications de cours activées", Toast.LENGTH_SHORT).show()
            } else {
                androidx.work.WorkManager.getInstance(this).cancelUniqueWork(CourseSyncWorker.WORK_NAME)
                Toast.makeText(this, "Notifications désactivées", Toast.LENGTH_SHORT).show()
            }
        }

        switchSessionNotifications.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("notify_session_expired", isChecked).apply()
            if (isChecked) {
                checkNotificationPermissionIfNeeded()
                SessionNotificationManager.createNotificationChannel(this)
                Toast.makeText(this, "Alerte de session expirée activée", Toast.LENGTH_SHORT).show()
            } else {
                SessionNotificationManager.cancelNotification(this)
                Toast.makeText(this, "Alerte de session désactivée", Toast.LENGTH_SHORT).show()
            }
        }

        // 4. OTA Update Check
        textAppVersion.text = "Version ${BuildConfig.VERSION_NAME}"
        btnCheckUpdates.setOnClickListener {
            btnCheckUpdates.isEnabled = false
            Toast.makeText(this, "Recherche de mises à jour...", Toast.LENGTH_SHORT).show()
            lifecycleScope.launch {
                val release = UpdateManager.checkLatestRelease()
                btnCheckUpdates.isEnabled = true
                if (release != null && UpdateManager.isNewerVersion(BuildConfig.VERSION_NAME, release.tagName)) {
                    val changelog = UpdateManager.fetchChangelogMarkdown(BuildConfig.VERSION_NAME, release.tagName)
                    UpdateManager.showUpdateDialog(this@MainActivity, release, changelog) {}
                } else if (release != null) {
                    Toast.makeText(this@MainActivity, "MyeFoss est à jour (${BuildConfig.VERSION_NAME})", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@MainActivity, "Impossible de vérifier les mises à jour", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun scheduleCourseSyncWorker() {
        val constraints = androidx.work.Constraints.Builder()
            .setRequiredNetworkType(androidx.work.NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()

        val syncRequest = androidx.work.PeriodicWorkRequestBuilder<CourseSyncWorker>(
            1, java.util.concurrent.TimeUnit.HOURS
        )
            .setConstraints(constraints)
            .build()

        androidx.work.WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            CourseSyncWorker.WORK_NAME,
            androidx.work.ExistingPeriodicWorkPolicy.KEEP,
            syncRequest
        )
    }

    private lateinit var tvGeneralAverage: TextView
    private lateinit var tvGradesSemesterLabel: TextView
    private lateinit var layoutGradesEmptyState: LinearLayout

    private var currentSelectedYear: String = ""
    private var lastSchoolYearsList: List<String> = emptyList()

    private fun setupSchoolYearSpinner(availableYears: List<String> = emptyList()) {
        val currentYear = getCurrentAcademicYear()
        val yearsList = if (availableYears.isNotEmpty()) {
            val list = availableYears.distinct().toMutableList()
            if (!list.contains(currentYear)) list.add(0, currentYear)
            list.sortedDescending()
        } else {
            val cal = Calendar.getInstance()
            val year = cal.get(Calendar.YEAR)
            val month = cal.get(Calendar.MONTH)
            val baseYear = if (month >= 8) year else year - 1
            val list = mutableListOf<String>()
            for (i in 0..3) {
                val y = baseYear - i
                list.add("$y-${y + 1}")
            }
            list
        }

        if (currentSelectedYear.isEmpty()) {
            currentSelectedYear = yearsList.firstOrNull() ?: currentYear
        }

        // Only recreate adapter if the year items list actually changed or spinner has no adapter
        if (spinnerSchoolYear.adapter == null || lastSchoolYearsList != yearsList) {
            lastSchoolYearsList = yearsList
            val spinnerAdapter = android.widget.ArrayAdapter(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                yearsList
            )
            spinnerSchoolYear.adapter = spinnerAdapter
            val targetIdx = yearsList.indexOf(currentSelectedYear).takeIf { it >= 0 } ?: 0
            spinnerSchoolYear.setSelection(targetIdx, false)

            spinnerSchoolYear.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                    val selected = yearsList[position]
                    if (selected != currentSelectedYear) {
                        currentSelectedYear = selected
                        loadStudentGrades(selected)
                    }
                }

                override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
            }
        } else {
            val targetIdx = yearsList.indexOf(currentSelectedYear).takeIf { it >= 0 } ?: 0
            if (spinnerSchoolYear.selectedItemPosition != targetIdx) {
                spinnerSchoolYear.setSelection(targetIdx, false)
            }
        }
    }

    private fun loadStudentGrades(schoolYear: String = getCurrentAcademicYear(), isSwipe: Boolean = false) {
        currentSelectedYear = schoolYear
        setupSchoolYearSpinner(cachedStudentPeriods.map { it.schoolYear })
        if (!isSwipe) layoutGradesList.removeAllViews()

        // 1. Load cached grades first for this specific year
        val cachedGrades = OfflineCacheManager.loadGrades(this, schoolYear)
        if (cachedGrades.isNotEmpty()) {
            displayGradesGroupedByUe(cachedGrades, schoolYear)
        } else if (!isSwipe) {
            displayGradesGroupedByUe(emptyList(), schoolYear)
        }

        if (isSwipe) {
            swipeRefreshGrades.isRefreshing = true
        }

        // 2. Fetch fresh grades asynchronously from official API
        lifecycleScope.launch {
            try {
                android.util.Log.d("MyeFossGrades", "Fetching grades for year: '$schoolYear'...")
                val freshGrades = withContext(Dispatchers.IO) {
                    fetchGradesFromApi(schoolYear)
                }
                android.util.Log.d("MyeFossGrades", "Received ${freshGrades.size} grades for year: '$schoolYear'")

                if (cachedStudentPeriods.isNotEmpty()) {
                    setupSchoolYearSpinner(cachedStudentPeriods.map { it.schoolYear })
                }

                if (freshGrades.isNotEmpty()) {
                    OfflineCacheManager.saveGrades(this@MainActivity, freshGrades, schoolYear)
                    displayGradesGroupedByUe(freshGrades, schoolYear)
                } else if (cachedGrades.isEmpty()) {
                    displayGradesGroupedByUe(emptyList(), schoolYear)
                }
            } catch (e: Exception) {
                android.util.Log.e("MyeFossGrades", "Error fetching grades: ${e.message}", e)
                if (cachedGrades.isEmpty()) {
                    displayGradesGroupedByUe(emptyList(), schoolYear)
                }
            } finally {
                swipeRefreshGrades.isRefreshing = false
            }
        }
    }

    private fun getCurrentAcademicYear(): String {
        val cal = Calendar.getInstance()
        val year = cal.get(Calendar.YEAR)
        val month = cal.get(Calendar.MONTH) // 0-indexed: 8 is September
        return if (month >= 8) {
            "$year-${year + 1}"
        } else {
            "${year - 1}-$year"
        }
    }

    private fun displayGradesGroupedByUe(grades: List<StudentGrade>, schoolYear: String) {
        layoutGradesList.removeAllViews()
        val inflater = LayoutInflater.from(this)

        tvGradesSemesterLabel.text = "Année académique $schoolYear"

        if (grades.isEmpty()) {
            tvGeneralAverage.text = "--"
            layoutGradesEmptyState.visibility = View.VISIBLE
            return
        }

        layoutGradesEmptyState.visibility = View.GONE

        // Calculate overall average across all valid numeric grades
        var totalPoints = 0.0
        var totalCount = 0
        grades.forEach { grade ->
            val numStr = grade.gradeValue.split("/").firstOrNull()?.trim()?.replace(",", ".")
            numStr?.toDoubleOrNull()?.let {
                totalPoints += it
                totalCount++
            }
        }

        if (totalCount > 0) {
            val avg = totalPoints / totalCount
            tvGeneralAverage.text = String.format(Locale.FRANCE, "%.1f", avg)
        } else {
            tvGeneralAverage.text = "--"
        }

        // Group by UE
        val groupedByUe = grades.groupBy { it.ue }

        groupedByUe.forEach { (ueName, ueGrades) ->
            val ueGroupView = inflater.inflate(R.layout.item_ue_group, layoutGradesList, false)
            val tvUeTitle: TextView = ueGroupView.findViewById(R.id.tvUeTitle)
            val tvUeAverageBadge: TextView = ueGroupView.findViewById(R.id.tvUeAverageBadge)
            val layoutUeGradesContainer: LinearLayout = ueGroupView.findViewById(R.id.layoutUeGradesContainer)

            tvUeTitle.text = ueName

            // Calculate UE sub-average
            var uePoints = 0.0
            var ueCount = 0
            ueGrades.forEach { g ->
                val numStr = g.gradeValue.split("/").firstOrNull()?.trim()?.replace(",", ".")
                numStr?.toDoubleOrNull()?.let {
                    uePoints += it
                    ueCount++
                }
            }

            if (ueCount > 0) {
                val ueAvg = uePoints / ueCount
                tvUeAverageBadge.visibility = View.VISIBLE
                tvUeAverageBadge.text = String.format(Locale.FRANCE, "Moy. %.1f", ueAvg)
            } else {
                tvUeAverageBadge.visibility = View.GONE
            }

            // Populate grade cards inside this UE container
            ueGrades.forEach { item ->
                val gradeCard = inflater.inflate(R.layout.item_grade_card, layoutUeGradesContainer, false)
                val tvName: TextView = gradeCard.findViewById(R.id.tvCourseName)
                val tvValue: TextView = gradeCard.findViewById(R.id.tvGradeValue)
                val tvDetails: TextView = gradeCard.findViewById(R.id.tvGradeDetails)
                val tvDate: TextView = gradeCard.findViewById(R.id.tvGradeDate)

                tvName.text = item.courseName
                tvValue.text = item.gradeValue
                tvDetails.text = item.details
                tvDate.text = item.date

                layoutUeGradesContainer.addView(gradeCard)
            }

            layoutGradesList.addView(ueGroupView)
        }
    }

    private var cachedStudentPeriods: List<StudentPeriod> = emptyList()

    private fun fetchStudentPeriodsFromServer(mergedCookies: String): List<StudentPeriod> {
        val periodsUrl = "https://www.myefrei.fr/api/rest/student/periods?withHistory=true"
        try {
            val conn = (URL(periodsUrl).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 10000
                readTimeout = 10000
                setRequestProperty("Accept", "application/json, text/plain, */*")
                setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 14; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36")
                setRequestProperty("Referer", "https://www.myefrei.fr/portal/student/grades")
                setRequestProperty("Origin", "https://www.myefrei.fr")
                if (mergedCookies.isNotBlank()) setRequestProperty("Cookie", mergedCookies)
            }
            if (conn.responseCode in 200..299) {
                val resp = conn.inputStream.bufferedReader().use { it.readText() }.trim()
                android.util.Log.d("MyeFossGrades", "Periods response: $resp")
                val foundList = mutableListOf<StudentPeriod>()
                val arr = if (resp.startsWith("[")) {
                    JSONArray(resp)
                } else if (resp.startsWith("{")) {
                    val root = JSONObject(resp)
                    root.optJSONArray("periods") ?: root.optJSONArray("data") ?: JSONArray()
                } else {
                    JSONArray()
                }

                for (i in 0 until arr.length()) {
                    val item = arr.optJSONObject(i) ?: continue
                    val sy = item.optString("schoolYear", item.optString("year", "")).trim()
                    val p = item.optString("period", item.optString("semester", "")).trim()
                    val prog = item.optString("programId", item.optString("program", "")).trim()
                    val parity = if (item.has("parity")) item.optString("parity") else null
                    val isCurrent = item.optBoolean("currentSchoolYear", false)
                    if (sy.isNotBlank()) {
                        foundList.add(StudentPeriod(sy, p, prog, parity, isCurrent))
                    }
                }
                if (foundList.isNotEmpty()) {
                    cachedStudentPeriods = foundList
                    OfflineCacheManager.saveStudentPeriods(this@MainActivity, foundList)
                }
                return foundList
            } else {
                android.util.Log.w("MyeFossGrades", "Periods failed with code: ${conn.responseCode}")
            }
        } catch (e: Exception) {
            android.util.Log.e("MyeFossGrades", "Periods fetch error: ${e.message}")
        }
        if (cachedStudentPeriods.isEmpty()) {
            cachedStudentPeriods = OfflineCacheManager.loadStudentPeriods(this@MainActivity)
        }
        return cachedStudentPeriods
    }

    private fun fetchGradesFromApi(schoolYear: String): List<StudentGrade> {
        val cookieManager = CookieManager.getInstance()
        val directCookies = cookieManager.getCookie("https://www.myefrei.fr/api/rest/student/grades") ?: ""
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

        // Fetch student periods to discover exact server format
        val periodsFromServer = fetchStudentPeriodsFromServer(mergedCookies)
        android.util.Log.d("MyeFossGrades", "Valid periods from server: $periodsFromServer")

        fun normalizeYear(y: String) = y.trim().replace("/", "-")
        val reqNormYear = normalizeYear(schoolYear)

        val endpointsToTry = mutableListOf<String>()

        // 1. Find periods matching the requested schoolYear (exact or normalized)
        val matchingPeriods = periodsFromServer.filter {
            it.schoolYear.equals(schoolYear, ignoreCase = true) ||
            normalizeYear(it.schoolYear) == reqNormYear
        }

        for (mp in matchingPeriods) {
            val encPeriod = URLEncoder.encode(mp.period, "UTF-8")
            val encProgram = URLEncoder.encode(mp.programId, "UTF-8")
            val encServerYear = URLEncoder.encode(mp.schoolYear, "UTF-8")
            if (mp.period.isNotBlank() && mp.programId.isNotBlank()) {
                endpointsToTry.add("https://www.myefrei.fr/api/rest/student/grades?period=$encPeriod&programId=$encProgram")
                endpointsToTry.add("https://www.myefrei.fr/api/rest/student/grades?schoolYear=$encServerYear&period=$encPeriod&programId=$encProgram")
            }
            if (mp.period.isNotBlank()) {
                endpointsToTry.add("https://www.myefrei.fr/api/rest/student/grades?period=$encPeriod")
            }
        }

        // 2. Direct schoolYear parameter (both original and slash/dash variants)
        endpointsToTry.add("https://www.myefrei.fr/api/rest/student/grades?schoolYear=${URLEncoder.encode(schoolYear, "UTF-8")}")
        endpointsToTry.add("https://www.myefrei.fr/api/rest/student/grades?schoolYear=${URLEncoder.encode(schoolYear.replace("-", "/"), "UTF-8")}")
        endpointsToTry.add("https://www.myefrei.fr/api/rest/student/grades?schoolYear=${URLEncoder.encode(schoolYear.replace("/", "-"), "UTF-8")}")

        // 3. Fallback: bare endpoint without query params ONLY for current academic year
        if (reqNormYear == normalizeYear(getCurrentAcademicYear())) {
            endpointsToTry.add("https://www.myefrei.fr/api/rest/student/grades")
        }

        for (urlStr in endpointsToTry) {
            try {
                val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 15000
                    readTimeout = 15000
                    setRequestProperty("Accept", "application/json, text/plain, */*")
                    setRequestProperty("Accept-Language", "fr-FR,fr;q=0.9,en-US;q=0.8,en;q=0.7")
                    setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 14; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36")
                    setRequestProperty("Referer", "https://www.myefrei.fr/portal/student/grades")
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
                    android.util.Log.d("MyeFossGrades", "Endpoint $urlStr SUCCESS HTTP $code. Length: ${response.length}")
                    val list = parseAnyGradesResponse(response)
                    if (list.isNotEmpty()) {
                        return list
                    }
                } else {
                    val errBody = conn.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                    android.util.Log.w("MyeFossGrades", "Endpoint $urlStr failed with HTTP $code: $errBody")
                }
            } catch (e: Exception) {
                android.util.Log.e("MyeFossGrades", "Endpoint $urlStr exception: ${e.message}")
            }
        }

        return emptyList()
    }

    private fun parseAnyGradesResponse(response: String): List<StudentGrade> {
        val list = mutableListOf<StudentGrade>()
        try {
            val trimmed = response.trim()
            if (trimmed.startsWith("[")) {
                val arr = JSONArray(trimmed)
                for (i in 0 until arr.length()) {
                    val item = arr.opt(i)
                    if (item is JSONObject) {
                        extractGradesRecursive(item, "Modules Généraux", list)
                    }
                }
            } else if (trimmed.startsWith("{")) {
                val root = JSONObject(trimmed)
                extractGradesRecursive(root, "Modules Généraux", list)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    private fun extractGradesRecursive(obj: JSONObject, currentUe: String, outList: MutableList<StudentGrade>) {
        // Detect if this object itself has a UE name
        val ueName = obj.optString("ue", obj.optString("ueName", obj.optString("unit", obj.optString("title", currentUe)))).ifBlank { currentUe }

        // 1. Direct grade check
        val courseName = obj.optString("courseName", obj.optString("course", obj.optString("module", obj.optString("subject", obj.optString("name", "")))))
        val gradeVal = obj.optString("grade", obj.optString("value", obj.optString("note", obj.optString("result", ""))))

        if (courseName.isNotBlank() && (gradeVal.isNotBlank() || obj.has("grade") || obj.has("value") || obj.has("note"))) {
            val formattedGrade = if (gradeVal.contains("/")) gradeVal else if (gradeVal.isNotBlank()) "$gradeVal / 20" else "-- / 20"
            val details = obj.optString("details", obj.optString("type", obj.optString("comment", "Évaluation")))
            val date = obj.optString("date", "")
            outList.add(
                StudentGrade(
                    courseName = courseName,
                    gradeValue = formattedGrade,
                    details = details,
                    date = date,
                    ue = ueName
                )
            )
        }

        // 2. Sub-arrays inspection (e.g. "grades", "modules", "courses", "evaluations", "results")
        val keys = obj.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            val child = obj.opt(key)
            if (child is JSONArray) {
                val derivedUe = if (key.contains("ue", ignoreCase = true) || ueName != "Modules Généraux") ueName else key
                for (i in 0 until child.length()) {
                    val childObj = child.optJSONObject(i) ?: continue
                    extractGradesRecursive(childObj, derivedUe, outList)
                }
            } else if (child is JSONObject && !key.equals("student", ignoreCase = true)) {
                extractGradesRecursive(child, ueName, outList)
            }
        }
    }

    private var currentSelectedAbsencesYear: String = ""
    private var lastAbsencesYearsList: List<String> = emptyList()

    private fun setupAbsencesSchoolYearSpinner(availableYears: List<String> = emptyList()) {
        val currentYear = getCurrentAcademicYear()
        val yearsList = if (availableYears.isNotEmpty()) {
            val list = availableYears.distinct().toMutableList()
            if (!list.contains(currentYear)) list.add(0, currentYear)
            list.sortedDescending()
        } else {
            val cal = Calendar.getInstance()
            val year = cal.get(Calendar.YEAR)
            val month = cal.get(Calendar.MONTH)
            val baseYear = if (month >= 8) year else year - 1
            val list = mutableListOf<String>()
            for (i in 0..3) {
                val y = baseYear - i
                list.add("$y-${y + 1}")
            }
            list
        }

        if (currentSelectedAbsencesYear.isEmpty()) {
            currentSelectedAbsencesYear = yearsList.firstOrNull() ?: currentYear
        }

        // Only recreate adapter if the year items list actually changed or spinner has no adapter
        if (spinnerAbsencesSchoolYear.adapter == null || lastAbsencesYearsList != yearsList) {
            lastAbsencesYearsList = yearsList
            val spinnerAdapter = android.widget.ArrayAdapter(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                yearsList
            )
            spinnerAbsencesSchoolYear.adapter = spinnerAdapter
            val targetIdx = yearsList.indexOf(currentSelectedAbsencesYear).takeIf { it >= 0 } ?: 0
            spinnerAbsencesSchoolYear.setSelection(targetIdx, false)

            spinnerAbsencesSchoolYear.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                    val selected = yearsList[position]
                    if (selected != currentSelectedAbsencesYear) {
                        currentSelectedAbsencesYear = selected
                        loadStudentAbsences(selected)
                    }
                }

                override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
            }
        } else {
            val targetIdx = yearsList.indexOf(currentSelectedAbsencesYear).takeIf { it >= 0 } ?: 0
            if (spinnerAbsencesSchoolYear.selectedItemPosition != targetIdx) {
                spinnerAbsencesSchoolYear.setSelection(targetIdx, false)
            }
        }
    }

    private fun loadStudentAbsences(schoolYear: String = getCurrentAcademicYear(), isSwipe: Boolean = false) {
        currentSelectedAbsencesYear = schoolYear
        setupAbsencesSchoolYearSpinner(cachedStudentPeriods.map { it.schoolYear })
        if (!isSwipe) layoutAbsencesList.removeAllViews()

        // 1. Load cached absences first
        val cached = OfflineCacheManager.loadAbsences(this, schoolYear)
        if (cached.isNotEmpty()) {
            displayAbsences(cached, schoolYear)
        } else if (!isSwipe) {
            displayAbsences(emptyList(), schoolYear)
        }

        if (isSwipe) {
            swipeRefreshAbsences.isRefreshing = true
        }

        // 2. Fetch fresh absences from server
        lifecycleScope.launch {
            try {
                android.util.Log.d("MyeFossAbsences", "Fetching absences for year: '$schoolYear'...")
                val fresh = withContext(Dispatchers.IO) {
                    fetchAbsencesFromApi(schoolYear)
                }
                android.util.Log.d("MyeFossAbsences", "Received ${fresh.size} absences for year: '$schoolYear'")

                if (cachedStudentPeriods.isNotEmpty()) {
                    setupAbsencesSchoolYearSpinner(cachedStudentPeriods.map { it.schoolYear })
                }

                OfflineCacheManager.saveAbsences(this@MainActivity, fresh, schoolYear)
                displayAbsences(fresh, schoolYear)
            } catch (e: Exception) {
                android.util.Log.e("MyeFossAbsences", "Error fetching absences: ${e.message}", e)
                if (cached.isEmpty()) {
                    displayAbsences(emptyList(), schoolYear)
                }
            } finally {
                swipeRefreshAbsences.isRefreshing = false
            }
        }
    }

    private fun displayAbsences(absences: List<StudentAbsence>, schoolYear: String) {
        layoutAbsencesList.removeAllViews()
        val inflater = LayoutInflater.from(this)

        tvAbsencesSummaryLabel.text = "Année académique $schoolYear"

        val justifiedList = absences.filter { it.justified }
        val unjustifiedList = absences.filter { !it.justified }

        val justifiedCount = justifiedList.size
        val unjustifiedCount = unjustifiedList.size

        tvAbsencesJustifiedCount.text = "$justifiedCount ${if (justifiedCount > 1) "justifiées" else "justifiée"}"
        tvAbsencesUnjustifiedCount.text = "$unjustifiedCount ${if (unjustifiedCount > 1) "injustifiées" else "injustifiée"}"

        if (absences.isEmpty()) {
            tvAbsencesTotalHours.text = "0"
            layoutAbsencesEmptyState.visibility = View.VISIBLE
            return
        }

        layoutAbsencesEmptyState.visibility = View.GONE
        tvAbsencesTotalHours.text = absences.size.toString()

        absences.forEach { abs ->
            val view = inflater.inflate(R.layout.item_absence_card, layoutAbsencesList, false)
            val tvSubject: TextView = view.findViewById(R.id.tvAbsenceSubject)
            val tvBadge: TextView = view.findViewById(R.id.tvAbsenceJustifiedBadge)
            val tvDate: TextView = view.findViewById(R.id.tvAbsenceDate)
            val tvDuration: TextView = view.findViewById(R.id.tvAbsenceDuration)
            val tvType: TextView = view.findViewById(R.id.tvAbsenceType)
            val tvReason: TextView = view.findViewById(R.id.tvAbsenceReason)

            tvSubject.text = abs.courseName
            if (abs.date.isNotBlank() && !abs.date.equals("Date inconnue", ignoreCase = true)) {
                tvDate.text = abs.date
                tvDate.visibility = View.VISIBLE
            } else {
                tvDate.visibility = View.GONE
            }

            if (abs.hours.isNotBlank() && !abs.hours.equals("Durée inconnue", ignoreCase = true)) {
                tvDuration.text = if (tvDate.visibility == View.VISIBLE) "• ${abs.hours}" else abs.hours
                tvDuration.visibility = View.VISIBLE
            } else {
                tvDuration.visibility = View.GONE
            }

            val hasPreceding = tvDate.visibility == View.VISIBLE || tvDuration.visibility == View.VISIBLE
            if (!abs.type.isNullOrBlank()) {
                tvType.text = if (hasPreceding) "• ${abs.type}" else abs.type
                tvType.visibility = View.VISIBLE
            } else {
                tvType.visibility = View.GONE
            }

            if (abs.justified) {
                tvBadge.text = "Justifiée"
                tvBadge.setTextColor(com.google.android.material.color.MaterialColors.getColor(this, com.google.android.material.R.attr.colorPrimary, android.graphics.Color.BLUE))
            } else {
                tvBadge.text = "Non justifiée"
                tvBadge.setTextColor(com.google.android.material.color.MaterialColors.getColor(this, com.google.android.material.R.attr.colorError, android.graphics.Color.RED))
            }

            if (!abs.reason.isNullOrBlank()) {
                tvReason.visibility = View.VISIBLE
                tvReason.text = "Motif : ${abs.reason}"
            } else {
                tvReason.visibility = View.GONE
            }

            layoutAbsencesList.addView(view)
        }
    }

    private fun fetchAbsencesFromApi(schoolYear: String): List<StudentAbsence> {
        val cookieManager = CookieManager.getInstance()
        val directCookies = cookieManager.getCookie("https://www.myefrei.fr/api/rest/student/absences") ?: ""
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

        val periodsFromServer = fetchStudentPeriodsFromServer(mergedCookies)

        fun normalizeYear(y: String) = y.trim().replace("/", "-")
        val reqNormYear = normalizeYear(schoolYear)

        val endpointsToTry = mutableListOf<String>()

        // 1. Find periods matching the requested schoolYear (exact or normalized)
        val matchingPeriods = periodsFromServer.filter {
            it.schoolYear.equals(schoolYear, ignoreCase = true) ||
            normalizeYear(it.schoolYear) == reqNormYear
        }

        for (mp in matchingPeriods) {
            val encPeriod = URLEncoder.encode(mp.period, "UTF-8")
            val encProgram = URLEncoder.encode(mp.programId, "UTF-8")
            val encServerYear = URLEncoder.encode(mp.schoolYear, "UTF-8")
            if (mp.period.isNotBlank() && mp.programId.isNotBlank()) {
                endpointsToTry.add("https://www.myefrei.fr/api/rest/student/absences?schoolYear=$encServerYear&period=$encPeriod&programId=$encProgram")
                endpointsToTry.add("https://www.myefrei.fr/api/rest/student/absences?period=$encPeriod&programId=$encProgram")
            }
            if (mp.period.isNotBlank()) {
                endpointsToTry.add("https://www.myefrei.fr/api/rest/student/absences?period=$encPeriod")
            }
        }

        // 2. Direct schoolYear parameter (both original and slash/dash variants)
        endpointsToTry.add("https://www.myefrei.fr/api/rest/student/absences?schoolYear=${URLEncoder.encode(schoolYear, "UTF-8")}")
        endpointsToTry.add("https://www.myefrei.fr/api/rest/student/absences?schoolYear=${URLEncoder.encode(schoolYear.replace("-", "/"), "UTF-8")}")
        endpointsToTry.add("https://www.myefrei.fr/api/rest/student/absences?schoolYear=${URLEncoder.encode(schoolYear.replace("/", "-"), "UTF-8")}")

        // 3. Fallback: bare endpoint without query params ONLY for current academic year
        if (reqNormYear == normalizeYear(getCurrentAcademicYear())) {
            endpointsToTry.add("https://www.myefrei.fr/api/rest/student/absences")
        }

        for (urlStr in endpointsToTry) {
            try {
                val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 15000
                    readTimeout = 15000
                    setRequestProperty("Accept", "application/json, text/plain, */*")
                    setRequestProperty("Accept-Language", "fr-FR,fr;q=0.9,en-US;q=0.8,en;q=0.7")
                    setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 14; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36")
                    setRequestProperty("Referer", "https://www.myefrei.fr/portal/student/absences")
                    setRequestProperty("Origin", "https://www.myefrei.fr")
                    setRequestProperty("Sec-Fetch-Dest", "empty")
                    setRequestProperty("Sec-Fetch-Mode", "cors")
                    setRequestProperty("Sec-Fetch-Site", "same-origin")
                    if (mergedCookies.isNotBlank()) setRequestProperty("Cookie", mergedCookies)
                }

                val code = conn.responseCode
                if (code in 200..299) {
                    val response = conn.inputStream.bufferedReader().use { it.readText() }
                    android.util.Log.d("MyeFossAbsences", "Endpoint $urlStr SUCCESS HTTP $code. Length: ${response.length}")
                    android.util.Log.i("MyeFossAbsences", "Raw absences response: $response")
                    val list = parseAnyAbsencesResponse(response)
                    if (list.isNotEmpty()) {
                        return list
                    }
                } else {
                    val errBody = conn.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                    android.util.Log.w("MyeFossAbsences", "Endpoint $urlStr failed with HTTP $code: $errBody")
                }
            } catch (e: Exception) {
                android.util.Log.e("MyeFossAbsences", "Endpoint $urlStr exception: ${e.message}")
            }
        }

        return emptyList()
    }

    private fun parseAnyAbsencesResponse(response: String): List<StudentAbsence> {
        val list = mutableListOf<StudentAbsence>()
        try {
            val trimmed = response.trim()
            if (trimmed.startsWith("[")) {
                val arr = JSONArray(trimmed)
                for (i in 0 until arr.length()) {
                    val item = arr.optJSONObject(i) ?: continue
                    extractAbsencesRecursive(item, list)
                }
            } else if (trimmed.startsWith("{")) {
                val root = JSONObject(trimmed)
                extractAbsencesRecursive(root, list)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    private fun extractAbsencesRecursive(obj: JSONObject, outList: MutableList<StudentAbsence>) {
        val courseName = obj.optString("courseName").ifBlank {
            obj.optString("subject").ifBlank {
                obj.optString("subjectName").ifBlank {
                    obj.optString("module").ifBlank {
                        obj.optString("moduleName").ifBlank {
                            obj.optString("course").ifBlank {
                                obj.optString("name").ifBlank {
                                    obj.optString("title").ifBlank {
                                        obj.optString("label").ifBlank {
                                            obj.optString("matiere").ifBlank {
                                                obj.optJSONObject("course")?.optString("name")?.ifBlank { null }
                                                    ?: obj.optJSONObject("course")?.optString("title")?.ifBlank { null }
                                                    ?: obj.optJSONObject("subject")?.optString("name")?.ifBlank { null }
                                                    ?: obj.optJSONObject("module")?.optString("name") ?: ""
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        val sessionObj = obj.optJSONObject("session") ?: obj.optJSONObject("creneau") ?: obj.optJSONObject("slot") ?: obj.optJSONObject("cours")

        val rawDate = obj.optString("date").ifBlank {
            obj.optString("startDate").ifBlank {
                obj.optString("start").ifBlank {
                    obj.optString("dateSession").ifBlank {
                        obj.optString("sessionDate").ifBlank {
                            obj.optString("dateDebut").ifBlank {
                                obj.optString("debut").ifBlank {
                                    obj.optString("day").ifBlank {
                                        obj.optString("createdAt").ifBlank {
                                            sessionObj?.optString("date")?.ifBlank { null }
                                                ?: sessionObj?.optString("startDate")?.ifBlank { null }
                                                ?: sessionObj?.optString("start")?.ifBlank { null }
                                                ?: sessionObj?.optString("dateSession") ?: ""
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        val hasDuration = obj.has("duration") || obj.has("duree") || obj.has("hours") || obj.has("nbHours") ||
                obj.has("nbHeures") || obj.has("volume") || obj.has("totalHours") || obj.has("creneau") ||
                obj.has("durationInMinutes") || obj.has("lateDurationInMinutes") ||
                (sessionObj != null && (sessionObj.has("duration") || sessionObj.has("duree") || sessionObj.has("hours")))

        val hasAbsenceMarkers = obj.has("justified") || obj.has("isJustified") || obj.has("justifie") ||
                obj.has("motif") || obj.has("reason") || obj.has("late") || obj.has("retard") ||
                obj.has("absenceType") || obj.has("sessionType") || obj.has("missed")

        if (courseName.isNotBlank() && (rawDate.isNotBlank() || hasDuration || hasAbsenceMarkers)) {
            fun parseDateToFrenchString(dStr: String): String {
                if (dStr.isBlank()) return ""
                // Try timestamp in milliseconds
                val timestamp = dStr.toLongOrNull()
                if (timestamp != null && timestamp > 100000000000L) {
                    val outFmt = SimpleDateFormat("dd MMM yyyy", Locale.FRANCE)
                    return outFmt.format(Date(timestamp))
                }
                // Try ISO 8601 strings (e.g. 2024-10-12 or 2024-10-12T08:30:00.000Z)
                if (dStr.length >= 10 && (dStr[4] == '-' || dStr[4] == '/')) {
                    val cleanDate = dStr.substring(0, 10).replace('/', '-')
                    try {
                        val inFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                        val outFmt = SimpleDateFormat("dd MMM yyyy", Locale.FRANCE)
                        val d = inFmt.parse(cleanDate)
                        if (d != null) return outFmt.format(d)
                    } catch (e: Exception) {}
                }
                // Try dd/MM/yyyy format
                if (dStr.length >= 10 && (dStr[2] == '/' || dStr[2] == '-')) {
                    val cleanDate = dStr.substring(0, 10).replace('-', '/')
                    try {
                        val inFmt = SimpleDateFormat("dd/MM/yyyy", Locale.FRANCE)
                        val outFmt = SimpleDateFormat("dd MMM yyyy", Locale.FRANCE)
                        val d = inFmt.parse(cleanDate)
                        if (d != null) return outFmt.format(d)
                    } catch (e: Exception) {}
                }
                return dStr
            }

            fun formatMinutes(totalM: Int): String {
                val h = totalM / 60
                val m = totalM % 60
                return if (m > 0) "${h}h${String.format(Locale.FRANCE, "%02d", m)}" else "${h}h"
            }

            fun formatDoubleHours(dh: Double): String {
                val totalM = (dh * 60.0).toInt()
                return formatMinutes(totalM)
            }

            fun parseTimeToMinutes(tStr: String): Int? {
                val clean = tStr.trim().lowercase().replace('h', ':')
                val parts = clean.split(':')
                if (parts.size >= 2) {
                    val h = parts[0].toIntOrNull() ?: return null
                    val m = parts[1].toIntOrNull() ?: 0
                    return (h * 60) + m
                }
                return null
            }

            val dateStr = parseDateToFrenchString(rawDate)

            // Duration extraction with variable duration priority
            var hoursStr = ""

            // 1. Check direct minutes numeric fields
            val durationMin = when {
                obj.has("durationInMinutes") -> obj.optInt("durationInMinutes", -1)
                obj.has("durationMinutes") -> obj.optInt("durationMinutes", -1)
                obj.has("nbMinutes") -> obj.optInt("nbMinutes", -1)
                obj.has("dureeMinutes") -> obj.optInt("dureeMinutes", -1)
                obj.has("lateDurationInMinutes") -> obj.optInt("lateDurationInMinutes", -1)
                obj.has("lateMinutes") -> obj.optInt("lateMinutes", -1)
                sessionObj?.has("durationInMinutes") == true -> sessionObj.optInt("durationInMinutes", -1)
                sessionObj?.has("durationMinutes") == true -> sessionObj.optInt("durationMinutes", -1)
                else -> -1
            }
            if (durationMin > 0) {
                hoursStr = formatMinutes(durationMin)
            }

            // 2. Check numeric duration or hours (e.g. 1.5, 3.5, 2)
            if (hoursStr.isBlank()) {
                val doubleH = when {
                    obj.has("duration") && obj.optDouble("duration", -1.0) > 0 -> obj.optDouble("duration", -1.0)
                    obj.has("duree") && obj.optDouble("duree", -1.0) > 0 -> obj.optDouble("duree", -1.0)
                    obj.has("hours") && obj.optDouble("hours", -1.0) > 0 -> obj.optDouble("hours", -1.0)
                    obj.has("nbHours") && obj.optDouble("nbHours", -1.0) > 0 -> obj.optDouble("nbHours", -1.0)
                    obj.has("nbHeures") && obj.optDouble("nbHeures", -1.0) > 0 -> obj.optDouble("nbHeures", -1.0)
                    obj.has("totalHours") && obj.optDouble("totalHours", -1.0) > 0 -> obj.optDouble("totalHours", -1.0)
                    sessionObj?.has("duration") == true && sessionObj.optDouble("duration", -1.0) > 0 -> sessionObj.optDouble("duration", -1.0)
                    sessionObj?.has("duree") == true && sessionObj.optDouble("duree", -1.0) > 0 -> sessionObj.optDouble("duree", -1.0)
                    sessionObj?.has("hours") == true && sessionObj.optDouble("hours", -1.0) > 0 -> sessionObj.optDouble("hours", -1.0)
                    else -> -1.0
                }
                if (doubleH > 0) {
                    hoursStr = formatDoubleHours(doubleH)
                }
            }

            // 3. Check string hours/duration (e.g. "1h30", "03:30", "2.0")
            if (hoursStr.isBlank()) {
                val rawHours = obj.optString("hours").ifBlank {
                    obj.optString("duration").ifBlank {
                        obj.optString("duree").ifBlank {
                            obj.optString("nbHours").ifBlank {
                                obj.optString("nbHeures").ifBlank {
                                    obj.optString("totalHours").ifBlank {
                                        sessionObj?.optString("hours")?.ifBlank { null }
                                            ?: sessionObj?.optString("duration")?.ifBlank { null }
                                            ?: sessionObj?.optString("duree") ?: ""
                                    }
                                }
                            }
                        }
                    }
                }.trim()
                if (rawHours.isNotBlank()) {
                    val rawDouble = rawHours.toDoubleOrNull()
                    if (rawDouble != null && rawDouble > 0) {
                        hoursStr = formatDoubleHours(rawDouble)
                    } else {
                        val mParsed = parseTimeToMinutes(rawHours)
                        if (mParsed != null && mParsed > 0) {
                            hoursStr = formatMinutes(mParsed)
                        } else {
                            hoursStr = rawHours
                        }
                    }
                }
            }

            // 4. Calculate duration from startTime and endTime (e.g. "08:30" and "12:00")
            if (hoursStr.isBlank()) {
                val startTime = obj.optString("startTime").ifBlank {
                    obj.optString("startHour").ifBlank {
                        obj.optString("heureDebut").ifBlank {
                            sessionObj?.optString("startTime")?.ifBlank { null }
                                ?: sessionObj?.optString("heureDebut") ?: ""
                        }
                    }
                }
                val endTime = obj.optString("endTime").ifBlank {
                    obj.optString("endHour").ifBlank {
                        obj.optString("heureFin").ifBlank {
                            sessionObj?.optString("endTime")?.ifBlank { null }
                                ?: sessionObj?.optString("heureFin") ?: ""
                        }
                    }
                }
                val startM = parseTimeToMinutes(startTime)
                val endM = parseTimeToMinutes(endTime)
                if (startM != null && endM != null && endM > startM) {
                    hoursStr = formatMinutes(endM - startM)
                }
            }

            // 5. Calculate duration from ISO start and end (e.g. "2024-10-12T08:30:00Z" and "2024-10-12T12:00:00Z")
            if (hoursStr.isBlank()) {
                val startIso = obj.optString("start").ifBlank {
                    obj.optString("startDate").ifBlank {
                        sessionObj?.optString("start")?.ifBlank { null }
                            ?: sessionObj?.optString("startDate") ?: ""
                    }
                }
                val endIso = obj.optString("end").ifBlank {
                    obj.optString("endDate").ifBlank {
                        sessionObj?.optString("end")?.ifBlank { null }
                            ?: sessionObj?.optString("endDate") ?: ""
                    }
                }
                if (startIso.length >= 19 && endIso.length >= 19) {
                    try {
                        val isoFmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply {
                            timeZone = TimeZone.getTimeZone("UTC")
                        }
                        val d1 = isoFmt.parse(startIso.substring(0, 19))
                        val d2 = isoFmt.parse(endIso.substring(0, 19))
                        if (d1 != null && d2 != null && d2.time > d1.time) {
                            val diffM = ((d2.time - d1.time) / (1000 * 60)).toInt()
                            hoursStr = formatMinutes(diffM)
                        }
                    } catch (e: Exception) {}
                }
            }

            val isJustified = obj.optBoolean("justified",
                obj.optBoolean("isJustified",
                obj.optBoolean("justifie",
                obj.optString("status", "").equals("justified", ignoreCase = true) ||
                obj.optString("justificationStatus", "").equals("justified", ignoreCase = true))))

            val reason = obj.optString("reason",
                obj.optString("motif",
                obj.optString("comment",
                obj.optString("description",
                obj.optString("justification", "")))))

            val type = obj.optString("type",
                obj.optString("absenceType",
                obj.optString("sessionType",
                obj.optString("activity", ""))))

            val absenceItem = StudentAbsence(
                id = obj.optString("id", obj.optString("_id", outList.size.toString())),
                courseName = courseName,
                date = dateStr,
                hours = hoursStr,
                justified = isJustified,
                type = type.ifBlank { null },
                reason = reason.ifBlank { null }
            )
            android.util.Log.d("MyeFossAbsences", "Extracted: course='$courseName', date='$dateStr' (raw='$rawDate'), duration='$hoursStr', justified=$isJustified, rawKeys=${obj.keys().asSequence().toList()}")
            outList.add(absenceItem)
        }

        val keys = obj.keys()
        while (keys.hasNext()) {
            val k = keys.next()
            val child = obj.opt(k)
            if (child is JSONArray) {
                for (i in 0 until child.length()) {
                    val cObj = child.optJSONObject(i) ?: continue
                    extractAbsencesRecursive(cObj, outList)
                }
            } else if (child is JSONObject && !k.equals("student", ignoreCase = true)) {
                extractAbsencesRecursive(child, outList)
            }
        }
    }

    // ─────────────────────────────────────────────────────────────
    // CAMPUS HUB & NAVIGATION
    // ─────────────────────────────────────────────────────────────

    private fun getDefaultCampuses(): List<CampusInfo> {
        return listOf(
            // Campus Bordeaux
            CampusInfo(
                id = "bordeaux_main",
                name = "Campus Bordeaux",
                city = "Bordeaux",
                address = "83 Rue Lucien Faure\n33000 Bordeaux",
                postalCode = "33000",
                campusGroup = "Bordeaux",
                sitesDescription = "Site Bassins à flot (Bâtiment BDX)",
                latitude = 44.861783,
                longitude = -0.555776,
                phone = null,
                caretakerPhone = null,
                hours = null,
                email = null,
                accessTransport = "Tram B (Arrêt Rue Achard ou Cité du Vin), Bus 7, 32",
                description = null
            ),
            // Campus Paris - Site La Maison
            CampusInfo(
                id = "paris_maison",
                name = "La Maison (Bâtiments A & B)",
                city = "Villejuif",
                address = "30-32 Avenue de la République\n94800 Villejuif",
                postalCode = "94800",
                campusGroup = "Paris",
                sitesDescription = "Bâtiments A & B",
                latitude = 48.788591,
                longitude = 2.363765,
                phone = "01 88 28 90 00",
                caretakerPhone = "01 88 28 90 01",
                hours = "Lundi au Vendredi : 07h30 - 21h00\nSamedi : 08h00 - 13h00",
                email = null,
                accessTransport = "Métro 7 (Villejuif Louis Aragon), Tramway T7, Bus 131, 172",
                description = null
            ),
            // Campus Paris - Site L'Aquarium
            CampusInfo(
                id = "paris_aquarium",
                name = "L'Aquarium",
                city = "Villejuif",
                address = "136 bis Boulevard Maxime Gorki\n94800 Villejuif",
                postalCode = "94800",
                campusGroup = "Paris",
                sitesDescription = "Bâtiment Aquarium",
                latitude = 48.785420,
                longitude = 2.365310,
                phone = "01 88 28 90 00",
                caretakerPhone = "01 88 28 90 01",
                hours = "Lundi au Vendredi : 07h30 - 20h00",
                email = null,
                accessTransport = "Métro 7 (Villejuif Paul Vaillant-Couturier ou Louis Aragon), Bus 131, 162",
                description = null
            ),
            // Campus Paris - Site La Factory
            CampusInfo(
                id = "paris_factory",
                name = "La Factory",
                city = "Villejuif",
                address = "143 Boulevard Maxime Gorki\n94800 Villejuif",
                postalCode = "94800",
                campusGroup = "Paris",
                sitesDescription = "Bâtiment Factory",
                latitude = 48.784950,
                longitude = 2.365820,
                phone = "01 88 28 90 00",
                caretakerPhone = "01 88 28 90 01",
                hours = "Lundi au Vendredi : 08h00 - 20h00",
                email = null,
                accessTransport = "Métro 7 (Villejuif Paul Vaillant-Couturier ou Louis Aragon)",
                description = null
            ),
            // Campus Paris - Site New Republic
            CampusInfo(
                id = "paris_new_republic",
                name = "New Republic",
                city = "Villejuif",
                address = "11 Avenue de la République\n94800 Villejuif",
                postalCode = "94800",
                campusGroup = "Paris",
                sitesDescription = "Bâtiment New Republic",
                latitude = 48.790120,
                longitude = 2.362140,
                phone = "01 88 28 90 00",
                caretakerPhone = "01 88 28 90 01",
                hours = "Lundi au Vendredi : 08h00 - 19h30",
                email = null,
                accessTransport = "Métro 7 (Villejuif Louis Aragon), Bus 131, 172",
                description = null
            )
        )
    }

    private fun loadCampusesList(isSwipe: Boolean = false) {
        if (!isSwipe) layoutCampusList.removeAllViews()

        // 1. Load cached campuses (fallback to default official campuses if empty)
        val cached = OfflineCacheManager.loadCampuses(this)
        val initialList = if (cached.isNotEmpty()) cached else getDefaultCampuses()
        displayCampuses(initialList)

        if (isSwipe) {
            swipeRefreshCampus.isRefreshing = true
        }

        // 2. Fetch fresh campuses from API in background if possible
        lifecycleScope.launch {
            try {
                val fresh = withContext(Dispatchers.IO) {
                    fetchCampusesFromApi()
                }
                if (fresh.isNotEmpty()) {
                    OfflineCacheManager.saveCampuses(this@MainActivity, fresh)
                    displayCampuses(fresh)
                }
            } catch (e: Exception) {
                android.util.Log.e("MyeFossCampus", "Error fetching campus info: ${e.message}", e)
            } finally {
                swipeRefreshCampus.isRefreshing = false
            }
        }
    }

    private fun displayCampuses(campuses: List<CampusInfo>) {
        layoutCampusList.removeAllViews()
        val inflater = LayoutInflater.from(this)

        val groups = campuses.groupBy { it.campusGroup }

        // Desired display order: Campus Bordeaux first, then Campus Paris
        val orderedGroupKeys = listOf("Bordeaux", "Paris") + groups.keys.filter { it != "Bordeaux" && it != "Paris" }

        for (groupName in orderedGroupKeys) {
            val groupCampuses = groups[groupName] ?: continue
            if (groupCampuses.isEmpty()) continue

            // Section Header for each Campus Group (e.g. "Campus Bordeaux", "Campus Paris")
            val headerTv = TextView(this).apply {
                text = "Campus $groupName"
                textSize = 15f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                val typedValue = android.util.TypedValue()
                if (theme.resolveAttribute(com.google.android.material.R.attr.colorPrimary, typedValue, true)) {
                    setTextColor(typedValue.data)
                }
                setPadding(4, 16, 4, 10)
            }
            layoutCampusList.addView(headerTv)

            groupCampuses.forEach { campus ->
                val view = inflater.inflate(R.layout.item_campus_card, layoutCampusList, false)
                val tvName: TextView = view.findViewById(R.id.tvCampusName)
                val tvAddress: TextView = view.findViewById(R.id.tvCampusAddress)
                val btnNavigate: MaterialButton = view.findViewById(R.id.btnNavigateCampus)

                tvName.text = campus.name
                tvAddress.text = campus.address.replace("\n", ", ")

                // Clicking the tile opens bottom sheet with complete details (caretaker, phone, hours, access)
                view.setOnClickListener {
                    CampusDetailsBottomSheet.newInstance(campus) { selectedCampus ->
                        openNavigationForCampus(selectedCampus)
                    }.show(supportFragmentManager, "CampusDetails_${campus.id}")
                }

                // Clicking "Y aller" triggers navigation app chooser directly
                btnNavigate.setOnClickListener {
                    openNavigationForCampus(campus)
                }

                layoutCampusList.addView(view)
            }
        }
    }

    private fun openNavigationForCampus(campus: CampusInfo) {
        try {
            val cleanAddr = campus.address.replace("\n", ", ")
            val label = "${campus.name}, $cleanAddr"
            val encAddr = URLEncoder.encode(label, "UTF-8")

            // Prefer coordinates if available, query label as destination
            val geoUri = if (campus.latitude != null && campus.longitude != null) {
                Uri.parse("geo:${campus.latitude},${campus.longitude}?q=${campus.latitude},${campus.longitude}($encAddr)")
            } else {
                Uri.parse("geo:0,0?q=$encAddr")
            }

            // Create ACTION_VIEW intent for map applications
            val mapIntent = Intent(Intent.ACTION_VIEW, geoUri)

            // Let the user choose between installed navigation apps (Google Maps, Waze, Citymapper, OsmAnd, etc.)
            val chooserTitle = "Choisir une application de navigation"
            val chooserIntent = Intent.createChooser(mapIntent, chooserTitle).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            if (mapIntent.resolveActivity(packageManager) != null) {
                startActivity(chooserIntent)
            } else {
                // Fallback to web browser (Google Maps web search)
                val webMapsUrl = if (campus.latitude != null && campus.longitude != null) {
                    "https://www.google.com/maps/search/?api=1&query=${campus.latitude},${campus.longitude}"
                } else {
                    "https://www.google.com/maps/search/?api=1&query=$encAddr"
                }
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(webMapsUrl)))
            }
        } catch (e: Exception) {
            android.util.Log.e("MyeFossCampus", "Error launching navigation app: ${e.message}", e)
            Toast.makeText(this, "Impossible d'ouvrir une application de cartographie", Toast.LENGTH_SHORT).show()
        }
    }

    private fun fetchCampusesFromApi(): List<CampusInfo> {
        val cookieManager = CookieManager.getInstance()
        val directCookies = cookieManager.getCookie("https://www.myefrei.fr/api/rest/student/campus") ?: ""
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

        val urlsToTry = listOf(
            "https://www.myefrei.fr/api/rest/student/campus",
            "https://www.myefrei.fr/api/rest/campus"
        )

        for (urlStr in urlsToTry) {
            try {
                val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 10000
                    readTimeout = 10000
                    setRequestProperty("Accept", "application/json, text/plain, */*")
                    setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 14; Pixel 7) AppleWebKit/537.36")
                    if (mergedCookies.isNotBlank()) setRequestProperty("Cookie", mergedCookies)
                }

                val code = conn.responseCode
                if (code in 200..299) {
                    val response = conn.inputStream.bufferedReader().use { it.readText() }
                    android.util.Log.d("MyeFossCampus", "Campus response ($code): $response")
                    val parsed = parseCampusesResponse(response)
                    if (parsed.isNotEmpty()) {
                        return parsed
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("MyeFossCampus", "Exception querying campus api $urlStr: ${e.message}")
            }
        }

        return emptyList()
    }

    private fun parseCampusesResponse(response: String): List<CampusInfo> {
        val list = mutableListOf<CampusInfo>()
        try {
            val trimmed = response.trim()
            val jsonArray = if (trimmed.startsWith("[")) {
                JSONArray(trimmed)
            } else if (trimmed.startsWith("{")) {
                val root = JSONObject(trimmed)
                root.optJSONArray("campuses") ?: root.optJSONArray("data") ?: root.optJSONArray("items")
            } else null

            if (jsonArray != null) {
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.optJSONObject(i) ?: continue
                    val name = obj.optString("name", obj.optString("campusName", obj.optString("title", "")))
                    val city = obj.optString("city", obj.optString("ville", ""))
                    val address = obj.optString("address", obj.optString("adresse", obj.optString("street", "")))
                    val postalCode = obj.optString("postalCode", obj.optString("zipCode", obj.optString("codePostal", "")))
                    val sites = obj.optString("sites", obj.optString("description", obj.optString("sitesDescription", "")))
                    val phone = obj.optString("phone", obj.optString("telephone", ""))
                    val caretakerPhone = obj.optString("caretakerPhone", obj.optString("gardien", ""))
                    val hours = obj.optString("hours", obj.optString("horaires", ""))
                    val access = obj.optString("access", obj.optString("transports", ""))
                    val group = obj.optString("group", obj.optString("campusGroup", if (city.contains("Bordeaux", ignoreCase = true) || name.contains("Bordeaux", ignoreCase = true)) "Bordeaux" else "Paris"))

                    val lat = when {
                        obj.has("latitude") -> obj.optDouble("latitude")
                        obj.has("lat") -> obj.optDouble("lat")
                        else -> null
                    }
                    val lon = when {
                        obj.has("longitude") -> obj.optDouble("longitude")
                        obj.has("lng") || obj.has("lon") -> obj.optDouble(if (obj.has("lng")) "lng" else "lon")
                        else -> null
                    }

                    if (name.isNotBlank() && (address.isNotBlank() || city.isNotBlank())) {
                        list.add(
                            CampusInfo(
                                id = obj.optString("id", i.toString()),
                                name = name,
                                city = city,
                                address = address,
                                postalCode = postalCode.ifBlank { null },
                                sitesDescription = sites.ifBlank { null },
                                latitude = lat,
                                longitude = lon,
                                campusGroup = group,
                                phone = phone.ifBlank { null },
                                caretakerPhone = caretakerPhone.ifBlank { null },
                                hours = hours.ifBlank { null },
                                accessTransport = access.ifBlank { null }
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    private fun loadLxpActions(isSwipe: Boolean = false) {
        if (!isSwipe) {
            layoutLxpList.removeAllViews()
            layoutLxpLoading.visibility = View.VISIBLE
            layoutLxpEmpty.visibility = View.GONE
        }

        // 1. Load cached actions
        val cached = OfflineCacheManager.loadLxpActions(this)
        if (cached.isNotEmpty()) {
            displayLxpActions(cached)
            layoutLxpLoading.visibility = View.GONE
        }

        if (isSwipe) {
            swipeRefreshLxp.isRefreshing = true
        }

        // 2. Fetch fresh actions in background
        lifecycleScope.launch {
            try {
                val fresh = fetchLxpActionsFromApi()
                layoutLxpLoading.visibility = View.GONE
                if (fresh.isNotEmpty()) {
                    showSessionExpiredBanner(false)
                    OfflineCacheManager.saveLxpActions(this@MainActivity, fresh)
                    displayLxpActions(fresh)
                } else if (cached.isEmpty()) {
                    layoutLxpEmpty.visibility = View.VISIBLE
                }
            } catch (e: Exception) {
                if (e.message?.contains("401") == true || e.message?.contains("403") == true) {
                    showSessionExpiredBanner(true)
                }
                layoutLxpLoading.visibility = View.GONE
                if (cached.isEmpty()) {
                    layoutLxpEmpty.visibility = View.VISIBLE
                    tvLxpEmptyMessage.text = "Impossible de charger les actions LXP (connexion ou session requise)."
                }
            } finally {
                swipeRefreshLxp.isRefreshing = false
            }
        }
    }

    private fun displayLxpActions(actions: List<LxpAction>) {
        layoutLxpList.removeAllViews()
        val filtered = actions.filter {
            val t = it.title.trim().lowercase(Locale.FRANCE)
            t.length >= 3 &&
                t != "learning xp" &&
                t != "learningxp" &&
                t != "lxp" &&
                t != "actions suggérées" &&
                t != "catalogue" &&
                t != "connexion" &&
                !t.startsWith("learning xp") &&
                !t.startsWith("learningxp")
        }

        if (filtered.isEmpty()) {
            layoutLxpEmpty.visibility = View.VISIBLE
            return
        }
        layoutLxpEmpty.visibility = View.GONE
        val inflater = LayoutInflater.from(this)

        filtered.forEach { action ->
            val view = inflater.inflate(R.layout.item_lxp_action_card, layoutLxpList, false)
            val tvCategory: TextView = view.findViewById(R.id.tvActionCategory)
            val tvXpBadge: TextView = view.findViewById(R.id.tvActionXpBadge)
            val tvStatusChip: TextView = view.findViewById(R.id.tvActionStatusChip)
            val tvTitle: TextView = view.findViewById(R.id.tvActionTitle)
            val tvDate: TextView = view.findViewById(R.id.tvActionDate)
            val ivCornerFading: CornerFadingImageView = view.findViewById(R.id.ivActionCornerFading)
            val btnRegister: MaterialButton = view.findViewById(R.id.btnRegisterAction)

            tvCategory.text = if (action.category.isNotBlank()) action.category else "Formation / Atelier"
            tvTitle.text = action.title

            // Dedicated XP Badge (clean, single-line, no \n)
            if (action.xpPoints.isNotBlank()) {
                val cleanXp = action.xpPoints.replace(Regex("\\s+"), " ").trim()
                tvXpBadge.text = if (cleanXp.startsWith("+")) cleanXp else "+$cleanXp"
                tvXpBadge.visibility = View.VISIBLE
            } else {
                tvXpBadge.visibility = View.GONE
            }

            if (action.isRegistered) {
                tvStatusChip.text = "Inscrit"
                btnRegister.text = "Inscrit"
                btnRegister.isEnabled = false
            } else if (!action.canRegister) {
                tvStatusChip.text = "Clôturé"
                btnRegister.text = "Fermé"
                btnRegister.isEnabled = false
            } else {
                tvStatusChip.text = if (action.status.isNotBlank()) action.status else "Disponible"
                btnRegister.text = "S'inscrire"
                btnRegister.isEnabled = true
            }

            tvDate.text = if (action.dateOrPeriod.isNotBlank()) action.dateOrPeriod else "Date à venir"

            // Discreet Corner-Fading Image (fades smoothly to transparent alpha, no white gradient)
            if (action.imageUrl.isNotBlank()) {
                val rawUrl = action.imageUrl
                val fullUrl = if (rawUrl.startsWith("http://") || rawUrl.startsWith("https://")) {
                    rawUrl
                } else {
                    "https://www.myefrei.fr" + (if (rawUrl.startsWith("/")) "" else "/") + rawUrl
                }
                lifecycleScope.launch {
                    val bitmap = withContext(Dispatchers.IO) {
                        try {
                            val conn = (URL(fullUrl).openConnection() as HttpURLConnection).apply {
                                connectTimeout = 8000
                                readTimeout = 8000
                                val cookies = CookieManager.getInstance().getCookie("https://www.myefrei.fr")
                                if (!cookies.isNullOrBlank()) {
                                    setRequestProperty("Cookie", cookies)
                                }
                                setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 14; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36")
                            }
                            if (conn.responseCode in 200..299) {
                                conn.inputStream.use { BitmapFactory.decodeStream(it) }
                            } else null
                        } catch (e: Exception) {
                            null
                        }
                    }
                    if (bitmap != null) {
                        ivCornerFading.setImageBitmap(bitmap)
                        ivCornerFading.visibility = View.VISIBLE
                    }
                }
            } else {
                ivCornerFading.visibility = View.GONE
            }

            // Click card to open full details sheet
            view.setOnClickListener {
                LxpDetailsBottomSheet.newInstance(action) { act ->
                    handleActionRegistration(act)
                }.show(supportFragmentManager, "LxpDetails_${action.id}")
            }

            btnRegister.setOnClickListener {
                handleActionRegistration(action)
            }

            layoutLxpList.addView(view)
        }
    }

    private fun handleActionRegistration(action: LxpAction) {
        val targetUrl = when {
            action.registrationUrl.isNotBlank() -> action.registrationUrl
            action.detailUrl.isNotBlank() -> action.detailUrl
            action.id.isNotBlank() -> "https://www.myefrei.fr/portal/student/lxp/catalog/${action.id}"
            else -> "https://www.myefrei.fr/portal/student/lxp/catalog/"
        }
        val resolvedUrl = if (targetUrl.startsWith("http://") || targetUrl.startsWith("https://")) {
            targetUrl
        } else {
            "https://www.myefrei.fr" + (if (targetUrl.startsWith("/")) "" else "/") + targetUrl
        }
        val webDialog = LxpWebDialogFragment.newInstance(resolvedUrl, action.title)
        webDialog.show(supportFragmentManager, "LxpWebDialog_${action.id}")
    }

    private fun openLxpWebCatalog() {
        val catalogUrl = "https://www.myefrei.fr/portal/student/lxp/catalog/"
        val webDialog = LxpWebDialogFragment.newInstance(catalogUrl, "Catalogue LXP")
        webDialog.show(supportFragmentManager, "LxpWebCatalogDialog")
    }

    private suspend fun fetchLxpActionsFromApi(): List<LxpAction> {
        // Step 1: Direct HTTP endpoints (REST / HTML)
        val httpActions = withContext(Dispatchers.IO) {
            fetchLxpFromHttp()
        }
        if (httpActions.isNotEmpty()) {
            return httpActions
        }

        // Step 2: Headless WebView scraper with JavaScript execution
        return withContext(Dispatchers.Main) {
            fetchLxpFromHeadlessWeb()
        }
    }

    private fun fetchLxpFromHttp(): List<LxpAction> {
        val cookieManager = CookieManager.getInstance()
        val directCookies = cookieManager.getCookie("https://www.myefrei.fr/portal/student/lxp") ?: ""
        val wwwCookies = cookieManager.getCookie("https://www.myefrei.fr") ?: ""
        val authCookies = cookieManager.getCookie("https://auth.myefrei.fr") ?: ""

        val cookieMap = mutableMapOf<String, String>()
        for (cookieStr in listOf(authCookies, wwwCookies, directCookies)) {
            if (cookieStr.isNotBlank()) {
                cookieStr.split(";").forEach { part ->
                    val trimmed = part.trim()
                    val eqIdx = trimmed.indexOf("=")
                    if (eqIdx > 0) {
                        cookieMap[trimmed.substring(0, eqIdx).trim()] = trimmed
                    }
                }
            }
        }
        val mergedCookies = cookieMap.values.joinToString("; ")

        val list = mutableListOf<LxpAction>()

        val endpointsToTry = listOf(
            "https://www.myefrei.fr/api/rest/student/lxp/catalog",
            "https://www.myefrei.fr/api/rest/student/lxp",
            "https://www.myefrei.fr/api/rest/student/lxp/actions",
            "https://www.myefrei.fr/api/rest/student/catalog",
            "https://www.myefrei.fr/portal/student/lxp/catalog/",
            "https://www.myefrei.fr/portal/student/lxp"
        )

        for (endpoint in endpointsToTry) {
            try {
                val conn = URL(endpoint).openConnection() as HttpURLConnection
                conn.connectTimeout = 6000
                conn.readTimeout = 6000
                conn.instanceFollowRedirects = true
                conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 14; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36")
                conn.setRequestProperty("Accept", "application/json, text/html, */*")
                conn.setRequestProperty("Accept-Language", "fr-FR,fr;q=0.9,en-US;q=0.8,en;q=0.7")
                if (mergedCookies.isNotBlank()) {
                    conn.setRequestProperty("Cookie", mergedCookies)
                }

                val code = conn.responseCode
                if (code in 200..299) {
                    val raw = conn.inputStream.bufferedReader().use { it.readText() }
                    val trimmed = raw.trim()

                    if (trimmed.startsWith("[") || trimmed.startsWith("{")) {
                        val parsed = parseLxpJson(trimmed)
                        if (parsed.isNotEmpty()) {
                            list.addAll(parsed)
                            break
                        }
                    }

                    val fromHtml = parseLxpFromHtml(trimmed)
                    if (fromHtml.isNotEmpty()) {
                        list.addAll(fromHtml)
                        break
                    }
                }
            } catch (e: Exception) {
                // Ignore and continue to next endpoint
            }
        }
        return list
    }

    private suspend fun fetchLxpFromHeadlessWeb(): List<LxpAction> = kotlinx.coroutines.suspendCancellableCoroutine { cont ->
        try {
            var resumed = false
            val webView = WebView(this@MainActivity)

            fun finish(result: List<LxpAction>) {
                if (!resumed) {
                    resumed = true
                    try {
                        webView.stopLoading()
                        webView.destroy()
                    } catch (e: Exception) {}
                    if (cont.isActive) {
                        cont.resume(result, onCancellation = null)
                    }
                }
            }

            // Safety timeout after 10 seconds
            val timeoutHandler = Handler(Looper.getMainLooper())
            val timeoutRunnable = Runnable {
                finish(emptyList())
            }
            timeoutHandler.postDelayed(timeoutRunnable, 10000)

            cont.invokeOnCancellation {
                timeoutHandler.removeCallbacks(timeoutRunnable)
                try {
                    webView.stopLoading()
                    webView.destroy()
                } catch (e: Exception) {}
            }

            val cookieManager = CookieManager.getInstance()
            cookieManager.setAcceptCookie(true)
            cookieManager.setAcceptThirdPartyCookies(webView, true)
            cookieManager.flush()

            val settings = webView.settings
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.databaseEnabled = true
            settings.userAgentString = "Mozilla/5.0 (Linux; Android 14; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"

            class LxpJsBridge {
                @JavascriptInterface
                fun onScraped(jsonStr: String) {
                    val actions = parseLxpJson(jsonStr)
                    if (actions.isNotEmpty()) {
                        timeoutHandler.removeCallbacks(timeoutRunnable)
                        Handler(Looper.getMainLooper()).post {
                            finish(actions)
                        }
                    }
                }
            }

            webView.addJavascriptInterface(LxpJsBridge(), "LxpBridge")

            var hasTriedCatalog = false

            webView.webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    val currentUrl = url ?: ""

                    // If redirected to login/SSO portal, let it settle or redirect back
                    if (currentUrl.contains("/auth/efrei")) {
                        return
                    }

                    // Schedule multiple DOM extraction passes to catch asynchronous React hydrate
                    listOf(1000L, 2500L, 4000L).forEach { delayMs ->
                        timeoutHandler.postDelayed({
                            if (!resumed) {
                                executeLxpExtractorScript(webView)
                            }
                        }, delayMs)
                    }

                    // If after 4.5s still empty on /lxp, try /portal/student/lxp/catalog/
                    if (!hasTriedCatalog && currentUrl.endsWith("/lxp")) {
                        hasTriedCatalog = true
                        timeoutHandler.postDelayed({
                            if (!resumed) {
                                webView.loadUrl("https://www.myefrei.fr/portal/student/lxp/catalog/")
                            }
                        }, 4500)
                    }
                }
            }

            webView.loadUrl("https://www.myefrei.fr/portal/student/lxp")
        } catch (e: Exception) {
            if (cont.isActive) {
                cont.resume(emptyList(), onCancellation = null)
            }
        }
    }

    private fun executeLxpExtractorScript(webView: WebView) {
        val js = """
            (function() {
                try {
                    // 1. Next.js props inspection
                    var nextData = document.getElementById('__NEXT_DATA__');
                    if (nextData) {
                        try {
                            var json = JSON.parse(nextData.textContent);
                            var props = json.props && json.props.pageProps;
                            if (props) {
                                window.LxpBridge.onScraped(JSON.stringify(props));
                            }
                        } catch(e) {}
                    }

                    // 2. Global state or window variables (Redux / Apollo / React Query cache)
                    if (window.__PRELOADED_STATE__) {
                        window.LxpBridge.onScraped(JSON.stringify(window.__PRELOADED_STATE__));
                    }

                    // 3. Search for card containers & action elements anywhere in DOM
                    var items = [];
                    var selectors = [
                        'div[class*="card"]', 'div[class*="action"]', 'div[class*="item"]',
                        'div[class*="catalog"]', 'div[class*="course"]', 'div[class*="Module"]',
                        'a[href*="/lxp"]', 'a[href*="/catalog"]', 'article', 'tr'
                    ];
                    var elements = document.querySelectorAll(selectors.join(', '));
                    elements.forEach(function(el, i) {
                        var h = el.querySelector('h1, h2, h3, h4, h5, h6, strong, b, [class*="title"], [class*="Title"]');
                        var title = h ? (h.innerText || '').trim() : '';
                        if (!title && el.tagName === 'A') {
                            title = (el.innerText || '').trim().split('\n')[0];
                        }
                        var lowerTitle = title.toLowerCase();
                        if (title && title.length > 3 && title.length < 120 &&
                            !lowerTitle.includes('learning xp') &&
                            !lowerTitle.includes('learningxp') &&
                            !lowerTitle.includes('actions suggérées') &&
                            !lowerTitle.includes('catalogue') &&
                            !lowerTitle.includes('connexion') &&
                            !lowerTitle.includes('accueil') &&
                            !lowerTitle.includes('déconnexion')) {
                            var fullText = (el.innerText || '').trim();
                            var link = el.getAttribute('href') || (el.querySelector('a') ? el.querySelector('a').getAttribute('href') : '');
                            
                            // Extract metadata badges if present
                            var cat = '';
                            var status = 'Disponible';
                            var badge = el.querySelector('[class*="badge"], [class*="chip"], [class*="tag"], [class*="status"]');
                            if (badge) {
                                status = badge.innerText.trim();
                            }
                            
                            // Extract XP points
                            var xp = '';
                            var xpMatch = fullText.match(/([+]?\s*\d+\s*(?:XP|xp|LXP|lxp|points|pts|point|pt))\b/i);
                            if (xpMatch) {
                                xp = xpMatch[1].replace(/\s+/g, ' ').trim();
                                if (!xp.startsWith('+')) xp = '+' + xp;
                            }

                            // Extract date or period
                            var dateStr = '';
                            var dateMatch = fullText.match(/(\d{1,2}\s+(?:janv|févr|mars|avr|mai|juin|juil|août|sept|oct|nov|déc)[a-z]*\s*(?:\d{4})?)/i) ||
                                            fullText.match(/(\d{1,2}[\/-]\d{1,2}[\/-]\d{2,4})/);
                            if (dateMatch) {
                                dateStr = dateMatch[1].trim();
                            }

                            // Clean description: strip title, status, xp, and buttons
                            var cleanedDesc = fullText.replace(title, '');
                            if (xp) cleanedDesc = cleanedDesc.replace(xp, '');
                            cleanedDesc = cleanedDesc.replace(/(?:S'inscrire|Inscrit|Détails|En savoir plus|Disponible|Clôturé|Fermé)/gi, ' ');
                            cleanedDesc = cleanedDesc.replace(/\s+/g, ' ').trim();
                            if (cleanedDesc.length > 220) {
                                cleanedDesc = cleanedDesc.substring(0, 220) + '...';
                            }
                            if (cleanedDesc.length < 5 || /^[^a-zA-Z0-9]+$/.test(cleanedDesc)) {
                                cleanedDesc = '';
                            }
                            
                            // Extract image or thumbnail if present
                            var imgEl = el.querySelector('img');
                            var imgUrl = imgEl ? (imgEl.src || imgEl.getAttribute('data-src') || '') : '';
                            if (!imgUrl) {
                                var bgEl = el.querySelector('[style*="background-image"]');
                                if (bgEl && bgEl.style && bgEl.style.backgroundImage) {
                                    var bgMatch = bgEl.style.backgroundImage.match(/url\(["']?([^"']*)["']?\)/);
                                    if (bgMatch) imgUrl = bgMatch[1];
                                }
                            }
                            
                            items.push({
                                id: 'item_' + i,
                                title: title,
                                description: cleanedDesc,
                                category: cat || 'Formation',
                                status: status,
                                xpPoints: xp,
                                date: dateStr,
                                detailUrl: link || '',
                                imageUrl: imgUrl || '',
                                canRegister: true
                            });
                        }
                    });

                    if (items.length > 0) {
                        // Deduplicate items by title
                        var uniqueMap = {};
                        var deduped = [];
                        items.forEach(function(it) {
                            if (!uniqueMap[it.title]) {
                                uniqueMap[it.title] = true;
                                deduped.push(it);
                            }
                        });
                        window.LxpBridge.onScraped(JSON.stringify(deduped));
                    }
                } catch(e) {}
            })();
        """.trimIndent()
        webView.evaluateJavascript(js, null)
    }

    private fun parseLxpJson(rawJson: String): List<LxpAction> {
        val list = mutableListOf<LxpAction>()
        try {
            val trimmed = rawJson.trim()
            if (trimmed.startsWith("[")) {
                val array = JSONArray(trimmed)
                for (i in 0 until array.length()) {
                    val obj = array.optJSONObject(i) ?: continue
                    list.add(LxpAction.fromJson(obj))
                }
            } else if (trimmed.startsWith("{")) {
                val obj = JSONObject(trimmed)
                val array = obj.optJSONArray("actions")
                    ?: obj.optJSONArray("data")
                    ?: obj.optJSONArray("items")
                    ?: obj.optJSONArray("catalog")
                    ?: obj.optJSONArray("courses")
                    ?: obj.optJSONArray("suggestedActions")
                if (array != null) {
                    for (i in 0 until array.length()) {
                        val item = array.optJSONObject(i) ?: continue
                        list.add(LxpAction.fromJson(item))
                    }
                } else {
                    val keys = obj.keys()
                    while (keys.hasNext()) {
                        val k = keys.next()
                        val subArr = obj.optJSONArray(k)
                        if (subArr != null && subArr.length() > 0 && subArr.optJSONObject(0) != null) {
                            for (i in 0 until subArr.length()) {
                                val item = subArr.optJSONObject(i) ?: continue
                                list.add(LxpAction.fromJson(item))
                            }
                            if (list.isNotEmpty()) break
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // Ignore
        }
        return list
    }

    private fun parseLxpFromHtml(html: String): List<LxpAction> {
        val list = mutableListOf<LxpAction>()
        try {
            val nextDataRegex = Regex("<script id=\"__NEXT_DATA__\" type=\"application/json\">([\\s\\S]*?)</script>")
            val match = nextDataRegex.find(html)
            if (match != null) {
                val jsonStr = match.groupValues[1].trim()
                val nextObj = JSONObject(jsonStr)
                val pageProps = nextObj.optJSONObject("props")?.optJSONObject("pageProps")
                if (pageProps != null) {
                    val parsed = parseLxpJson(pageProps.toString())
                    if (parsed.isNotEmpty()) return parsed
                }
            }
        } catch (e: Exception) {
            // Ignore
        }
        return list
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
        val prefs = getSharedPreferences("myefoss_prefs", MODE_PRIVATE)
        val isExplicitlyLoggedOut = prefs.getBoolean("is_logged_out", false)
        val cookieManager = CookieManager.getInstance()
        val cookies = cookieManager.getCookie("https://www.myefrei.fr") ?: ""
        if (!isExplicitlyLoggedOut && (cookies.contains("myefrei.sid") || allCachedCourses.isNotEmpty())) {
            showScreen(Screen.APP)
            loadAgendaForCurrentWeek()
            fetchAndDisplayUserProfile()
        } else {
            showScreen(Screen.LOGIN)
        }
    }

    private fun displayUserProfile(profile: StudentProfile) {
        val displayName = when {
            profile.fullName.isNotBlank() -> profile.fullName
            profile.firstName.isNotBlank() && profile.lastName.isNotBlank() -> "${profile.firstName} ${profile.lastName}"
            profile.firstName.isNotBlank() -> profile.firstName
            else -> "Étudiant"
        }
        tvDrawerUserName?.text = displayName
        tvStudentSubtitle.text = displayName
        if (profile.email.isNotBlank()) {
            tvDrawerUserEmail?.text = profile.email
        } else if (profile.program.isNotBlank()) {
            tvDrawerUserEmail?.text = profile.program
        }
    }

    private fun showSessionExpiredBanner(show: Boolean) {
        runOnUiThread {
            bannerSessionExpired.visibility = if (show) View.VISIBLE else View.GONE
            if (show) {
                SessionNotificationManager.notifySessionExpired(this@MainActivity)
            } else {
                SessionNotificationManager.cancelNotification(this@MainActivity)
            }
        }
    }

    private fun fetchAndDisplayUserProfile() {
        lifecycleScope.launch {
            val profile = withContext(Dispatchers.IO) {
                fetchStudentProfileFromApi() ?: fetchStudentProfileFromHome()
            }
            if (profile != null) {
                OfflineCacheManager.saveStudentProfile(this@MainActivity, profile)
                displayUserProfile(profile)
            }
        }
    }

    private fun fetchStudentProfileFromApi(): StudentProfile? {
        val cookieManager = CookieManager.getInstance()
        val directCookies = cookieManager.getCookie("https://www.myefrei.fr/api/rest/student/user/info") ?: ""
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

        val profileEndpoints = listOf(
            "https://www.myefrei.fr/api/rest/student/user/info",
            "https://www.myefrei.fr/api/rest/student/user",
            "https://www.myefrei.fr/api/rest/user/info"
        )

        for (endpoint in profileEndpoints) {
            try {
                val conn = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 8000
                    readTimeout = 8000
                    setRequestProperty("Accept", "application/json, text/plain, */*")
                    setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 14; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36")
                    setRequestProperty("Referer", "https://www.myefrei.fr/portal/student/home")
                    setRequestProperty("Origin", "https://www.myefrei.fr")
                    if (mergedCookies.isNotBlank()) setRequestProperty("Cookie", mergedCookies)
                }

                if (conn.responseCode in 200..299) {
                    val resp = conn.inputStream.bufferedReader().use { it.readText() }.trim()
                    if (resp.startsWith("{")) {
                        val root = JSONObject(resp)
                        val userObj = root.optJSONObject("user") ?: root.optJSONObject("data") ?: root
                        val firstName = userObj.optString("firstName", userObj.optString("prenom", "")).trim()
                        val lastName = userObj.optString("lastName", userObj.optString("nom", "")).trim()
                        val fullName = userObj.optString("fullName", userObj.optString("name", "")).trim()
                        val email = userObj.optString("email", userObj.optString("mail", "")).trim()
                        val studentId = userObj.optString("studentId", userObj.optString("id", "")).trim()
                        val program = userObj.optString("program", userObj.optString("filiere", "")).trim()

                        if (firstName.isNotBlank() || lastName.isNotBlank() || fullName.isNotBlank()) {
                            return StudentProfile(
                                fullName = if (fullName.isNotBlank()) fullName else "$firstName $lastName".trim(),
                                firstName = firstName,
                                lastName = lastName,
                                email = email,
                                studentId = studentId,
                                program = program
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                // Try next
            }
        }
        return null
    }

    private fun fetchStudentProfileFromHome(): StudentProfile? {
        val cookieManager = CookieManager.getInstance()
        val wwwCookies = cookieManager.getCookie("https://www.myefrei.fr") ?: ""
        val authCookies = cookieManager.getCookie("https://auth.myefrei.fr") ?: ""

        val cookieMap = mutableMapOf<String, String>()
        for (cookieStr in listOf(authCookies, wwwCookies)) {
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

        val urls = listOf(
            "https://www.myefrei.fr/portal/student/home",
            "https://www.myefrei.fr/portal/student/planning"
        )

        for (urlStr in urls) {
            try {
                val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 8000
                    readTimeout = 8000
                    instanceFollowRedirects = false
                    setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 14; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36")
                    setRequestProperty("Referer", "https://www.myefrei.fr/")
                    if (mergedCookies.isNotBlank()) setRequestProperty("Cookie", mergedCookies)
                }

                if (conn.responseCode in 200..299) {
                    val html = conn.inputStream.bufferedReader().use { it.readText() }
                    val scriptIdx = html.indexOf("<script id=\"__NEXT_DATA__\" type=\"application/json\">")
                    if (scriptIdx != -1) {
                        val start = html.indexOf('>', scriptIdx) + 1
                        val end = html.indexOf("</script>", start)
                        if (start != -1 && end > start) {
                            val jsonStr = html.substring(start, end).trim()
                            val root = JSONObject(jsonStr)
                            val props = root.optJSONObject("props")
                            val pageProps = props?.optJSONObject("pageProps")
                            val userObj = pageProps?.optJSONObject("user")
                                ?: pageProps?.optJSONObject("userInfo")
                                ?: props?.optJSONObject("user")
                                ?: root.optJSONObject("user")

                            if (userObj != null) {
                                val firstName = userObj.optString("firstName", userObj.optString("prenom", "")).trim()
                                val lastName = userObj.optString("lastName", userObj.optString("nom", "")).trim()
                                val fullName = userObj.optString("fullName", userObj.optString("name", "")).trim()
                                val email = userObj.optString("email", userObj.optString("mail", "")).trim()
                                val studentId = userObj.optString("studentId", userObj.optString("id", "")).trim()
                                val program = userObj.optString("program", userObj.optString("filiere", "")).trim()

                                if (firstName.isNotBlank() || lastName.isNotBlank() || fullName.isNotBlank()) {
                                    return StudentProfile(
                                        fullName = if (fullName.isNotBlank()) fullName else "$firstName $lastName".trim(),
                                        firstName = firstName,
                                        lastName = lastName,
                                        email = email,
                                        studentId = studentId,
                                        program = program
                                    )
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                // Try next
            }
        }
        return null
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
            userAgentString = "Mozilla/5.0 (Linux; Android 14; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"
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
                        getSharedPreferences("myefoss_prefs", MODE_PRIVATE).edit().putBoolean("is_logged_out", false).apply()
                        showSessionExpiredBanner(false)
                        loginWebView.visibility = View.GONE
                        showScreen(Screen.APP)
                        loadAgendaForCurrentWeek()
                        fetchAndDisplayUserProfile()
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

        // Step 1: Render immediately from cache in-memory
        displayPlanning(allCachedCourses)

        // Generate a cache key for the month, e.g. "2026-10"
        val monthKeyFormat = SimpleDateFormat("yyyy-MM", Locale.US)
        val targetMonthKey = monthKeyFormat.format(currentWeekCal.time)

        // Check if we already have courses cached for this week or month
        val hasCoursesForCurrentWeek = allCachedCourses.any { course ->
            course.startDate != null && course.startDate >= startOfWeek && course.startDate <= endOfWeek
        }

        // Only hit network if:
        // 1) User explicitly swiped to refresh (pull-to-refresh)
        // 2) We haven't fetched this month yet in this session AND either has no courses or is initial load
        if (!isSwipe && hasCoursesForCurrentWeek && lastFetchedMonthKey == targetMonthKey) {
            // Instant render from cache without network lag
            return
        }

        if (!isSwipe && allCachedCourses.isEmpty()) {
            layoutLoading.visibility = View.VISIBLE
        }

        // Step 2: Fetch fresh data from network in background
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

                lastFetchedMonthKey = targetMonthKey

                if (freshCourses.isNotEmpty()) {
                    showSessionExpiredBanner(false)
                    OfflineCacheManager.saveCourses(this@MainActivity, freshCourses)
                    allCachedCourses = OfflineCacheManager.loadCourses(this@MainActivity)
                    displayPlanning(allCachedCourses)
                }

            } catch (e: Exception) {
                if (e.message?.contains("401") == true || e.message?.contains("403") == true) {
                    showSessionExpiredBanner(true)
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
                val url = request?.url?.toString() ?: ""
                // Only consider successful re-auth once redirected to an authenticated destination
                if (url.startsWith("https://www.myefrei.fr/home") ||
                    url.startsWith("https://www.myefrei.fr/dashboard") ||
                    url.startsWith("https://www.myefrei.fr/portal")) {
                    val cm = CookieManager.getInstance()
                    val c = cm.getCookie("https://www.myefrei.fr") ?: ""
                    if (c.contains("myefrei.sid")) {
                        loadAgendaForCurrentWeek()
                        return true
                    }
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

        val primaryColor = com.google.android.material.color.MaterialColors.getColor(this, com.google.android.material.R.attr.colorPrimary, android.graphics.Color.BLUE)
        val onPrimaryColor = com.google.android.material.color.MaterialColors.getColor(this, com.google.android.material.R.attr.colorOnPrimary, android.graphics.Color.WHITE)
        val onPrimaryContainerColor = com.google.android.material.color.MaterialColors.getColor(this, com.google.android.material.R.attr.colorOnPrimaryContainer, primaryColor)
        val outlineColor = com.google.android.material.color.MaterialColors.getColor(this, com.google.android.material.R.attr.colorOutline, android.graphics.Color.GRAY)

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
                chipView.alpha = 1.0f
                tvDayName.setTextColor(onPrimaryColor)
                tvDayNumber.setTextColor(onPrimaryColor)
            } else if (isToday) {
                chipView.setBackgroundResource(R.drawable.bg_day_chip)
                chipView.isActivated = true
                chipView.alpha = 1.0f
                tvDayName.setTextColor(onPrimaryContainerColor)
                tvDayNumber.setTextColor(onPrimaryContainerColor)
            } else if (hasCourses) {
                // Surbrillance dynamique avec la couleur primaire du thème
                chipView.setBackgroundResource(R.drawable.bg_day_chip)
                chipView.isSelected = false
                chipView.isActivated = false
                chipView.alpha = 1.0f
                tvDayName.setTextColor(primaryColor)
                tvDayNumber.setTextColor(primaryColor)
            } else {
                // Journée sans cours : visiblement grisée avec outline
                chipView.setBackgroundResource(R.drawable.bg_day_chip)
                chipView.isSelected = false
                chipView.isActivated = false
                chipView.alpha = 0.38f
                tvDayName.setTextColor(outlineColor)
                tvDayNumber.setTextColor(outlineColor)
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
                        alpha = 1.0f
                        setTextColor(com.google.android.material.color.MaterialColors.getColor(this@MainActivity, com.google.android.material.R.attr.colorOnPrimary, android.graphics.Color.WHITE))
                    } else if (isToday) {
                        setBackgroundResource(R.drawable.bg_day_chip)
                        isActivated = true
                        alpha = 1.0f
                        val fallbackPrimary = com.google.android.material.color.MaterialColors.getColor(this@MainActivity, com.google.android.material.R.attr.colorPrimary, android.graphics.Color.BLUE)
                        setTextColor(com.google.android.material.color.MaterialColors.getColor(this@MainActivity, com.google.android.material.R.attr.colorOnPrimaryContainer, fallbackPrimary))
                    } else if (hasCourses) {
                        alpha = 1.0f
                        setTextColor(com.google.android.material.color.MaterialColors.getColor(this@MainActivity, com.google.android.material.R.attr.colorPrimary, android.graphics.Color.BLUE))
                        setTypeface(typeface, android.graphics.Typeface.BOLD)
                    } else {
                        // Journée sans cours : grisée discrètement
                        alpha = 0.38f
                        setTextColor(com.google.android.material.color.MaterialColors.getColor(this@MainActivity, com.google.android.material.R.attr.colorOutline, android.graphics.Color.GRAY))
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
        getSharedPreferences("myefoss_prefs", MODE_PRIVATE).edit().putBoolean("is_logged_out", true).apply()
        CookieManager.getInstance().removeAllCookies(null)
        CookieManager.getInstance().flush()
        loginWebView.clearHistory()
        loginWebView.clearCache(true)
        loginWebView.clearFormData()
        OfflineCacheManager.clearAllCache(this)
        allCachedCourses = emptyList()
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
        if (tabContainerGrades.visibility == View.VISIBLE || tabContainerAbsences.visibility == View.VISIBLE || tabContainerLxp.visibility == View.VISIBLE) {
            showScolarityTab()
            return
        }
        if (tabContainerSettings.visibility == View.VISIBLE || tabContainerCampus.visibility == View.VISIBLE) {
            showPlanningTab()
            return
        }
        if (tabContainerScolarity.visibility == View.VISIBLE) {
            bottomNavigation.selectedItemId = R.id.nav_planning
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
