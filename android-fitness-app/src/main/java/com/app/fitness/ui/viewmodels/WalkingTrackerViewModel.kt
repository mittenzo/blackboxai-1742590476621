package com.app.fitness.ui.viewmodels

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.app.fitness.data.database.WalkingEntity
import com.app.fitness.data.repository.WalkingRepository
import com.app.fitness.utils.FirebaseAnalyticsManager
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.util.Date
import java.util.concurrent.TimeUnit

class WalkingTrackerViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: WalkingRepository

    // LiveData for UI updates
    private val _todaySteps = MutableLiveData<Int>()
    val todaySteps: LiveData<Int> = _todaySteps

    private val _todayDistance = MutableLiveData<Float>()
    val todayDistance: LiveData<Float> = _todayDistance

    private val _activeMinutes = MutableLiveData<Int>()
    val activeMinutes: LiveData<Int> = _activeMinutes

    private val _dailyGoal = MutableLiveData<Int>()
    val dailyGoal: LiveData<Int> = _dailyGoal

    private val _goalProgress = MutableLiveData<Float>()
    val goalProgress: LiveData<Float> = _goalProgress

    private val _weeklyHistory = MutableLiveData<List<WalkingEntity>>()
    val weeklyHistory: LiveData<List<WalkingEntity>> = _weeklyHistory

    private val _weeklyAverage = MutableLiveData<Float>()
    val weeklyAverage: LiveData<Float> = _weeklyAverage

    private val _goalAchievedCount = MutableLiveData<Int>()
    val goalAchievedCount: LiveData<Int> = _goalAchievedCount

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    companion object {
        private const val TAG = "WalkingTrackerViewModel"
        private val ONE_WEEK_MILLIS = TimeUnit.DAYS.toMillis(7)
    }

    init {
        // Initialize repository (this should be injected in a production app)
        repository = WalkingRepository(
            (application as com.app.fitness.FitnessApplication)
                .getDatabase()
                .walkingDao()
        )

        // Load initial data
        loadWalkingData()
    }

    /**
     * Load walking data from repository
     */
    private fun loadWalkingData() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Get today's record
                repository.getTodayWalkingRecord()?.let { record ->
                    _todaySteps.value = record.steps
                    _todayDistance.value = record.distance
                    _activeMinutes.value = record.activeMinutes
                    _dailyGoal.value = record.dailyGoal
                    _goalProgress.value = record.calculateGoalProgress()
                }

                // Get weekly history
                repository.getWeeklyWalkingRecords()
                    .catch { e ->
                        Log.e(TAG, "Error loading weekly history", e)
                        _error.value = "Failed to load weekly history"
                    }
                    .collect { records ->
                        _weeklyHistory.value = records
                        calculateStatistics(records)
                    }

            } catch (e: Exception) {
                Log.e(TAG, "Error in loadWalkingData", e)
                _error.value = "Failed to load walking data"
                FirebaseAnalyticsManager.logError("walking_load", e.message ?: "Unknown error")
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Calculate walking statistics
     */
    private fun calculateStatistics(records: List<WalkingEntity>) {
        if (records.isEmpty()) return

        viewModelScope.launch {
            try {
                val weekStart = Date().time - ONE_WEEK_MILLIS

                // Calculate weekly average
                _weeklyAverage.value = repository.getAverageSteps(weekStart)

                // Get goal achievement count
                _goalAchievedCount.value = repository.getGoalAchievedCount(weekStart)

            } catch (e: Exception) {
                Log.e(TAG, "Error calculating statistics", e)
                FirebaseAnalyticsManager.logError("walking_stats", e.message ?: "Unknown error")
            }
        }
    }

    /**
     * Update walking data
     */
    fun updateWalkingData(steps: Int, distance: Float, activeMinutes: Int) {
        viewModelScope.launch {
            try {
                repository.updateSteps(steps, distance, activeMinutes)
                _todaySteps.value = steps
                _todayDistance.value = distance
                _activeMinutes.value = activeMinutes

                // Check if goal is achieved
                val progress = (steps.toFloat() / (_dailyGoal.value ?: WalkingEntity.DEFAULT_DAILY_GOAL)) * 100
                _goalProgress.value = progress

                if (progress >= 100f) {
                    FirebaseAnalyticsManager.logDailyGoalAchieved(steps, distance)
                }

                // Check for milestones
                checkMilestones(steps)

                // Reload weekly data
                loadWalkingData()

            } catch (e: Exception) {
                Log.e(TAG, "Error updating walking data", e)
                _error.value = "Failed to update walking data"
                FirebaseAnalyticsManager.logError("walking_update", e.message ?: "Unknown error")
            }
        }
    }

    /**
     * Update daily goal
     */
    fun updateDailyGoal(newGoal: Int) {
        if (newGoal < WalkingEntity.MIN_DAILY_GOAL || newGoal > WalkingEntity.MAX_DAILY_GOAL) {
            _error.value = "Invalid goal value"
            return
        }

        viewModelScope.launch {
            try {
                repository.updateDailyGoal(newGoal)
                _dailyGoal.value = newGoal

                // Recalculate progress
                _todaySteps.value?.let { steps ->
                    _goalProgress.value = (steps.toFloat() / newGoal) * 100
                }

                FirebaseAnalyticsManager.logDailyGoalUpdate(newGoal)

            } catch (e: Exception) {
                Log.e(TAG, "Error updating daily goal", e)
                _error.value = "Failed to update daily goal"
                FirebaseAnalyticsManager.logError("goal_update", e.message ?: "Unknown error")
            }
        }
    }

    /**
     * Check for walking milestones
     */
    private fun checkMilestones(steps: Int) {
        val milestones = listOf(1000, 5000, 10000, 20000, 50000)
        for (milestone in milestones) {
            if (steps >= milestone && (_todaySteps.value ?: 0) < milestone) {
                FirebaseAnalyticsManager.logWalkingMilestone("steps", milestone)
            }
        }
    }

    /**
     * Refresh data
     */
    fun refresh() {
        loadWalkingData()
    }

    /**
     * Clear error message
     */
    fun clearError() {
        _error.value = null
    }
}