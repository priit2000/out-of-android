# Out of Android

Updated: 2026-11-26

## Read CLAUDE.md before every response

Before answering any question, read both:
- Global: C:\Users\priit\.claude\CLAUDE.md
- Local: C:\Users\priit\OneDrive\projektid\amperly\ai-tools-coding-chatgpt\out-of-android\CLAUDE.md

## What the app does

Android app (package `com.outofandroid`) that rejects incoming calls and texts the caller an auto-reply SMS. It has a whitelist and a schedule.

## How to work here

Priit is the sole developer. Commit and push straight to `main`. No branches, no pull requests.

## Current state

The fix for "SMS not sent to rejected callers" is on main (commits `726e694` and `7561359`).

- On Android 10+, `CallScreeningServiceImpl` handles calls when the app holds `ROLE_CALL_SCREENING`. `CallStateReceiver` returns early in that case.
- `CallScreeningServiceImpl` sends the SMS before `respondToCall()`.
- `CallScreeningServiceImpl` lets outgoing calls through, so they never trigger an SMS.
- `CallManagementService` checks `SEND_SMS` before sending.

CI is green. Release v1.0.31 is published on GitHub and signed with the release key.

## Open task: on-device test

The fix is not yet verified on a phone. v1.0.31 is installed on Priit's Samsung S26 Ultra (Android 17, SDK 37), with all permissions and the call screening role set over adb.

To test:
1. In the app, switch Auto responder on. It is off after a fresh install.
2. Call the phone from another number.
3. Watch `adb logcat | grep -E "CallScreeningService|CallStateReceiver|CallManagementService"` for "SMS sent successfully".
4. Confirm the caller received the SMS.

About "unanswered" calls: while the app holds the screening role and auto-reply is on, every non-whitelisted call is rejected at once. Calls that ring through (whitelisted, or outside the schedule) get no SMS by design. An SMS for missed calls the app let through would need new code.

## Signing

Debug and release builds share one key.

- Locally: `keystore.properties` and `out-of-android-release.keystore` in the project root. Both are gitignored, so a fresh clone has neither. Without them, release falls back to the debug key.
- CI: repository secrets `SIGNING_KEYSTORE_BASE64`, `SIGNING_STORE_PASSWORD`, `SIGNING_KEY_ALIAS`, `SIGNING_KEY_PASSWORD`.
- Release certificate SHA-256: `a8bf0afe266bfcdd8ff714b3422962b07fc8ca87f95573c10c5d61350735c05d`.

An APK signed with another key will not install over this one ("App not installed"). Uninstall first; that deletes the app's settings.

## Versioning

`versionCode` counts 10-minute steps since 2025-01-01, so every build is higher than the last. `versionName` is `1.0.<GITHUB_RUN_NUMBER>` in CI and `1.0-local` locally. Set `GITHUB_RUN_NUMBER` locally to pick a name.

## Building locally

OneDrive locks files in `app/build`, which breaks Gradle. Build with `--no-daemon` and send the build output outside OneDrive with an init script:

```groovy
// outbuild.gradle (keep it outside the repo)
allprojects {
    layout.buildDirectory.set(new File("C:/Users/priit/ooa-build/${project.name}"))
}
```

```powershell
.\gradlew --init-script C:\path\to\outbuild.gradle test assembleRelease --no-daemon
```

The APK lands in `C:\Users\priit\ooa-build\app\outputs\apk\release\`. The project has no unit tests yet.

## Installing with adb

adb is at `%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe`. The phone needs USB debugging on and USB mode "Transferring files".

```powershell
adb install -r app-release.apk
adb shell pm grant com.outofandroid android.permission.SEND_SMS
adb shell pm grant com.outofandroid android.permission.READ_PHONE_STATE
adb shell pm grant com.outofandroid android.permission.READ_CALL_LOG
adb shell pm grant com.outofandroid android.permission.ANSWER_PHONE_CALLS
adb shell pm grant com.outofandroid android.permission.READ_CONTACTS
adb shell pm grant com.outofandroid android.permission.POST_NOTIFICATIONS
adb shell cmd role add-role-holder android.app.role.CALL_SCREENING com.outofandroid 0
```

`pm grant` gets past Android's "restricted settings" block on SMS permissions for sideloaded apps.
