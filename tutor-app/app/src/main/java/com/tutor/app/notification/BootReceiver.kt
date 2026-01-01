package com.tutor.app.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.tutor.app.data.ScheduleCache
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * 开机后重新设置课程提醒
 * 因为 AlarmManager 的闹钟在设备重启后会丢失
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            CoroutineScope(Dispatchers.IO).launch {
                val scheduleCache = ScheduleCache(context)
                val reminderManager = LessonReminderManager(context)
                val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
                
                // 重新设置今天和明天的课程提醒
                listOf(LocalDate.now(), LocalDate.now().plusDays(1)).forEach { date ->
                    val dateStr = date.format(dateFormatter)
                    scheduleCache.getSchedules(dateStr)?.let { schedules ->
                        reminderManager.scheduleReminders(schedules)
                    }
                }
            }
        }
    }
}
