package com.app.fitness

import android.app.Application
import com.app.fitness.utils.FirebaseAnalyticsManager

class FitnessApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialize Firebase Analytics
        FirebaseAnalyticsManager.initialize(this)
    }
}