#!/bin/bash

# Simple idempotent sync script for development
PACKAGE_NAME="com.sans.finance"
SNAPSHOT_NAME="sans_finance_db_snapshot.sqlite"
DB_NAME="sans_finance_db"

echo "🔄 Initializing Snapshot Pull (App -> PC)..."
adb pull /sdcard/Download/$SNAPSHOT_NAME .

echo "🛑 Stopping application..."
adb shell am force-stop $PACKAGE_NAME

echo "📤 Preparing for Restore (PC -> Phone)..."
# Push to temp location first (run-as cannot read /sdcard/ on many Android versions)
adb push $SNAPSHOT_NAME /data/local/tmp/$SNAPSHOT_NAME
adb shell "chmod 666 /data/local/tmp/$SNAPSHOT_NAME"

echo "📂 Overwriting database snapshot..."
# WRAP the entire redirection inside run-as to handle permissions
adb shell "run-as $PACKAGE_NAME sh -c 'cat /data/local/tmp/$SNAPSHOT_NAME > /data/data/$PACKAGE_NAME/databases/$DB_NAME'"

echo "🧹 Cleaning up intermediate database files..."
adb shell "run-as $PACKAGE_NAME rm /data/data/$PACKAGE_NAME/databases/$DB_NAME-wal /data/data/$PACKAGE_NAME/databases/$DB_NAME-shm 2>/dev/null"
adb shell "rm /data/local/tmp/$SNAPSHOT_NAME"

echo "🚀 Restarting application..."
adb shell am start -n $PACKAGE_NAME/.MainActivity

echo "✅ Sync complete!"
