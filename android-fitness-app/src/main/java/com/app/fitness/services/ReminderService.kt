package com.app.fitness.services

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.app.fitness.R
import com.app.fitness.data.repository.WalkingRepository
import com.app.fitness.ui.activities.MainActivity
import kotlinx.coroutines.*
import java.util.*
import java.util.concurrent.TimeUnit

class ReminderService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Default + Job())
    private lateinit var walkingRepository: WalkingRepository
    private lateinit var notificationManager: NotificationManager
    
    companion object {
        private const val TAG = "ReminderService"
        private const val NOTIFICATION_CHANNEL_ID = "fitness_reminders"
        private const val NOTIFICATION_ID = 1001
        private const val REMINDER_INTERVAL = TimeUnit.HOURS.toMillis(2) // Check every 2 hours
        private const val INACTIVITY_THRESHOLD = TimeUnit.HOURS.toMillis(1) // Alert after 1 hour of inactivity
        
        // Notification actions
        const val ACTION_DISMISS = "com.app.fitness.ACTION_DISMISS"
        const val ACTION_OPEN_APP = "com.app.fitness.ACTION_OPEN_APP"
    }

    override fun onCreate() {
        super.onCreate()
        
        // Initialize repository (should be injected in production)
        walkingRepository = WalkingRepository(
            (application as com.app.fitness.FitnessApplication)
                .getDatabase()
                .walkingDao()
        )
        
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()
        
        // Start periodic checks
        startPeriodicChecks()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_DISMISS -> {
                cancelNotification()
            }
            ACTION_OPEN_APP -> {
                openApp()
                cancelNotification()
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = getString(R.string.notification_channel_description)
                enableLights(true)
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun startPeriodicChecks() {
        serviceScope.launch {
            while (isActive) {
                checkActivityAndNotify()
                delay(REMINDER_INTERVAL)
            }
        }
    }

    private suspend fun checkActivityAndNotify() {
        try {
            val todayRecord = walkingRepository.getTodayWalkingRecord()
            val currentTime = System.currentTimeMillis()
            
            if (todayRecord != null) {
                // Check if daily goal is achieved
                if (todayRecord.isGoalAchieved()) {
                    showGoalAchievedNotification()
                }
                // Check for inactivity
                else if (isInactive(currentTime)) {
                    showInactivityReminder()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking activity status", e)
        }
    }

    private fun isInactive(currentTime: Long): Boolean {
        // Check if current time is between 8 AM and 8 PM
        val calendar = Calendar.getInstance()
        val hourOfDay = calendar.get(Calendar.HOUR_OF_DAY)
        return hourOfDay in 8..20
    }

    private fun showGoalAchievedNotification() {
        val notification = createNotificationBuilder()
            .setContentTitle(getString(R.string.notification_goal_achieved_title))
            .setContentText(getString(R.string.notification_goal_achieved_message))
            .setAutoCancel(true)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun showInactivityReminder() {
        val notification = createNotificationBuilder()
            .setContentTitle(getString(R.string.notification_reminder_title))
            .setContentText(getString(R.string.notification_reminder_message))
            .setAutoCancel(true)
            .addAction(createDismissAction())
            .addAction(createOpenAppAction())
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun createNotificationBuilder(): NotificationCompat.Builder {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
    }

    private fun createDismissAction(): NotificationCompat.Action {
        val intent = Intent(this, ReminderService::class.java).apply {
            action = ACTION_DISMISS
        }
        
        val pendingIntent = PendingIntent.getService(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Action.Builder(
            0,
            getString(R.string.notification_action_dismiss),
            pendingIntent
        ).build()
    }

    private fun createOpenAppAction(): NotificationCompat.Action {
        val intent = Intent(this, ReminderService::class.java).apply {
            action = ACTION_OPEN_APP
        }
        
        val pendingIntent = PendingIntent.getService(
            this,
            1,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Action.Builder(
            0,
            getString(R.string.notification_action_open),
            pendingIntent
        ).build()
    }

    private fun cancelNotification() {
        notificationManager.cancel(NOTIFICATION_ID)
    }

    private fun openApp() {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
    }
}