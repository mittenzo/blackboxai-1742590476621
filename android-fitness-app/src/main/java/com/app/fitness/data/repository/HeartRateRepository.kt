package com.app.fitness.data.repository

import android.util.Log
import com.app.fitness.data.database.HeartRateDao
import com.app.fitness.data.database.HeartRateEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.util.Date
import java.util.concurrent.TimeUnit

class HeartRateRepository(private val heartRateDao: HeartRateDao) {
    
    companion object {
        private const val TAG = "HeartRateRepository"
        
        // Time constants
        private val ONE_DAY_MILLIS = TimeUnit.DAYS.toMillis(1)
        private val ONE_WEEK_MILLIS = TimeUnit.DAYS.toMillis(7)
        private val ONE_MONTH_MILLIS = TimeUnit.DAYS.toMillis(30)
    }

    /**
     * Insert a new heart rate measurement
     */
    suspend fun insertHeartRate(heartRate: Int, accuracy: Int = 0, note: String? = null) {
        try {
            val heartRateEntity = HeartRateEntity(
                heartRate = heartRate,
                accuracy = accuracy,
                note = note
            )
            
            if (heartRateEntity.isValidHeartRate() && heartRateEntity.isValidAccuracy()) {
                heartRateDao.insertHeartRate(heartRateEntity)
            } else {
                Log.w(TAG, "Invalid heart rate data: rate=$heartRate, accuracy=$accuracy")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error inserting heart rate", e)
            throw e
        }
    }

    /**
     * Get the latest heart rate measurement
     */
    suspend fun getLatestHeartRate(): HeartRateEntity? {
        return try {
            heartRateDao.getLatestHeartRate()
        } catch (e: Exception) {
            Log.e(TAG, "Error getting latest heart rate", e)
            null
        }
    }

    /**
     * Get all heart rate measurements as a Flow
     */
    fun getAllHeartRates(): Flow<List<HeartRateEntity>> {
        return heartRateDao.getAllHeartRates()
            .catch { e ->
                Log.e(TAG, "Error getting all heart rates", e)
                emit(emptyList())
            }
    }

    /**
     * Get heart rates for today
     */
    fun getTodayHeartRates(): Flow<List<HeartRateEntity>> {
        val todayStart = Date().time - ONE_DAY_MILLIS
        return heartRateDao.getHeartRatesBetween(todayStart)
            .catch { e ->
                Log.e(TAG, "Error getting today's heart rates", e)
                emit(emptyList())
            }
    }

    /**
     * Get heart rates for the last week
     */
    fun getWeeklyHeartRates(): Flow<List<HeartRateEntity>> {
        val weekStart = Date().time - ONE_WEEK_MILLIS
        return heartRateDao.getHeartRatesBetween(weekStart)
            .catch { e ->
                Log.e(TAG, "Error getting weekly heart rates", e)
                emit(emptyList())
            }
    }

    /**
     * Get heart rates for the last month
     */
    fun getMonthlyHeartRates(): Flow<List<HeartRateEntity>> {
        val monthStart = Date().time - ONE_MONTH_MILLIS
        return heartRateDao.getHeartRatesBetween(monthStart)
            .catch { e ->
                Log.e(TAG, "Error getting monthly heart rates", e)
                emit(emptyList())
            }
    }

    /**
     * Get average heart rate for a time period
     */
    suspend fun getAverageHeartRate(startTime: Long, endTime: Long = Date().time): Float {
        return try {
            heartRateDao.getAverageHeartRate(startTime, endTime) ?: 0f
        } catch (e: Exception) {
            Log.e(TAG, "Error getting average heart rate", e)
            0f
        }
    }

    /**
     * Get maximum heart rate for a time period
     */
    suspend fun getMaxHeartRate(startTime: Long, endTime: Long = Date().time): Int {
        return try {
            heartRateDao.getMaxHeartRate(startTime, endTime) ?: 0
        } catch (e: Exception) {
            Log.e(TAG, "Error getting max heart rate", e)
            0
        }
    }

    /**
     * Get minimum heart rate for a time period
     */
    suspend fun getMinHeartRate(startTime: Long, endTime: Long = Date().time): Int {
        return try {
            heartRateDao.getMinHeartRate(startTime, endTime) ?: 0
        } catch (e: Exception) {
            Log.e(TAG, "Error getting min heart rate", e)
            0
        }
    }

    /**
     * Clean up old heart rate data
     */
    suspend fun cleanupOldData(keepDataDuration: Long = ONE_MONTH_MILLIS) {
        try {
            val cutoffTime = Date().time - keepDataDuration
            heartRateDao.deleteHeartRatesOlderThan(cutoffTime)
        } catch (e: Exception) {
            Log.e(TAG, "Error cleaning up old heart rate data", e)
        }
    }
}