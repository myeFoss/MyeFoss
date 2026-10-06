package org.myefoss.app

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

    // Grades views
    private lateinit var btnBackFromGrades: MaterialButton
    private lateinit var layoutGradesList: LinearLayout

    // Absences views
    private lateinit var btnBackFromAbsences: MaterialButton
    private lateinit var layoutAbsencesList: LinearLayout
    private lateinit var layoutAbsencesEmptyState: LinearLayout
    private lateinit var tvAbsencesTotalHours: TextView
    private lateinit var tvAbsencesSummaryLabel: TextView
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

    // Login
    private lateinit var btnLogin: MaterialButton

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

        // Restore active screen after theme recreation
        val lastScreen = prefs.getString("last_active_screen", "planning")
        when (lastScreen) {
            "settings" -> openSettingsScreen()
            "scolarity" -> showScolarityTab()
            "grades" -> openGradesScreen()
            "absences" -> openAbsencesScreen()
            else -> showPlanningTab()
        }

        checkSessionAndLoad()
        checkForUpdatesSilently()
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
                    openGradesScreen()
                    true
                }
                R.id.drawer_absences -> {
                    openAbsencesScreen()
                    true
                }
                R.id.drawer_lxp -> {
                    showScolarityFeature("LXP / E-learning")
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
        cardLxp.setOnClickListener { showScolarityFeature("LXP / E-learning") }

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
        tvToolbarTitle.text = "Scolarité"
        btnRefresh.visibility = View.GONE
        bottomNavigation.menu.findItem(R.id.nav_scolarity)?.isChecked = true
        // Clear drawer check or leave unselected for sub-items
        navigationDrawer.checkedItem?.isChecked = false
    }

    private fun openSettingsScreen() {
        getSharedPreferences("myefoss_prefs", MODE_PRIVATE).edit().putString("last_active_screen", "settings").apply()
        tabContainerPlanning.visibility = View.GONE
        tabContainerScolarity.visibility = View.GONE
        tabContainerGrades.visibility = View.GONE
        tabContainerAbsences.visibility = View.GONE
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

        // 3. Course Change Notifications
        val isNotifEnabled = prefs.getBoolean("notify_course_changes", false)
        switchCourseNotifications.isChecked = isNotifEnabled

        switchCourseNotifications.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("notify_course_changes", isChecked).apply()

            if (isChecked) {
                // Request POST_NOTIFICATIONS permission on Android 13+ if needed
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                    if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                        requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 101)
                    }
                }
                CourseSyncWorker.createNotificationChannel(this)
                scheduleCourseSyncWorker()
                Toast.makeText(this, "Notifications de cours activées", Toast.LENGTH_SHORT).show()
            } else {
                androidx.work.WorkManager.getInstance(this).cancelUniqueWork(CourseSyncWorker.WORK_NAME)
                Toast.makeText(this, "Notifications désactivées", Toast.LENGTH_SHORT).show()
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

                if (fresh.isNotEmpty()) {
                    OfflineCacheManager.saveAbsences(this@MainActivity, fresh, schoolYear)
                    displayAbsences(fresh, schoolYear)
                } else if (cached.isEmpty()) {
                    displayAbsences(emptyList(), schoolYear)
                }
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

        if (absences.isEmpty()) {
            tvAbsencesTotalHours.text = "0h"
            layoutAbsencesEmptyState.visibility = View.VISIBLE
            return
        }

        layoutAbsencesEmptyState.visibility = View.GONE

        // Calculate total hours
        var totalMinutes = 0
        absences.forEach { abs ->
            val hParts = abs.hours.lowercase().replace("h", ":").split(":")
            val h = hParts.getOrNull(0)?.trim()?.toIntOrNull() ?: 1
            val m = hParts.getOrNull(1)?.trim()?.toIntOrNull() ?: 0
            totalMinutes += (h * 60) + m
        }
        val displayH = totalMinutes / 60
        val displayM = totalMinutes % 60
        tvAbsencesTotalHours.text = if (displayM > 0) "${displayH}h${String.format(Locale.FRANCE, "%02d", displayM)}" else "${displayH}h"

        absences.forEach { abs ->
            val view = inflater.inflate(R.layout.item_absence_card, layoutAbsencesList, false)
            val tvSubject: TextView = view.findViewById(R.id.tvAbsenceSubject)
            val tvBadge: TextView = view.findViewById(R.id.tvAbsenceJustifiedBadge)
            val tvDate: TextView = view.findViewById(R.id.tvAbsenceDate)
            val tvDuration: TextView = view.findViewById(R.id.tvAbsenceDuration)
            val tvType: TextView = view.findViewById(R.id.tvAbsenceType)

            tvSubject.text = abs.courseName
            tvDate.text = abs.date
            tvDuration.text = "• ${abs.hours}"
            tvType.text = if (!abs.type.isNullOrBlank()) "• ${abs.type}" else ""

            if (abs.justified) {
                tvBadge.text = "Justifiée"
                tvBadge.setTextColor(com.google.android.material.color.MaterialColors.getColor(this, com.google.android.material.R.attr.colorPrimary, android.graphics.Color.BLUE))
            } else {
                tvBadge.text = "Non justifiée"
                tvBadge.setTextColor(com.google.android.material.color.MaterialColors.getColor(this, com.google.android.material.R.attr.colorError, android.graphics.Color.RED))
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
                endpointsToTry.add("https://www.myefrei.fr/api/rest/student/absences?period=$encPeriod&programId=$encProgram")
                endpointsToTry.add("https://www.myefrei.fr/api/rest/student/absences?schoolYear=$encServerYear&period=$encPeriod&programId=$encProgram")
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
        val courseName = obj.optString("courseName", obj.optString("subject", obj.optString("module", obj.optString("course", obj.optString("name", obj.optString("title", ""))))))
        val hasDate = obj.has("date") || obj.has("startDate") || obj.has("start") || obj.has("sessionDate") || obj.has("dateSession") || obj.has("createdAt")
        val hasDuration = obj.has("duration") || obj.has("hours") || obj.has("nbHours") || obj.has("totalHours")
        val hasAbsenceMarkers = obj.has("justified") || obj.has("isJustified") || obj.has("motif") || obj.has("reason") || obj.has("late") || obj.has("retard")

        if (courseName.isNotBlank() && (hasDate || hasDuration || hasAbsenceMarkers)) {
            val rawDate = obj.optString("date", obj.optString("startDate", obj.optString("start", obj.optString("sessionDate", obj.optString("dateSession", "")))))
            val dateStr = if (rawDate.length >= 10 && rawDate.contains("T")) {
                rawDate.substring(0, 10)
            } else {
                rawDate
            }
            val hoursStr = obj.optString("hours", obj.optString("duration", obj.optString("nbHours", obj.optString("totalHours", "1h30"))))
            val isJustified = obj.optBoolean("justified", obj.optBoolean("isJustified", obj.optBoolean("justifie", false)))
            val reason = obj.optString("reason", obj.optString("motif", obj.optString("comment", "")))
            val type = obj.optString("type", obj.optString("sessionType", obj.optString("activity", "")))

            outList.add(
                StudentAbsence(
                    id = obj.optString("id", obj.optString("_id", outList.size.toString())),
                    courseName = courseName,
                    date = dateStr,
                    hours = hoursStr,
                    justified = isJustified,
                    type = type.ifBlank { null },
                    reason = reason.ifBlank { null }
                )
            )
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
        if (tabContainerGrades.visibility == View.VISIBLE || tabContainerAbsences.visibility == View.VISIBLE) {
            showScolarityTab()
            return
        }
        if (tabContainerSettings.visibility == View.VISIBLE) {
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
