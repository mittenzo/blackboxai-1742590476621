package com.app.fitness.data.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import java.util.Date

@Dao
interface HeartRateDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHeartRate(heartRate: HeartRateEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHeartRates(heartRates: List<HeartRateEntity>)

    @Update
    suspend fun updateHeartRate(heartRate: HeartRateEntity)

    @Delete
    suspend fun deleteHeartRate(heartRate: HeartRateEntity)

    @Query("DELETE FROM heart_rate_records")
    suspend fun deleteAllHeartRates()

    @Query("SELECT * FROM heart_rate_records ORDER BY timestamp DESC")
    fun getAllHeartRates(): Flow<List<HeartRateEntity>>

    @Query("SELECT * FROM heart_rate_records WHERE timestamp >= :startTime AND timestamp <= :endTime ORDER BY timestamp DESC")
    fun getHeartRatesBetween(startTime: Long, endTime: Long = Date().time): Flow<List<HeartRateEntity>>

    @Query("SELECT * FROM heart_rate_records ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestHeartRate(): HeartRateEntity?

    @Query("SELECT AVG(heartRate) FROM heart_rate_records WHERE timestamp >= :startTime AND timestamp <= :endTime")
    suspend fun getAverageHeartRate(startTime: Long, endTime: Long = Date().time): Float?

    @Query("SELECT MAX(heartRate) FROM heart_rate_records WHERE timestamp >= :startTime AND timestamp <= :endTime")
    suspend fun getMaxHeartRate(startTime: Long, endTime: Long = Date().time): Int?

    @Query("SELECT MIN(heartRate) FROM heart_rate_records WHERE timestamp >= :startTime AND timestamp <= :endTime")
    suspend fun getMinHeartRate(startTime: Long, endTime: Long = Date().time): Int?

    @Query("DELETE FROM heart_rate_records WHERE timestamp < :timestamp")
    suspend fun deleteHeartRatesOlderThan(timestamp: Long)

    @Transaction
    @Query("SELECT COUNT(*) FROM heart_rate_records WHERE timestamp >= :startTime AND timestamp <= :endTime")
    suspend fun getHeartRateCount(startTime: Long, endTime: Long = Date().time): Int
}