package com.timetracker.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 健康数据实体
 * 存储从 Health Connect 读取的健康数据
 */
@Entity(tableName = "health_data")
data class HealthDataEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    val date: String,           // 日期 YYYY-MM-DD
    val steps: Int = 0,         // 步数
    val sleepMinutes: Int = 0,  // 睡眠时长（分钟）
    val heartRateAvg: Int = 0,  // 平均心率（可选）
    val heartRateMax: Int = 0,  // 最大心率（可选）
    val heartRateMin: Int = 0,  // 最小心率（可选）
    val synced: Boolean = false, // 是否已同步到服务器
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
