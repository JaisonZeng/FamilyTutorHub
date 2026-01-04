package com.tutor.app.network

import com.tutor.app.data.Schedule
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface ApiService {

    @POST("api/login")
    suspend fun login(@Body request: LoginRequest): LoginResponse

    @GET("health")
    suspend fun healthCheck(): HealthCheckResponse

    @GET("api/dashboard/today")
    suspend fun getTodaySchedules(): List<Schedule>

    @GET("api/dashboard/date")
    suspend fun getSchedulesByDate(@Query("date") date: String): List<Schedule>
}

data class LoginRequest(
    val username: String,
    val password: String
)

data class LoginResponse(
    val token: String,
    val currentUser: UserInfo
)

data class UserInfo(
    val id: Int,
    val username: String,
    val name: String,
    val avatar: String?
)

data class HealthCheckResponse(
    val status: String
)
