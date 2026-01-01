package com.tutor.app.data

import com.google.gson.annotations.SerializedName

data class Schedule(
    @SerializedName("id")
    val id: Int,
    @SerializedName("student_name")
    val studentName: String,
    @SerializedName("time_slot")
    val timeSlot: String,
    @SerializedName("subject")
    val subject: String,
    @SerializedName("status")
    val status: String, // pending, ongoing, completed
    @SerializedName("date")
    val date: String = "" // yyyy-MM-dd 格式
) {
    val startTime: String
        get() = timeSlot.split("-").firstOrNull() ?: ""
    
    val endTime: String
        get() = timeSlot.split("-").getOrNull(1) ?: ""
    
    val isOngoing: Boolean
        get() = status == "ongoing"
    
    val isCompleted: Boolean
        get() = status == "completed"
}
