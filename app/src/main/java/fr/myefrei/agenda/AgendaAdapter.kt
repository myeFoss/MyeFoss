package fr.myefrei.agenda

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView

class AgendaAdapter(
    private var daySections: List<DaySection> = emptyList(),
    private val onCourseClick: ((CourseEvent) -> Unit)? = null
) : RecyclerView.Adapter<AgendaAdapter.DayViewHolder>() {

    fun submitList(newList: List<DaySection>) {
        daySections = newList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DayViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_day_section, parent, false)
        return DayViewHolder(view, onCourseClick)
    }

    override fun onBindViewHolder(holder: DayViewHolder, position: Int) {
        holder.bind(daySections[position])
    }

    override fun getItemCount(): Int = daySections.size

    class DayViewHolder(
        itemView: View,
        private val onCourseClick: ((CourseEvent) -> Unit)?
    ) : RecyclerView.ViewHolder(itemView) {
        private val tvDayHeader: TextView = itemView.findViewById(R.id.tvDayHeader)
        private val viewTodayDot: View = itemView.findViewById(R.id.viewTodayDot)
        private val tvTodayBadge: TextView = itemView.findViewById(R.id.tvTodayBadge)
        private val tvEmptyDay: TextView = itemView.findViewById(R.id.tvEmptyDay)
        private val layoutCoursesContainer: LinearLayout = itemView.findViewById(R.id.layoutCoursesContainer)

        fun bind(section: DaySection) {
            val ctx = itemView.context
            tvDayHeader.text = section.dayLabel
            viewTodayDot.visibility = if (section.isToday) View.VISIBLE else View.GONE
            tvTodayBadge.visibility = if (section.isToday) View.VISIBLE else View.GONE

            layoutCoursesContainer.removeAllViews()

            if (section.courses.isEmpty()) {
                tvEmptyDay.visibility = View.VISIBLE
            } else {
                tvEmptyDay.visibility = View.GONE
                val inflater = LayoutInflater.from(ctx)

                section.courses.forEach { course ->
                    val cardView = inflater.inflate(R.layout.item_course_card, layoutCoursesContainer, false) as MaterialCardView
                    val tvCourseTime: TextView = cardView.findViewById(R.id.tvCourseTime)
                    val tvActivityBadge: TextView = cardView.findViewById(R.id.tvActivityBadge)
                    val tvCourseName: TextView = cardView.findViewById(R.id.tvCourseName)
                    val tvCourseLocation: TextView = cardView.findViewById(R.id.tvCourseLocation)
                    val tvCourseTeacher: TextView = cardView.findViewById(R.id.tvCourseTeacher)
                    val ivTeacherIcon: ImageView? = cardView.findViewById(R.id.ivTeacherIcon)

                    // Format Time
                    val timeStr = when {
                        !course.startTime.isNullOrBlank() && !course.endTime.isNullOrBlank() ->
                            "${course.startTime} - ${course.endTime}"
                        course.startDate != null && course.endDate != null -> {
                            val timeFormat = java.text.SimpleDateFormat("HH:mm", java.util.Locale.FRANCE)
                            "${timeFormat.format(course.startDate)} - ${timeFormat.format(course.endDate)}"
                        }
                        else -> "Horaire non précisé"
                    }
                    tvCourseTime.text = timeStr
                    tvCourseName.text = course.name

                    // Badge activity & styling
                    val isExam = course.sessionType.equals("exam", ignoreCase = true) ||
                            course.name.contains("exam", ignoreCase = true) ||
                            course.name.contains("partiel", ignoreCase = true) ||
                            course.name.contains("contrôle", ignoreCase = true)

                    val isTp = course.courseActivity.equals("TP", ignoreCase = true) ||
                            course.name.contains("tp", ignoreCase = true)

                    val isRemote = course.modality.equals("online", ignoreCase = true) ||
                            course.modality.equals("remote", ignoreCase = true) ||
                            course.name.contains("distanciel", ignoreCase = true)

                    when {
                        isExam -> {
                            tvActivityBadge.text = "Examen"
                            tvActivityBadge.setBackgroundColor(ContextCompat.getColor(ctx, R.color.badge_exam_bg))
                            tvActivityBadge.setTextColor(ContextCompat.getColor(ctx, R.color.badge_exam_text))
                            cardView.strokeColor = ContextCompat.getColor(ctx, R.color.badge_exam_text)
                        }
                        isTp -> {
                            tvActivityBadge.text = course.courseActivityName ?: "TP"
                            tvActivityBadge.setBackgroundColor(ContextCompat.getColor(ctx, R.color.badge_tp_bg))
                            tvActivityBadge.setTextColor(ContextCompat.getColor(ctx, R.color.badge_tp_text))
                        }
                        isRemote -> {
                            tvActivityBadge.text = "Distanciel"
                            tvActivityBadge.setBackgroundColor(ContextCompat.getColor(ctx, R.color.badge_remote_bg))
                            tvActivityBadge.setTextColor(ContextCompat.getColor(ctx, R.color.badge_remote_text))
                        }
                        else -> {
                            tvActivityBadge.text = course.courseActivityName ?: (course.courseActivity ?: "Cours")
                            tvActivityBadge.setBackgroundColor(ContextCompat.getColor(ctx, R.color.badge_normal_bg))
                            tvActivityBadge.setTextColor(ContextCompat.getColor(ctx, R.color.badge_normal_text))
                        }
                    }

                    // Location
                    val locDisplay = if (course.locations.isNotEmpty()) {
                        course.locations.first().formatDisplay()
                    } else if (isRemote) {
                        "En ligne (Teams / Visio)"
                    } else {
                        "Lieu non spécifié"
                    }
                    tvCourseLocation.text = locDisplay

                    // Teacher
                    if (course.teachers.isNotEmpty()) {
                        tvCourseTeacher.text = course.teachers.joinToString(", ")
                        tvCourseTeacher.visibility = View.VISIBLE
                        ivTeacherIcon?.visibility = View.VISIBLE
                    } else {
                        tvCourseTeacher.visibility = View.GONE
                        ivTeacherIcon?.visibility = View.GONE
                    }

                    // Click listener to show course details
                    cardView.setOnClickListener {
                        onCourseClick?.invoke(course)
                    }

                    layoutCoursesContainer.addView(cardView)
                }
            }
        }
    }
}
