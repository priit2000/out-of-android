package com.outofandroid

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import androidx.preference.Preference
import android.app.TimePickerDialog
import android.content.SharedPreferences
import androidx.preference.PreferenceManager as AndroidPreferenceManager
import com.outofandroid.util.PreferenceManager
import java.util.Calendar

class SettingsActivity : AppCompatActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        
        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.settings, SettingsFragment())
                .commit()
        }
        
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Settings"
    }
    
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}

class SettingsFragment : PreferenceFragmentCompat(), SharedPreferences.OnSharedPreferenceChangeListener {
    
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var preferenceManager: PreferenceManager
    
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)
        
        sharedPreferences = AndroidPreferenceManager.getDefaultSharedPreferences(requireContext())
        preferenceManager = PreferenceManager(requireContext())
        
        setupTimePreferences()
        setupPresetPreferences()
        updatePreferenceSummaries()
    }
    
    private fun setupTimePreferences() {
        val startTimePref = findPreference<Preference>("schedule_start_time")
        val endTimePref = findPreference<Preference>("schedule_end_time")
        
        startTimePref?.setOnPreferenceClickListener {
            showTimePickerDialog(true)
            true
        }
        
        endTimePref?.setOnPreferenceClickListener {
            showTimePickerDialog(false)
            true
        }
    }
    
    private fun showTimePickerDialog(isStartTime: Boolean) {
        val calendar = Calendar.getInstance()
        val currentTime = if (isStartTime) {
            sharedPreferences.getString("schedule_start_time", "09:00") ?: "09:00"
        } else {
            sharedPreferences.getString("schedule_end_time", "17:00") ?: "17:00"
        }
        
        val timeParts = currentTime.split(":")
        val hour = timeParts[0].toInt()
        val minute = timeParts[1].toInt()
        
        val timePickerDialog = TimePickerDialog(
            requireContext(),
            { _, selectedHour, selectedMinute ->
                val formattedTime = String.format("%02d:%02d", selectedHour, selectedMinute)
                
                // Update both Android preferences and our custom PreferenceManager
                if (isStartTime) {
                    sharedPreferences.edit().putString("schedule_start_time", formattedTime).apply()
                    preferenceManager.setScheduleStartTime(formattedTime)
                } else {
                    sharedPreferences.edit().putString("schedule_end_time", formattedTime).apply()
                    preferenceManager.setScheduleEndTime(formattedTime)
                }
                    
                updatePreferenceSummaries()
            },
            hour,
            minute,
            true // 24-hour format
        )
        
        timePickerDialog.setTitle(if (isStartTime) "Select Start Time" else "Select End Time")
        timePickerDialog.show()
    }
    
    private fun setupPresetPreferences() {
        val workHoursPref = findPreference<Preference>("preset_work_hours")
        val sleepModePref = findPreference<Preference>("preset_sleep_mode") 
        val meetingModePref = findPreference<Preference>("preset_meeting_mode")
        
        workHoursPref?.setOnPreferenceClickListener {
            applyPreset("09:00", "17:00", "Work Hours Active")
            true
        }
        
        sleepModePref?.setOnPreferenceClickListener {
            applyPreset("22:00", "07:00", "Sleep Mode Active")
            true
        }
        
        meetingModePref?.setOnPreferenceClickListener {
            val calendar = Calendar.getInstance()
            val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
            val currentMinute = calendar.get(Calendar.MINUTE)
            
            calendar.add(Calendar.HOUR_OF_DAY, 2)
            val endHour = calendar.get(Calendar.HOUR_OF_DAY)
            val endMinute = calendar.get(Calendar.MINUTE)
            
            val startTime = String.format("%02d:%02d", currentHour, currentMinute)
            val endTime = String.format("%02d:%02d", endHour, endMinute)
            
            applyPreset(startTime, endTime, "Meeting Mode Active")
            true
        }
    }
    
    private fun applyPreset(startTime: String, endTime: String, toastMessage: String) {
        // Update both Android preferences and our custom PreferenceManager
        sharedPreferences.edit()
            .putString("schedule_start_time", startTime)
            .putString("schedule_end_time", endTime)
            .putBoolean("scheduled_mode_enabled", true)
            .apply()
            
        preferenceManager.setScheduleStartTime(startTime)
        preferenceManager.setScheduleEndTime(endTime)
        preferenceManager.setScheduledModeEnabled(true)
        
        updatePreferenceSummaries()
        android.widget.Toast.makeText(requireContext(), toastMessage, android.widget.Toast.LENGTH_SHORT).show()
    }

    private fun updatePreferenceSummaries() {
        val scheduleEnabled = preferenceManager.isScheduledModeEnabled()
        val startTime = preferenceManager.getScheduleStartTime()
        val endTime = preferenceManager.getScheduleEndTime()
        
        val startTimePref = findPreference<Preference>("schedule_start_time")
        val endTimePref = findPreference<Preference>("schedule_end_time")
        
        startTimePref?.summary = if (scheduleEnabled) {
            "Auto-response starts at $startTime"
        } else {
            "Set start time (currently disabled)"
        }
        
        endTimePref?.summary = if (scheduleEnabled) {
            "Auto-response ends at $endTime" 
        } else {
            "Set end time (currently disabled)"
        }
        
        // Enable/disable time preferences based on schedule switch
        startTimePref?.isEnabled = scheduleEnabled
        endTimePref?.isEnabled = scheduleEnabled
    }
    
    override fun onResume() {
        super.onResume()
        preferenceScreen.sharedPreferences?.registerOnSharedPreferenceChangeListener(this)
    }
    
    override fun onPause() {
        super.onPause()
        preferenceScreen.sharedPreferences?.unregisterOnSharedPreferenceChangeListener(this)
    }
    
    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        when (key) {
            "scheduled_mode_enabled" -> {
                val enabled = sharedPreferences?.getBoolean(key, false) ?: false
                preferenceManager.setScheduledModeEnabled(enabled)
                updatePreferenceSummaries()
            }
            "schedule_start_time", "schedule_end_time" -> {
                updatePreferenceSummaries()
            }
        }
    }
}