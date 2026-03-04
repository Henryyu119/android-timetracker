package com.timetracker.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface AppUsageDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(usage: AppUsageEntity): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(usages: List<AppUsageEntity>)
    
    @Query("SELECT * FROM app_usage WHERE synced = 0 ORDER BY timestamp ASC")
    suspend fun getUnsyncedUsages(): List<AppUsageEntity>
    
    @Query("SELECT * FROM app_usage WHERE timestamp >= :startTime AND timestamp <= :endTime ORDER BY timestamp ASC")
    suspend fun getUsagesByTimeRange(startTime: Long, endTime: Long): List<AppUsageEntity>
    
    @Query("UPDATE app_usage SET synced = 1 WHERE id IN (:ids)")
    suspend fun markAsSynced(ids: List<Long>)
    
    @Query("DELETE FROM app_usage WHERE synced = 1 AND createdAt < :beforeTime")
    suspend fun deleteOldSyncedData(beforeTime: Long)
    
    @Query("SELECT COUNT(*) FROM app_usage WHERE synced = 0")
    suspend fun getUnsyncedCount(): Int
    
    @Query("SELECT * FROM app_usage ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentUsages(limit: Int = 100): List<AppUsageEntity>
}
