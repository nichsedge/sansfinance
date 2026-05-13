#!/bin/bash
set -e
cd "$(dirname "$0")/.."

PACKAGE="com.sans.finance"
APK_PATH="app/build/outputs/apk/debug/app-debug.apk"
STATE_DIR=".gradle/run_on_device_state"

mkdir -p "$STATE_DIR"

if ! command -v adb >/dev/null 2>&1; then
    echo "❌ adb not found. Make sure Android platform-tools are installed."
    exit 1
fi

DEVICE_SERIAL="$(adb get-serialno 2>/dev/null || true)"
if [ -z "$DEVICE_SERIAL" ] || [ "$DEVICE_SERIAL" = "unknown" ]; then
    echo "❌ No Android device detected."
    exit 1
fi

STATE_FILE="$STATE_DIR/$DEVICE_SERIAL.sha256"

echo "🚀 Building latest debug APK..."

# Build only. Gradle incremental build keeps this fast when nothing changed.
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
if ! adb shell pm path "$PACKAGE" >/dev/null 2>&1; then
    APP_INSTALLED="no"
fi

if [ "$APK_HASH" != "$PREV_HASH" ] || [ "$APP_INSTALLED" = "no" ]; then
    echo "📦 Installing updated APK..."
    adb install -r -t "$APK_PATH" >/dev/null
    echo "$APK_HASH" > "$STATE_FILE"
else
    echo "⚡ No APK changes detected; skipping install."
fi

echo "🏁 Starting $PACKAGE..."

# -S force-stops the app before starting to ensure the new version is loaded
adb shell am start -S -n "$PACKAGE/.MainActivity" -a android.intent.action.MAIN -c android.intent.category.LAUNCHER > /dev/null

echo "✅ App is running!"
