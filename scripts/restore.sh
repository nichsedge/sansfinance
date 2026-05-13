#!/bin/bash
cd "$(dirname "$0")/.." || exit
# 📤 RESTORE: PC -> APP
PACKAGE_NAME="com.sans.finance"
SNAPSHOT_NAME="sans_finance_db_snapshot.sqlite"
DB_NAME="sans_finance_db"

if [ ! -f "$SNAPSHOT_NAME" ]; then
    echo "❌ Error: $SNAPSHOT_NAME not found in current directory!"
    exit 1
fi

echo "🛑 Stopping application..."
adb shell am force-stop $PACKAGE_NAME

echo "📤 Pushing snapshot to staging..."
adb push $SNAPSHOT_NAME /data/local/tmp/$SNAPSHOT_NAME
adb shell "chmod 666 /data/local/tmp/$SNAPSHOT_NAME"

echo "📂 Injecting snapshot into application storage..."
adb shell "run-as $PACKAGE_NAME sh -c 'cat /data/local/tmp/$SNAPSHOT_NAME > /data/data/$PACKAGE_NAME/databases/$DB_NAME'"

echo "🧹 Cleaning up intermediate database files..."
adb shell "run-as $PACKAGE_NAME rm /data/data/$PACKAGE_NAME/databases/$DB_NAME-wal /data/data/$PACKAGE_NAME/databases/$DB_NAME-shm 2>/dev/null"
adb shell "rm /data/local/tmp/$SNAPSHOT_NAME"

echo "🚀 Restarting application..."
adb shell am start -n $PACKAGE_NAME/.MainActivity

echo "✅ Restore complete! App is now running with your snapshot data."
