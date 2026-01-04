package com.tutor.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tutor.app.data.SettingsManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupConfigPage(
    settingsViewModel: SettingsViewModel = viewModel(),
    loginViewModel: LoginViewModel = viewModel(),
    snackbarHostState: SnackbarHostState,
    onConfigured: () -> Unit = {}
) {
    var baseUrl by remember { mutableStateOf(SettingsManager.DEFAULT_BASE_URL) }
    var isChecking by remember { mutableStateOf(false) }
    var checkResult by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        settingsViewModel.baseUrl.collect { url ->
            baseUrl = url
        }
    }

    fun checkConnection() {
        isChecking = true
        checkResult = null
        coroutineScope.launch {
            try {
                settingsViewModel.saveBaseUrl(baseUrl)
                loginViewModel.setApiService(com.tutor.app.network.RetrofitClient.apiService)
                val healthOk = loginViewModel.checkHealth()
                checkResult = if (healthOk) {
                    "连接成功！"
                } else {
                    "连接失败，请检查服务器地址"
                }
            } catch (e: Exception) {
                checkResult = "连接失败：${e.message}"
            } finally {
                isChecking = false
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("配置服务器") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "欢迎使用家教管理系统",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "请配置后端服务器地址",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(32.dp))

            OutlinedTextField(
                value = baseUrl,
                onValueChange = { baseUrl = it },
                label = { Text("服务器地址") },
                singleLine = true,
                placeholder = { Text("http://192.168.1.100:8080") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isChecking
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "示例: http://192.168.1.100:8080",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth()
            )

            when (checkResult) {
                null -> {
                    Spacer(modifier = Modifier.height(16.dp))
                }
                "连接成功！" -> {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = checkResult!!,
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
                else -> {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = checkResult!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            Button(
                onClick = { checkConnection() },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isChecking && baseUrl.isNotBlank()
            ) {
                if (isChecking) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text(
                        "测试连接",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
                onClick = {
                    settingsViewModel.saveBaseUrl(baseUrl)
                    onConfigured()
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isChecking && checkResult == "连接成功！"
            ) {
                Text("下一步")
            }
        }
    }
}