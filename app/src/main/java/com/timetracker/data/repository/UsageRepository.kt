package com.timetracker.data.repository

import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import com.timetracker.data.local.AppUsageDao
import com.timetracker.data.local.AppUsageEntity
import com.timetracker.data.remote.ApiService
import com.timetracker.data.remote.SyncRequest
import com.timetracker.data.remote.UsageData
import com.timetracker.data.remote.UsageEvent
import com.timetracker.util.PreferenceManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class UsageRepository(
    private val context: Context,
    private val dao: AppUsageDao,
    private val apiService: ApiService,
    private val preferenceManager: PreferenceManager
) {
    
    private val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
    private val packageManager = context.packageManager
    
    companion object {
        private const val TAG = "UsageRepository"
        private const val BUCKET_NAME_PREFIX = "aw-watcher-android"
    }
    
    /**
     * 收集当前时间段的应用使用数据
     */
    suspend fun collectUsageStats(): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val endTime = System.currentTimeMillis()
            val startTime = endTime - (60 * 1000) // 最近1分钟
            
            val usageStatsList = usageStatsManager.queryUsageStats(
                UsageStatsManager.INTERVAL_BEST,
                startTime,
                endTime
            )
            
            if (usageStatsList.isNullOrEmpty()) {
                Log.d(TAG, "No usage stats found")
                return@withContext Result.success(0)
            }
            
            // 找到最近使用的应用
            val recentApp = usageStatsList
                .filter { it.lastTimeUsed > 0 }
                .maxByOrNull { it.lastTimeUsed }
            
            if (recentApp != null && recentApp.totalTimeInForeground > 0) {
                val appName = getAppName(recentApp.packageName)
                val entity = AppUsageEntity(
                    packageName = recentApp.packageName,
                    appName = appName,
                    timestamp = recentApp.lastTimeUsed,
                    duration = 60 // 1分钟采样间隔
                )
                
                dao.insert(entity)
                Log.d(TAG, "Collected usage: $appName (${recentApp.packageName})")
                Result.success(1)
            } else {
                Result.success(0)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error collecting usage stats", e)
            Result.failure(e)
        }
    }
    
    /**
     * 同步未同步的数据到服务器
     */
    suspend fun syncToServer(): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val unsyncedData = dao.getUnsyncedUsages()
            
            if (unsyncedData.isEmpty()) {
                Log.d(TAG, "No data to sync")
                return@withContext Result.success(0)
            }
            
            // 按日期分组
            val groupedByDate = unsyncedData.groupBy { entity ->
                val date = Date(entity.timestamp)
                SimpleDateFormat("yyyy-MM-dd", Locale.US).format(date)
            }
            
            var totalSynced = 0
            
            for ((date, entities) in groupedByDate) {
                val events = entities.map { entity ->
                    UsageEvent(
                        id = entity.id,
                        timestamp = formatTimestamp(entity.timestamp),
                        duration = entity.duration.toDouble(),
                        data = UsageData(
                            app = entity.packageName,
                            title = entity.appName
                        )
                    )
                }
                
                val deviceId = preferenceManager.getDeviceId()
                val bucketName = "${BUCKET_NAME_PREFIX}_$deviceId"
                
                val request = SyncRequest(
                    date = date,
                    buckets = mapOf(bucketName to events)
                )
                
                val token = preferenceManager.getApiToken()
                val response = apiService.syncUsageData(token, request)
                
                if (response.isSuccessful) {
                    val ids = entities.map { it.id }
                    dao.markAsSynced(ids)
                    totalSynced += entities.size
                    Log.d(TAG, "Synced ${entities.size} records for date $date")
                } else {
                    Log.e(TAG, "Sync failed: ${response.code()} ${response.message()}")
                    return@withContext Result.failure(Exception("Sync failed: ${response.code()}"))
                }
            }
            
            // 清理7天前的已同步数据
            val sevenDaysAgo = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000)
            dao.deleteOldSyncedData(sevenDaysAgo)
            
            Result.success(totalSynced)
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing to server", e)
            Result.failure(e)
        }
    }
    
    /**
     * 获取未同步数据数量
     */
    suspend fun getUnsyncedCount(): Int = withContext(Dispatchers.IO) {
        dao.getUnsyncedCount()
    }
    
    /**
     * 获取最近的使用记录
     */
    suspend fun getRecentUsages(limit: Int = 100): List<AppUsageEntity> = withContext(Dispatchers.IO) {
        dao.getRecentUsages(limit)
    }
    
    private fun getAppName(packageName: String): String {
        return try {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(appInfo).toString()
        } catch (e: PackageManager.NameNotFoundException) {
            packageName
        }
    }
    
    private fun formatTimestamp(timestamp: Long): String {
        val date = Date(timestamp)
        val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        format.timeZone = TimeZone.getTimeZone("UTC")
        return format.format(date)
    }
}
