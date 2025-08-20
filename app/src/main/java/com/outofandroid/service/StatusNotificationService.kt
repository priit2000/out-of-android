package com.outofandroid.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.outofandroid.MainActivity
import com.outofandroid.R
import com.outofandroid.util.PreferenceManager

class StatusNotificationService : Service() {
    
    companion object {
        private const val NOTIFICATION_ID = 2001
        private const val CHANNEL_ID = "ooa_status_channel"
        
        fun startService(context: Context) {
            val intent = Intent(context, StatusNotificationService::class.java)
            context.startForegroundService(intent)
        }
        
        fun stopService(context: Context) {
            val intent = Intent(context, StatusNotificationService::class.java)
            context.stopService(intent)
        }
    }
    
    private lateinit var notificationManager: NotificationManager
    private lateinit var preferenceManager: PreferenceManager
    
    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        preferenceManager = PreferenceManager(this)
        createNotificationChannel()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, createStatusNotification())
        return START_STICKY
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "OOA Status Indicator",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Shows when Out of Android auto-responder is active"
            setShowBadge(false)
            enableLights(false)
            enableVibration(false)
            setSound(null, null)
        }
        
        notificationManager.createNotificationChannel(channel)
    }
    
    private fun createStatusNotification(): Notification {
        val tapIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        
        val pendingIntent = PendingIntent.getActivity(
            this, 0, tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val isScheduled = preferenceManager.isScheduledModeEnabled()
        val isInSchedule = preferenceManager.isCurrentTimeInSchedule()
        
        val title = "🔴 OOA"
        val text = when {
            !isScheduled -> "Auto-responder active"
            isInSchedule -> "Auto-responder active (${preferenceManager.getScheduleStartTime()}-${preferenceManager.getScheduleEndTime()})"
            else -> "Auto-responder scheduled (${preferenceManager.getScheduleStartTime()}-${preferenceManager.getScheduleEndTime()})"
        }
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_ooa)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setAutoCancel(false)
            .setShowWhen(false)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setColor(0xFFE53E3E.toInt()) // Red color
            .build()
    }
    
    fun updateNotification() {
        if (::notificationManager.isInitialized) {
            notificationManager.notify(NOTIFICATION_ID, createStatusNotification())
        }
    }
}