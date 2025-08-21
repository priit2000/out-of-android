package com.outofandroid.service

import android.os.Build
import android.telecom.Call
import android.telecom.CallScreeningService
import android.telephony.SmsManager
import android.util.Log
import androidx.annotation.RequiresApi
import com.outofandroid.util.PreferenceManager

@RequiresApi(Build.VERSION_CODES.N)
class CallScreeningServiceImpl : CallScreeningService() {
    
    companion object {
        private const val TAG = "CallScreeningService"
    }
    
    private lateinit var preferenceManager: PreferenceManager
    
    override fun onCreate() {
        super.onCreate()
        preferenceManager = PreferenceManager(this)
    }
    
    override fun onScreenCall(callDetails: Call.Details) {
        val phoneNumber = callDetails.handle?.schemeSpecificPart
        Log.d(TAG, "Screening call from: $phoneNumber")
        
        if (!preferenceManager.isAutoResponseEnabled()) {
            Log.d(TAG, "Auto response is disabled")
            respondToCall(callDetails, CallResponse.Builder().build())
            return
        }
        
        // Check if scheduled mode is enabled and current time is outside schedule
        if (preferenceManager.isScheduledModeEnabled() && !preferenceManager.isCurrentTimeInSchedule()) {
            Log.d(TAG, "Outside scheduled auto-response hours")
            respondToCall(callDetails, CallResponse.Builder().build())
            return
        }
        
        if (phoneNumber == null) {
            Log.w(TAG, "Phone number is null")
            respondToCall(callDetails, CallResponse.Builder().build())
            return
        }
        
        // Check if number is whitelisted
        if (preferenceManager.isWhitelistEnabled() && preferenceManager.isNumberWhitelisted(phoneNumber)) {
            Log.d(TAG, "Number $phoneNumber is whitelisted, allowing call")
            respondToCall(callDetails, CallResponse.Builder().build())
            return
        }
        
        Log.d(TAG, "Rejecting and responding to call from: $phoneNumber")
        
        // Build response to reject the call
        val response = CallResponse.Builder()
            .setDisallowCall(true)
            .setRejectCall(true)
            .setSkipNotification(false)
            .build()
        
        respondToCall(callDetails, response)
        
        // Send SMS after rejecting
        sendAutoResponseMessage(phoneNumber)
    }
    
    private fun sendAutoResponseMessage(phoneNumber: String) {
        try {
            val message = preferenceManager.getAutoResponseMessage()
            Log.d(TAG, "Sending SMS to $phoneNumber: $message")
            
            val smsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                getSystemService(SmsManager::class.java)
            } else {
                @Suppress("DEPRECATION")
                SmsManager.getDefault()
            }
            
            if (checkSelfPermission(android.Manifest.permission.SEND_SMS) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                smsManager.sendTextMessage(phoneNumber, null, message, null, null)
                Log.d(TAG, "SMS sent successfully")
            } else {
                Log.w(TAG, "Missing SEND_SMS permission")
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception when sending SMS", e)
        } catch (e: Exception) {
            Log.e(TAG, "Error sending SMS", e)
        }
    }
}