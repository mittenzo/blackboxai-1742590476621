package com.app.fitness.data.repository

import android.util.Log
import com.app.fitness.data.database.WalkingDao
import com.app.fitness.data.database.WalkingEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.util.Date
import java.util.concurrent.TimeUnit

class WalkingRepository(private val walkingDao: WalkingDao) {
    
    companion object {
        private const val TAG = "WalkingRepository"
        
        // Time constants
        private val ONE_DAY_MILLIS = TimeUnit.DAYS.toMillis(1)
        private val ONE_WEEK_MILLIS = TimeUnit.DAYS.toMillis(7)
        private val ONE_MONTH_MILLIS = TimeUnit.DAYS.toMillis(30)
    }

    /**
     * Get or create today's walking record
     */
    private suspend fun getOrCreateTodayRecord(): WalkingEntity {
        return walkingDao.getTodayWalkingRecord() ?: WalkingEntity()
    }

    /**
     * Update today's step count
     */
    suspend fun updateSteps(steps: Int, distance: Float, activeMinutes: Int) {
        try {
            if (steps < 0 || distance < 0 || activeMinutes < 0) {
                Log.w(TAG, "Invalid walking data: steps=$steps, distance=$distance, activeMinutes=$activeMinutes")
                return
            }
            walkingDao.updateTodaySteps(steps, distance, activeMinutes)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating steps", e)
            throw e
        }
    }

    /**
     * Update daily step goal
     */
    suspend fun updateDailyGoal(newGoal: Int) {
        try {
            if (newGoal < WalkingEntity.MIN_DAILY_GOAL || newGoal > WalkingEntity.MAX_DAILY_GOAL) {
                Log.w(TAG, "Invalid daily goal: $newGoal")
                return
            }
            val today = Date().time
            walkingDao.updateDailyGoalFromDate(newGoal, today)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating daily goal", e)
            throw e
        }
    }

    /**
     * Get today's walking record as Flow
     */
    suspend fun getTodayWalkingRecord(): WalkingEntity? {
        return try {
            walkingDao.getTodayWalkingRecord()
        } catch (e: Exception) {
            Log.e(TAG, "Error getting today's walking record", e)
            null
        }
    }

    /**
     * Get walking records for the last week
     */
    fun getWeeklyWalkingRecords(): Flow<List<WalkingEntity>> {
        val weekStart = Date().time - ONE_WEEK_MILLIS
        return walkingDao.getWalkingRecordsBetween(weekStart)
            .catch { e ->
                Log.e(TAG, "Error getting weekly walking records", e)
                emit(emptyList())
            }
    }

    /**
     * Get walking records for the last month
     */
    fun getMonthlyWalkingRecords(): Flow<List<WalkingEntity>> {
        val monthStart = Date().time - ONE_MONTH_MILLIS
        return walkingDao.getWalkingRecordsBetween(monthStart)
            .catch { e ->
                Log.e(TAG, "Error getting monthly walking records", e)
                emit(emptyList())
            }
    }

    /**
     * Get total steps for a time period
     */
    suspend fun getTotalSteps(startDate: Long, endDate: Long = Date().time): Int {
        return try {
            walkingDao.getTotalSteps(startDate, endDate) ?: 0
        } catch (e: Exception) {
            Log.e(TAG, "Error getting total steps", e)
            0
        }
    }

    /**
     * Get total distance for a time period
     */
    suspend fun getTotalDistance(startDate: Long, endDate: Long = Date().time): Float {
        return try {
            walkingDao.getTotalDistance(startDate, endDate) ?: 0f
        } catch (e: Exception) {
            Log.e(TAG, "Error getting total distance", e)
            0f
        }
    }

    /**
     * Get average daily steps for a time period
     */
    suspend fun getAverageSteps(startDate: Long, endDate: Long = Date().time): Float {
        return try {
            walkingDao.getAverageSteps(startDate, endDate) ?: 0f
        } catch (e: Exception) {
            Log.e(TAG, "Error getting average steps", e)
            0f
        }
    }

    /**
     * Get days where goal was achieved
     */
    fun getGoalAchievedDays(startDate: Long, endDate: Long = Date().time): Flow<List<WalkingEntity>> {
        return walkingDao.getAchievedGoalDays(startDate, endDate)
            .catch { e ->
                Log.e(TAG, "Error getting goal achieved days", e)
                emit(emptyList())
            }
    }

    /**
     * Get count of days where goal was achieved
     */
    suspend fun getGoalAchievedCount(startDate: Long, endDate: Long = Date().time): Int {
        return try {
            walkingDao.getGoalAchievedCount(startDate, endDate)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting goal achieved count", e)
            0
        }
    }

    /**
     * Calculate current progress towards daily goal
     */
    suspend fun calculateDailyGoalProgress(): Float {
        return try {
            val todayRecord = getTodayWalkingRecord()
            todayRecord?.calculateGoalProgress() ?: 0f
        } catch (e: Exception) {
            Log.e(TAG, "Error calculating daily goal progress", e)
            0f
        }
    }

    /**
     * Check if today's goal has been achieved
     */
    suspend fun isTodayGoalAchieved(): Boolean {
        return try {
            val todayRecord = getTodayWalkingRecord()
            todayRecord?.isGoalAchieved() ?: false
        } catch (e: Exception) {
            Log.e(TAG, "Error checking if today's goal is achieved", e)
            false
        }
    }
}