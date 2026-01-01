package com.tutor.app.network

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {
    
    private var currentBaseUrl: String = "http://172.20.10.4:8080/"
    private var retrofit: Retrofit? = null
    private var _apiService: ApiService? = null
    
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()
    
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
