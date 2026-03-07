package com.timetracker.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.timetracker.data.local.AppUsageDatabase
import com.timetracker.data.remote.ApiService
import com.timetracker.data.remote.HealthSyncRequest
import com.timetracker.util.PreferenceManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * 健康数据上传 Worker
 * 将本地未同步的健康数据上传到服务器
 */
class HealthDataUploadWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    
    private val database = AppUsageDatabase.getDatabase(context)
    private val healthDataDao = database.healthDataDao()
    private val preferenceManager = PreferenceManager(context)
    
    companion object {
        private const val TAG = "HealthDataUploadWorker"
        const val WORK_NAME = "health_data_upload"
    }
    
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Starting health data upload")
            
            // 获取服务器配置
            val serverUrl = preferenceManager.getServerUrl()
            val token = preferenceManager.getToken()
            
            // 创建 API 服务
            val retrofit = Retrofit.Builder()
                .baseUrl(serverUrl)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
            
            val apiService = retrofit.create(ApiService::class.java)
            
            // 获取未同步的数据
            val unsyncedData = healthDataDao.getUnsyncedData()
            
            if (unsyncedData.isEmpty()) {
                Log.d(TAG, "No unsynced health data")
                return@withContext Result.success()
            }
            
            Log.d(TAG, "Found ${unsyncedData.size} unsynced health records")
            
            var successCount = 0
            var failCount = 0
            
            // 逐条上传
            for (data in unsyncedData) {
                try {
                    val request = HealthSyncRequest(
                        date = data.date,
                        steps = data.steps,
                        sleep_minutes = data.sleepMinutes
                    )
                    
                    val response = apiService.syncHealthData(token, request)
                    
                    if (response.isSuccessful) {
                        // 标记为已同步
                        healthDataDao.markAsSynced(data.id)
                        successCount++
                        Log.d(TAG, "Uploaded health data for ${data.date}")
                    } else {
                        failCount++
                        Log.w(TAG, "Failed to upload health data for ${data.date}: ${response.code()}")
                    }
                } catch (e: Exception) {
                    failCount++
                    Log.e(TAG, "Error uploading health data for ${data.date}", e)
                }
            }
            
            Log.d(TAG, "Health data upload completed: $successCount success, $failCount failed")
            
            if (failCount > 0 && successCount == 0) {
                Result.retry()
            } else {
                Result.success()
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error in health data upload worker", e)
            Result.retry()
        }
    }
}
