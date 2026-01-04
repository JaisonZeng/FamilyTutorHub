package com.tutor.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tutor.app.data.AuthManager
import com.tutor.app.network.ApiService
import com.tutor.app.network.LoginRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class LoginState {
    object Idle : LoginState()
    object Loading : LoginState()
    data class Success(val username: String) : LoginState()
    data class Error(val message: String) : LoginState()
}

class LoginViewModel(application: Application) : AndroidViewModel(application) {

    private val authManager = AuthManager(application)
    private lateinit var apiService: ApiService

    private val _loginState = MutableStateFlow<LoginState>(LoginState.Idle)
    val loginState: StateFlow<LoginState> = _loginState.asStateFlow()

    private val _baseUrl = MutableStateFlow("")
    val baseUrl: StateFlow<String> = _baseUrl.asStateFlow()

    fun setApiService(service: ApiService) {
        apiService = service
    }

    fun setBaseUrl(url: String) {
        _baseUrl.value = url
    }

    fun login(username: String, password: String) {
        if (username.isBlank() || password.isBlank()) {
            _loginState.value = LoginState.Error("用户名和密码不能为空")
            return
        }

        _loginState.value = LoginState.Loading

        viewModelScope.launch {
            try {
                val response = apiService.login(LoginRequest(username, password))
                authManager.saveLoginInfo(
                    token = response.token,
                    username = response.currentUser.username,
                    userId = response.currentUser.id.toString()
                )
                _loginState.value = LoginState.Success(response.currentUser.name)
            } catch (e: Exception) {
                _loginState.value = LoginState.Error(
                    e.message ?: "登录失败，请检查网络连接和服务器地址"
                )
            }
        }
    }

    fun clearLoginState() {
        _loginState.value = LoginState.Idle
    }

    suspend fun checkHealth(): Boolean {
        return try {
            val response = apiService.healthCheck()
            response.status == "ok"
        } catch (e: Exception) {
            false
        }
    }
}