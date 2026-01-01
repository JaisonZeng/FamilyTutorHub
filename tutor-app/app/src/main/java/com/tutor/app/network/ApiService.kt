package com.tutor.app.network

import com.tutor.app.data.Schedule
import retrofit2.http.GET
import retrofit2.http.Query

interface ApiService {
    @GET("api/dashboard/today")
    suspend fun getTodaySchedules(): List<Schedule>
    
    @GET("api/dashboard/date")
    suspend fun getSchedulesByDate(@Query("date") date: String): List<Schedule>
}
