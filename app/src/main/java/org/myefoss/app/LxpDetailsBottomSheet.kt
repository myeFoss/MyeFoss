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

class LxpDetailsBottomSheet : BottomSheetDialogFragment() {

    private var action: LxpAction? = null
    private var onRegisterClick: ((LxpAction) -> Unit)? = null

    companion object {
        fun newInstance(
            action: LxpAction,
            onRegisterClick: (LxpAction) -> Unit
        ): LxpDetailsBottomSheet {
            val sheet = LxpDetailsBottomSheet()
            sheet.action = action
            sheet.onRegisterClick = onRegisterClick
            return sheet
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_lxp_details, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val act = action ?: return

        val dialogLxpCategory: TextView = view.findViewById(R.id.dialogLxpCategory)
        val dialogLxpStatusBadge: TextView = view.findViewById(R.id.dialogLxpStatusBadge)
        val dialogLxpTitle: TextView = view.findViewById(R.id.dialogLxpTitle)
        val dialogLxpDescription: TextView = view.findViewById(R.id.dialogLxpDescription)

        val dialogLxpDate: TextView = view.findViewById(R.id.dialogLxpDate)
        val layoutDialogLxpSpeaker: View = view.findViewById(R.id.layoutDialogLxpSpeaker)
        val dialogLxpSpeaker: TextView = view.findViewById(R.id.dialogLxpSpeaker)
        val layoutDialogLxpLocation: View = view.findViewById(R.id.layoutDialogLxpLocation)
        val dialogLxpLocation: TextView = view.findViewById(R.id.dialogLxpLocation)

        val btnDialogLxpClose: MaterialButton = view.findViewById(R.id.btnDialogLxpClose)
        val btnDialogLxpRegister: MaterialButton = view.findViewById(R.id.btnDialogLxpRegister)

        dialogLxpCategory.text = if (act.category.isNotBlank()) act.category else "Action pédagogique"
        dialogLxpTitle.text = act.title
        dialogLxpDescription.text = if (act.description.isNotBlank()) act.description else "Aucune description détaillée fournie."

        if (act.isRegistered) {
            dialogLxpStatusBadge.text = "Inscrit"
            btnDialogLxpRegister.text = "Déjà inscrit"
            btnDialogLxpRegister.isEnabled = false
        } else if (!act.canRegister) {
            dialogLxpStatusBadge.text = "Complet / Clôturé"
            btnDialogLxpRegister.text = "Inscriptions fermées"
            btnDialogLxpRegister.isEnabled = false
        } else {
            dialogLxpStatusBadge.text = if (act.status.isNotBlank()) act.status else "Disponible"
            btnDialogLxpRegister.text = "S'inscrire"
            btnDialogLxpRegister.isEnabled = true
        }

        if (act.dateOrPeriod.isNotBlank()) {
            dialogLxpDate.text = act.dateOrPeriod
        } else {
            dialogLxpDate.text = "Date non spécifiée"
        }

        if (act.teacherOrSpeaker.isNotBlank()) {
            layoutDialogLxpSpeaker.visibility = View.VISIBLE
            dialogLxpSpeaker.text = act.teacherOrSpeaker
        } else {
            layoutDialogLxpSpeaker.visibility = View.GONE
        }

        if (act.locationOrRoom.isNotBlank()) {
            layoutDialogLxpLocation.visibility = View.VISIBLE
            dialogLxpLocation.text = act.locationOrRoom
        } else {
            layoutDialogLxpLocation.visibility = View.GONE
        }

        btnDialogLxpClose.setOnClickListener { dismiss() }

        btnDialogLxpRegister.setOnClickListener {
            dismiss()
            onRegisterClick?.invoke(act)
        }
    }
}
