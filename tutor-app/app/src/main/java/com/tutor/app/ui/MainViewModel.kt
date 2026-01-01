package com.tutor.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tutor.app.data.*
import com.tutor.app.network.RetrofitClient
import com.tutor.app.notification.LessonReminderManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

sealed class UiState {
    data object Loading : UiState()
    data class Success(val schedules: List<Schedule>) : UiState()
    data class Error(val message: String) : UiState()
}

class MainViewModel(application: Application) : AndroidViewModel(application) {
    
    private val settingsManager = SettingsManager(application)
    private val scheduleCache = ScheduleCache(application)
    private val syncLogManager = SyncLogManager(application)
    private val reminderManager = LessonReminderManager(application)
    
    // 缓存多天数据 date -> schedules
    private val _schedulesMap = MutableStateFlow<Map<LocalDate, List<Schedule>>>(emptyMap())
    val schedulesMap: StateFlow<Map<LocalDate, List<Schedule>>> = _schedulesMap.asStateFlow()
    
    // 加载状态 date -> isLoading
    private val _loadingStates = MutableStateFlow<Map<LocalDate, Boolean>>(emptyMap())
    val loadingStates: StateFlow<Map<LocalDate, Boolean>> = _loadingStates.asStateFlow()
    
    // 错误状态 date -> errorMessage
    private val _errorStates = MutableStateFlow<Map<LocalDate, String?>>(emptyMap())
    val errorStates: StateFlow<Map<LocalDate, String?>> = _errorStates.asStateFlow()
    
    // 刷新失败提示（用于 Snackbar）
    private val _snackbarMessage = MutableSharedFlow<String>()
    val snackbarMessage: SharedFlow<String> = _snackbarMessage.asSharedFlow()
    
    private val _currentDate = MutableStateFlow(LocalDate.now())
    val currentDate: StateFlow<LocalDate> = _currentDate.asStateFlow()
    
    private val _syncLogs = MutableStateFlow<List<SyncLog>>(emptyList())
    val syncLogs: StateFlow<List<SyncLog>> = _syncLogs.asStateFlow()
    
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    
    // 预加载范围：前后各2天
    private val preloadRange = 2
    
    init {
        viewModelScope.launch {
            settingsManager.baseUrl.collect { url ->
                RetrofitClient.updateBaseUrl(url)
            }
        }
        // 初始加载今天及前后两天
        preloadAroundDate(LocalDate.now())
        loadSyncLogs()
    }
    
    fun preloadAroundDate(centerDate: LocalDate) {
        for (i in -preloadRange..preloadRange) {
            val date = centerDate.plusDays(i.toLong())
            loadSchedulesForDate(date)
        }
    }
    
    private fun loadSchedulesForDate(date: LocalDate, isManualRefresh: Boolean = false) {
        // 如果已经在加载中，跳过
        if (_loadingStates.value[date] == true) return
        
        viewModelScope.launch {
            val dateStr = date.format(dateFormatter)
            
            // 设置加载状态
            _loadingStates.value = _loadingStates.value + (date to true)
            _errorStates.value = _errorStates.value + (date to null)
            
            // 先尝试显示缓存
            val cached = scheduleCache.getSchedules(dateStr)
            if (cached != null) {
                _schedulesMap.value = _schedulesMap.value + (date to cached)
            }
            
            // 从网络获取
            try {
                val schedules = if (date == LocalDate.now()) {
                    RetrofitClient.apiService.getTodaySchedules()
                } else {
                    RetrofitClient.apiService.getSchedulesByDate(dateStr)
                }
                
                val result = scheduleCache.compareAndSave(dateStr, schedules)
                handleDataChange(dateStr, result)
                
                // 为待上课程设置提醒
                scheduleReminders(schedules)
                
                _schedulesMap.value = _schedulesMap.value + (date to schedules)
                _errorStates.value = _errorStates.value + (date to null)
                
                // 手动刷新时显示成功提示
                if (isManualRefresh) {
                    _snackbarMessage.emit("刷新成功")
                }
            } catch (e: Exception) {
                if (cached == null) {
                    _errorStates.value = _errorStates.value + (date to (e.message ?: "网络错误"))
                } else {
                    // 有缓存时，通过 Snackbar 提示刷新失败
                    _snackbarMessage.emit("刷新失败: ${e.message ?: "网络错误"}")
                }
            } finally {
                _loadingStates.value = _loadingStates.value + (date to false)
            }
        }
    }
    
    private fun handleDataChange(date: String, result: DataChangeResult) {
        when (result) {
            is DataChangeResult.NoChange -> {}
            is DataChangeResult.NewData -> {
                syncLogManager.addLog(date, "首次同步", "获取到 ${result.count} 节课程")
                loadSyncLogs()
            }
            is DataChangeResult.Changed -> {
                val details = buildString {
                    if (result.added.isNotEmpty()) {
                        append("新增: ${result.added.joinToString { it.studentName }}")
                    }
                    if (result.updated.isNotEmpty()) {
                        if (isNotEmpty()) append("; ")
                        append("更新: ${result.updated.joinToString { it.studentName }}")
                    }
                    if (result.removed.isNotEmpty()) {
                        if (isNotEmpty()) append("; ")
                        append("删除: ${result.removed.joinToString { it.studentName }}")
                    }
                }
                syncLogManager.addLog(date, "数据变更", details)
                loadSyncLogs()
            }
        }
    }
    
    fun onPageChanged(date: LocalDate) {
        _currentDate.value = date
        // 预加载新位置周围的数据
        preloadAroundDate(date)
    }
    
    fun goToToday() {
        _currentDate.value = LocalDate.now()
        preloadAroundDate(LocalDate.now())
    }
    
    fun refreshCurrentDate() {
        val date = _currentDate.value
        // 强制重新加载
        _loadingStates.value = _loadingStates.value + (date to false)
        loadSchedulesForDate(date, isManualRefresh = true)
    }
    
    /**
     * 重试加载指定日期的数据（用于错误页面重试按钮）
     */
    fun retryLoadDate(date: LocalDate) {
        _loadingStates.value = _loadingStates.value + (date to false)
        loadSchedulesForDate(date)
    }
    
    private fun loadSyncLogs() {
        _syncLogs.value = syncLogManager.getLogs()
    }
    
    fun clearLogs() {
        syncLogManager.clearLogs()
        loadSyncLogs()
    }
    
    /**
     * 为课程设置提醒（上课前10分钟 + 上课时）
     * 对于正在上课的课程，立即发送通知
     */
    private fun scheduleReminders(schedules: List<Schedule>) {
        schedules.forEach { schedule ->
            when (schedule.status) {
                "pending" -> reminderManager.scheduleReminder(schedule)
                "ongoing" -> reminderManager.showOngoingLessonNotification(schedule)
            }
        }
    }
}
