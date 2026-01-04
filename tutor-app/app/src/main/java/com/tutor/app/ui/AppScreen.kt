package com.tutor.app.ui

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tutor.app.data.AuthManager
import kotlinx.coroutines.launch

enum class AppState {
    SetupConfig, Login, Main
}

@Composable
fun AppScreen(
    authManager: AuthManager,
    settingsViewModel: SettingsViewModel = viewModel(),
    loginViewModel: LoginViewModel = viewModel()
) {
    var appState by remember { mutableStateOf<AppState>(AppState.SetupConfig) }
    var isInitialized by remember { mutableStateOf(false) }
    var apiServiceInitialized by remember { mutableStateOf(false) }
    var showLoginSuccessMessage by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // 初始化时根据登录状态决定进入哪个页面
    LaunchedEffect(Unit) {
        authManager.isLoggedIn.collect { loggedIn ->
            if (!isInitialized) {
                appState = if (loggedIn) AppState.Main else AppState.SetupConfig
                isInitialized = true
            }
        }
    }

    // 监听登录状态变化，如果已经初始化且在主页，突然登出了，跳转到登录页
    LaunchedEffect(Unit) {
        authManager.isLoggedIn.collect { loggedIn ->
            if (isInitialized && !loggedIn && appState == AppState.Main) {
                appState = AppState.Login
            }
        }
    }

    // 在首页显示登录成功消息
    LaunchedEffect(showLoginSuccessMessage) {
        if (showLoginSuccessMessage && appState == AppState.Main) {
            snackbarHostState.showSnackbar(
                message = "登录成功！",
                duration = SnackbarDuration.Short
            )
            showLoginSuccessMessage = false
        }
    }

    fun onLogout() {
        coroutineScope.launch {
            authManager.clearAuth()
            // 立即跳转到登录页
            appState = AppState.Login
        }
    }

    when (appState) {
        AppState.SetupConfig -> {
            SetupConfigPage(
                settingsViewModel = settingsViewModel,
                loginViewModel = loginViewModel,
                snackbarHostState = snackbarHostState,
                onConfigured = {
                    coroutineScope.launch {
                        loginViewModel.setApiService(com.tutor.app.network.RetrofitClient.apiService)
                        apiServiceInitialized = true
                        val healthOk = loginViewModel.checkHealth()
                        if (healthOk) {
                            appState = AppState.Login
                        } else {
                            snackbarHostState.showSnackbar(
                                message = "服务器连接失败",
                                duration = SnackbarDuration.Short
                            )
                        }
                    }
                }
            )
        }
        AppState.Login -> {
            // 只在第一次进入登录页时初始化apiService（如果还没初始化）
            LaunchedEffect(apiServiceInitialized) {
                if (!apiServiceInitialized) {
                    loginViewModel.setApiService(com.tutor.app.network.RetrofitClient.apiService)
                    apiServiceInitialized = true
                }
            }

            LoginPage(
                loginViewModel = loginViewModel,
                snackbarHostState = snackbarHostState,
                onLoginSuccess = {
                    appState = AppState.Main
                    showLoginSuccessMessage = true
                },
                onBackToSettings = {
                    appState = AppState.SetupConfig
                }
            )
        }
        AppState.Main -> {
            MainScreen(
                authManager = authManager,
                snackbarHostState = snackbarHostState,
                onLogout = { onLogout() }
            )
        }
    }
}