package com.timetracker.util

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.work.*
import com.timetracker.worker.CollectorWorker
import com.timetracker.worker.HealthDataSyncWorker
import com.timetracker.worker.HealthDataUploadWorker
import com.timetracker.worker.SyncWorker
import java.util.concurrent.TimeUnit

object WorkManagerHelper {
    
    /**
     * 启动所有后台任务
     */
    fun startAllWorkers(context: Context, syncIntervalMinutes: Long = 30) {
        startCollectorWorker(context)
        startSyncWorker(context, syncIntervalMinutes)
        startHealthWorkers(context)
    }
    
    /**
     * 停止所有后台任务
     */
    fun stopAllWorkers(context: Context) {
        val workManager = WorkManager.getInstance(context)
        workManager.cancelUniqueWork(CollectorWorker.WORK_NAME)
        workManager.cancelUniqueWork(SyncWorker.WORK_NAME)
        workManager.cancelUniqueWork(HealthDataSyncWorker.WORK_NAME)
        workManager.cancelUniqueWork(HealthDataUploadWorker.WORK_NAME)
    }
    
    /**
     * 启动数据采集 Worker（每分钟）
     */
    private fun startCollectorWorker(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(false)
            .build()
        
        val collectorRequest = PeriodicWorkRequestBuilder<CollectorWorker>(
            15, TimeUnit.MINUTES,
            5, TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .build()
        
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            CollectorWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            collectorRequest
        )
    }
    
    /**
     * 启动数据同步 Worker
     */
    private fun startSyncWorker(context: Context, intervalMinutes: Long) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        
        val syncRequest = PeriodicWorkRequestBuilder<SyncWorker>(
            intervalMinutes, TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .build()
        
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            SyncWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.REPLACE,
            syncRequest
        )
    }
    
    /**
     * 启动健康数据相关 Workers
     */
    private fun startHealthWorkers(context: Context) {
        // 检查 Health Connect 是否可用
        val sdkStatus = HealthConnectClient.getSdkStatus(context)
        if (sdkStatus != HealthConnectClient.SDK_AVAILABLE) {
            return
        }
        
        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(false)
            .build()
        
        // 健康数据同步 Worker（每小时从 Health Connect 读取）
        val healthSyncRequest = PeriodicWorkRequestBuilder<HealthDataSyncWorker>(
            1, TimeUnit.HOURS
        )
            .setConstraints(constraints)
            .build()
        
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            HealthDataSyncWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            healthSyncRequest
        )
        
        // 健康数据上传 Worker（每 30 分钟上传到服务器）
        val uploadConstraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        
        val healthUploadRequest = PeriodicWorkRequestBuilder<HealthDataUploadWorker>(
            30, TimeUnit.MINUTES
        )
            .setConstraints(uploadConstraints)
            .build()
        
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            HealthDataUploadWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            healthUploadRequest
        )
    }
    
    /**
     * 手动触发健康数据同步
     */
    fun triggerHealthSync(context: Context) {
        val request = OneTimeWorkRequestBuilder<HealthDataSyncWorker>()
            .build()
        
        WorkManager.getInstance(context).enqueue(request)
    }
    
    /**
     * 手动触发健康数据上传
     */
    fun triggerHealthUpload(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        
        val request = OneTimeWorkRequestBuilder<HealthDataUploadWorker>()
            .setConstraints(constraints)
            .build()
        
        WorkManager.getInstance(context).enqueue(request)
    }
    
    /**
     * 手动触发应用使用数据同步
     */
    fun triggerSync(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        
        val syncRequest = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(constraints)
            .build()
        
        WorkManager.getInstance(context).enqueue(syncRequest)
    }
}
