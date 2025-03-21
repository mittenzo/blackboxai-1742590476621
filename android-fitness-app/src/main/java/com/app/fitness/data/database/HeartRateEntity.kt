package com.app.fitness.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "heart_rate_records")
data class HeartRateEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    /**
     * Timestamp of the heart rate measurement
     */
    val timestamp: Long = Date().time,
    
    /**
     * Heart rate value in beats per minute (BPM)
     */
    val heartRate: Int,
    
    /**
     * Accuracy level of the measurement (0-3)
     * 0: Unreliable
     * 1: Low accuracy
     * 2: Medium accuracy
     * 3: High accuracy
     */
    val accuracy: Int = 0,
    
    /**
     * Optional note or label for this measurement
     */
    val note: String? = null
) {
    /**
     * Validates if the heart rate value is within a reasonable range
     * Normal resting heart rate for adults: 60-100 BPM
     * Extreme cases: 40-220 BPM
     */
    fun isValidHeartRate(): Boolean {
        return heartRate in 40..220
    }

    /**
     * Validates if the accuracy level is valid
     */
    fun isValidAccuracy(): Boolean {
        return accuracy in 0..3
    }

    companion object {
        const val MIN_HEART_RATE = 40
        const val MAX_HEART_RATE = 220
        const val MAX_ACCURACY = 3
    }
}