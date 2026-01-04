package com.tutor.app.ui

import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tutor.app.data.Schedule
import com.tutor.app.data.SyncLog
import com.tutor.app.util.CalendarHelper
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

enum class Screen {
    Schedule, Settings, Logs
}

// Pager 的中心页索引，允许向前后滑动很多天
private const val INITIAL_PAGE = 500
private const val TOTAL_PAGES = 1000

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel = viewModel(),
    authManager: com.tutor.app.data.AuthManager? = null,
    onLogout: () -> Unit = {}
) {
    val currentDate by viewModel.currentDate.collectAsState()
    val syncLogs by viewModel.syncLogs.collectAsState()
    val schedulesMap by viewModel.schedulesMap.collectAsState()
    val loadingStates by viewModel.loadingStates.collectAsState()
    val errorStates by viewModel.errorStates.collectAsState()
    
    var currentScreen by remember { mutableStateOf(Screen.Schedule) }
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    
    // 监听提示消息
    LaunchedEffect(Unit) {
        viewModel.snackbarMessage.collect { message ->
            snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Short
            )
        }
    }
    
    // 基准日期（今天）
    val baseDate = remember { LocalDate.now() }
    
    // Pager 状态
    val pagerState = rememberPagerState(
        initialPage = INITIAL_PAGE,
        pageCount = { TOTAL_PAGES }
    )
    
    // 当前页对应的日期
    val displayDate = remember(pagerState.currentPage) {
        baseDate.plusDays((pagerState.currentPage - INITIAL_PAGE).toLong())
    }
    
    // 页面变化时通知 ViewModel
    LaunchedEffect(pagerState.currentPage) {
        val date = baseDate.plusDays((pagerState.currentPage - INITIAL_PAGE).toLong())
        viewModel.onPageChanged(date)
    }
    
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        when (currentScreen) {
                            Screen.Schedule -> {
                                if (displayDate == LocalDate.now()) "今日课程" 
                                else displayDate.format(DateTimeFormatter.ofPattern("M月d日"))
                            }
                            Screen.Settings -> "设置"
                            Screen.Logs -> "同步日志"
                        }
                    ) 
                },
                actions = {
                    when (currentScreen) {
                        Screen.Schedule -> {
                            if (pagerState.currentPage != INITIAL_PAGE) {
                                TextButton(
                                    onClick = {
                                        coroutineScope.launch {
                                            pagerState.animateScrollToPage(
                                                INITIAL_PAGE,
                                                animationSpec = tween(300)
                                            )
                                        }
                                        viewModel.goToToday()
                                    }
                                ) {
                                    Text("今天", color = Color.White)
                                }
                            }
                            IconButton(onClick = { viewModel.refreshCurrentDate() }) {
                                Icon(Icons.Default.Refresh, contentDescription = "刷新")
                            }
                        }
                        Screen.Logs -> {
                            if (syncLogs.isNotEmpty()) {
                                IconButton(onClick = { viewModel.clearLogs() }) {
                                    Icon(Icons.Default.Delete, contentDescription = "清空日志")
                                }
                            }
                        }
                        else -> {}
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.DateRange, contentDescription = null) },
                    label = { Text("课程") },
                    selected = currentScreen == Screen.Schedule,
                    onClick = { currentScreen = Screen.Schedule }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.List, contentDescription = null) },
                    label = { Text("日志") },
                    selected = currentScreen == Screen.Logs,
                    onClick = { currentScreen = Screen.Logs }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                    label = { Text("设置") },
                    selected = currentScreen == Screen.Settings,
                    onClick = { currentScreen = Screen.Settings }
                )
            }
        }
    ) { padding ->
        when (currentScreen) {
            Screen.Schedule -> {
                Column(modifier = Modifier.padding(padding)) {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.weight(1f),
                        beyondBoundsPageCount = 2 // 预渲染前后各2页
                    ) { page ->
                        val date = baseDate.plusDays((page - INITIAL_PAGE).toLong())
                        val schedules = schedulesMap[date]
                        val isLoading = loadingStates[date] == true
                        val error = errorStates[date]
                        
                        SchedulePage(
                            date = date,
                            schedules = schedules,
                            isLoading = isLoading && schedules == null,
                            error = if (schedules == null) error else null,
                            onRetry = { viewModel.retryLoadDate(date) }
                        )
                    }
                    
                    // 滑动提示
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "← 滑动切换日期 →",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                }
            }
            Screen.Settings -> SettingsContent(
                modifier = Modifier.padding(padding),
                authManager = authManager,
                onLogout = onLogout
            )
            Screen.Logs -> LogsContent(
                logs = syncLogs,
                modifier = Modifier.padding(padding)
            )
        }
    }
}

@Composable
private fun SchedulePage(
    date: LocalDate,
    schedules: List<Schedule>?,
    isLoading: Boolean,
    error: String?,
    onRetry: () -> Unit
) {
    val isToday = date == LocalDate.now()
    
    when {
        isLoading -> LoadingContent()
        error != null -> ErrorContent(error, onRetry)
        schedules != null -> ScheduleList(schedules, date, isToday)
        else -> LoadingContent()
    }
}

