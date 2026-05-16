#!/bin/bash
set -euo pipefail
cd "$(dirname "$0")/.." || exit

PACKAGE_NAME="com.sans.finance"
DB_NAME="sans_finance_db"
TEMP_DB="sans_finance_db"
TEMP_DB_BEFORE="sans_finance_db_before_backfill"

echo "🚀 Starting Portfolio Backfill Automation..."

# 1. Run on device (Build & Launch)
echo "📱 Ensuring app is installed and running..."
./scripts/run_on_device.sh || exit 1

# 2. Force-stop to flush/close SQLite before pulling DB
echo "🛑 Stopping app before snapshot pull (prevents WAL data loss)..."
adb shell am force-stop "$PACKAGE_NAME"
sleep 1

# 3. Pull Database from device, including WAL/SHM if present.
echo "📥 Pulling database from device..."
rm -f "$TEMP_DB" "$TEMP_DB-wal" "$TEMP_DB-shm" "$TEMP_DB_BEFORE" "$TEMP_DB_BEFORE-wal" "$TEMP_DB_BEFORE-shm"
adb shell "run-as $PACKAGE_NAME cat databases/$DB_NAME" > "$TEMP_DB"
if [ $? -ne 0 ]; then
    echo "❌ Error: Failed to pull database. Is the device connected and app installed?"
    exit 1
fi
if adb shell "run-as $PACKAGE_NAME sh -c 'test -f databases/$DB_NAME-wal'" >/dev/null 2>&1; then
    adb shell "run-as $PACKAGE_NAME cat databases/$DB_NAME-wal" > "$TEMP_DB-wal"
fi
if adb shell "run-as $PACKAGE_NAME sh -c 'test -f databases/$DB_NAME-shm'" >/dev/null 2>&1; then
    adb shell "run-as $PACKAGE_NAME cat databases/$DB_NAME-shm" > "$TEMP_DB-shm"
fi

# Keep a local pre-backfill copy for recovery.
cp "$TEMP_DB" "$TEMP_DB_BEFORE"
[ -f "$TEMP_DB-wal" ] && cp "$TEMP_DB-wal" "$TEMP_DB_BEFORE-wal"
[ -f "$TEMP_DB-shm" ] && cp "$TEMP_DB-shm" "$TEMP_DB_BEFORE-shm"

before_accounts=$(python3 - <<'PY'
import sqlite3
conn = sqlite3.connect("sans_finance_db")
cur = conn.cursor()
cur.execute("SELECT COUNT(*) FROM accounts")
print(cur.fetchone()[0])
conn.close()
PY
)
echo "🔎 Accounts before backfill: $before_accounts"

# 4. Run Python script to backfill data
echo "🐍 Running backfill script..."
python3 ./scripts/backfill_portfolio.py
if [ $? -ne 0 ]; then
    echo "❌ Error: Python backfill script failed."
    rm -f "$TEMP_DB" "$TEMP_DB_BEFORE"
    exit 1
fi

after_accounts=$(python3 - <<'PY'
import sqlite3
conn = sqlite3.connect("sans_finance_db")
cur = conn.cursor()
cur.execute("SELECT COUNT(*) FROM accounts")
print(cur.fetchone()[0])
conn.close()
PY
)
echo "🔎 Accounts after backfill: $after_accounts"

if [ "$after_accounts" -lt "$before_accounts" ]; then
    echo "❌ Safety check failed: account count decreased ($before_accounts -> $after_accounts)."
    echo "🧯 Aborting push. Local pre-backfill DB kept at ./$TEMP_DB_BEFORE"
    rm -f "$TEMP_DB"
    exit 1
fi

python3 - <<'PY'
import sqlite3
conn = sqlite3.connect("sans_finance_db")
conn.execute("PRAGMA wal_checkpoint(TRUNCATE)")
conn.close()
PY
rm -f "$TEMP_DB-wal" "$TEMP_DB-shm"

# 5. Push Database back to device
echo "📤 Pushing updated database to device..."
adb push "$TEMP_DB" /data/local/tmp/"$TEMP_DB"
adb shell "chmod 666 /data/local/tmp/$TEMP_DB"
adb shell "run-as $PACKAGE_NAME sh -c 'rm -f databases/$DB_NAME-wal databases/$DB_NAME-shm && cat /data/local/tmp/$TEMP_DB > databases/$DB_NAME'"

# 6. Clean up intermediate files
echo "🧹 Cleaning up..."
adb shell "run-as $PACKAGE_NAME rm databases/$DB_NAME-wal databases/$DB_NAME-shm 2>/dev/null"
adb shell "rm /data/local/tmp/$TEMP_DB"
rm -f "$TEMP_DB" "$TEMP_DB-wal" "$TEMP_DB-shm"
echo "💾 Pre-backfill recovery copy kept at ./$TEMP_DB_BEFORE"

# 7. Restart App
echo "🚀 Restarting application..."
adb shell am force-stop $PACKAGE_NAME
adb shell am start -n $PACKAGE_NAME/.MainActivity

echo "✅ Portfolio backfill completed successfully!"
