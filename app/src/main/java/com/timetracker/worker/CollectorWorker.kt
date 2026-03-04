package com.timetracker.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.timetracker.data.local.AppUsageDatabase
import com.timetracker.data.remote.ApiService
import com.timetracker.data.repository.UsageRepository
import com.timetracker.util.PreferenceManager

class CollectorWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    
    companion object {
        const val TAG = "CollectorWorker"
        const val WORK_NAME = "usage_collector"
    }
    
    override suspend fun doWork(): Result {
        Log.d(TAG, "Starting usage collection")
        
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
            
            val result = repository.collectUsageStats()
            
            return if (result.isSuccess) {
                Log.d(TAG, "Collection successful: ${result.getOrNull()} records")
                Result.success()
            } else {
                Log.e(TAG, "Collection failed", result.exceptionOrNull())
                Result.retry()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error in collector", e)
            return Result.retry()
        }
    }
}
