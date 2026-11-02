#!/bin/bash
set -euo pipefail
cd "$(dirname "$0")/.." || exit

PACKAGE_NAME="com.sans.finance"
DB_NAME="sans_finance_db"
TEMP_DB="sans_finance_db"

APPLY_FLAG=""
if [[ " $* " =~ " --apply " ]]; then
    APPLY_FLAG="--apply"
fi

echo "🚀 Starting Portfolio Monthly Pruning Process..."

# Check if ADB device is connected
DEVICE_CONNECTED=false
if adb devices 2>/dev/null | grep -q -v "List" | grep -q "device"; then
    DEVICE_CONNECTED=true
    echo "📱 Connected Android device detected."
else
    echo "ℹ️ No Android device detected. Skipping live app DB pull/push."
fi

if [ "$DEVICE_CONNECTED" = true ]; then
    echo "🛑 Stopping app before DB pull..."
    adb shell am force-stop "$PACKAGE_NAME" || true
    sleep 1

    echo "📥 Pulling database from device..."
    rm -f "$TEMP_DB" "$TEMP_DB-wal" "$TEMP_DB-shm"
    if adb shell "run-as $PACKAGE_NAME cat databases/$DB_NAME" > "$TEMP_DB" 2>/dev/null; then
        echo "✅ Pulled $DB_NAME from device."
        if adb shell "run-as $PACKAGE_NAME sh -c 'test -f databases/$DB_NAME-wal'" >/dev/null 2>&1; then
            adb shell "run-as $PACKAGE_NAME cat databases/$DB_NAME-wal" > "$TEMP_DB-wal"
        fi
        if adb shell "run-as $PACKAGE_NAME sh -c 'test -f databases/$DB_NAME-shm'" >/dev/null 2>&1; then
            adb shell "run-as $PACKAGE_NAME cat databases/$DB_NAME-shm" > "$TEMP_DB-shm"
        fi
    else
        echo "⚠️ Could not pull DB via run-as. Falling back to existing local DB if present."
    fi
fi

PYTHON_BIN="python3"

# Run Python prune script
echo "🐍 Running prune_portfolio.py using $PYTHON_BIN..."
"$PYTHON_BIN" ./scripts/prune_portfolio.py "$@"

if [ "$DEVICE_CONNECTED" = true ] && [ -n "$APPLY_FLAG" ] && [ -f "$TEMP_DB" ]; then
    echo "🧹 Truncating SQLite WAL before pushing DB back..."
    "$PYTHON_BIN" - <<'PY'
import os, sqlite3
if os.path.exists("sans_finance_db"):
    conn = sqlite3.connect("sans_finance_db")
    conn.execute("PRAGMA wal_checkpoint(TRUNCATE)")
    conn.close()
PY
    rm -f "$TEMP_DB-wal" "$TEMP_DB-shm"

    echo "📤 Pushing updated DB back to Android device..."
    adb push "$TEMP_DB" /data/local/tmp/"$TEMP_DB"
    adb shell "chmod 666 /data/local/tmp/$TEMP_DB"
    adb shell "run-as $PACKAGE_NAME sh -c 'rm -f databases/$DB_NAME-wal databases/$DB_NAME-shm && cat /data/local/tmp/$TEMP_DB > databases/$DB_NAME'"
    adb shell "rm /data/local/tmp/$TEMP_DB"

    echo "🚀 Restarting application on device..."
    adb shell am force-stop "$PACKAGE_NAME"
    adb shell am start -n "$PACKAGE_NAME/.MainActivity"
fi

echo "✅ Portfolio pruning workflow complete!"
