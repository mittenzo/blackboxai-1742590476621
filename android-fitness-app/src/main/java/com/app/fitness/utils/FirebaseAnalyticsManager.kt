package com.app.fitness.utils

import android.content.Context
import android.os.Bundle
import android.util.Log
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase

object FirebaseAnalyticsManager {
    private const val TAG = "FirebaseAnalytics"
    private lateinit var firebaseAnalytics: FirebaseAnalytics

    // Event Names
    private object Events {
        const val HEART_RATE_MEASURED = "heart_rate_measured"
        const val DAILY_GOAL_ACHIEVED = "daily_goal_achieved"
        const val DAILY_GOAL_UPDATED = "daily_goal_updated"
        const val WALKING_MILESTONE = "walking_milestone"
        const val APP_ERROR = "app_error"
        const val SENSOR_ERROR = "sensor_error"
        const val FEATURE_USED = "feature_used"
        const val NOTIFICATION_INTERACTION = "notification_interaction"
    }

    // Parameter Names
    private object Params {
        const val HEART_RATE_VALUE = "heart_rate_value"
        const val HEART_RATE_ACCURACY = "heart_rate_accuracy"
        const val STEPS_COUNT = "steps_count"
        const val DISTANCE = "distance"
        const val GOAL_VALUE = "goal_value"
        const val ERROR_TYPE = "error_type"
        const val ERROR_MESSAGE = "error_message"
        const val FEATURE_NAME = "feature_name"
        const val NOTIFICATION_TYPE = "notification_type"
        const val INTERACTION_TYPE = "interaction_type"
        const val MILESTONE_TYPE = "milestone_type"
        const val MILESTONE_VALUE = "milestone_value"
    }

    /**
     * Initialize Firebase Analytics
     */
    fun initialize(context: Context) {
        try {
            firebaseAnalytics = Firebase.analytics
            Log.d(TAG, "Firebase Analytics initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing Firebase Analytics", e)
        }
    }

    /**
     * Log heart rate measurement
     */
    fun logHeartRateMeasurement(heartRate: Int, accuracy: Int) {
        try {
            val params = Bundle().apply {
                putInt(Params.HEART_RATE_VALUE, heartRate)
                putInt(Params.HEART_RATE_ACCURACY, accuracy)
            }
            firebaseAnalytics.logEvent(Events.HEART_RATE_MEASURED, params)
        } catch (e: Exception) {
            Log.e(TAG, "Error logging heart rate measurement", e)
        }
    }

    /**
     * Log daily goal achievement
     */
    fun logDailyGoalAchieved(steps: Int, distance: Float) {
        try {
            val params = Bundle().apply {
                putInt(Params.STEPS_COUNT, steps)
                putFloat(Params.DISTANCE, distance)
            }
            firebaseAnalytics.logEvent(Events.DAILY_GOAL_ACHIEVED, params)
        } catch (e: Exception) {
            Log.e(TAG, "Error logging daily goal achievement", e)
        }
    }

    /**
     * Log daily goal update
     */
    fun logDailyGoalUpdate(newGoal: Int) {
        try {
            val params = Bundle().apply {
                putInt(Params.GOAL_VALUE, newGoal)
            }
            firebaseAnalytics.logEvent(Events.DAILY_GOAL_UPDATED, params)
        } catch (e: Exception) {
            Log.e(TAG, "Error logging daily goal update", e)
        }
    }

    /**
     * Log walking milestone
     */
    fun logWalkingMilestone(type: String, value: Int) {
        try {
            val params = Bundle().apply {
                putString(Params.MILESTONE_TYPE, type)
                putInt(Params.MILESTONE_VALUE, value)
            }
            firebaseAnalytics.logEvent(Events.WALKING_MILESTONE, params)
        } catch (e: Exception) {
            Log.e(TAG, "Error logging walking milestone", e)
        }
    }

    /**
     * Log app error
     */
    fun logError(type: String, message: String) {
        try {
            val params = Bundle().apply {
                putString(Params.ERROR_TYPE, type)
                putString(Params.ERROR_MESSAGE, message)
            }
            firebaseAnalytics.logEvent(Events.APP_ERROR, params)
        } catch (e: Exception) {
            Log.e(TAG, "Error logging app error", e)
        }
    }

    /**
     * Log sensor error
     */
    fun logSensorError(sensorType: String, errorMessage: String) {
        try {
            val params = Bundle().apply {
                putString(Params.ERROR_TYPE, sensorType)
                putString(Params.ERROR_MESSAGE, errorMessage)
            }
            firebaseAnalytics.logEvent(Events.SENSOR_ERROR, params)
        } catch (e: Exception) {
            Log.e(TAG, "Error logging sensor error", e)
        }
    }

    /**
     * Log feature usage
     */
    fun logFeatureUsed(featureName: String) {
        try {
            val params = Bundle().apply {
                putString(Params.FEATURE_NAME, featureName)
            }
            firebaseAnalytics.logEvent(Events.FEATURE_USED, params)
        } catch (e: Exception) {
            Log.e(TAG, "Error logging feature usage", e)
        }
    }

    /**
     * Log notification interaction
     */
    fun logNotificationInteraction(notificationType: String, interactionType: String) {
        try {
            val params = Bundle().apply {
                putString(Params.NOTIFICATION_TYPE, notificationType)
                putString(Params.INTERACTION_TYPE, interactionType)
            }
            firebaseAnalytics.logEvent(Events.NOTIFICATION_INTERACTION, params)
        } catch (e: Exception) {
            Log.e(TAG, "Error logging notification interaction", e)
        }
    }

    /**
     * Set user property
     */
    fun setUserProperty(name: String, value: String) {
        try {
            firebaseAnalytics.setUserProperty(name, value)
        } catch (e: Exception) {
            Log.e(TAG, "Error setting user property", e)
        }
    }
}