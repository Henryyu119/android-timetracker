package com.timetracker.health

import android.content.Context
import android.util.Log
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.aggregate.AggregateMetric
import androidx.health.connect.client.aggregate.AggregationResult
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.timetracker.data.local.HealthDataEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/**
 * Health Connect 数据管理器
 * 负责从 Health Connect 读取健康数据
 */
class HealthDataManager(private val context: Context) {
    
    private val healthConnectClient: HealthConnectClient by lazy {
        HealthConnectClient.getOrCreate(context)
    }
    
    companion object {
        private const val TAG = "HealthDataManager"
        
        val PERMISSIONS = setOf(
            HealthPermission.getReadPermission(StepsRecord::class),
            HealthPermission.getReadPermission(SleepSessionRecord::class),
            HealthPermission.getReadPermission(HeartRateRecord::class)
        )
    }
    
    /**
     * 检查 Health Connect 是否可用
     */
    suspend fun isAvailable(): Boolean = withContext(Dispatchers.IO) {
        try {
            val status = HealthConnectClient.getSdkStatus(context)
            status == HealthConnectClient.SDK_AVAILABLE
        } catch (e: Exception) {
            Log.e(TAG, "Error checking Health Connect availability", e)
            false
        }
    }
    
    /**
     * 检查是否已授予所有权限
     */
    suspend fun hasAllPermissions(): Boolean = withContext(Dispatchers.IO) {
        try {
            val granted = healthConnectClient.permissionController.getGrantedPermissions()
            granted.containsAll(PERMISSIONS)
        } catch (e: Exception) {
            Log.e(TAG, "Error checking permissions", e)
            false
        }
    }
    
    /**
     * 读取指定日期的健康数据
     * @param date 日期字符串 YYYY-MM-DD
     * @return HealthDataEntity 或 null
     */
    suspend fun readHealthData(date: String): HealthDataEntity? = withContext(Dispatchers.IO) {
        try {
            val localDate = LocalDate.parse(date)
            val zoneId = ZoneId.systemDefault()
            
            // 当天开始和结束时间
            val startOfDay = localDate.atStartOfDay(zoneId).toInstant()
            val endOfDay = localDate.plusDays(1).atStartOfDay(zoneId).toInstant()
            
            // 读取步数（使用聚合避免重复计数）
            val steps = readSteps(startOfDay, endOfDay)
            
            // 读取睡眠（可能跨天，所以从前一天开始读）
            val sleepMinutes = readSleep(
                localDate.minusDays(1).atStartOfDay(zoneId).toInstant(),
                endOfDay
            )
            
            // 读取心率
            val heartRateData = readHeartRate(startOfDay, endOfDay)
            
            Log.d(TAG, "Read health data for $date: steps=$steps, sleep=$sleepMinutes min")
            
            HealthDataEntity(
                date = date,
                steps = steps,
                sleepMinutes = sleepMinutes,
                heartRateAvg = heartRateData.avg,
                heartRateMax = heartRateData.max,
                heartRateMin = heartRateData.min,
                synced = false
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error reading health data for $date", e)
            null
        }
    }
    
    /**
     * 读取步数
     */
    private suspend fun readSteps(start: Instant, end: Instant): Int {
        return try {
            val response = healthConnectClient.aggregate(
                AggregateRequest(
                    metrics = setOf(AggregateMetric.STEPS_COUNT_TOTAL),
                    timeRangeFilter = TimeRangeFilter.between(start, end)
                )
            )
            response[AggregateMetric.STEPS_COUNT_TOTAL]?.toInt() ?: 0
        } catch (e: Exception) {
            Log.e(TAG, "Error reading steps", e)
            0
        }
    }
    
    /**
     * 读取睡眠时长（分钟）
     */
    private suspend fun readSleep(start: Instant, end: Instant): Int {
        return try {
            val response = healthConnectClient.readRecords(
                ReadRecordsRequest(
                    recordType = SleepSessionRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(start, end)
                )
            )
            
            // 计算总睡眠时长
            response.records.sumOf { session ->
                ChronoUnit.MINUTES.between(session.startTime, session.endTime)
            }.toInt()
        } catch (e: Exception) {
            Log.e(TAG, "Error reading sleep", e)
            0
        }
    }
    
    /**
     * 读取心率数据
     */
    private suspend fun readHeartRate(start: Instant, end: Instant): HeartRateData {
        return try {
            val response = healthConnectClient.readRecords(
                ReadRecordsRequest(
                    recordType = HeartRateRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(start, end)
                )
            )
            
            if (response.records.isEmpty()) {
                return HeartRateData(0, 0, 0)
            }
            
            // 提取所有心率样本
            val allSamples = response.records.flatMap { record ->
                record.samples.map { it.beatsPerMinute }
            }
            
            if (allSamples.isEmpty()) {
                return HeartRateData(0, 0, 0)
            }
            
            HeartRateData(
                avg = allSamples.average().toInt(),
                max = allSamples.maxOrNull()?.toInt() ?: 0,
                min = allSamples.minOrNull()?.toInt() ?: 0
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error reading heart rate", e)
            HeartRateData(0, 0, 0)
        }
    }
    
    /**
     * 心率数据
     */
    data class HeartRateData(
        val avg: Int,
        val max: Int,
        val min: Int
    )
}
