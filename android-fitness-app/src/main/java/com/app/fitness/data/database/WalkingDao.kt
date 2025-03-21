package com.app.fitness.data.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import java.util.Date

@Dao
interface WalkingDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWalkingRecord(walkingRecord: WalkingEntity)

    @Update
    suspend fun updateWalkingRecord(walkingRecord: WalkingEntity)

    @Delete
    suspend fun deleteWalkingRecord(walkingRecord: WalkingEntity)

    @Query("DELETE FROM walking_records")
    suspend fun deleteAllWalkingRecords()

    @Query("SELECT * FROM walking_records WHERE date >= :startDate AND date <= :endDate ORDER BY date DESC")
    fun getWalkingRecordsBetween(startDate: Long, endDate: Long = Date().time): Flow<List<WalkingEntity>>

    @Query("SELECT * FROM walking_records ORDER BY date DESC")
    fun getAllWalkingRecords(): Flow<List<WalkingEntity>>

    @Query("SELECT * FROM walking_records WHERE date >= :todayStart AND date <= :todayEnd LIMIT 1")
    suspend fun getTodayWalkingRecord(
        todayStart: Long = getTodayStartTimestamp(),
        todayEnd: Long = Date().time
    ): WalkingEntity?

    @Query("SELECT SUM(steps) FROM walking_records WHERE date >= :startDate AND date <= :endDate")
    suspend fun getTotalSteps(startDate: Long, endDate: Long = Date().time): Int?

    @Query("SELECT SUM(distance) FROM walking_records WHERE date >= :startDate AND date <= :endDate")
    suspend fun getTotalDistance(startDate: Long, endDate: Long = Date().time): Float?

    @Query("SELECT AVG(steps) FROM walking_records WHERE date >= :startDate AND date <= :endDate")
    suspend fun getAverageSteps(startDate: Long, endDate: Long = Date().time): Float?

    @Query("SELECT MAX(steps) FROM walking_records WHERE date >= :startDate AND date <= :endDate")
    suspend fun getMaxSteps(startDate: Long, endDate: Long = Date().time): Int?

    @Query("SELECT * FROM walking_records WHERE steps >= dailyGoal AND date >= :startDate AND date <= :endDate")
    fun getAchievedGoalDays(startDate: Long, endDate: Long = Date().time): Flow<List<WalkingEntity>>

    @Query("SELECT COUNT(*) FROM walking_records WHERE steps >= dailyGoal AND date >= :startDate AND date <= :endDate")
    suspend fun getGoalAchievedCount(startDate: Long, endDate: Long = Date().time): Int

    @Query("UPDATE walking_records SET dailyGoal = :newGoal WHERE date >= :date")
    suspend fun updateDailyGoalFromDate(newGoal: Int, date: Long)

    @Transaction
    suspend fun updateTodaySteps(steps: Int, distance: Float, activeMinutes: Int) {
        val today = getTodayWalkingRecord()
        if (today != null) {
            val updatedRecord = today.copy(
                steps = steps,
                distance = distance,
                activeMinutes = activeMinutes,
                caloriesBurned = (steps * WalkingEntity.CALORIES_PER_STEP).toInt()
            )
            updateWalkingRecord(updatedRecord)
        } else {
            val newRecord = WalkingEntity(
                steps = steps,
                distance = distance,
                activeMinutes = activeMinutes,
                caloriesBurned = (steps * WalkingEntity.CALORIES_PER_STEP).toInt()
            )
            insertWalkingRecord(newRecord)
        }
    }

    companion object {
        private fun getTodayStartTimestamp(): Long {
            val calendar = java.util.Calendar.getInstance()
            calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
            calendar.set(java.util.Calendar.MINUTE, 0)
            calendar.set(java.util.Calendar.SECOND, 0)
            calendar.set(java.util.Calendar.MILLISECOND, 0)
            return calendar.timeInMillis
        }
    }
}