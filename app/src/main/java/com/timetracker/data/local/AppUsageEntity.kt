package com.timetracker.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_usage")
data class AppUsageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val packageName: String,
    val appName: String,
    val timestamp: Long,
    val duration: Long, // 秒
    val synced: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
