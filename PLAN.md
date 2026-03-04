# Android 时间追踪 App 开发规划

## 项目概述

**项目名称：** TimeTracker for ActivityWatch  
**目标：** 开发一个 Android App，自动监控前台应用使用情况，并同步到服务器  
**参考项目：** Google 官方示例 android-AppUsageStatistics

---

## 功能需求

### 核心功能
1. **自动监控前台应用**
   - 使用 `UsageStatsManager` API
   - 记录应用名称、包名、使用时长
   - 每分钟采样一次

2. **数据同步**
   - 每 30 分钟自动同步到服务器
   - 使用现有的 `/api/aw/sync` 接口
   - 数据格式与 ActivityWatch 兼容

3. **后台运行**
   - 使用 WorkManager 定期执行
   - 电池优化友好
   - 开机自启动

4. **配置界面**
   - 服务器地址配置
   - 同步频率设置
   - 查看同步状态

### 权限需求
- `PACKAGE_USAGE_STATS` - 访问应用使用统计
- `INTERNET` - 网络访问
- `RECEIVE_BOOT_COMPLETED` - 开机自启

---

## 技术架构

### 技术栈
- **语言：** Kotlin
- **最低 SDK：** API 21 (Android 5.0)
- **目标 SDK：** API 34 (Android 14)
- **架构：** MVVM + Repository 模式

### 核心组件
1. **UsageStatsCollector** - 收集应用使用数据
2. **DataRepository** - 本地数据存储（Room 数据库）
3. **SyncWorker** - 后台同步任务（WorkManager）
4. **ApiService** - 网络请求（Retrofit）
5. **MainActivity** - 配置界面（Jetpack Compose）

### 数据流
```
UsageStatsManager 
  → UsageStatsCollector (每分钟采样)
  → Room Database (本地存储)
  → SyncWorker (每30分钟)
  → ApiService (HTTP POST)
  → 服务器 /api/aw/sync
```

---

## 项目结构

```
TimeTracker/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/timetracker/
│   │   │   │   ├── data/
│   │   │   │   │   ├── local/
│   │   │   │   │   │   ├── AppUsageDao.kt
│   │   │   │   │   │   ├── AppUsageDatabase.kt
│   │   │   │   │   │   └── AppUsageEntity.kt
│   │   │   │   │   ├── remote/
│   │   │   │   │   │   ├── ApiService.kt
│   │   │   │   │   │   └── SyncRequest.kt
│   │   │   │   │   └── repository/
│   │   │   │   │       └── UsageRepository.kt
│   │   │   │   ├── worker/
│   │   │   │   │   ├── CollectorWorker.kt
│   │   │   │   │   └── SyncWorker.kt
│   │   │   │   ├── ui/
│   │   │   │   │   ├── MainActivity.kt
│   │   │   │   │   └── SettingsScreen.kt
│   │   │   │   └── util/
│   │   │   │       ├── PermissionHelper.kt
│   │   │   │       └── PreferenceManager.kt
│   │   │   ├── res/
│   │   │   └── AndroidManifest.xml
│   │   └── build.gradle.kts
│   └── build.gradle.kts
├── gradle/
├── build.gradle.kts
└── settings.gradle.kts
```

---

## 开发步骤

### 阶段 1：项目初始化（30分钟）
- [ ] 创建 Android 项目
- [ ] 配置 Gradle 依赖
- [ ] 设置权限和 Manifest

### 阶段 2：数据层（1小时）
- [ ] 创建 Room 数据库
- [ ] 实现 UsageStatsCollector
- [ ] 实现 Repository

### 阶段 3：后台任务（1小时）
- [ ] 实现 CollectorWorker（每分钟采样）
- [ ] 实现 SyncWorker（每30分钟同步）
- [ ] 配置 WorkManager

### 阶段 4：网络层（30分钟）
- [ ] 实现 ApiService（Retrofit）
- [ ] 适配现有 API 格式

