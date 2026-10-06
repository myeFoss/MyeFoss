package fr.myefrei.agenda

import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

data class CourseLocation(
    val campus: String?,
    val bat: String?,
    val building: String?,
    val room: String?
) {
    fun formatDisplay(): String {
        val b = building ?: bat
        val r = if (!room.isNullOrBlank()) "Salle $room" else null
        return listOfNotNull(b, r).joinToString(" - ").ifBlank { campus ?: "Lieu non spécifié" }
    }
}

data class CourseEvent(
    val id: String,
    val name: String,
    val module: String?,
    val startTime: String?,
    val endTime: String?,
    val startDate: Date?,
    val endDate: Date?,
    val sessionType: String?,
    val courseActivity: String?,
    val courseActivityName: String?,
    val modality: String?,
    val locations: List<CourseLocation>,
    val teachers: List<String>
) {
    companion object {
        private val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).apply {
            timeZone = TimeZone.getTimeZone("Europe/Paris")
        }

        fun fromJson(json: JSONObject): CourseEvent {
            val locations = mutableListOf<CourseLocation>()
            json.optJSONArray("locations")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val locObj = arr.getJSONObject(i)
                    locations.add(
                        CourseLocation(
                            campus = locObj.optString("campus").takeIf { it.isNotBlank() },
                            bat = locObj.optString("bat").takeIf { it.isNotBlank() },
                            building = locObj.optString("building").takeIf { it.isNotBlank() },
                            room = locObj.optString("room").takeIf { it.isNotBlank() }
                        )
                    )
                }
            }

            val teachers = mutableListOf<String>()
            json.optJSONArray("teachers")?.let { arr ->
                for (i in 0 until arr.length()) {
                    teachers.add(arr.getString(i))
                }
            }

            fun parseIso(s: String?): Date? {
                if (s.isNullOrBlank()) return null
                return try {
                    // strip timezone offset if standard simpledateformat
                    val clean = if (s.length >= 19) s.substring(0, 19) else s
                    isoFormat.parse(clean)
                } catch (e: Exception) { null }
            }

            return CourseEvent(
                id = json.optString("id", java.util.UUID.randomUUID().toString()),
                name = json.optString("name", "Cours"),
                module = json.optString("module").takeIf { it.isNotBlank() },
                startTime = json.optString("startTime").takeIf { it.isNotBlank() },
                endTime = json.optString("endTime").takeIf { it.isNotBlank() },
                startDate = parseIso(json.optString("start")),
                endDate = parseIso(json.optString("end")),
                sessionType = json.optString("sessionType"),
                courseActivity = json.optString("courseActivity"),
                courseActivityName = json.optString("courseActivityName"),
                modality = json.optString("modality"),
                locations = locations,
                teachers = teachers
            )
        }
    }
}

data class DaySection(
    val date: Date,
    val dayLabel: String,
    val isToday: Boolean,
    val courses: List<CourseEvent>
)
