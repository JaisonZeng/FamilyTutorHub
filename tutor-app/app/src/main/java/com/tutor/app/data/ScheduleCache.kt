package com.tutor.app.data

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class ScheduleCache(context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences("schedule_cache", Context.MODE_PRIVATE)
    private val gson = Gson()
    
    fun getSchedules(date: String): List<Schedule>? {
        val json = prefs.getString("schedules_$date", null) ?: return null
        val type = object : TypeToken<List<Schedule>>() {}.type
        return gson.fromJson(json, type)
    }
    
    fun saveSchedules(date: String, schedules: List<Schedule>) {
        prefs.edit().putString("schedules_$date", gson.toJson(schedules)).apply()
    }
    
    // 比较新旧数据，返回变化信息
    fun compareAndSave(date: String, newSchedules: List<Schedule>): DataChangeResult {
        val oldSchedules = getSchedules(date)
        
        if (oldSchedules == null) {
            saveSchedules(date, newSchedules)
            return if (newSchedules.isNotEmpty()) {
                DataChangeResult.NewData(newSchedules.size)
            } else {
                DataChangeResult.NoChange
            }
        }
        
        val oldIds = oldSchedules.map { it.id }.toSet()
        val newIds = newSchedules.map { it.id }.toSet()
        
        val added = newSchedules.filter { it.id !in oldIds }
        val removed = oldSchedules.filter { it.id !in newIds }
        val updated = newSchedules.filter { new ->
            oldSchedules.find { it.id == new.id }?.let { old ->
                old.studentName != new.studentName ||
                old.timeSlot != new.timeSlot ||
                old.subject != new.subject ||
                old.status != new.status
            } ?: false
        }
        
        if (added.isEmpty() && removed.isEmpty() && updated.isEmpty()) {
            return DataChangeResult.NoChange
        }
        
        saveSchedules(date, newSchedules)
        return DataChangeResult.Changed(added, updated, removed)
    }
}

sealed class DataChangeResult {
    data object NoChange : DataChangeResult()
    data class NewData(val count: Int) : DataChangeResult()
    data class Changed(
        val added: List<Schedule>,
        val updated: List<Schedule>,
        val removed: List<Schedule>
    ) : DataChangeResult()
}
