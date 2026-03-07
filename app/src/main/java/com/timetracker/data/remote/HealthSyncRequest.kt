package com.timetracker.data.remote

/**
 * 健康数据同步请求
 */
data class HealthSyncRequest(
    val date: String,           // YYYY-MM-DD
    val steps: Int,             // 步数
    val sleep_minutes: Int      // 睡眠时长（分钟）
)

/**
 * 健康数据同步响应
 */
data class HealthSyncResponse(
    val success: Boolean,
    val date: String
)
