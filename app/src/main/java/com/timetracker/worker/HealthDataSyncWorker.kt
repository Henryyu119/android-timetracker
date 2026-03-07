package com.timetracker.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.timetracker.data.local.AppUsageDatabase
import com.timetracker.data.local.HealthDataEntity
import com.timetracker.health.HealthDataManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * 健康数据同步 Worker
 * 定期从 Health Connect 读取健康数据并保存到本地数据库
 */
class HealthDataSyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    
    private val healthDataManager = HealthDataManager(context)
    private val database = AppUsageDatabase.getDatabase(context)
    private val healthDataDao = database.healthDataDao()
    
    companion object {
        private const val TAG = "HealthDataSyncWorker"
        const val WORK_NAME = "health_data_sync"
    }
    
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Starting health data sync")
            
            // 检查 Health Connect 是否可用
            if (!healthDataManager.isAvailable()) {
                Log.w(TAG, "Health Connect not available")
                return@withContext Result.failure()
            }
            
            // 检查权限
            if (!healthDataManager.hasAllPermissions()) {
                Log.w(TAG, "Health Connect permissions not granted")
                return@withContext Result.failure()
            }
            
            // 读取今天和昨天的数据
            val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
            val yesterday = LocalDate.now().minusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE)
            
            var successCount = 0
            
            // 同步今天的数据
            syncDateData(today)?.let { successCount++ }
            
            // 同步昨天的数据（补充睡眠数据）
            syncDateData(yesterday)?.let { successCount++ }
            
            // 清理 7 天前的已同步数据
            val sevenDaysAgo = LocalDate.now().minusDays(7).format(DateTimeFormatter.ISO_LOCAL_DATE)
            healthDataDao.deleteOldSyncedData(sevenDaysAgo)
            
            Log.d(TAG, "Health data sync completed: $successCount records updated")
            Result.success()
            
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing health data", e)
            Result.retry()
        }
    }
    
    /**
     * 同步指定日期的健康数据
     */
    private suspend fun syncDateData(date: String): HealthDataEntity? {
        return try {
            // 从 Health Connect 读取数据
            val healthData = healthDataManager.readHealthData(date)
            
            if (healthData != null) {
                // 检查是否已存在
                val existing = healthDataDao.getByDate(date)
                
                if (existing != null) {
                    // 更新现有记录
                    val updated = healthData.copy(
                        id = existing.id,
                        synced = existing.synced, // 保持同步状态
                        createdAt = existing.createdAt,
                        updatedAt = System.currentTimeMillis()
                    )
                    healthDataDao.insert(updated)
                    Log.d(TAG, "Updated health data for $date")
                } else {
                    // 插入新记录
                    healthDataDao.insert(healthData)
                    Log.d(TAG, "Inserted health data for $date")
                }
                
                healthData
            } else {
                Log.w(TAG, "No health data available for $date")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing data for $date", e)
            null
        }
    }
}
