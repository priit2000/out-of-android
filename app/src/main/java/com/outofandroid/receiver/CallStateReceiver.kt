package com.outofandroid.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager
import android.util.Log
import com.outofandroid.service.CallManagementService
import com.outofandroid.util.PreferenceManager

class CallStateReceiver : BroadcastReceiver() {
    
    companion object {
        private const val TAG = "CallStateReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == TelephonyManager.ACTION_PHONE_STATE_CHANGED) {
            val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
            val phoneNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)
            
            Log.d(TAG, "Phone state changed: $state, Number: $phoneNumber")
            
            when (state) {
                TelephonyManager.EXTRA_STATE_RINGING -> {
                    handleIncomingCall(context, phoneNumber)
                }
                TelephonyManager.EXTRA_STATE_IDLE -> {
                    Log.d(TAG, "Call ended or no call active")
                }
                TelephonyManager.EXTRA_STATE_OFFHOOK -> {
                    Log.d(TAG, "Call answered or outgoing call")
                }
            }
        }
    }
    
    private fun handleIncomingCall(context: Context, phoneNumber: String?) {
        val prefs = PreferenceManager(context)
        
        if (!prefs.isAutoResponseEnabled()) {
            Log.d(TAG, "Auto response is disabled")
            return
        }
        
        // Check if scheduled mode is enabled and current time is outside schedule
        if (prefs.isScheduledModeEnabled() && !prefs.isCurrentTimeInSchedule()) {
            Log.d(TAG, "Outside scheduled auto-response hours")
            return
        }
        
        if (phoneNumber == null) {
            Log.w(TAG, "Phone number is null")
            return
        }
        
        // Check if number is whitelisted
        if (prefs.isWhitelistEnabled() && prefs.isNumberWhitelisted(phoneNumber)) {
            Log.d(TAG, "Number $phoneNumber is whitelisted, allowing call")
            return
        }
        
        Log.d(TAG, "Handling incoming call from: $phoneNumber")
        
        // Start the call management service to handle the call
        val serviceIntent = Intent(context, CallManagementService::class.java).apply {
            putExtra("phone_number", phoneNumber)
            putExtra("action", "reject_and_respond")
        }
        
        context.startForegroundService(serviceIntent)
    }
}