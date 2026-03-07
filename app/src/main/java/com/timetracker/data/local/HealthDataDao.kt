package com.timetracker.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

/**
 * 健康数据 DAO
 */
@Dao
interface HealthDataDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(healthData: HealthDataEntity): Long
    
    @Query("SELECT * FROM health_data WHERE date = :date LIMIT 1")
    suspend fun getByDate(date: String): HealthDataEntity?
    
    @Query("SELECT * FROM health_data WHERE synced = 0 ORDER BY date DESC")
    suspend fun getUnsyncedData(): List<HealthDataEntity>
    
    @Query("UPDATE health_data SET synced = 1 WHERE id = :id")
    suspend fun markAsSynced(id: Long)
    
    @Query("SELECT * FROM health_data ORDER BY date DESC LIMIT :limit")
    suspend fun getRecentData(limit: Int = 30): List<HealthDataEntity>
    
    @Query("DELETE FROM health_data WHERE date < :beforeDate AND synced = 1")
    suspend fun deleteOldSyncedData(beforeDate: String)
}
