# Out of Android - Auto Call Responder

An Android application that automatically rejects incoming calls and sends customized SMS responses, similar to an "out of office" message for phone calls.

## Features

- **Automatic Call Rejection**: Automatically rejects incoming calls when enabled
- **Custom SMS Responses**: Sends personalized auto-response messages to missed callers
- **Contact Whitelist**: Allow specific contacts to bypass auto-response
- **Scheduled Operation**: Set specific time ranges for auto-response to be active
- **Simple UI**: Easy-to-use interface for managing settings and messages

## How It Works

1. When a call comes in, the app detects it through the `CallStateReceiver`
2. If auto-response is enabled and conditions are met, it:
   - Automatically rejects the call using `TelecomManager`
   - Sends a predefined SMS message to the caller
3. The app respects whitelist and scheduling settings

## Required Permissions

The app requires the following permissions to function:

- `READ_PHONE_STATE` - To detect incoming calls
- `ANSWER_PHONE_CALLS` - To automatically reject calls (Android 9+)
- `SEND_SMS` - To send auto-response messages
- `READ_CONTACTS` - To access caller information
- `RECEIVE_BOOT_COMPLETED` - To maintain settings after device restart
- `FOREGROUND_SERVICE` - To run the call management service

## Setup Instructions

1. **Build the app**: Open in Android Studio and build the project
2. **Install on device**: Install the APK on an Android device (minimum API 26)
3. **Grant permissions**: Open the app and grant all required permissions
4. **Configure settings**:
   - Enable auto-response toggle
   - Set your custom message
   - Configure scheduling if needed
   - Add contacts to whitelist if desired

## Important Notes

- **Testing**: The call rejection feature only works on a real device, not in the emulator
- **Android Version**: Requires Android 8.0 (API 26) or higher for call management features
- **Permissions**: The app will not function without the required permissions
- **Battery Optimization**: You may need to disable battery optimization for this app to ensure it works reliably

## Usage Scenarios

- **Meetings & Presentations**: Enable during important meetings
- **Focus Time**: Set scheduled hours for uninterrupted work
- **Vacation**: Use as an "out of office" for phone calls
- **Driving**: Automatically respond when unable to answer safely

## Technical Details

- **Architecture**: Uses Android Services and BroadcastReceivers
- **Call Detection**: Monitors `TelephonyManager.ACTION_PHONE_STATE_CHANGED`
- **Call Management**: Uses `TelecomManager.endCall()` for call rejection
- **SMS Sending**: Utilizes `SmsManager` for automatic responses
- **Data Storage**: SharedPreferences for settings persistence

## Troubleshooting

If the app doesn't work as expected:

1. Ensure all permissions are granted
2. Check if battery optimization is disabled for the app
3. Verify that the app is set as the default phone app (if required by your Android version)
4. Check device logs for any error messages

## Build Requirements

- Android Studio Arctic Fox or later
- Android SDK API 34
- Kotlin 1.8.20
- Gradle 8.1.0

## Disclaimer

This app is for educational and personal use. Please ensure compliance with local laws and regulations regarding call handling and SMS messaging. The app should not be used to ignore emergency calls or important communications.