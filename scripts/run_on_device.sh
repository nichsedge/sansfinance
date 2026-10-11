#!/bin/bash
set -e
cd "$(dirname "$0")/.."

PACKAGE="com.sans.finance"
APK_PATH="app/build/outputs/apk/debug/app-debug.apk"
STATE_DIR=".gradle/run_on_device_state"
mkdir -p "$STATE_DIR"

source scripts/resolve_adb_device.sh "$1"

# Extract device info
DEVICE_MODEL="$($ADB_CMD shell getprop ro.product.model 2>/dev/null | tr -d '\r\n' || echo "Android Device")"
DEVICE_MANUFACTURER="$($ADB_CMD shell getprop ro.product.manufacturer 2>/dev/null | tr -d '\r\n' || echo "")"
ANDROID_VERSION="$($ADB_CMD shell getprop ro.build.version.release 2>/dev/null | tr -d '\r\n' || echo "")"

echo "📱 Target Device: $DEVICE_MANUFACTURER $DEVICE_MODEL (Android $ANDROID_VERSION, Serial: $DEVICE_SERIAL)"

# Wake screen if asleep
WAKEFULNESS="$($ADB_CMD shell dumpsys power 2>/dev/null | grep -i "mWakefulness=" | head -n 1 | tr -d '\r\n')"
if [[ "$WAKEFULNESS" =~ Asleep|Dozing ]]; then
    echo "💡 Waking device screen..."
    $ADB_CMD shell input keyevent KEYCODE_WAKEUP >/dev/null 2>&1 || true
fi

STATE_FILE="$STATE_DIR/$DEVICE_SERIAL.sha256"

echo "🚀 Building latest debug APK..."
./gradlew :app:assembleDebug \
    -x lint \
    -x test \
    --daemon \
    --build-cache \
    --configuration-cache \
    --quiet

if [ ! -f "$APK_PATH" ]; then
    echo "❌ APK not found at $APK_PATH"
    exit 1
fi

APK_HASH="$(shasum -a 256 "$APK_PATH" | awk '{print $1}')"
PREV_HASH=""
if [ -f "$STATE_FILE" ]; then
    PREV_HASH="$(cat "$STATE_FILE")"
fi

APP_INSTALLED="yes"
if ! $ADB_CMD shell pm path "$PACKAGE" >/dev/null 2>&1; then
    APP_INSTALLED="no"
fi

if [ "$APK_HASH" != "$PREV_HASH" ] || [ "$APP_INSTALLED" = "no" ]; then
    echo "📦 Installing APK onto $DEVICE_MODEL..."
    INSTALL_OUTPUT="$($ADB_CMD install -r -t -d -g "$APK_PATH" 2>&1)"
    echo "$INSTALL_OUTPUT"

    if echo "$INSTALL_OUTPUT" | grep -q "Success"; then
        echo "$APK_HASH" > "$STATE_FILE"
        echo "✅ APK successfully installed."
    elif echo "$INSTALL_OUTPUT" | grep -q "INSTALL_FAILED_USER_RESTRICTED"; then
        echo "⚠️ Xiaomi/HyperOS Security Restriction: Please enable 'Install via USB' in Developer Options or tap 'Install' on your device screen."
        exit 1
    elif ! echo "$INSTALL_OUTPUT" | grep -q "Success"; then
        echo "❌ Installation failed."
        exit 1
    fi
else
    echo "⚡ No APK changes detected; skipping installation."
fi

if [ "${INSTALL_ONLY:-0}" = "1" ] || [ "${NO_LAUNCH:-0}" = "1" ]; then
    echo "✨ Installation complete."
    exit 0
fi

echo "🏁 Starting $PACKAGE on $DEVICE_MODEL..."
$ADB_CMD shell am start -S -n "$PACKAGE/.MainActivity" -a android.intent.action.MAIN -c android.intent.category.LAUNCHER >/dev/null

sleep 1
PID="$($ADB_CMD shell pidof "$PACKAGE" 2>/dev/null | tr -d '\r\n')"
if [ -n "$PID" ]; then
    echo "✨ App is running! (PID: $PID)"
else
    echo "✅ App launch intent sent."
fi
