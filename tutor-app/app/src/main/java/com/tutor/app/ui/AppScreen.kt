package com.tutor.app.ui

import androidx.compose.foundation.layout.*
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
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        authManager.isLoggedIn.collect { loggedIn ->
            if (!isInitialized) {
                if (loggedIn) {
                    appState = AppState.Main
                } else {
                    appState = AppState.SetupConfig
                }
                isInitialized = true
            } else if (!loggedIn && appState == AppState.Main) {
                appState = AppState.Login
            }
        }
    }

    fun onLogout() {
        coroutineScope.launch {
            authManager.clearAuth()
            appState = AppState.Login
        }
    }

    when (appState) {
        AppState.SetupConfig -> {
            SetupConfigPage(
                settingsViewModel = settingsViewModel,
                loginViewModel = loginViewModel,
                onConfigured = {
                    coroutineScope.launch {
                        loginViewModel.setApiService(com.tutor.app.network.RetrofitClient.apiService)
                        val healthOk = loginViewModel.checkHealth()
                        if (healthOk) {
                            appState = AppState.Login
                        }
                    }
                }
            )
        }
        AppState.Login -> {
            LoginPage(
                loginViewModel = loginViewModel,
                onLoginSuccess = {
                    appState = AppState.Main
                },
                onBackToSettings = {
                    appState = AppState.SetupConfig
                }
            )
        }
        AppState.Main -> {
            MainScreen(
                authManager = authManager,
                onLogout = { onLogout() }
            )
        }
    }
}