package fr.myefrei.agenda

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

object OfflineCacheManager {

    private const val CACHE_FILE_NAME = "efrei_planning_cache.json"

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
                        val fmt = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US)
                        put("start", fmt.format(it))
                    }
                    course.endDate?.let {
                        val fmt = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US)
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
}
