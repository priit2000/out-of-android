package com.outofandroid.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.telecom.TelecomManager
import android.telephony.SmsManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.outofandroid.R
import com.outofandroid.util.PreferenceManager

class CallManagementService : Service() {
    
    companion object {
        private const val TAG = "CallManagementService"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "auto_responder_channel"
    }
    
    private lateinit var preferenceManager: PreferenceManager
    
    override fun onCreate() {
        super.onCreate()
        preferenceManager = PreferenceManager(this)
        createNotificationChannel()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, createNotification())
        
        intent?.let { serviceIntent ->
            val phoneNumber = serviceIntent.getStringExtra("phone_number")
            val action = serviceIntent.getStringExtra("action")
            
            when (action) {
                "reject_and_respond" -> {
                    phoneNumber?.let { number ->
                        rejectCallAndSendMessage(number)
                    }
                }
                else -> {
                    Log.w(TAG, "Unknown action: $action")
                }
            }
        }
        
        return START_NOT_STICKY
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    private fun rejectCallAndSendMessage(phoneNumber: String) {
        Log.d(TAG, "Rejecting call and sending message to: $phoneNumber")
        
        try {
            // Reject the call using TelecomManager
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                val telecomManager = getSystemService(Context.TELECOM_SERVICE) as TelecomManager
                if (checkSelfPermission(android.Manifest.permission.ANSWER_PHONE_CALLS) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    telecomManager.endCall()
                    Log.d(TAG, "Call rejected successfully")
                } else {
                    Log.w(TAG, "Missing ANSWER_PHONE_CALLS permission")
                }
            } else {
                // For older Android versions, we can't programmatically reject calls
                // without using reflection or accessibility services
                Log.w(TAG, "Call rejection not supported on this Android version")
            }
            
            // Send SMS response regardless
            sendAutoResponseMessage(phoneNumber)
            
        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception when trying to reject call", e)
        } catch (e: Exception) {
            Log.e(TAG, "Error rejecting call", e)
        }
        
        // Stop the service
        stopSelf()
    }
    
    private fun sendAutoResponseMessage(phoneNumber: String) {
        try {
            val message = preferenceManager.getAutoResponseMessage()
            Log.d(TAG, "Sending SMS to $phoneNumber: $message")
            
            val smsManager = SmsManager.getDefault()
            smsManager.sendTextMessage(phoneNumber, null, message, null, null)
            
            Log.d(TAG, "SMS sent successfully")
        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception when sending SMS", e)
        } catch (e: Exception) {
            Log.e(TAG, "Error sending SMS", e)
        }
    }
    
    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Auto Responder Service",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Notification for auto responder service"
            setShowBadge(false)
        }
        
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
    }
    
    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.notification_text))
            .setSmallIcon(R.drawable.ic_phone)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }
}