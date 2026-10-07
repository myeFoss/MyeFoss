package org.myefoss.app

import org.json.JSONObject

data class LxpAction(
    val id: String,
    val title: String,
    val description: String = "",
    val dateOrPeriod: String = "",
    val category: String = "",
    val status: String = "", // e.g. "available", "registered", "closed"
    val isRegistered: Boolean = false,
    val canRegister: Boolean = true,
    val locationOrRoom: String = "",
    val teacherOrSpeaker: String = "",
    val maxParticipants: Int = 0,
    val currentParticipants: Int = 0,
    val detailUrl: String = "",
    val registrationUrl: String = ""
) {
    companion object {
        fun fromJson(json: JSONObject): LxpAction {
            val id = json.optString("id", json.optString("_id", json.optString("actionId", "")))
            val title = json.optString("title", json.optString("name", json.optString("label", "Action")))
            val desc = json.optString("description", json.optString("summary", ""))
            val date = json.optString("date", json.optString("period", json.optString("startDate", "")))
            val cat = json.optString("category", json.optString("type", ""))
            val status = json.optString("status", "")
            val isReg = json.optBoolean("isRegistered", json.optBoolean("registered", false))
            val canReg = json.optBoolean("canRegister", !isReg && status.lowercase() != "closed")
            val loc = json.optString("location", json.optString("room", ""))
            val speaker = json.optString("speaker", json.optString("teacher", json.optString("instructor", "")))
            val maxP = json.optInt("maxParticipants", json.optInt("capacity", 0))
            val curP = json.optInt("currentParticipants", json.optInt("enrolled", 0))
            val detailUrl = json.optString("url", json.optString("detailUrl", ""))
            val regUrl = json.optString("registrationUrl", json.optString("subscribeUrl", ""))

            return LxpAction(
                id = id,
                title = title,
                description = desc,
                dateOrPeriod = date,
                category = cat,
                status = status,
                isRegistered = isReg,
                canRegister = canReg,
                locationOrRoom = loc,
                teacherOrSpeaker = speaker,
                maxParticipants = maxP,
                currentParticipants = curP,
                detailUrl = detailUrl,
                registrationUrl = regUrl
            )
        }
    }
}
