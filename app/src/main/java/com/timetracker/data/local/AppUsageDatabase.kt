package com.timetracker.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [AppUsageEntity::class, HealthDataEntity::class],
    version = 2,
    exportSchema = false
)
abstract class AppUsageDatabase : RoomDatabase() {
    
    abstract fun appUsageDao(): AppUsageDao
    abstract fun healthDataDao(): HealthDataDao
    
    companion object {
        @Volatile
        private var INSTANCE: AppUsageDatabase? = null
        
        fun getDatabase(context: Context): AppUsageDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppUsageDatabase::class.java,
                    "app_usage_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
