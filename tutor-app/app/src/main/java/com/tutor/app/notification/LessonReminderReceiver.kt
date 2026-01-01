package com.tutor.app.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.tutor.app.MainActivity
import com.tutor.app.R

class LessonReminderReceiver : BroadcastReceiver() {

    companion object {
        const val CHANNEL_ID = "lesson_reminder_channel"
        const val EXTRA_STUDENT_NAME = "student_name"
        const val EXTRA_SUBJECT = "subject"
        const val EXTRA_TIME_SLOT = "time_slot"
        const val EXTRA_SCHEDULE_ID = "schedule_id"
        const val EXTRA_REMINDER_TYPE = "reminder_type"
        
        const val TYPE_BEFORE = 0  // 上课前提醒
        const val TYPE_START = 1   // 上课时提醒
    }

    override fun onReceive(context: Context, intent: Intent) {
        val studentName = intent.getStringExtra(EXTRA_STUDENT_NAME) ?: return
        val subject = intent.getStringExtra(EXTRA_SUBJECT) ?: ""
        val timeSlot = intent.getStringExtra(EXTRA_TIME_SLOT) ?: ""
        val scheduleId = intent.getIntExtra(EXTRA_SCHEDULE_ID, 0)
        val reminderType = intent.getIntExtra(EXTRA_REMINDER_TYPE, TYPE_BEFORE)

        createNotificationChannel(context)
        showNotification(context, scheduleId, studentName, subject, timeSlot, reminderType)
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "课程提醒"
            val descriptionText = "课程开始提醒"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                enableVibration(true)
            }
            val notificationManager = context.getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun showNotification(
        context: Context,
        scheduleId: Int,
        studentName: String,
        subject: String,
        timeSlot: String,
        reminderType: Int
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, scheduleId, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val (title, content, bigText) = when (reminderType) {
            TYPE_START -> Triple(
                "🎓 正在上课！",
                "$studentName - $subject",
                "学生: $studentName\n科目: $subject\n时间: $timeSlot\n\n课程已经开始啦！"
            )
            else -> Triple(
                "⏰ 课程即将开始",
                "$studentName - $subject ($timeSlot)",
                "学生: $studentName\n科目: $subject\n时间: $timeSlot\n\n课程将在10分钟后开始"
            )
        }

        // 使用不同的通知 ID 避免覆盖
        val notificationId = if (reminderType == TYPE_START) scheduleId + 100000 else scheduleId

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val notificationManager = context.getSystemService(NotificationManager::class.java)
        notificationManager.notify(notificationId, notification)
    }
}
