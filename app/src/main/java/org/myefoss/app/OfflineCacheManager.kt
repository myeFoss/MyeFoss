package org.myefoss.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

object OfflineCacheManager {

    private const val CACHE_FILE_NAME = "myefoss_planning_cache.json"

    fun saveCourses(context: Context, newCourses: List<CourseEvent>) {
        try {
            val file = File(context.filesDir, CACHE_FILE_NAME)
            val existing = loadCourses(context).associateBy { it.id }.toMutableMap()
            // Merge new courses into cache
            newCourses.forEach { existing[it.id] = it }

            val jsonArray = JSONArray()
            existing.values.forEach { course ->
                val obj = JSONObject().apply {
                    put("id", course.id)
                    put("name", course.name)
                    course.module?.let { put("module", it) }
                    course.startTime?.let { put("startTime", it) }
                    course.endTime?.let { put("endTime", it) }
                    course.startDate?.let {
                        val fmt = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US).apply {
                            timeZone = java.util.TimeZone.getTimeZone("UTC")
                        }
                        put("start", fmt.format(it))
                    }
                    course.endDate?.let {
                        val fmt = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US).apply {
                            timeZone = java.util.TimeZone.getTimeZone("UTC")
                        }
                        put("end", fmt.format(it))
                    }
                    course.sessionType?.let { put("sessionType", it) }
                    course.courseActivity?.let { put("courseActivity", it) }
                    course.courseActivityName?.let { put("courseActivityName", it) }
                    course.modality?.let { put("modality", it) }

                    val locArr = JSONArray()
                    course.locations.forEach { loc ->
                        val locObj = JSONObject().apply {
                            loc.campus?.let { put("campus", it) }
                            loc.bat?.let { put("bat", it) }
                            loc.building?.let { put("building", it) }
                            loc.room?.let { put("room", it) }
                        }
                        locArr.put(locObj)
                    }
                    put("locations", locArr)

                    val teachArr = JSONArray()
                    course.teachers.forEach { teachArr.put(it) }
                    put("teachers", teachArr)
                }
                jsonArray.put(obj)
            }

