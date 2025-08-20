package com.outofandroid.util

import android.content.Context
import android.content.SharedPreferences

class PreferenceManager(context: Context) {
    
    companion object {
        private const val PREF_NAME = "out_of_android_prefs"
        private const val KEY_AUTO_RESPONSE_ENABLED = "auto_response_enabled"
        private const val KEY_AUTO_RESPONSE_MESSAGE = "auto_response_message"
        private const val KEY_WHITELIST_ENABLED = "whitelist_enabled"
        private const val KEY_WHITELIST_CONTACTS = "whitelist_contacts"
        private const val KEY_SCHEDULED_ENABLED = "scheduled_enabled"
        private const val KEY_SCHEDULE_START_TIME = "schedule_start_time"
        private const val KEY_SCHEDULE_END_TIME = "schedule_end_time"
        private const val DEFAULT_MESSAGE = "Sorry, I'm currently unavailable. I'll get back to you soon!"
    }
    
    private val sharedPreferences: SharedPreferences = 
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    
    fun isAutoResponseEnabled(): Boolean {
        return sharedPreferences.getBoolean(KEY_AUTO_RESPONSE_ENABLED, false)
    }
    
    fun setAutoResponseEnabled(enabled: Boolean) {
        sharedPreferences.edit()
            .putBoolean(KEY_AUTO_RESPONSE_ENABLED, enabled)
            .apply()
    }
    
    fun getAutoResponseMessage(): String {
        return sharedPreferences.getString(KEY_AUTO_RESPONSE_MESSAGE, DEFAULT_MESSAGE) ?: DEFAULT_MESSAGE
    }
    
    fun setAutoResponseMessage(message: String) {
        sharedPreferences.edit()
            .putString(KEY_AUTO_RESPONSE_MESSAGE, message)
            .apply()
    }
    
    fun isWhitelistEnabled(): Boolean {
        return sharedPreferences.getBoolean(KEY_WHITELIST_ENABLED, false)
    }
    
    fun setWhitelistEnabled(enabled: Boolean) {
        sharedPreferences.edit()
            .putBoolean(KEY_WHITELIST_ENABLED, enabled)
            .apply()
    }
    
    fun getWhitelistContacts(): Set<String> {
        return sharedPreferences.getStringSet(KEY_WHITELIST_CONTACTS, emptySet()) ?: emptySet()
    }
    
    fun setWhitelistContacts(contacts: Set<String>) {
        sharedPreferences.edit()
            .putStringSet(KEY_WHITELIST_CONTACTS, contacts)
            .apply()
    }
    
    fun addToWhitelist(phoneNumber: String) {
        val currentContacts = getWhitelistContacts().toMutableSet()
        currentContacts.add(phoneNumber)
        setWhitelistContacts(currentContacts)
    }
    
    fun removeFromWhitelist(phoneNumber: String) {
        val currentContacts = getWhitelistContacts().toMutableSet()
        currentContacts.remove(phoneNumber)
        setWhitelistContacts(currentContacts)
    }
    
    fun isNumberWhitelisted(phoneNumber: String): Boolean {
        return if (isWhitelistEnabled()) {
            getWhitelistContacts().contains(phoneNumber)
        } else {
            false
        }
    }
    
    // Scheduling functions
    fun isScheduledModeEnabled(): Boolean {
        return sharedPreferences.getBoolean(KEY_SCHEDULED_ENABLED, false)
    }
    
    fun setScheduledModeEnabled(enabled: Boolean) {
        sharedPreferences.edit()
            .putBoolean(KEY_SCHEDULED_ENABLED, enabled)
            .apply()
    }
    
    fun getScheduleStartTime(): String {
        return sharedPreferences.getString(KEY_SCHEDULE_START_TIME, "09:00") ?: "09:00"
    }
    
    fun setScheduleStartTime(time: String) {
        sharedPreferences.edit()
            .putString(KEY_SCHEDULE_START_TIME, time)
            .apply()
    }
    
    fun getScheduleEndTime(): String {
        return sharedPreferences.getString(KEY_SCHEDULE_END_TIME, "17:00") ?: "17:00"
    }
    
    fun setScheduleEndTime(time: String) {
        sharedPreferences.edit()
            .putString(KEY_SCHEDULE_END_TIME, time)
            .apply()
    }
    
    fun isCurrentTimeInSchedule(): Boolean {
        if (!isScheduledModeEnabled()) return false
        
        val currentCalendar = java.util.Calendar.getInstance()
        val currentHour = currentCalendar.get(java.util.Calendar.HOUR_OF_DAY)
        val currentMinute = currentCalendar.get(java.util.Calendar.MINUTE)
        val currentTimeInMinutes = currentHour * 60 + currentMinute
        
        val startTime = getScheduleStartTime()
        val endTime = getScheduleEndTime()
        
        val startParts = startTime.split(":")
        val startTimeInMinutes = startParts[0].toInt() * 60 + startParts[1].toInt()
        
        val endParts = endTime.split(":")
        val endTimeInMinutes = endParts[0].toInt() * 60 + endParts[1].toInt()
        
        val isInSchedule = if (startTimeInMinutes <= endTimeInMinutes) {
            // Same day schedule (e.g., 09:00 - 17:00)
            currentTimeInMinutes >= startTimeInMinutes && currentTimeInMinutes <= endTimeInMinutes
        } else {
            // Cross-midnight schedule (e.g., 22:00 - 07:00)
            currentTimeInMinutes >= startTimeInMinutes || currentTimeInMinutes <= endTimeInMinutes
        }
        
        android.util.Log.d("PreferenceManager", 
            "Schedule check - Current: ${String.format("%02d:%02d", currentHour, currentMinute)}, " +
            "Range: $startTime-$endTime, InSchedule: $isInSchedule")
        
        return isInSchedule
    }
}