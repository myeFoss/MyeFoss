package org.myefoss.app

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton

class CourseDetailsBottomSheet : BottomSheetDialogFragment() {

    private var course: CourseEvent? = null

    companion object {
        fun newInstance(course: CourseEvent): CourseDetailsBottomSheet {
            val sheet = CourseDetailsBottomSheet()
            sheet.course = course
            return sheet
        }
    }

    override fun onStart() {
        super.onStart()
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            dialog?.window?.apply {
                addFlags(android.view.WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
                attributes = attributes.apply {
                    blurBehindRadius = 24
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_course_details, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val c = course ?: return

        val dialogTime: TextView = view.findViewById(R.id.dialogTime)
        val dialogActivityBadge: TextView = view.findViewById(R.id.dialogActivityBadge)
        val dialogCourseName: TextView = view.findViewById(R.id.dialogCourseName)
        val dialogModule: TextView = view.findViewById(R.id.dialogModule)
        val dialogLocation: TextView = view.findViewById(R.id.dialogLocation)
        val dialogTeacher: TextView = view.findViewById(R.id.dialogTeacher)
        val dialogModality: TextView = view.findViewById(R.id.dialogModality)
        val btnDialogClose: MaterialButton = view.findViewById(R.id.btnDialogClose)

        // Time format
        val timeStr = when {
            !c.startTime.isNullOrBlank() && !c.endTime.isNullOrBlank() -> "${c.startTime} - ${c.endTime}"
            c.startDate != null && c.endDate != null -> {
                val fmt = java.text.SimpleDateFormat("HH:mm", java.util.Locale.FRANCE)
                "${fmt.format(c.startDate)} - ${fmt.format(c.endDate)}"
            }
            else -> "Horaire non précisé"
        }
        dialogTime.text = timeStr
        dialogCourseName.text = c.name

        // Badges
        val isExam = c.sessionType.equals("exam", ignoreCase = true) ||
                c.name.contains("exam", ignoreCase = true) ||
                c.name.contains("partiel", ignoreCase = true)

        val isTp = c.courseActivity.equals("TP", ignoreCase = true) ||
                c.name.contains("tp", ignoreCase = true)

        val isRemote = c.modality.equals("online", ignoreCase = true) ||
                c.modality.equals("remote", ignoreCase = true) ||
                c.name.contains("distanciel", ignoreCase = true)

        val ctx = requireContext()
        when {
            isExam -> {
                dialogActivityBadge.text = "Examen"
                dialogActivityBadge.setBackgroundColor(ContextCompat.getColor(ctx, R.color.badge_exam_bg))
                dialogActivityBadge.setTextColor(ContextCompat.getColor(ctx, R.color.badge_exam_text))
            }
            isTp -> {
                dialogActivityBadge.text = c.courseActivityName ?: "TP"
                dialogActivityBadge.setBackgroundColor(ContextCompat.getColor(ctx, R.color.badge_tp_bg))
                dialogActivityBadge.setTextColor(ContextCompat.getColor(ctx, R.color.badge_tp_text))
            }
            isRemote -> {
                dialogActivityBadge.text = "Distanciel"
                dialogActivityBadge.setBackgroundColor(ContextCompat.getColor(ctx, R.color.badge_remote_bg))
                dialogActivityBadge.setTextColor(ContextCompat.getColor(ctx, R.color.badge_remote_text))
            }
            else -> {
                dialogActivityBadge.text = c.courseActivityName ?: (c.courseActivity ?: "Cours")
                dialogActivityBadge.setBackgroundColor(ContextCompat.getColor(ctx, R.color.badge_normal_bg))
                dialogActivityBadge.setTextColor(ContextCompat.getColor(ctx, R.color.badge_normal_text))
            }
        }

        // Module
        dialogModule.text = if (!c.module.isNullOrBlank()) "Module : ${c.module}" else "Code module non précisé"

        // Location
        val fullLocation = if (c.locations.isNotEmpty()) {
            val loc = c.locations.first()
            listOfNotNull(
                loc.campus?.let { "Campus $it" },
                (loc.building ?: loc.bat)?.let { "Bâtiment $it" },
                loc.room?.let { "Salle $it" }
            ).joinToString(" - ")
        } else if (isRemote) {
            "En ligne (Teams / Plateforme virtuelle)"
        } else {
            "Lieu non spécifié"
        }
        dialogLocation.text = fullLocation

        // Teacher
        dialogTeacher.text = if (c.teachers.isNotEmpty()) c.teachers.joinToString(", ") else "Non renseigné"

        // Modality
        dialogModality.text = when (c.modality) {
            "in_person" -> "Présentiel"
            "online", "remote" -> "Distanciel"
            else -> c.modality ?: "Présentiel"
        }

        btnDialogClose.setOnClickListener {
            dismiss()
        }
    }
}
