package com.app.fitness.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "walking_records")
data class WalkingEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    /**
     * Date of the walking record (stored as timestamp)
     */
    val date: Long = Date().time,
    
    /**
     * Total steps taken for the day
     */
    val steps: Int = 0,
    
    /**
     * Distance walked in meters
     */
    val distance: Float = 0f,
    
    /**
     * Daily step goal
     */
    val dailyGoal: Int = DEFAULT_DAILY_GOAL,
    
    /**
     * Average speed in meters per second
     */
    val averageSpeed: Float = 0f,
    
    /**
     * Calories burned (estimated)
     */
    val caloriesBurned: Int = 0,
    
    /**
     * Active time in minutes
     */
    val activeMinutes: Int = 0
) {
    /**
     * Calculate progress towards daily goal as a percentage
     */
    fun calculateGoalProgress(): Float {
        return (steps.toFloat() / dailyGoal.toFloat()) * 100
    }

    /**
     * Check if daily goal has been achieved
     */
    fun isGoalAchieved(): Boolean {
        return steps >= dailyGoal
    }

    /**
     * Validates if the steps count is within reasonable bounds
     */
    fun isValidSteps(): Boolean {
        return steps in 0..MAX_DAILY_STEPS
    }

    /**
     * Validates if the daily goal is reasonable
     */
    fun isValidGoal(): Boolean {
        return dailyGoal in MIN_DAILY_GOAL..MAX_DAILY_GOAL
    }

    companion object {
        const val DEFAULT_DAILY_GOAL = 10000
        const val MIN_DAILY_GOAL = 1000
        const val MAX_DAILY_GOAL = 100000
        const val MAX_DAILY_STEPS = 100000
        
        /**
         * Average stride length in meters (can be customized per user in future)
         */
        const val DEFAULT_STRIDE_LENGTH = 0.762f // approximately 30 inches
        
        /**
         * Calories burned per step (rough estimate, can be customized based on user's weight/height)
         */
        const val CALORIES_PER_STEP = 0.04f
    }
}