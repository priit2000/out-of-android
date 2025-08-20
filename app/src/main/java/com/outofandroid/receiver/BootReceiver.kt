package com.outofandroid.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.outofandroid.util.PreferenceManager

class BootReceiver : BroadcastReceiver() {
    
    companion object {
        private const val TAG = "BootReceiver"
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d(TAG, "Device boot completed")
            
            val prefs = PreferenceManager(context)
            if (prefs.isAutoResponseEnabled()) {
                Log.d(TAG, "Auto response was enabled, service will be ready for calls")
                // The service will be started automatically when calls are received
                // No need to start it immediately
            }
        }
    }
}