            file.writeText(jsonArray.toString())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun loadCourses(context: Context): List<CourseEvent> {
        return try {
            val file = File(context.filesDir, CACHE_FILE_NAME)
            if (!file.exists()) return emptyList()
            val text = file.readText()
            val jsonArray = JSONArray(text)
            val list = mutableListOf<CourseEvent>()
            for (i in 0 until jsonArray.length()) {
                list.add(CourseEvent.fromJson(jsonArray.getJSONObject(i)))
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    private const val GRADES_CACHE_PREFIX = "myefoss_grades_cache_"

    fun saveGrades(context: Context, grades: List<StudentGrade>, schoolYear: String) {
        try {
            val safeYear = schoolYear.replace("/", "-")
            val file = File(context.filesDir, "${GRADES_CACHE_PREFIX}${safeYear}.json")
            val jsonArray = JSONArray()
            grades.forEach { grade ->
                val obj = JSONObject().apply {
                    put("courseName", grade.courseName)
                    put("gradeValue", grade.gradeValue)
                    put("details", grade.details)
                    put("date", grade.date)
                    put("ue", grade.ue)
                    grade.average?.let { put("average", it) }
                    grade.semester?.let { put("semester", it) }
                }
                jsonArray.put(obj)
            }
            file.writeText(jsonArray.toString())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun loadGrades(context: Context, schoolYear: String): List<StudentGrade> {
        return try {
            val safeYear = schoolYear.replace("/", "-")
            val file = File(context.filesDir, "${GRADES_CACHE_PREFIX}${safeYear}.json")
            if (!file.exists()) return emptyList()
            val text = file.readText()
            val jsonArray = JSONArray(text)
            val list = mutableListOf<StudentGrade>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(
                    StudentGrade(
                        courseName = obj.optString("courseName", ""),
                        gradeValue = obj.optString("gradeValue", ""),
                        details = obj.optString("details", ""),
                        date = obj.optString("date", ""),
                        ue = obj.optString("ue", "Modules Généraux"),
                        average = if (obj.has("average")) obj.optString("average") else null,
                        semester = if (obj.has("semester")) obj.optString("semester") else null
                    )
                )
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    private const val ABSENCES_CACHE_PREFIX = "myefoss_absences_cache_"

    fun saveAbsences(context: Context, absences: List<StudentAbsence>, schoolYear: String) {
        try {
            val safeYear = schoolYear.replace("/", "-")
            val file = File(context.filesDir, "${ABSENCES_CACHE_PREFIX}${safeYear}.json")
            val jsonArray = JSONArray()
            absences.forEach { abs ->
                val obj = JSONObject().apply {
                    put("id", abs.id)
                    put("courseName", abs.courseName)
                    put("date", abs.date)
                    put("hours", abs.hours)
                    put("justified", abs.justified)
                    abs.type?.let { put("type", it) }
                    abs.reason?.let { put("reason", it) }
                }
                jsonArray.put(obj)
            }
            file.writeText(jsonArray.toString())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun loadAbsences(context: Context, schoolYear: String): List<StudentAbsence> {
        return try {
            val safeYear = schoolYear.replace("/", "-")
            val file = File(context.filesDir, "${ABSENCES_CACHE_PREFIX}${safeYear}.json")
            if (!file.exists()) return emptyList()
            val text = file.readText()
            val jsonArray = JSONArray(text)
            val list = mutableListOf<StudentAbsence>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(
                    StudentAbsence(
                        id = obj.optString("id", i.toString()),
                        courseName = obj.optString("courseName", "Cours"),
                        date = obj.optString("date", ""),
                        hours = obj.optString("hours", ""),
                        justified = obj.optBoolean("justified", false),
                        type = if (obj.has("type")) obj.optString("type") else null,
                        reason = if (obj.has("reason")) obj.optString("reason") else null
                    )
                )
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    private const val PERIODS_CACHE_FILE = "myefoss_periods_cache.json"

    fun saveStudentPeriods(context: Context, periods: List<StudentPeriod>) {
        try {
            val file = File(context.filesDir, PERIODS_CACHE_FILE)
            val jsonArray = JSONArray()
            periods.forEach { p ->
                val obj = JSONObject().apply {
                    put("schoolYear", p.schoolYear)
                    put("period", p.period)
                    put("programId", p.programId)
                    p.parity?.let { put("parity", it) }
                    put("isCurrentYear", p.isCurrentYear)
                }
                jsonArray.put(obj)
            }
            file.writeText(jsonArray.toString())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun loadStudentPeriods(context: Context): List<StudentPeriod> {
        return try {
            val file = File(context.filesDir, PERIODS_CACHE_FILE)
            if (!file.exists()) return emptyList()
            val text = file.readText()
            val jsonArray = JSONArray(text)
            val list = mutableListOf<StudentPeriod>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(
                    StudentPeriod(
                        schoolYear = obj.optString("schoolYear", ""),
                        period = obj.optString("period", ""),
                        programId = obj.optString("programId", ""),
                        parity = if (obj.has("parity")) obj.optString("parity") else null,
                        isCurrentYear = obj.optBoolean("isCurrentYear", false)
                    )
                )
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    private const val CAMPUS_CACHE_FILE = "myefoss_campus_cache.json"

    fun saveCampuses(context: Context, campuses: List<CampusInfo>) {
        try {
            val file = File(context.filesDir, CAMPUS_CACHE_FILE)
            val jsonArray = JSONArray()
            campuses.forEach { c ->
                val obj = JSONObject().apply {
                    put("id", c.id)
                    put("name", c.name)
                    put("city", c.city)
                    put("address", c.address)
                    c.postalCode?.let { put("postalCode", it) }
                    c.sitesDescription?.let { put("sitesDescription", it) }
                    c.latitude?.let { put("latitude", it) }
                    c.longitude?.let { put("longitude", it) }
                    put("campusGroup", c.campusGroup)
                    c.phone?.let { put("phone", it) }
                    c.caretakerPhone?.let { put("caretakerPhone", it) }
                    c.hours?.let { put("hours", it) }
                    c.email?.let { put("email", it) }
                    c.accessTransport?.let { put("accessTransport", it) }
                    c.description?.let { put("description", it) }
                }
                jsonArray.put(obj)
            }
            file.writeText(jsonArray.toString())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun loadCampuses(context: Context): List<CampusInfo> {
        return try {
            val file = File(context.filesDir, CAMPUS_CACHE_FILE)
            if (!file.exists()) return emptyList()
            val text = file.readText()
            val jsonArray = JSONArray(text)
            val list = mutableListOf<CampusInfo>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(
                    CampusInfo(
                        id = obj.optString("id", i.toString()),
                        name = obj.optString("name", "Campus Efrei"),
                        city = obj.optString("city", ""),
                        address = obj.optString("address", ""),
                        postalCode = if (obj.has("postalCode")) obj.optString("postalCode") else null,
                        sitesDescription = if (obj.has("sitesDescription")) obj.optString("sitesDescription") else null,
                        latitude = if (obj.has("latitude")) obj.optDouble("latitude") else null,
                        longitude = if (obj.has("longitude")) obj.optDouble("longitude") else null,
                        campusGroup = obj.optString("campusGroup", if (obj.optString("city").contains("Bordeaux", ignoreCase = true)) "Bordeaux" else "Paris"),
                        phone = if (obj.has("phone")) obj.optString("phone") else null,
                        caretakerPhone = if (obj.has("caretakerPhone")) obj.optString("caretakerPhone") else null,
                        hours = if (obj.has("hours")) obj.optString("hours") else null,
                        email = if (obj.has("email")) obj.optString("email") else null,
                        accessTransport = if (obj.has("accessTransport")) obj.optString("accessTransport") else null,
                        description = if (obj.has("description")) obj.optString("description") else null
                    )
                )
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    private const val LXP_CACHE_FILE = "myefoss_lxp_cache.json"

    fun saveLxpActions(context: Context, actions: List<LxpAction>) {
        try {
            val file = File(context.filesDir, LXP_CACHE_FILE)
            val jsonArray = JSONArray()
            actions.forEach { a ->
                val obj = JSONObject().apply {
                    put("id", a.id)
                    put("title", a.title)
                    put("description", a.description)
                    put("dateOrPeriod", a.dateOrPeriod)
                    put("category", a.category)
                    put("status", a.status)
                    put("isRegistered", a.isRegistered)
                    put("canRegister", a.canRegister)
                    put("locationOrRoom", a.locationOrRoom)
                    put("teacherOrSpeaker", a.teacherOrSpeaker)
                    put("maxParticipants", a.maxParticipants)
                    put("currentParticipants", a.currentParticipants)
                    put("xpPoints", a.xpPoints)
                    put("detailUrl", a.detailUrl)
                    put("registrationUrl", a.registrationUrl)
                    put("imageUrl", a.imageUrl)
                }
                jsonArray.put(obj)
            }
            file.writeText(jsonArray.toString())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun loadLxpActions(context: Context): List<LxpAction> {
        return try {
            val file = File(context.filesDir, LXP_CACHE_FILE)
            if (!file.exists()) return emptyList()
            val text = file.readText()
            val jsonArray = JSONArray(text)
            val list = mutableListOf<LxpAction>()
            for (i in 0 until jsonArray.length()) {
                list.add(LxpAction.fromJson(jsonArray.getJSONObject(i)))
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    private const val PROFILE_CACHE_FILE = "myefoss_profile_cache.json"

    fun saveStudentProfile(context: Context, profile: StudentProfile) {
        try {
            val file = File(context.filesDir, PROFILE_CACHE_FILE)
            val obj = JSONObject().apply {
                put("fullName", profile.fullName)
                put("firstName", profile.firstName)
                put("lastName", profile.lastName)
                put("email", profile.email)
                put("studentId", profile.studentId)
                put("program", profile.program)
            }
            file.writeText(obj.toString())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun loadStudentProfile(context: Context): StudentProfile? {
        return try {
            val file = File(context.filesDir, PROFILE_CACHE_FILE)
            if (!file.exists()) return null
            val obj = JSONObject(file.readText())
            StudentProfile(
                fullName = obj.optString("fullName", ""),
                firstName = obj.optString("firstName", ""),
                lastName = obj.optString("lastName", ""),
                email = obj.optString("email", ""),
                studentId = obj.optString("studentId", ""),
                program = obj.optString("program", "")
            )
        } catch (e: Exception) {
            null
        }
    }

    fun clearAllCache(context: Context) {
        try {
            val filesDir = context.filesDir ?: return
            filesDir.listFiles()?.forEach { file ->
                if (file.name.startsWith("myefoss_") && file.name.endsWith(".json")) {
                    file.delete()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

data class StudentGrade(
    val courseName: String,
    val gradeValue: String,
    val details: String,
    val date: String,
    val ue: String = "Modules Généraux",
    val average: String? = null,
    val semester: String? = null
)

data class StudentAbsence(
    val id: String,
    val courseName: String,
    val date: String,
    val hours: String,
    val justified: Boolean,
    val type: String? = null,
    val reason: String? = null
)

data class StudentPeriod(
    val schoolYear: String,
    val period: String,
    val programId: String,
    val parity: String? = null,
    val isCurrentYear: Boolean = false
)

data class CampusInfo(
    val id: String,
    val name: String,
    val city: String,
    val address: String,
    val postalCode: String? = null,
    val sitesDescription: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val campusGroup: String = "Paris",
    val phone: String? = null,
    val caretakerPhone: String? = null,
    val hours: String? = null,
    val email: String? = null,
    val accessTransport: String? = null,
    val description: String? = null
)

data class StudentProfile(
    val fullName: String,
    val firstName: String = "",
    val lastName: String = "",
    val email: String = "",
    val studentId: String = "",
    val program: String = ""
)

