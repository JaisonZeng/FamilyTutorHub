package com.tutor.app.data

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

data class SyncLog(
    val timestamp: String,
    val date: String,
    val action: String, // "新增", "更新", "删除"
    val details: String
)

class SyncLogManager(context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences("sync_logs", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val maxLogs = 100 // 最多保存100条日志
    
    fun getLogs(): List<SyncLog> {
        val json = prefs.getString("logs", "[]") ?: "[]"
        val type = object : TypeToken<List<SyncLog>>() {}.type
        return gson.fromJson(json, type)
    }
    
    fun addLog(date: String, action: String, details: String) {
        val logs = getLogs().toMutableList()
        val newLog = SyncLog(
            timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("MM-dd HH:mm:ss")),
            date = date,
            action = action,
            details = details
        )
        logs.add(0, newLog)
        
        // 保持日志数量在限制内
        val trimmedLogs = if (logs.size > maxLogs) logs.take(maxLogs) else logs
        
        prefs.edit().putString("logs", gson.toJson(trimmedLogs)).apply()
    }
    
    fun clearLogs() {
        prefs.edit().putString("logs", "[]").apply()
    }
}
