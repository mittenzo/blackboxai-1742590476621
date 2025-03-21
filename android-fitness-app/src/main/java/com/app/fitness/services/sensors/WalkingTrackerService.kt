package com.app.fitness.services.sensors

import android.app.Service
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.os.Binder
import android.os.IBinder
import android.util.Log
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.app.fitness.data.database.WalkingEntity
import com.app.fitness.data.repository.WalkingRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class WalkingTrackerService : Service(), SensorEventListener {

    private val serviceScope = CoroutineScope(Dispatchers.Default + Job())
    private lateinit var sensorManager: SensorManager
    private var stepSensor: Sensor? = null
    private lateinit var walkingRepository: WalkingRepository
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    
    // Binder for activity/fragment binding
    private val binder = WalkingTrackerBinder()
    
    // Tracking variables
    private var initialSteps: Int = -1
    private var currentSteps: Int = 0
    private var lastLocation: Location? = null
    private var totalDistance: Float = 0f
    private var activeMinutes: Int = 0
    private var lastActiveTime: Long = 0
    private var isTracking = false

    // Callbacks
    private var walkingDataCallback: ((Int, Float, Int) -> Unit)? = null

    companion object {
        private const val TAG = "WalkingTrackerService"
        private const val LOCATION_UPDATE_INTERVAL = 30000L // 30 seconds
        private const val ACTIVITY_THRESHOLD_MS = 60000L // 1 minute
        private const val SIGNIFICANT_MOVEMENT_METERS = 10f
    }

    inner class WalkingTrackerBinder : Binder() {
        fun getService(): WalkingTrackerService = this@WalkingTrackerService
    }

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            locationResult.lastLocation?.let { newLocation ->
                updateDistance(newLocation)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        
        // Initialize repository (this should be injected in a production app)
        walkingRepository = WalkingRepository(
            (application as com.app.fitness.FitnessApplication)
                .getDatabase()
                .walkingDao()
        )
    }

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    override fun onUnbind(intent: Intent?): Boolean {
        stopTracking()
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        stopTracking()
    }

    /**
     * Start tracking walking activity
     */
    fun startTracking(callback: (Int, Float, Int) -> Unit) {
        if (stepSensor == null) {
            Log.w(TAG, "Step counter sensor not available on this device")
            return
        }

        walkingDataCallback = callback
        if (!isTracking) {
            // Register step sensor
            sensorManager.registerListener(
                this,
                stepSensor,
                SensorManager.SENSOR_DELAY_NORMAL
            )

            // Request location updates
            try {
                val locationRequest = LocationRequest.create().apply {
                    interval = LOCATION_UPDATE_INTERVAL
                    priority = LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY
                }
                fusedLocationClient.requestLocationUpdates(
                    locationRequest,
                    locationCallback,
                    null
                )
            } catch (e: SecurityException) {
                Log.e(TAG, "Location permission not granted", e)
            }

            isTracking = true
            lastActiveTime = System.currentTimeMillis()
            Log.d(TAG, "Started walking tracking")
        }
    }

    /**
     * Stop tracking walking activity
     */
    fun stopTracking() {
        if (isTracking) {
            sensorManager.unregisterListener(this)
            fusedLocationClient.removeLocationUpdates(locationCallback)
            isTracking = false
            walkingDataCallback = null
            // Save final data
            updateWalkingData()
            Log.d(TAG, "Stopped walking tracking")
        }
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_STEP_COUNTER) {
            val steps = event.values[0].toInt()
            
            // Initialize step count if needed
            if (initialSteps == -1) {
                initialSteps = steps
            }
            
            // Calculate current steps
            currentSteps = steps - initialSteps
            
            // Update active minutes
            updateActiveMinutes()
            
            // Update walking data in repository
            updateWalkingData()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        if (sensor?.type == Sensor.TYPE_STEP_COUNTER) {
            Log.d(TAG, "Step counter accuracy changed: $accuracy")
        }
    }

    private fun updateDistance(newLocation: Location) {
        lastLocation?.let { lastLoc ->
            val distance = lastLoc.distanceTo(newLocation)
            if (distance >= SIGNIFICANT_MOVEMENT_METERS) {
                totalDistance += distance
                updateWalkingData()
            }
        }
        lastLocation = newLocation
    }

    private fun updateActiveMinutes() {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastActiveTime >= ACTIVITY_THRESHOLD_MS) {
            activeMinutes++
            lastActiveTime = currentTime
        }
    }

    private fun updateWalkingData() {
        serviceScope.launch {
            try {
                walkingRepository.updateSteps(
                    steps = currentSteps,
                    distance = totalDistance,
                    activeMinutes = activeMinutes
                )
                walkingDataCallback?.invoke(currentSteps, totalDistance, activeMinutes)
            } catch (e: Exception) {
                Log.e(TAG, "Error updating walking data", e)
            }
        }
    }

    /**
     * Check if step counting is available on this device
     */
    fun isStepCountingAvailable(): Boolean {
        return stepSensor != null
    }

    /**
     * Reset daily tracking data
     */
    fun resetDailyData() {
        currentSteps = 0
        totalDistance = 0f
        activeMinutes = 0
        initialSteps = -1
        lastLocation = null
        updateWalkingData()
    }
}