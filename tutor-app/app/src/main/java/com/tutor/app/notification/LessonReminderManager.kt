package com.tutor.app.notification

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.tutor.app.MainActivity
import com.tutor.app.R
import com.tutor.app.data.Schedule
import java.text.SimpleDateFormat
import java.util.*

class LessonReminderManager(private val context: Context) {

    companion object {
        private const val TAG = "LessonReminderManager"
        private const val REMINDER_MINUTES_BEFORE = 10L
        private const val CHANNEL_ID = "lesson_reminder_channel"
        // 用于区分不同类型的提醒，避免 ID 冲突
        private const val REMINDER_TYPE_BEFORE = 0
        private const val REMINDER_TYPE_START = 100000
        // 记录已通知的正在上课课程，避免重复通知
        private val notifiedOngoingLessons = mutableSetOf<Int>()
    }

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val notificationManager = context.getSystemService(NotificationManager::class.java)

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "课程提醒",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "课程开始提醒"
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * 立即显示"正在上课"通知（用于已经开始的课程）
     * 启动前台服务，持续显示剩余时间
     */
    fun showOngoingLessonNotification(schedule: Schedule) {
        // 避免重复启动
        if (notifiedOngoingLessons.contains(schedule.id)) {
            Log.d(TAG, "Already notified for ongoing lesson ${schedule.id}")
            return
        }
        notifiedOngoingLessons.add(schedule.id)

        // 启动前台服务显示持续通知
        OngoingLessonService.start(
            context = context,
            scheduleId = schedule.id,
            studentName = schedule.studentName,
            subject = schedule.subject,
            timeSlot = schedule.timeSlot,
            endTime = schedule.endTime
        )
        Log.d(TAG, "Started ongoing lesson service for ${schedule.studentName}")
    }

    /**
     * 停止正在上课的通知
     */
    fun stopOngoingLessonNotification() {
        OngoingLessonService.stop(context)
        notifiedOngoingLessons.clear()
    }

    /**
     * 为课程设置所有提醒（上课前10分钟 + 上课时）
     */
    fun scheduleReminder(schedule: Schedule) {
        scheduleBeforeReminder(schedule)
        scheduleStartReminder(schedule)
    }

    /**
     * 设置上课前10分钟提醒
     */
    private fun scheduleBeforeReminder(schedule: Schedule) {
        val triggerTime = calculateTriggerTime(schedule, REMINDER_MINUTES_BEFORE) ?: return

        if (triggerTime <= System.currentTimeMillis()) {
            Log.d(TAG, "Before reminder time already passed for schedule ${schedule.id}")
            return
        }

        val intent = Intent(context, LessonReminderReceiver::class.java).apply {
            putExtra(LessonReminderReceiver.EXTRA_SCHEDULE_ID, schedule.id)
            putExtra(LessonReminderReceiver.EXTRA_STUDENT_NAME, schedule.studentName)
            putExtra(LessonReminderReceiver.EXTRA_SUBJECT, schedule.subject)
            putExtra(LessonReminderReceiver.EXTRA_TIME_SLOT, schedule.timeSlot)
            putExtra(LessonReminderReceiver.EXTRA_REMINDER_TYPE, LessonReminderReceiver.TYPE_BEFORE)
        }

        scheduleAlarm(schedule.id + REMINDER_TYPE_BEFORE, triggerTime, intent)
        Log.d(TAG, "Before reminder scheduled for ${schedule.studentName} at ${Date(triggerTime)}")
    }

    /**
     * 设置上课时提醒
     */
    private fun scheduleStartReminder(schedule: Schedule) {
        val triggerTime = calculateTriggerTime(schedule, 0) ?: return

        if (triggerTime <= System.currentTimeMillis()) {
            Log.d(TAG, "Start reminder time already passed for schedule ${schedule.id}")
            return
        }

        val intent = Intent(context, LessonReminderReceiver::class.java).apply {
            putExtra(LessonReminderReceiver.EXTRA_SCHEDULE_ID, schedule.id)
            putExtra(LessonReminderReceiver.EXTRA_STUDENT_NAME, schedule.studentName)
            putExtra(LessonReminderReceiver.EXTRA_SUBJECT, schedule.subject)
            putExtra(LessonReminderReceiver.EXTRA_TIME_SLOT, schedule.timeSlot)
            putExtra(LessonReminderReceiver.EXTRA_REMINDER_TYPE, LessonReminderReceiver.TYPE_START)
        }

        scheduleAlarm(schedule.id + REMINDER_TYPE_START, triggerTime, intent)
        Log.d(TAG, "Start reminder scheduled for ${schedule.studentName} at ${Date(triggerTime)}")
    }

    private fun scheduleAlarm(requestCode: Int, triggerTime: Long, intent: Intent) {
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                    )
                } else {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                    )
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to schedule alarm", e)
        }
    }

    /**
     * 为多个课程批量设置提醒
     */
    fun scheduleReminders(schedules: List<Schedule>) {
        schedules.forEach { schedule ->
            if (schedule.status == "pending") {
                scheduleReminder(schedule)
            }
        }
    }

    /**
     * 取消课程提醒
     */
    fun cancelReminder(scheduleId: Int) {
        // 取消上课前提醒
        cancelAlarm(scheduleId + REMINDER_TYPE_BEFORE)
        // 取消上课时提醒
        cancelAlarm(scheduleId + REMINDER_TYPE_START)
        Log.d(TAG, "Reminders cancelled for schedule $scheduleId")
    }

    private fun cancelAlarm(requestCode: Int) {
        val intent = Intent(context, LessonReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    /**
     * 计算提醒触发时间
     * @param minutesBefore 提前多少分钟，0表示准时
     */
    private fun calculateTriggerTime(schedule: Schedule, minutesBefore: Long): Long? {
        return try {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

            val date = dateFormat.parse(schedule.date) ?: return null
            val time = timeFormat.parse(schedule.startTime) ?: return null

            val calendar = Calendar.getInstance().apply {
                this.time = date
                val timeCalendar = Calendar.getInstance().apply { this.time = time }
                set(Calendar.HOUR_OF_DAY, timeCalendar.get(Calendar.HOUR_OF_DAY))
                set(Calendar.MINUTE, timeCalendar.get(Calendar.MINUTE))
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                // 提前指定分钟数
                if (minutesBefore > 0) {
                    add(Calendar.MINUTE, -minutesBefore.toInt())
                }
            }

            calendar.timeInMillis
        } catch (e: Exception) {
            Log.e(TAG, "Failed to calculate trigger time", e)
            null
        }
    }
}
