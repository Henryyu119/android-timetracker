package com.timetracker.util

import android.content.Context
import androidx.work.*
import com.timetracker.worker.CollectorWorker
import com.timetracker.worker.SyncWorker
import java.util.concurrent.TimeUnit

object WorkManagerHelper {
    
    /**
     * 启动数据收集任务（每分钟）
     */
    fun startCollectorWork(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(false)
            .build()
        
        val collectorWork = PeriodicWorkRequestBuilder<CollectorWorker>(
            15, TimeUnit.MINUTES, // 最小间隔15分钟
            5, TimeUnit.MINUTES   // 弹性窗口5分钟
        )
            .setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.LINEAR,
                PeriodicWorkRequest.MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS
            )
            .build()
        
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            CollectorWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            collectorWork
        )
    }
    
    /**
     * 启动数据同步任务（每30分钟）
     */
    fun startSyncWork(context: Context, intervalMinutes: Long = 30) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(false)
            .build()
        
        val syncWork = PeriodicWorkRequestBuilder<SyncWorker>(
            intervalMinutes, TimeUnit.MINUTES,
            5, TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                PeriodicWorkRequest.MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS
            )
            .build()
        
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            SyncWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.REPLACE,
            syncWork
        )
    }
    
    /**
     * 立即执行一次同步
     */
    fun syncNow(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        
        val syncWork = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(constraints)
            .build()
        
        WorkManager.getInstance(context).enqueue(syncWork)
    }
    
    /**
     * 停止所有任务
     */
    fun stopAllWork(context: Context) {
        WorkManager.getInstance(context).apply {
            cancelUniqueWork(CollectorWorker.WORK_NAME)
            cancelUniqueWork(SyncWorker.WORK_NAME)
        }
    }
    
    /**
     * 获取任务状态
     */
    fun getWorkStatus(context: Context): Pair<WorkInfo.State?, WorkInfo.State?> {
        val workManager = WorkManager.getInstance(context)
        
        val collectorStatus = workManager
            .getWorkInfosForUniqueWork(CollectorWorker.WORK_NAME)
            .get()
            .firstOrNull()?.state
        
        val syncStatus = workManager
            .getWorkInfosForUniqueWork(SyncWorker.WORK_NAME)
            .get()
            .firstOrNull()?.state
        
        return Pair(collectorStatus, syncStatus)
    }
}
