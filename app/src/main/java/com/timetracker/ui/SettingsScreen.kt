package com.timetracker.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.timetracker.data.local.AppUsageDatabase
import com.timetracker.data.remote.ApiService
import com.timetracker.data.repository.UsageRepository
import com.timetracker.util.PermissionHelper
import com.timetracker.util.PreferenceManager
import com.timetracker.util.WorkManagerHelper
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val preferenceManager = remember { PreferenceManager(context) }
    
    var hasPermission by remember { mutableStateOf(PermissionHelper.isAccessibilityServiceEnabled(context)) }
    var isEnabled by remember { mutableStateOf(preferenceManager.isCollectorEnabled()) }
    var serverUrl by remember { mutableStateOf(preferenceManager.getServerUrl()) }
    var syncInterval by remember { mutableStateOf(preferenceManager.getSyncInterval().toString()) }
    var unsyncedCount by remember { mutableStateOf(0) }
    var lastSyncTime by remember { mutableStateOf(preferenceManager.getLastSyncTime()) }
    var isSyncing by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf("") }
    
    val lastSyncTimeFlow by preferenceManager.lastSyncTimeFlow.collectAsStateWithLifecycle(initialValue = 0L)
    
    LaunchedEffect(lastSyncTimeFlow) {
        lastSyncTime = lastSyncTimeFlow
    }
    
    LaunchedEffect(Unit) {
        val database = AppUsageDatabase.getDatabase(context)
        val apiService = ApiService.create(serverUrl)
        val repository = UsageRepository(context, database.appUsageDao(), apiService, preferenceManager)
        unsyncedCount = repository.getUnsyncedCount()
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("TimeTracker") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 权限状态卡片
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (hasPermission) 
                        MaterialTheme.colorScheme.primaryContainer 
                    else 
                        MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "权限状态",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = if (hasPermission) "✓ 已启用无障碍服务" else "✗ 需要启用无障碍服务",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    if (!hasPermission) {
                        Button(
                            onClick = {
                                PermissionHelper.openAccessibilitySettings(context)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("启用无障碍服务")
                        }
                    } else {
                        Button(
                            onClick = {
                                PermissionHelper.openBatteryOptimizationSettings(context)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("电池优化设置（可选）")
                        }
                    }
                }
            }
            
            // 启用/禁用开关
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "启用追踪",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "自动收集应用使用数据",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = isEnabled,
                        onCheckedChange = { enabled ->
                            scope.launch {
                                preferenceManager.setCollectorEnabled(enabled)
                                isEnabled = enabled
                                
                                if (enabled && hasPermission) {
                                    WorkManagerHelper.startCollectorWork(context)
                                    WorkManagerHelper.startSyncWork(context, syncInterval.toLongOrNull() ?: 30)
                                    statusMessage = "追踪已启动"
                                } else {
                                    WorkManagerHelper.stopAllWork(context)
                                    statusMessage = "追踪已停止"
                                }
                            }
                        },
                        enabled = hasPermission
                    )
                }
            }
            
            // 服务器配置
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "服务器配置",
                        style = MaterialTheme.typography.titleMedium
                    )
                    OutlinedTextField(
                        value = serverUrl,
                        onValueChange = { serverUrl = it },
                        label = { Text("服务器地址") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = syncInterval,
                        onValueChange = { syncInterval = it },
                        label = { Text("同步间隔（分钟）") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Button(
                        onClick = {
                            scope.launch {
                                preferenceManager.setServerUrl(serverUrl)
                                preferenceManager.setSyncInterval(syncInterval.toLongOrNull() ?: 30)
                                statusMessage = "配置已保存"
                                
                                if (isEnabled) {
                                    WorkManagerHelper.startSyncWork(context, syncInterval.toLongOrNull() ?: 30)
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("保存配置")
                    }
                }
            }
            
            // 同步状态
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "同步状态",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "未同步记录: $unsyncedCount 条",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "上次同步: ${formatTime(lastSyncTime)}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "设备ID: ${preferenceManager.getDeviceId()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(
                        onClick = {
                            scope.launch {
                                isSyncing = true
                                statusMessage = "正在同步..."
                                
                                try {
                                    WorkManagerHelper.syncNow(context)
                                    kotlinx.coroutines.delay(2000) // 等待同步完成
                                    
                                    val database = AppUsageDatabase.getDatabase(context)
                                    val apiService = ApiService.create(serverUrl)
                                    val repository = UsageRepository(context, database.appUsageDao(), apiService, preferenceManager)
                                    unsyncedCount = repository.getUnsyncedCount()
                                    lastSyncTime = preferenceManager.getLastSyncTime()
                                    
                                    statusMessage = "同步完成"
                                } catch (e: Exception) {
                                    statusMessage = "同步失败: ${e.message}"
                                } finally {
                                    isSyncing = false
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isSyncing && hasPermission
                    ) {
                        if (isSyncing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(if (isSyncing) "同步中..." else "立即同步")
                    }
                }
            }
            
            // 状态消息
            if (statusMessage.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Text(
                        text = statusMessage,
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            
            // 使用说明
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "使用说明",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "1. 启用「无障碍服务」权限\n" +
                                "2. 在无障碍设置中找到 TimeTracker 并开启\n" +
                                "3. 启用追踪开关\n" +
                                "4. 应用将自动收集前台应用使用数据\n" +
                                "5. 数据每${syncInterval}分钟自动同步到服务器\n" +
                                "6. 建议关闭电池优化以确保后台运行",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

private fun formatTime(timestamp: Long): String {
    if (timestamp == 0L) return "从未同步"
    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