### 阶段 5：UI 层（30分钟）
- [ ] 实现配置界面（Jetpack Compose）
- [ ] 权限请求流程
- [ ] 同步状态显示

### 阶段 6：测试和打包（30分钟）
- [ ] 本地测试
- [ ] 配置签名
- [ ] 生成 APK

---

## 依赖库

```kotlin
dependencies {
    // Core
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    
    // Compose UI
    implementation("androidx.compose.ui:ui:1.5.4")
    implementation("androidx.compose.material3:material3:1.1.2")
    implementation("androidx.activity:activity-compose:1.8.2")
    
    // Room Database
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    kapt("androidx.room:room-compiler:2.6.1")
    
    // WorkManager
    implementation("androidx.work:work-runtime-ktx:2.9.0")
    
    // Retrofit
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    
    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
}
```

---

## API 接口适配

### 现有接口
```
POST http://43.163.97.77:3002/api/aw/sync
Headers:
  x-aw-token: aw-sync-2026-secret
  Content-Type: application/json

Body:
{
  "date": "2026-03-04",
  "buckets": {
    "aw-watcher-window_DEVICE": [
      {
        "id": 1,
        "timestamp": "2026-03-04T05:00:00Z",
        "duration": 60.0,
        "data": {
          "app": "com.example.app",
          "title": "App Title"
        }
      }
    ]
  }
}
```

### Android 数据格式
```kotlin
data class SyncRequest(
    val date: String,
    val buckets: Map<String, List<UsageEvent>>
)

data class UsageEvent(
    val id: Int,
    val timestamp: String,
    val duration: Double,
    val data: UsageData
)

data class UsageData(
    val app: String,
    val title: String
)
```

---

## 关键代码片段

### 1. 获取应用使用统计
```kotlin
fun getUsageStats(startTime: Long, endTime: Long): List<UsageStats> {
    val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
    return usageStatsManager.queryUsageStats(
        UsageStatsManager.INTERVAL_DAILY,
        startTime,
        endTime
    )
}
```

### 2. WorkManager 定期任务
```kotlin
val syncWork = PeriodicWorkRequestBuilder<SyncWorker>(30, TimeUnit.MINUTES)
    .setConstraints(
        Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
    )
    .build()

WorkManager.getInstance(context).enqueueUniquePeriodicWork(
    "sync_work",
    ExistingPeriodicWorkPolicy.KEEP,
    syncWork
)
```

---

## 风险和挑战

### 技术风险
1. **电池优化限制**
   - 解决：使用 WorkManager，遵循 Android 最佳实践
   - 提示用户关闭电池优化

2. **权限获取**
   - 解决：清晰的引导流程
   - 跳转到系统设置页面

3. **数据同步失败**
   - 解决：本地缓存，失败重试
   - 指数退避策略

### 用户体验风险
1. **首次配置复杂**
   - 解决：简化配置流程
   - 提供默认配置

2. **后台运行不稳定**
   - 解决：提供手动同步按钮
   - 显示同步状态

---

## 编译和分发

### 方式 1：GitHub Actions（推荐）
1. 代码推送到 GitHub
2. 配置 GitHub Actions workflow
3. 自动编译生成 APK
4. 用户直接下载

### 方式 2：本地编译
1. 用户安装 Android Studio
2. 导入项目
3. Build → Build APK
4. 安装到手机

---

## 时间估算

| 阶段 | 预计时间 |
|------|---------|
| 项目初始化 | 30分钟 |
| 数据层开发 | 1小时 |
| 后台任务 | 1小时 |
| 网络层 | 30分钟 |
| UI 层 | 30分钟 |
| 测试打包 | 30分钟 |
| **总计** | **4小时** |

---

## 下一步

1. **用户确认方案**
2. **创建 GitHub 仓库**
3. **开始编码**
4. **配置 GitHub Actions**
5. **提供 APK 下载链接**

---

## 备注

- 代码将完全开源
- 遵循 Android 开发最佳实践
- 优先考虑电池和性能
- 数据完全本地存储，隐私安全
