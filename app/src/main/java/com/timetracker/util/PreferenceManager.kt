package com.timetracker.util

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import java.util.UUID

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class PreferenceManager(private val context: Context) {
    
    companion object {
        private val SERVER_URL = stringPreferencesKey("server_url")
        private val API_TOKEN = stringPreferencesKey("api_token")
        private val DEVICE_ID = stringPreferencesKey("device_id")
        private val SYNC_INTERVAL = longPreferencesKey("sync_interval")
        private val LAST_SYNC_TIME = longPreferencesKey("last_sync_time")
        private val COLLECTOR_ENABLED = stringPreferencesKey("collector_enabled")
        
        private const val DEFAULT_SERVER_URL = "http://43.163.97.77:3002/"
        private const val DEFAULT_API_TOKEN = "aw-sync-2026-secret"
        private const val DEFAULT_SYNC_INTERVAL = 30L // 分钟
    }
    
    // Server URL
    fun getServerUrl(): String = runBlocking {
        context.dataStore.data.map { preferences ->
            preferences[SERVER_URL] ?: DEFAULT_SERVER_URL
        }.first()
    }
    
    suspend fun setServerUrl(url: String) {
        context.dataStore.edit { preferences ->
            preferences[SERVER_URL] = url
        }
    }
    
    // API Token
    fun getApiToken(): String = runBlocking {
        context.dataStore.data.map { preferences ->
            preferences[API_TOKEN] ?: DEFAULT_API_TOKEN
        }.first()
    }
    
    suspend fun setApiToken(token: String) {
        context.dataStore.edit { preferences ->
            preferences[API_TOKEN] = token
        }
    }
    
    // Device ID
    fun getDeviceId(): String = runBlocking {
        context.dataStore.data.map { preferences ->
            preferences[DEVICE_ID] ?: generateAndSaveDeviceId()
        }.first()
    }
    
    private fun generateAndSaveDeviceId(): String {
        val deviceId = UUID.randomUUID().toString().substring(0, 8)
        runBlocking {
            context.dataStore.edit { preferences ->
                preferences[DEVICE_ID] = deviceId
            }
        }
        return deviceId
    }
    
    // Sync Interval
    fun getSyncInterval(): Long = runBlocking {
        context.dataStore.data.map { preferences ->
            preferences[SYNC_INTERVAL] ?: DEFAULT_SYNC_INTERVAL
        }.first()
    }
    
    suspend fun setSyncInterval(minutes: Long) {
        context.dataStore.edit { preferences ->
            preferences[SYNC_INTERVAL] = minutes
        }
    }
    
    // Last Sync Time
    fun getLastSyncTime(): Long = runBlocking {
        context.dataStore.data.map { preferences ->
            preferences[LAST_SYNC_TIME] ?: 0L
        }.first()
    }
    
    suspend fun setLastSyncTime(time: Long) {
        context.dataStore.edit { preferences ->
            preferences[LAST_SYNC_TIME] = time
        }
    }
    
    // Collector Enabled
    fun isCollectorEnabled(): Boolean = runBlocking {
        context.dataStore.data.map { preferences ->
            preferences[COLLECTOR_ENABLED] != "false"
        }.first()
    }
    
    suspend fun setCollectorEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[COLLECTOR_ENABLED] = enabled.toString()
        }
    }
    
    // Flow versions for UI
    val serverUrlFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[SERVER_URL] ?: DEFAULT_SERVER_URL
    }
    
    val lastSyncTimeFlow: Flow<Long> = context.dataStore.data.map { preferences ->
        preferences[LAST_SYNC_TIME] ?: 0L
    }
    
    val collectorEnabledFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[COLLECTOR_ENABLED] != "false"
    }
}
