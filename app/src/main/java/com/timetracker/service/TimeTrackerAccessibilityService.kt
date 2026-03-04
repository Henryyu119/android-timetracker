package com.timetracker.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.timetracker.data.local.AppUsageDatabase
import com.timetracker.data.local.AppUsageEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class TimeTrackerAccessibilityService : AccessibilityService() {
    
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var lastPackageName: String? = null
    private var lastEventTime: Long = 0
    
    companion object {
        const val TAG = "TimeTrackerA11yService"
        private const val MIN_EVENT_INTERVAL = 5000L // 5秒最小间隔
    }
    
    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d(TAG, "Accessibility service connected")
        
        val info = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS
            notificationTimeout = 100
        }
        serviceInfo = info
    }
    
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            return
        }
        
        val packageName = event.packageName?.toString() ?: return
        val currentTime = System.currentTimeMillis()
        
        // 过滤系统包和重复事件
        if (packageName.startsWith("com.android.") || 
            packageName.startsWith("com.miui.") ||
            packageName == lastPackageName && 
            currentTime - lastEventTime < MIN_EVENT_INTERVAL) {
            return
        }
        
        lastPackageName = packageName
        lastEventTime = currentTime
        
        // 记录应用使用
        serviceScope.launch {
            try {
                val database = AppUsageDatabase.getDatabase(applicationContext)
                val appName = getAppName(packageName)
                
                val entity = AppUsageEntity(
                    packageName = packageName,
                    appName = appName,
                    timestamp = currentTime,
                    duration = 60 // 默认1分钟
                )
                
                database.appUsageDao().insert(entity)
                Log.d(TAG, "Recorded app usage: $appName ($packageName)")
            } catch (e: Exception) {
                Log.e(TAG, "Error recording app usage", e)
            }
        }
    }
    
    override fun onInterrupt() {
        Log.d(TAG, "Accessibility service interrupted")
    }
    
    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Accessibility service destroyed")
    }
    
    private fun getAppName(packageName: String): String {
        return try {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(appInfo).toString()
        } catch (e: Exception) {
            packageName
        }
    }
}
