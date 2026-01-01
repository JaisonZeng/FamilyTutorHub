package com.tutor.app.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.tutor.app.MainActivity
import com.tutor.app.R
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.*

class OngoingLessonService : Service() {

    companion object {
        private const val TAG = "OngoingLessonService"
        const val CHANNEL_ID = "ongoing_lesson_channel"
        const val NOTIFICATION_ID = 1001

        const val ACTION_START = "START"
        const val ACTION_STOP = "STOP"

        const val EXTRA_SCHEDULE_ID = "schedule_id"
        const val EXTRA_STUDENT_NAME = "student_name"
        const val EXTRA_SUBJECT = "subject"
        const val EXTRA_TIME_SLOT = "time_slot"
        const val EXTRA_END_TIME = "end_time"

        fun start(
            context: Context,
            scheduleId: Int,
            studentName: String,
            subject: String,
            timeSlot: String,
            endTime: String
        ) {
            val intent = Intent(context, OngoingLessonService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_SCHEDULE_ID, scheduleId)
                putExtra(EXTRA_STUDENT_NAME, studentName)
                putExtra(EXTRA_SUBJECT, subject)
                putExtra(EXTRA_TIME_SLOT, timeSlot)
                putExtra(EXTRA_END_TIME, endTime)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, OngoingLessonService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    private var updateJob: Job? = null
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private var scheduleId: Int = 0
    private var studentName: String = ""
    private var subject: String = ""
    private var timeSlot: String = ""
    private var endTimeMillis: Long = 0

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                scheduleId = intent.getIntExtra(EXTRA_SCHEDULE_ID, 0)
                studentName = intent.getStringExtra(EXTRA_STUDENT_NAME) ?: ""
                subject = intent.getStringExtra(EXTRA_SUBJECT) ?: ""
                timeSlot = intent.getStringExtra(EXTRA_TIME_SLOT) ?: ""
                val endTimeStr = intent.getStringExtra(EXTRA_END_TIME) ?: ""

                endTimeMillis = parseEndTime(endTimeStr)

                startForeground(NOTIFICATION_ID, createNotification())
                startUpdating()
                Log.d(TAG, "Service started for $studentName - $subject")
            }
            ACTION_STOP -> {
                stopUpdating()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                Log.d(TAG, "Service stopped")
            }
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        stopUpdating()
        serviceScope.cancel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "正在上课",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "显示当前正在进行的课程"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val remainingText = getRemainingTimeText()

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("🎓 正在上课")
            .setContentText("$studentName - $subject")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("$studentName - $subject\n时间: $timeSlot\n$remainingText")
            )
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .build()
    }

    private fun getRemainingTimeText(): String {
        val now = System.currentTimeMillis()
        val remaining = endTimeMillis - now

        return if (remaining <= 0) {
            "课程已结束"
        } else {
            val minutes = (remaining / 1000 / 60).toInt()
            val seconds = ((remaining / 1000) % 60).toInt()
            if (minutes > 0) {
                "剩余 ${minutes} 分钟下课"
            } else {
                "剩余 ${seconds} 秒下课"
            }
        }
    }

    private fun parseEndTime(endTimeStr: String): Long {
        return try {
            val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            val time = timeFormat.parse(endTimeStr) ?: return 0

            val calendar = Calendar.getInstance().apply {
                val timeCalendar = Calendar.getInstance().apply { this.time = time }
                set(Calendar.HOUR_OF_DAY, timeCalendar.get(Calendar.HOUR_OF_DAY))
                set(Calendar.MINUTE, timeCalendar.get(Calendar.MINUTE))
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            calendar.timeInMillis
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse end time: $endTimeStr", e)
            0
        }
    }

    private fun startUpdating() {
        updateJob?.cancel()
        updateJob = serviceScope.launch {
            while (isActive) {
                val now = System.currentTimeMillis()
                if (now >= endTimeMillis) {
                    // 课程结束，停止服务
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                    break
                }

                // 更新通知
                val manager = getSystemService(NotificationManager::class.java)
                manager.notify(NOTIFICATION_ID, createNotification())

                // 每30秒更新一次
                delay(30_000)
            }
        }
    }

    private fun stopUpdating() {
        updateJob?.cancel()
        updateJob = null
    }
}
