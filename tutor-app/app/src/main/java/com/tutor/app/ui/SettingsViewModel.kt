package com.tutor.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tutor.app.data.SettingsManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    
    private val settingsManager = SettingsManager(application)
    
    private val _baseUrl = MutableStateFlow(SettingsManager.DEFAULT_BASE_URL)
    val baseUrl: StateFlow<String> = _baseUrl.asStateFlow()
    
    private val _saveSuccess = MutableStateFlow<Boolean?>(null)
    val saveSuccess: StateFlow<Boolean?> = _saveSuccess.asStateFlow()
    
    init {
        viewModelScope.launch {
            settingsManager.baseUrl.collect { url ->
                _baseUrl.value = url
            }
        }
    }
    
    fun saveBaseUrl(url: String) {
        viewModelScope.launch {
            try {
                settingsManager.setBaseUrl(url)
                _saveSuccess.value = true
            } catch (e: Exception) {
                _saveSuccess.value = false
            }
        }
    }
    
    fun clearSaveStatus() {
        _saveSuccess.value = null
    }
}
