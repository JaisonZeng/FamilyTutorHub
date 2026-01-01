package com.tutor.app.util

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.provider.CalendarContract
import com.tutor.app.data.Schedule
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object CalendarHelper {
    
    // 使用 Intent 方式添加日历事件（不需要权限，用户确认）
    fun addToCalendarWithIntent(context: Context, schedule: Schedule, date: LocalDate) {
        val startTime = parseTime(date, schedule.startTime)
        val endTime = parseTime(date, schedule.endTime)
        
        // 提前20分钟提醒
        val reminderTime = startTime.minusMinutes(20)
        
        val intent = Intent(Intent.ACTION_INSERT).apply {
            data = CalendarContract.Events.CONTENT_URI
            putExtra(CalendarContract.Events.TITLE, "${schedule.studentName} - ${schedule.subject}")
            putExtra(CalendarContract.Events.DESCRIPTION, "课程时间: ${schedule.timeSlot}")
            putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, startTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli())
            putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli())
            putExtra(CalendarContract.Events.HAS_ALARM, true)
            // 设置提醒（提前20分钟）
            putExtra(Intent.EXTRA_EMAIL, "") // 清空邮件
        }
        
        context.startActivity(intent)
    }
    
    // 直接写入日历（需要权限）
    fun addToCalendarDirect(context: Context, schedule: Schedule, date: LocalDate): Boolean {
        return try {
            val startTime = parseTime(date, schedule.startTime)
            val endTime = parseTime(date, schedule.endTime)
            
            val startMillis = startTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val endMillis = endTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            
            // 获取默认日历ID
            val calendarId = getDefaultCalendarId(context) ?: return false
            
            val values = ContentValues().apply {
                put(CalendarContract.Events.DTSTART, startMillis)
                put(CalendarContract.Events.DTEND, endMillis)
                put(CalendarContract.Events.TITLE, "${schedule.studentName} - ${schedule.subject}")
                put(CalendarContract.Events.DESCRIPTION, "课程时间: ${schedule.timeSlot}")
                put(CalendarContract.Events.CALENDAR_ID, calendarId)
                put(CalendarContract.Events.EVENT_TIMEZONE, ZoneId.systemDefault().id)
                put(CalendarContract.Events.HAS_ALARM, 1)
            }
            
            val uri = context.contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)
            
            // 添加提醒（提前20分钟）
            uri?.lastPathSegment?.toLongOrNull()?.let { eventId ->
                val reminderValues = ContentValues().apply {
                    put(CalendarContract.Reminders.EVENT_ID, eventId)
                    put(CalendarContract.Reminders.MINUTES, 20)
                    put(CalendarContract.Reminders.METHOD, CalendarContract.Reminders.METHOD_ALERT)
                }
                context.contentResolver.insert(CalendarContract.Reminders.CONTENT_URI, reminderValues)
            }
            
            uri != null
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    private fun getDefaultCalendarId(context: Context): Long? {
        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.IS_PRIMARY
        )
        
        context.contentResolver.query(
            CalendarContract.Calendars.CONTENT_URI,
            projection,
            null,
            null,
            null
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                val id = cursor.getLong(0)
                val isPrimary = cursor.getInt(1) == 1
                if (isPrimary) return id
            }
            // 如果没有主日历，返回第一个
            if (cursor.moveToFirst()) {
                return cursor.getLong(0)
            }
        }
        return null
    }
    
    private fun parseTime(date: LocalDate, time: String): LocalDateTime {
        val timeParts = time.trim().split(":")
        val hour = timeParts.getOrNull(0)?.toIntOrNull() ?: 0
        val minute = timeParts.getOrNull(1)?.toIntOrNull() ?: 0
        return date.atTime(hour, minute)
    }
}
