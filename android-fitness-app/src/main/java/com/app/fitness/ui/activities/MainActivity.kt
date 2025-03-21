package com.app.fitness.ui.activities

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.app.fitness.R
import com.app.fitness.databinding.ActivityMainBinding
import com.app.fitness.services.ReminderService
import com.app.fitness.utils.FirebaseAnalyticsManager
import android.content.Intent
import com.google.android.material.snackbar.Snackbar

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController

    companion object {
        private const val TAG = "MainActivity"
        private const val PERMISSION_REQUEST_CODE = 123
        
        private val REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.BODY_SENSORS,
            Manifest.permission.ACTIVITY_RECOGNITION,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize view binding
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set up navigation
        setupNavigation()
        
        // Check and request permissions
        checkAndRequestPermissions()
        
        // Start reminder service
        startReminderService()
        
        // Log app launch
        FirebaseAnalyticsManager.logFeatureUsed("app_launch")
    }

    private fun setupNavigation() {
        // Set up Navigation Controller
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        // Connect bottom navigation with navigation controller
        binding.bottomNavigation.setupWithNavController(navController)

        // Handle navigation item selection
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_heart_rate -> {
                    navController.navigate(R.id.heartRateFragment)
                    FirebaseAnalyticsManager.logFeatureUsed("heart_rate_screen")
                    true
                }
                R.id.navigation_walking -> {
                    navController.navigate(R.id.walkingTrackerFragment)
                    FirebaseAnalyticsManager.logFeatureUsed("walking_screen")
                    true
                }
                else -> false
            }
        }
    }

    private fun checkAndRequestPermissions() {
        val permissionsToRequest = mutableListOf<String>()

        // Check which permissions we need to request
        for (permission in REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    permission
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissionsToRequest.add(permission)
            }
        }

        // Request permissions if needed
        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                permissionsToRequest.toTypedArray(),
                PERMISSION_REQUEST_CODE
            )
        } else {
            // All permissions are already granted
            onPermissionsGranted()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        
        when (requestCode) {
            PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && 
                    grantResults.all { it == PackageManager.PERMISSION_GRANTED }
                ) {
                    // All permissions granted
                    onPermissionsGranted()
                } else {
                    // Some permissions denied
                    onPermissionsDenied()
                }
            }
        }
    }

    private fun onPermissionsGranted() {
        Log.d(TAG, "All required permissions granted")
        // Initialize features that require permissions
    }

    private fun onPermissionsDenied() {
        Log.w(TAG, "Some permissions were denied")
        Snackbar.make(
            binding.root,
            R.string.permissions_required_message,
            Snackbar.LENGTH_LONG
        ).setAction(R.string.settings) {
            // Open app settings
            openAppSettings()
        }.show()
    }

    private fun openAppSettings() {
        Intent().apply {
            action = android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS
            data = android.net.Uri.fromParts("package", packageName, null)
            startActivity(this)
        }
    }

    private fun startReminderService() {
        try {
            val serviceIntent = Intent(this, ReminderService::class.java)
            startService(serviceIntent)
        } catch (e: Exception) {
            Log.e(TAG, "Error starting reminder service", e)
            FirebaseAnalyticsManager.logError("service_start", "Failed to start reminder service")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Cleanup if needed
    }
}