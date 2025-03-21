package com.app.fitness.services.sensors

import android.app.Service
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Binder
import android.os.IBinder
import android.util.Log
import com.app.fitness.data.repository.HeartRateRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class HeartRateService : Service(), SensorEventListener {

    private val serviceScope = CoroutineScope(Dispatchers.Default + Job())
    private lateinit var sensorManager: SensorManager
    private var heartRateSensor: Sensor? = null
    private lateinit var heartRateRepository: HeartRateRepository
    
    // Binder for activity/fragment binding
    private val binder = HeartRateBinder()
    
    // Callback for heart rate updates
    private var heartRateCallback: ((Int) -> Unit)? = null
    
    // Tracking variables
    private var lastReading: Long = 0
    private var isMonitoring = false

    companion object {
        private const val TAG = "HeartRateService"
        private const val MIN_UPDATE_INTERVAL_MS = 1000 // Minimum time between readings
    }

    inner class HeartRateBinder : Binder() {
        fun getService(): HeartRateService = this@HeartRateService
    }

    override fun onCreate() {
        super.onCreate()
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        heartRateSensor = sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE)
        
        // Initialize repository (this should be injected in a production app)
        heartRateRepository = HeartRateRepository(
            (application as com.app.fitness.FitnessApplication)
                .getDatabase()
                .heartRateDao()
        )
    }

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    override fun onUnbind(intent: Intent?): Boolean {
        stopMonitoring()
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        stopMonitoring()
    }

    /**
     * Start monitoring heart rate
     */
    fun startMonitoring(callback: (Int) -> Unit) {
        if (heartRateSensor == null) {
            Log.w(TAG, "Heart rate sensor not available on this device")
            return
        }

        heartRateCallback = callback
        if (!isMonitoring) {
            sensorManager.registerListener(
                this,
                heartRateSensor,
                SensorManager.SENSOR_DELAY_NORMAL
            )
            isMonitoring = true
            Log.d(TAG, "Started heart rate monitoring")
        }
    }

    /**
     * Stop monitoring heart rate
     */
    fun stopMonitoring() {
        if (isMonitoring) {
            sensorManager.unregisterListener(this)
            isMonitoring = false
            heartRateCallback = null
            Log.d(TAG, "Stopped heart rate monitoring")
        }
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_HEART_RATE) {
            val heartRate = event.values[0].toInt()
            val currentTime = System.currentTimeMillis()
            
            // Check if enough time has passed since last reading
            if (currentTime - lastReading >= MIN_UPDATE_INTERVAL_MS) {
                lastReading = currentTime
                
                // Validate heart rate value
                if (isValidHeartRate(heartRate)) {
                    // Store in database
                    serviceScope.launch {
                        try {
                            heartRateRepository.insertHeartRate(
                                heartRate = heartRate,
                                accuracy = event.accuracy
                            )
                        } catch (e: Exception) {
                            Log.e(TAG, "Error storing heart rate", e)
                        }
                    }
                    
                    // Notify callback
                    heartRateCallback?.invoke(heartRate)
                } else {
                    Log.w(TAG, "Invalid heart rate reading: $heartRate")
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        if (sensor?.type == Sensor.TYPE_HEART_RATE) {
            Log.d(TAG, "Heart rate sensor accuracy changed: $accuracy")
        }
    }

    /**
     * Check if heart rate is within valid range
     */
    private fun isValidHeartRate(heartRate: Int): Boolean {
        return heartRate in 40..220 // Normal human heart rate range
    }

    /**
     * Check if heart rate monitoring is available on this device
     */
    fun isHeartRateMonitorAvailable(): Boolean {
        return heartRateSensor != null
    }
}