package com.timetracker.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.timetracker.util.PreferenceManager
import com.timetracker.util.WorkManagerHelper

class BootReceiver : BroadcastReceiver() {
    
    companion object {
        private const val TAG = "BootReceiver"
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d(TAG, "Boot completed, starting workers")
            
            val preferenceManager = PreferenceManager(context)
            
            if (preferenceManager.isCollectorEnabled()) {
                WorkManagerHelper.startCollectorWork(context)
                WorkManagerHelper.startSyncWork(
                    context,
                    preferenceManager.getSyncInterval()
                )
                Log.d(TAG, "Workers started successfully")
            } else {
                Log.d(TAG, "Collector is disabled, skipping worker start")
            }
        }
    }
}
