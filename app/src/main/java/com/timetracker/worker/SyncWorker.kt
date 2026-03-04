package com.timetracker.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.timetracker.data.local.AppUsageDatabase
import com.timetracker.data.remote.ApiService
import com.timetracker.data.repository.UsageRepository
import com.timetracker.util.PreferenceManager

class SyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    
    companion object {
        const val TAG = "SyncWorker"
        const val WORK_NAME = "usage_sync"
    }
    
    override suspend fun doWork(): Result {
        Log.d(TAG, "Starting data sync")
        
        try {
            val database = AppUsageDatabase.getDatabase(applicationContext)
            val preferenceManager = PreferenceManager(applicationContext)
            val apiService = ApiService.create(preferenceManager.getServerUrl())
            val repository = UsageRepository(
                applicationContext,
                database.appUsageDao(),
                apiService,
                preferenceManager
            )
            
            val result = repository.syncToServer()
            
            return if (result.isSuccess) {
                val syncedCount = result.getOrNull() ?: 0
                Log.d(TAG, "Sync successful: $syncedCount records")
                
                // 更新最后同步时间
                preferenceManager.setLastSyncTime(System.currentTimeMillis())
                
                Result.success()
            } else {
                Log.e(TAG, "Sync failed", result.exceptionOrNull())
                Result.retry()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error in sync", e)
            return Result.retry()
        }
    }
}
