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
        Log.i(TAG, "=== Accessibility service connected ===")
        
        val info = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS or
                    AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS
            notificationTimeout = 100
        }
        serviceInfo = info
        Log.i(TAG, "Service info configured: eventTypes=${info.eventTypes}")
    }
    
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        Log.v(TAG, "onAccessibilityEvent called: type=${event?.eventType}, package=${event?.packageName}")
        
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            Log.v(TAG, "Ignoring non-window-state-changed event")
            return
        }
        
        val packageName = event.packageName?.toString()
        if (packageName.isNullOrEmpty()) {
            Log.v(TAG, "Package name is null or empty")
            return
        }
        
        val currentTime = System.currentTimeMillis()
        
        // 过滤系统核心包（但保留用户可见的应用）
        val systemPackages = setOf(
            "com.android.systemui",
            "com.android.launcher3",
            "android"
        )
        
        if (systemPackages.contains(packageName)) {
            Log.v(TAG, "Filtered system package: $packageName")
            return
        }
        
        // 防抖：同一个应用5秒内只记录一次
        if (packageName == lastPackageName && 
            currentTime - lastEventTime < MIN_EVENT_INTERVAL) {
            Log.v(TAG, "Debounced: $packageName (last event ${currentTime - lastEventTime}ms ago)")
            return
        }
        
        lastPackageName = packageName
        lastEventTime = currentTime
        
        Log.i(TAG, "Detected app switch: $packageName")
        
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
                Log.i(TAG, "✓ Recorded: $appName ($packageName)")
            } catch (e: Exception) {
                Log.e(TAG, "✗ Error recording app usage: $packageName", e)
            }
        }
    }
    
    override fun onInterrupt() {
        Log.w(TAG, "=== Accessibility service interrupted ===")
    }
    
    override fun onDestroy() {
        super.onDestroy()
        Log.w(TAG, "=== Accessibility service destroyed ===")
    }
    
    private fun getAppName(packageName: String): String {
        return try {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(appInfo).toString()
        } catch (e: Exception) {
            Log.w(TAG, "Could not get app name for $packageName, using package name")
            packageName
        }
    }
}
