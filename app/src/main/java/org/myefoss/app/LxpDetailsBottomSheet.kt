package org.myefoss.app

import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.imageview.ShapeableImageView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

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
        return inflater.inflate(R.layout.dialog_lxp_details, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val act = action ?: return

        val dialogLxpCategory: TextView = view.findViewById(R.id.dialogLxpCategory)
        val dialogLxpXpBadge: TextView = view.findViewById(R.id.dialogLxpXpBadge)
        val dialogLxpStatusBadge: TextView = view.findViewById(R.id.dialogLxpStatusBadge)
        val dialogLxpTitle: TextView = view.findViewById(R.id.dialogLxpTitle)
        val dialogLxpThumbnail: ShapeableImageView = view.findViewById(R.id.dialogLxpThumbnail)
        val dialogLxpDescription: TextView = view.findViewById(R.id.dialogLxpDescription)

        val dialogLxpDate: TextView = view.findViewById(R.id.dialogLxpDate)
        val layoutDialogLxpSpeaker: View = view.findViewById(R.id.layoutDialogLxpSpeaker)
        val dialogLxpSpeaker: TextView = view.findViewById(R.id.dialogLxpSpeaker)
        val layoutDialogLxpLocation: View = view.findViewById(R.id.layoutDialogLxpLocation)
        val dialogLxpLocation: TextView = view.findViewById(R.id.dialogLxpLocation)
        val layoutDialogLxpCapacity: View = view.findViewById(R.id.layoutDialogLxpCapacity)
        val dialogLxpCapacity: TextView = view.findViewById(R.id.dialogLxpCapacity)

        val btnDialogLxpClose: MaterialButton = view.findViewById(R.id.btnDialogLxpClose)
        val btnDialogLxpRegister: MaterialButton = view.findViewById(R.id.btnDialogLxpRegister)

        dialogLxpCategory.text = if (act.category.isNotBlank()) act.category else "Formation / Atelier"
        dialogLxpTitle.text = act.title
        dialogLxpDescription.text = if (act.description.isNotBlank()) act.description else "Aucune description détaillée fournie."

        // XP Badge
        if (act.xpPoints.isNotBlank()) {
            val cleanXp = act.xpPoints.replace(Regex("\\s+"), " ").trim()
            dialogLxpXpBadge.text = if (cleanXp.startsWith("+")) cleanXp else "+$cleanXp"
            dialogLxpXpBadge.visibility = View.VISIBLE
        } else {
            dialogLxpXpBadge.visibility = View.GONE
        }

        // Thumbnail image
        if (act.imageUrl.isNotBlank()) {
            val rawUrl = act.imageUrl
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
                if (bitmap != null && isAdded) {
                    dialogLxpThumbnail.setImageBitmap(bitmap)
                    dialogLxpThumbnail.visibility = View.VISIBLE
                }
            }
        } else {
            dialogLxpThumbnail.visibility = View.GONE
        }

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
            dialogLxpDate.text = "Date à venir"
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

        if (act.maxParticipants > 0) {
            layoutDialogLxpCapacity.visibility = View.VISIBLE
            val placesLeft = act.maxParticipants - act.currentParticipants
            dialogLxpCapacity.text = if (placesLeft > 0) {
                "${act.currentParticipants}/${act.maxParticipants} participants ($placesLeft place${if (placesLeft > 1) "s" else ""} restante${if (placesLeft > 1) "s" else ""})"
            } else {
                "${act.currentParticipants}/${act.maxParticipants} participants (Complet)"
            }
        } else if (act.currentParticipants > 0) {
            layoutDialogLxpCapacity.visibility = View.VISIBLE
            dialogLxpCapacity.text = "${act.currentParticipants} inscrit${if (act.currentParticipants > 1) "s" else ""}"
        } else {
            layoutDialogLxpCapacity.visibility = View.GONE
        }

        val layoutDialogLxpPointsRow: View = view.findViewById(R.id.layoutDialogLxpPointsRow)
        val dialogLxpPointsDetail: TextView = view.findViewById(R.id.dialogLxpPointsDetail)

        if (act.xpPoints.isNotBlank()) {
            val cleanXp = act.xpPoints.replace(Regex("\\s+"), " ").trim()
            layoutDialogLxpPointsRow.visibility = View.VISIBLE
            dialogLxpPointsDetail.text = "$cleanXp à valider"
        } else {
            layoutDialogLxpPointsRow.visibility = View.GONE
        }

        btnDialogLxpClose.setOnClickListener { dismiss() }

        btnDialogLxpRegister.setOnClickListener {
            dismiss()
            onRegisterClick?.invoke(act)
        }
    }
}
