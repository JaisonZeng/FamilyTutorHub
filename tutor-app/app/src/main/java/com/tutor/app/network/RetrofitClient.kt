package com.tutor.app.network

import android.content.Context
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

    private var currentBaseUrl: String = "http://172.20.10.4:8080/"
    private var retrofit: Retrofit? = null
    private var _apiService: ApiService? = null
    private var appContext: Context? = null

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val authInterceptor = Interceptor { chain ->
        val originalRequest = chain.request()
        val requestBuilder = originalRequest.newBuilder()

        appContext?.let { context ->
            try {
                val authManager = com.tutor.app.data.AuthManager(context)
                val token = runBlocking {
                    authManager.token.first()
                }
                token?.let {
                    requestBuilder.addHeader("Authorization", "Bearer $it")
                }
            } catch (e: Exception) {
                // Ignore token errors
            }
        }

        chain.proceed(requestBuilder.build())
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .addInterceptor(authInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    val apiService: ApiService
        get() {
            if (_apiService == null) {
                _apiService = createApiService(currentBaseUrl)
            }
            return _apiService!!
        }

    fun updateBaseUrl(newUrl: String) {
        if (newUrl != currentBaseUrl) {
            currentBaseUrl = newUrl
            _apiService = createApiService(newUrl)
        }
    }

    private fun createApiService(baseUrl: String): ApiService {
        val url = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        retrofit = Retrofit.Builder()
            .baseUrl(url)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        return retrofit!!.create(ApiService::class.java)
    }
}
