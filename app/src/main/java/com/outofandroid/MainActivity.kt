package com.outofandroid

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.telephony.SmsManager
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textfield.TextInputEditText
import android.widget.LinearLayout
import android.widget.TextView
import com.outofandroid.util.PreferenceManager
import com.outofandroid.service.StatusNotificationService
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class MainActivity : AppCompatActivity() {
    
    private lateinit var preferenceManager: PreferenceManager
    private lateinit var enableSwitch: SwitchMaterial
    private lateinit var messageEditText: TextInputEditText
    private lateinit var statusText: TextView
    private lateinit var scheduleLayout: LinearLayout
    private lateinit var scheduleText: TextView
    private lateinit var scheduleStatus: TextView
    private lateinit var permissionsCard: MaterialCardView
    private lateinit var saveMessageButton: MaterialButton
    private lateinit var requestPermissionsButton: MaterialButton
    private lateinit var testButton: MaterialButton
    
    private val requiredPermissions = arrayOf(
        Manifest.permission.READ_PHONE_STATE,
        Manifest.permission.ANSWER_PHONE_CALLS,
        Manifest.permission.SEND_SMS,
        Manifest.permission.READ_CONTACTS
    )
    
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            updatePermissionsUI()
            Toast.makeText(this, "All permissions granted!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Some permissions were denied. App may not work properly.", Toast.LENGTH_LONG).show()
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        preferenceManager = PreferenceManager(this)
        initializeViews()
        setupListeners()
        updateUI()
    }
    
    override fun onResume() {
        super.onResume()
        updateScheduleDisplay()
        updateStatusNotification()
    }
    
    private fun initializeViews() {
        enableSwitch = findViewById(R.id.enableSwitch)
        messageEditText = findViewById(R.id.messageEditText)
        statusText = findViewById(R.id.statusText)
        scheduleLayout = findViewById(R.id.scheduleLayout)
        scheduleText = findViewById(R.id.scheduleText)
        scheduleStatus = findViewById(R.id.scheduleStatus)
        permissionsCard = findViewById(R.id.permissionsCard)
        saveMessageButton = findViewById(R.id.saveMessageButton)
        requestPermissionsButton = findViewById(R.id.requestPermissionsButton)
        testButton = findViewById(R.id.testButton)
    }
    
    private fun setupListeners() {
        enableSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked && !hasAllPermissions()) {
                enableSwitch.isChecked = false
                requestPermissions()
                return@setOnCheckedChangeListener
            }
            
            preferenceManager.setAutoResponseEnabled(isChecked)
            updateStatusText()
            updateStatusNotification()
            
            val message = if (isChecked) "Auto responder enabled" else "Auto responder disabled"
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }
        
        saveMessageButton.setOnClickListener {
            val message = messageEditText.text.toString().trim()
            if (message.isNotEmpty()) {
                preferenceManager.setAutoResponseMessage(message)
                Toast.makeText(this, "Message saved!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Please enter a message", Toast.LENGTH_SHORT).show()
            }
        }
        
        requestPermissionsButton.setOnClickListener {
            requestPermissions()
        }
        
        testButton.setOnClickListener {
            testSMSFunctionality()
        }

        val settingsButton = findViewById<MaterialButton>(R.id.settingsButton)
        settingsButton.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }
    }
    
    private fun updateUI() {
        enableSwitch.isChecked = preferenceManager.isAutoResponseEnabled()
        messageEditText.setText(preferenceManager.getAutoResponseMessage())
        updateStatusText()
        updateScheduleDisplay()
        updatePermissionsUI()
    }
    
    private fun updateStatusText() {
        val isEnabled = preferenceManager.isAutoResponseEnabled()
        statusText.text = if (isEnabled) getString(R.string.active) else getString(R.string.inactive)
        statusText.setTextColor(
            ContextCompat.getColor(
                this,
                if (isEnabled) R.color.teal_700 else android.R.color.darker_gray
            )
        )
    }
    
    private fun updateScheduleDisplay() {
        val isScheduledEnabled = preferenceManager.isScheduledModeEnabled()
        
        if (isScheduledEnabled) {
            scheduleLayout.visibility = android.view.View.VISIBLE
            
            val startTime = preferenceManager.getScheduleStartTime()
            val endTime = preferenceManager.getScheduleEndTime()
            val isInSchedule = preferenceManager.isCurrentTimeInSchedule()
            
            scheduleText.text = "Schedule: $startTime - $endTime"
            
            if (isInSchedule) {
                scheduleStatus.text = "Active"
                scheduleStatus.setTextColor(ContextCompat.getColor(this, R.color.white))
                scheduleStatus.background = ContextCompat.getDrawable(this, R.drawable.schedule_status_active)
            } else {
                scheduleStatus.text = "Inactive"
                scheduleStatus.setTextColor(ContextCompat.getColor(this, R.color.white))
                scheduleStatus.background = ContextCompat.getDrawable(this, R.drawable.schedule_status_inactive)
            }
        } else {
            scheduleLayout.visibility = android.view.View.GONE
        }
    }
    
    private fun updateStatusNotification() {
        val isEnabled = preferenceManager.isAutoResponseEnabled()
        
        if (isEnabled) {
            StatusNotificationService.startService(this)
        } else {
            StatusNotificationService.stopService(this)
        }
    }
    
    private fun updatePermissionsUI() {
        val hasPermissions = hasAllPermissions()
        permissionsCard.visibility = if (hasPermissions) 
            android.view.View.GONE else android.view.View.VISIBLE
    }
    
    private fun hasAllPermissions(): Boolean {
        return requiredPermissions.all { permission ->
            ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
        }
    }
    
    private fun requestPermissions() {
        requestPermissionLauncher.launch(requiredPermissions)
    }
    
    private fun testSMSFunctionality() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) 
            != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "SMS permission not granted", Toast.LENGTH_SHORT).show()
            return
        }
        
        try {
            // This is just a test - in reality you'd send to the actual caller's number
            val testMessage = preferenceManager.getAutoResponseMessage()
            Toast.makeText(this, "Test message ready: $testMessage", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Error testing SMS: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}