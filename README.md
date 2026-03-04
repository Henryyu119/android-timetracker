# TimeTracker for Android

自动追踪 Android 应用使用时间并同步到服务器的时间管理工具。

## 功能特性

- ✅ 自动监控前台应用使用情况
- ✅ 每分钟采样一次，记录应用名称和使用时长
- ✅ 每 30 分钟自动同步到服务器
- ✅ 后台运行，电池优化友好
- ✅ 开机自启动
- ✅ 简洁的配置界面

## 技术栈

- **语言**: Kotlin
- **UI**: Jetpack Compose + Material 3
- **架构**: MVVM + Repository
- **数据库**: Room
- **后台任务**: WorkManager
- **网络**: Retrofit + OkHttp
- **最低版本**: Android 5.0 (API 21)
- **目标版本**: Android 14 (API 34)

## 安装说明

### 方式 1: 下载预编译 APK（推荐）

1. 前往 [Releases](https://github.com/YOUR_USERNAME/android-timetracker/releases) 页面
2. 下载最新版本的 APK 文件
3. 在手机上安装 APK
4. 按照应用内提示授予权限

### 方式 2: 从源码编译

#### 前置要求

- Android Studio Hedgehog (2023.1.1) 或更高版本
- JDK 17
- Android SDK (API 34)

#### 编译步骤

```bash
# 克隆仓库
git clone https://github.com/YOUR_USERNAME/android-timetracker.git
cd android-timetracker

# 使用 Gradle 编译
./gradlew assembleRelease

# APK 输出位置
# app/build/outputs/apk/release/app-release-unsigned.apk
```

## 使用指南

### 1. 授予权限

首次打开应用，需要授予以下权限：

- **使用统计权限**: 用于读取应用使用数据
  - 点击「授予权限」按钮
  - 在系统设置中找到 TimeTracker
  - 开启「允许使用统计信息」

### 2. 配置服务器

默认配置：
- 服务器地址: `http://43.163.97.77:3002/`
- API Token: `aw-sync-2026-secret`
- 同步间隔: 30 分钟

如需修改，在「服务器配置」卡片中编辑后点击「保存配置」。

### 3. 启用追踪

在主界面打开「启用追踪」开关，应用将开始：
- 每分钟采样一次前台应用
- 每 30 分钟自动同步到服务器

### 4. 电池优化（可选但推荐）

为确保后台稳定运行：
1. 点击「电池优化设置」按钮
2. 找到 TimeTracker
3. 选择「不优化」

## API 接口

应用使用以下接口同步数据：

```
POST http://YOUR_SERVER/api/aw/sync
Headers:
  x-aw-token: aw-sync-2026-secret
  Content-Type: application/json

Body:
{
  "date": "2026-03-04",
  "buckets": {
    "aw-watcher-android_DEVICE_ID": [
      {
        "id": 1,
        "timestamp": "2026-03-04T05:00:00Z",
        "duration": 60.0,
        "data": {
          "app": "com.example.app",
          "title": "App Name"
        }
      }
    ]
  }
}
```

## 项目结构

```
app/src/main/java/com/timetracker/
├── data/
│   ├── local/              # Room 数据库
│   │   ├── AppUsageDao.kt
│   │   ├── AppUsageDatabase.kt
│   │   └── AppUsageEntity.kt
│   ├── remote/             # 网络层
│   │   ├── ApiService.kt
│   │   └── SyncRequest.kt
│   └── repository/         # 数据仓库
│       └── UsageRepository.kt
├── worker/                 # 后台任务
│   ├── CollectorWorker.kt
│   └── SyncWorker.kt
├── ui/                     # 界面
│   ├── MainActivity.kt
│   ├── SettingsScreen.kt
│   └── theme/
├── util/                   # 工具类
│   ├── PermissionHelper.kt
│   ├── PreferenceManager.kt
│   └── WorkManagerHelper.kt
└── receiver/               # 广播接收器
    └── BootReceiver.kt
```

## 常见问题

### Q: 为什么数据没有同步？

A: 请检查：
1. 是否授予了「使用统计」权限
2. 是否开启了「启用追踪」开关
3. 手机是否联网
4. 服务器地址是否正确
5. 查看「同步状态」中的未同步记录数

### Q: 如何确保后台稳定运行？

A: 
1. 关闭电池优化
2. 在系统设置中允许应用后台运行
3. 部分手机需要在「自启动管理」中允许应用自启动

### Q: 数据会保存多久？

A: 
- 未同步的数据会一直保存
- 已同步的数据保留 7 天后自动清理

### Q: 如何更换服务器？

A: 在「服务器配置」中修改服务器地址，点击「保存配置」即可。

## 隐私说明

- 应用仅收集应用包名和应用名称
- 不收集应用内容、截图或其他敏感信息
- 所有数据仅同步到您配置的服务器
- 数据完全本地存储，不会发送到第三方

## 开发计划

- [ ] 支持手动添加/编辑记录
- [ ] 数据可视化（图表）
- [ ] 应用分类管理
- [ ] 导出数据功能
- [ ] 多服务器支持

## 贡献

欢迎提交 Issue 和 Pull Request！

## 许可证

MIT License

## 联系方式

- GitHub: [YOUR_USERNAME](https://github.com/YOUR_USERNAME)
- Email: your.email@example.com

---

**注意**: 本应用需要「使用统计」权限，这是 Android 系统的敏感权限。请确保您信任此应用后再授予权限。
