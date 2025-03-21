package com.app.fitness.ui.viewmodels

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.app.fitness.data.database.HeartRateEntity
import com.app.fitness.data.repository.HeartRateRepository
import com.app.fitness.utils.FirebaseAnalyticsManager
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.util.Date
import java.util.concurrent.TimeUnit

class HeartRateViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: HeartRateRepository
    
    // LiveData for UI updates
    private val _currentHeartRate = MutableLiveData<Int>()
    val currentHeartRate: LiveData<Int> = _currentHeartRate

    private val _heartRateHistory = MutableLiveData<List<HeartRateEntity>>()
    val heartRateHistory: LiveData<List<HeartRateEntity>> = _heartRateHistory

    private val _averageHeartRate = MutableLiveData<Float>()
    val averageHeartRate: LiveData<Float> = _averageHeartRate

    private val _maxHeartRate = MutableLiveData<Int>()
    val maxHeartRate: LiveData<Int> = _maxHeartRate

    private val _minHeartRate = MutableLiveData<Int>()
    val minHeartRate: LiveData<Int> = _minHeartRate

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    companion object {
        private const val TAG = "HeartRateViewModel"
        private val ONE_DAY_MILLIS = TimeUnit.DAYS.toMillis(1)
    }

    init {
        // Initialize repository (this should be injected in a production app)
        repository = HeartRateRepository(
            (application as com.app.fitness.FitnessApplication)
                .getDatabase()
                .heartRateDao()
        )
        
        // Load initial data
        loadHeartRateData()
    }

    /**
     * Load heart rate data from repository
     */
    private fun loadHeartRateData() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Get today's heart rates
                repository.getTodayHeartRates()
                    .catch { e ->
                        Log.e(TAG, "Error loading heart rates", e)
                        _error.value = "Failed to load heart rate data"
                    }
                    .collect { heartRates ->
                        _heartRateHistory.value = heartRates
                        calculateStatistics(heartRates)
                    }
                
                // Get latest heart rate
                repository.getLatestHeartRate()?.let {
                    _currentHeartRate.value = it.heartRate
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in loadHeartRateData", e)
                _error.value = "Failed to load heart rate data"
                FirebaseAnalyticsManager.logError("heart_rate_load", e.message ?: "Unknown error")
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Calculate heart rate statistics
     */
    private fun calculateStatistics(heartRates: List<HeartRateEntity>) {
        if (heartRates.isEmpty()) return

        viewModelScope.launch {
            try {
                val todayStart = Date().time - ONE_DAY_MILLIS
                
                // Calculate average
                _averageHeartRate.value = repository.getAverageHeartRate(todayStart)
                
                // Get max and min
                _maxHeartRate.value = repository.getMaxHeartRate(todayStart)
                _minHeartRate.value = repository.getMinHeartRate(todayStart)
            } catch (e: Exception) {
                Log.e(TAG, "Error calculating statistics", e)
                FirebaseAnalyticsManager.logError("heart_rate_stats", e.message ?: "Unknown error")
            }
        }
    }

    /**
     * Update current heart rate
     */
    fun updateHeartRate(heartRate: Int, accuracy: Int) {
        viewModelScope.launch {
            try {
                repository.insertHeartRate(heartRate, accuracy)
                _currentHeartRate.value = heartRate
                
                // Log analytics
                FirebaseAnalyticsManager.logHeartRateMeasurement(heartRate, accuracy)
                
                // Reload data to update statistics
                loadHeartRateData()
            } catch (e: Exception) {
                Log.e(TAG, "Error updating heart rate", e)
                _error.value = "Failed to update heart rate"
                FirebaseAnalyticsManager.logError("heart_rate_update", e.message ?: "Unknown error")
            }
        }
    }

    /**
     * Clean up old data
     */
    fun cleanupOldData() {
        viewModelScope.launch {
            try {
                repository.cleanupOldData()
            } catch (e: Exception) {
                Log.e(TAG, "Error cleaning up old data", e)
                FirebaseAnalyticsManager.logError("heart_rate_cleanup", e.message ?: "Unknown error")
            }
        }
    }

    /**
     * Refresh data
     */
    fun refresh() {
        loadHeartRateData()
    }

    /**
     * Clear error message
     */
    fun clearError() {
        _error.value = null
    }
}