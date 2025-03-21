package com.app.fitness.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        HeartRateEntity::class,
        WalkingEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun heartRateDao(): HeartRateDao
    abstract fun walkingDao(): WalkingDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "fitness_database"
                )
                    .addCallback(object : RoomDatabase.Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            // Initialize database with default values if needed
                            INSTANCE?.let { database ->
                                CoroutineScope(Dispatchers.IO).launch {
                                    // Insert default walking goal
                                    database.walkingDao().insertWalkingRecord(
                                        WalkingEntity(
                                            steps = 0,
                                            dailyGoal = WalkingEntity.DEFAULT_DAILY_GOAL
                                        )
                                    )
                                }
                            }
                        }
                    })
                    .fallbackToDestructiveMigration() // Handle migrations by recreating tables
                    .build()
                
                INSTANCE = instance
                instance
            }
        }

        /**
         * Helper method to clear the database instance
         * Useful for testing or when user logs out
         */
        fun destroyInstance() {
            INSTANCE = null
        }
    }
}