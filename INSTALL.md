# Android TimeTracker - 安装和使用指南

## 快速开始

### 1. 获取 APK

#### 方式 A: GitHub Actions 自动编译（推荐）

1. 将代码推送到 GitHub 仓库
2. GitHub Actions 会自动编译生成 APK
3. 在 Actions 页面下载编译好的 APK
4. 或在 Releases 页面下载发布版本

#### 方式 B: 本地编译

```bash
cd /root/.openclaw/workspace/android-timetracker
./gradlew assembleRelease
```

编译完成后，APK 位于：
```
app/build/outputs/apk/release/app-release-unsigned.apk
```

### 2. 安装 APK

1. 将 APK 传输到 Android 手机
2. 在手机上打开 APK 文件
3. 允许「安装未知来源应用」
4. 点击安装

### 3. 配置应用

#### 第一步：授予权限

1. 打开 TimeTracker 应用
2. 点击「授予权限」按钮
3. 在系统设置中找到 TimeTracker
4. 开启「允许使用统计信息」开关
5. 返回应用，权限状态应显示为「✓ 已授予使用统计权限」

#### 第二步：配置服务器（可选）

默认配置已经设置好，如需修改：

- **服务器地址**: `http://43.163.97.77:3002/`
- **API Token**: `aw-sync-2026-secret`（在代码中配置）
- **同步间隔**: 30 分钟

修改后点击「保存配置」。

#### 第三步：启用追踪

1. 打开「启用追踪」开关
2. 应用开始后台运行
3. 每分钟采样一次前台应用
4. 每 30 分钟自动同步到服务器

#### 第四步：电池优化（推荐）

为确保后台稳定运行：

1. 点击「电池优化设置（可选）」按钮
2. 找到 TimeTracker
3. 选择「不优化」或「允许后台运行」

不同手机品牌可能需要额外设置：
- **小米**: 设置 → 应用设置 → 应用管理 → TimeTracker → 省电策略 → 无限制
- **华为**: 设置 → 应用 → 应用启动管理 → TimeTracker → 手动管理 → 全部允许
- **OPPO/vivo**: 设置 → 电池 → 应用耗电管理 → TimeTracker → 允许后台运行

## 使用说明

### 查看同步状态

在主界面「同步状态」卡片中可以看到：
- 未同步记录数量
- 上次同步时间
- 设备 ID

### 手动同步

点击「立即同步」按钮可以手动触发一次同步。

### 停止追踪

关闭「启用追踪」开关即可停止后台任务。

## 数据格式

应用会将数据同步到服务器的 `/api/aw/sync` 接口：

```json
{
  "date": "2026-03-04",
  "buckets": {
    "aw-watcher-android_abc12345": [
      {
        "id": 1,
        "timestamp": "2026-03-04T05:00:00Z",
        "duration": 60.0,
        "data": {
          "app": "com.android.chrome",
          "title": "Chrome"
        }
      }
    ]
  }
}
```

## 故障排查

### 问题 1: 权限授予后仍显示未授予

**解决方案**:
1. 完全关闭应用（从后台清除）
2. 重新打开应用
3. 如果仍未解决，重启手机

### 问题 2: 数据没有同步

**检查清单**:
- [ ] 是否授予了使用统计权限
- [ ] 是否开启了「启用追踪」开关
- [ ] 手机是否联网
- [ ] 服务器地址是否正确
- [ ] 查看「未同步记录」数量是否增加

**调试方法**:
1. 点击「立即同步」测试网络连接
2. 检查服务器是否可访问：`curl http://43.163.97.77:3002/api/aw/sync`

### 问题 3: 后台任务被杀死

**解决方案**:
1. 关闭电池优化
2. 允许应用自启动
3. 锁定应用在后台（部分手机支持）
4. 在「开发者选项」中关闭「不保留活动」

### 问题 4: 编译失败

**常见原因**:
- JDK 版本不对（需要 JDK 17）
- Gradle 下载失败（网络问题）
- Android SDK 未安装

**解决方案**:
```bash
# 检查 JDK 版本
java -version

# 清理并重新编译
./gradlew clean
./gradlew assembleRelease
```

## 开发者信息

### 项目结构

```
android-timetracker/
├── app/                        # 应用模块
│   ├── src/main/
│   │   ├── java/com/timetracker/
│   │   │   ├── data/          # 数据层
│   │   │   ├── worker/        # 后台任务
│   │   │   ├── ui/            # 界面
│   │   │   ├── util/          # 工具类
│   │   │   └── receiver/      # 广播接收器
│   │   ├── res/               # 资源文件
│   │   └── AndroidManifest.xml
│   └── build.gradle.kts
├── .github/workflows/         # GitHub Actions
├── gradle/                    # Gradle 配置
├── build.gradle.kts          # 根构建文件
├── settings.gradle.kts       # 项目设置
└── README.md                 # 项目说明
```

### 修改服务器地址

编辑 `app/src/main/java/com/timetracker/data/remote/ApiService.kt`:

```kotlin
companion object {
    private const val DEFAULT_BASE_URL = "http://YOUR_SERVER:PORT/"
    private const val DEFAULT_TOKEN = "your-token"
}
```

### 修改同步间隔

编辑 `app/src/main/java/com/timetracker/util/PreferenceManager.kt`:

```kotlin
private const val DEFAULT_SYNC_INTERVAL = 30L // 分钟
```

## 下一步

1. **推送代码到 GitHub**
   ```bash
   cd /root/.openclaw/workspace/android-timetracker
   git init
   git add .
   git commit -m "Initial commit: Android TimeTracker"
   git remote add origin https://github.com/YOUR_USERNAME/android-timetracker.git
   git push -u origin main
   ```

2. **等待 GitHub Actions 编译**
   - 访问仓库的 Actions 页面
   - 等待编译完成（约 5-10 分钟）
   - 下载生成的 APK

3. **安装到手机测试**
   - 下载 APK 到手机
   - 按照上述步骤安装和配置
   - 测试数据采集和同步功能

## 技术支持

如有问题，请：
1. 查看本文档的「故障排查」部分
2. 在 GitHub 仓库提交 Issue
3. 联系开发者

---

**预计编译时间**: 5-10 分钟（GitHub Actions）  
**APK 大小**: 约 5-8 MB  
**最低 Android 版本**: Android 5.0 (API 21)