@Composable
private fun SettingsContent(
    modifier: Modifier = Modifier,
    authManager: com.tutor.app.data.AuthManager? = null,
    viewModel: SettingsViewModel = viewModel(),
    onLogout: () -> Unit = {}
) {
    val currentUrl by viewModel.baseUrl.collectAsState()
    val saveSuccess by viewModel.saveSuccess.collectAsState()
    var username by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(authManager) {
        authManager?.username?.collect { user ->
            username = user
        }
    }

    var urlInput by remember(currentUrl) { mutableStateOf(currentUrl) }
    
    LaunchedEffect(saveSuccess) {
        if (saveSuccess != null) {
            viewModel.clearSaveStatus()
        }
    }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "服务器设置",
            style = MaterialTheme.typography.titleMedium
        )

        OutlinedTextField(
            value = urlInput,
            onValueChange = { urlInput = it },
            label = { Text("后端地址") },
            placeholder = { Text("http://192.168.1.100:8080/") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Text(
            text = "提示：如果家里IP变动了，在这里修改后端地址即可",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = { urlInput = currentUrl },
                modifier = Modifier.weight(1f)
            ) {
                Text("重置")
            }

            Button(
                onClick = { viewModel.saveBaseUrl(urlInput) },
                modifier = Modifier.weight(1f)
            ) {
                Text("保存")
            }
        }

        Divider()

        Text(
            text = "账号",
            style = MaterialTheme.typography.titleMedium
        )

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "当前登录",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = username ?: "未知用户",
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }

        Button(
            onClick = onLogout,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error
            )
        ) {
            Text("退出登录")
        }

        Divider()

        Text(
            text = "关于",
            style = MaterialTheme.typography.titleMedium
        )

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("课程表 App", style = MaterialTheme.typography.titleSmall)
                Spacer(modifier = Modifier.height(4.dp))
                Text("版本: 1.0.0", style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "GitHub: github.com/JaisonZeng/FamilyTutorHub",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun LogsContent(
    logs: List<SyncLog>,
    modifier: Modifier = Modifier
) {
    if (logs.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "暂无同步日志",
                color = Color.Gray,
                fontSize = 16.sp
            )
        }
    } else {
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(logs) { log ->
                LogItem(log)
            }
        }
    }
}

@Composable
private fun LogItem(log: SyncLog) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = log.timestamp,
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                Surface(
                    color = when (log.action) {
                        "首次同步" -> Color(0xFF4CAF50)
                        "数据变更" -> Color(0xFFFF9800)
                        else -> Color.Gray
                    },
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = log.action,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        fontSize = 10.sp,
                        color = Color.White
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "日期: ${log.date}",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = log.details,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun LoadingContent(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun ErrorContent(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "加载失败",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Gray
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = message,
            fontSize = 14.sp,
            color = Color.Gray,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onRetry) {
            Text("重试")
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "提示：可在设置中修改后端地址",
            fontSize = 12.sp,
            color = Color.Gray
        )
    }
}

@Composable
private fun ScheduleList(
    schedules: List<Schedule>,
    currentDate: LocalDate,
    isToday: Boolean,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            HeaderCard(schedules.size, currentDate, isToday)
        }

        if (schedules.isEmpty()) {
            item {
                EmptyCard(isToday)
            }
        } else {
            items(schedules) { schedule ->
                ScheduleCard(schedule, currentDate)
            }
        }
    }
}

@Composable
private fun HeaderCard(count: Int, date: LocalDate, isToday: Boolean) {
    val formatter = DateTimeFormatter.ofPattern("M月d日 EEEE", Locale.CHINESE)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = date.format(formatter),
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            if (!isToday) {
                val daysFromToday = java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), date)
                Text(
                    text = when {
                        daysFromToday == 1L -> "明天"
                        daysFromToday == -1L -> "昨天"
                        daysFromToday > 0 -> "${daysFromToday}天后"
                        else -> "${-daysFromToday}天前"
                    },
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "$count",
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "节课程",
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
private fun EmptyCard(isToday: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFF5F5F5)
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(40.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (isToday) "今天没有课程安排 🎉" else "这天没有课程安排",
                fontSize = 16.sp,
                color = Color.Gray
            )
        }
    }
}

@Composable
private fun ScheduleCard(schedule: Schedule, currentDate: LocalDate) {
    val context = LocalContext.current
    var showDialog by remember { mutableStateOf(false) }
    
    val backgroundColor = when {
        schedule.isOngoing -> Color(0xFF4CAF50)
        schedule.isCompleted -> Color(0xFFE0E0E0)
        else -> Color.White
    }

    val textColor = when {
        schedule.isOngoing -> Color.White
        schedule.isCompleted -> Color.Gray
        else -> Color.Black
    }
    
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("添加日历提醒") },
            text = { 
                Text("将为「${schedule.studentName} - ${schedule.subject}」添加日历事件，并设置开课前20分钟提醒") 
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        CalendarHelper.addToCalendarWithIntent(context, schedule, currentDate)
                        showDialog = false
                    }
                ) {
                    Text("添加")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { showDialog = true },
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (schedule.isOngoing) 8.dp else 2.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.width(60.dp)
            ) {
                Text(
                    text = schedule.startTime,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(50.dp)
                    .background(
                        if (schedule.isOngoing) Color.White.copy(alpha = 0.5f)
                        else MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                        RoundedCornerShape(2.dp)
                    )
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = schedule.studentName,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = schedule.subject,
                    fontSize = 14.sp,
                    color = if (schedule.isOngoing) Color.White.copy(alpha = 0.8f)
                           else Color.Gray
                )
                Text(
                    text = schedule.timeSlot,
                    fontSize = 12.sp,
                    color = if (schedule.isOngoing) Color.White.copy(alpha = 0.7f)
                           else Color.Gray
                )
            }

            if (schedule.isOngoing) {
                Surface(
                    color = Color.White,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "正在上课",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4CAF50)
                    )
                }
            } else if (!schedule.isCompleted) {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = "添加提醒",
                    tint = Color.Gray.copy(alpha = 0.5f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
