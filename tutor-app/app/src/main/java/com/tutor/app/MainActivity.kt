package com.tutor.app

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import com.tutor.app.data.AuthManager
import com.tutor.app.network.RetrofitClient
import com.tutor.app.ui.AppScreen
import com.tutor.app.ui.SettingsViewModel
import com.tutor.app.ui.theme.TutorAppTheme

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        // 权限结果处理（可选：记录日志或显示提示）
    }

    private lateinit var authManager: AuthManager
    private lateinit var settingsViewModel: SettingsViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 设置软键盘模式，让键盘顶起页面而不是覆盖
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        enableEdgeToEdge()

        // 初始化管理器
        authManager = AuthManager(applicationContext)
        settingsViewModel = SettingsViewModel(application)

        // 初始化RetrofitClient
        RetrofitClient.init(applicationContext)

        // 请求通知权限 (Android 13+)
        requestNotificationPermission()

        // 检查精确闹钟权限 (Android 12+)
        checkExactAlarmPermission()

        setContent {
            TutorAppTheme {
                val baseUrl by settingsViewModel.baseUrl.collectAsState()
                LaunchedEffect(baseUrl) {
                    RetrofitClient.updateBaseUrl(baseUrl)
                }

                AppScreen(
                    authManager = authManager,
                    settingsViewModel = settingsViewModel
                )
            }
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    // 已有权限
                }
                else -> {
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }
    }

    private fun checkExactAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                // 引导用户去设置页面开启精确闹钟权限
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                startActivity(intent)
            }
        }
    }
}