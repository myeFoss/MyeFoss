package org.myefoss.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton

class CampusDetailsBottomSheet : BottomSheetDialogFragment() {

    private var campus: CampusInfo? = null
    private var onNavigateClick: ((CampusInfo) -> Unit)? = null

    companion object {
        fun newInstance(
            campus: CampusInfo,
            onNavigateClick: (CampusInfo) -> Unit
        ): CampusDetailsBottomSheet {
            val sheet = CampusDetailsBottomSheet()
            sheet.campus = campus
            sheet.onNavigateClick = onNavigateClick
            return sheet
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_campus_details, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val c = campus ?: return

        val dialogCampusGroupBadge: TextView = view.findViewById(R.id.dialogCampusGroupBadge)
        val dialogCampusCity: TextView = view.findViewById(R.id.dialogCampusCity)
        val dialogCampusTitle: TextView = view.findViewById(R.id.dialogCampusTitle)
        val dialogCampusSubtitle: TextView = view.findViewById(R.id.dialogCampusSubtitle)
        val dialogCampusAddress: TextView = view.findViewById(R.id.dialogCampusAddress)

        val layoutDialogCaretaker: View = view.findViewById(R.id.layoutDialogCaretaker)
        val dialogCampusCaretaker: TextView = view.findViewById(R.id.dialogCampusCaretaker)
        val btnCallCaretaker: MaterialButton = view.findViewById(R.id.btnCallCaretaker)

        val layoutDialogPhone: View = view.findViewById(R.id.layoutDialogPhone)
        val dialogCampusPhone: TextView = view.findViewById(R.id.dialogCampusPhone)
        val btnCallPhone: MaterialButton = view.findViewById(R.id.btnCallPhone)

        val layoutDialogEmail: View = view.findViewById(R.id.layoutDialogEmail)
        val dialogCampusEmail: TextView = view.findViewById(R.id.dialogCampusEmail)
        val btnSendEmail: MaterialButton = view.findViewById(R.id.btnSendEmail)

        val layoutDialogHours: View = view.findViewById(R.id.layoutDialogHours)
        val dialogCampusHours: TextView = view.findViewById(R.id.dialogCampusHours)

        val layoutDialogAccess: View = view.findViewById(R.id.layoutDialogAccess)
        val dialogCampusAccess: TextView = view.findViewById(R.id.dialogCampusAccess)

        val btnDialogNavigate: MaterialButton = view.findViewById(R.id.btnDialogNavigate)
        val btnDialogDismiss: MaterialButton = view.findViewById(R.id.btnDialogDismiss)

        // Header info
        dialogCampusGroupBadge.text = "Campus ${c.campusGroup}"
        dialogCampusCity.text = if (!c.postalCode.isNullOrBlank()) "${c.city} (${c.postalCode})" else c.city
        dialogCampusTitle.text = c.name
        val desc = c.description?.takeIf { it.isNotBlank() } ?: c.sitesDescription?.takeIf { it.isNotBlank() }
        if (desc != null) {
            dialogCampusSubtitle.text = desc
            dialogCampusSubtitle.visibility = View.VISIBLE
        } else {
            dialogCampusSubtitle.visibility = View.GONE
        }
        dialogCampusAddress.text = c.address.replace("\n", ", ")

        // Caretaker (Gardien / Sécurité)
        if (!c.caretakerPhone.isNullOrBlank()) {
            layoutDialogCaretaker.visibility = View.VISIBLE
            dialogCampusCaretaker.text = c.caretakerPhone
            btnCallCaretaker.setOnClickListener {
                val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${c.caretakerPhone.replace(" ", "")}"))
                startActivity(dialIntent)
            }
        } else {
            layoutDialogCaretaker.visibility = View.GONE
        }

        // Standard / Accueil Phone
        if (!c.phone.isNullOrBlank()) {
            layoutDialogPhone.visibility = View.VISIBLE
            dialogCampusPhone.text = c.phone
            btnCallPhone.setOnClickListener {
                val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${c.phone.replace(" ", "")}"))
                startActivity(dialIntent)
            }
        } else {
            layoutDialogPhone.visibility = View.GONE
        }

        // Email
        if (!c.email.isNullOrBlank()) {
            layoutDialogEmail.visibility = View.VISIBLE
            dialogCampusEmail.text = c.email
            btnSendEmail.setOnClickListener {
                val mailIntent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:${c.email}")).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                startActivity(mailIntent)
            }
        } else {
            layoutDialogEmail.visibility = View.GONE
        }

        // Hours
        if (!c.hours.isNullOrBlank()) {
            layoutDialogHours.visibility = View.VISIBLE
            dialogCampusHours.text = c.hours
        } else {
            layoutDialogHours.visibility = View.GONE
        }

        // Access & transport
        if (!c.accessTransport.isNullOrBlank()) {
            layoutDialogAccess.visibility = View.VISIBLE
            dialogCampusAccess.text = c.accessTransport
        } else {
            layoutDialogAccess.visibility = View.GONE
        }

        // Navigate button
        btnDialogNavigate.setOnClickListener {
            dismiss()
            onNavigateClick?.invoke(c)
        }

        btnDialogDismiss.setOnClickListener {
            dismiss()
        }
    }
}
