#!/usr/bin/env bash
set -euo pipefail

# scripts/restore_from_r2.sh
# Safely pulls database snapshot from Cloudflare R2, verifies SQLite validity (>0 expenses),
# and restores it to the connected Android device via ADB.

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
TEMP_DB="${PROJECT_DIR}/tmp_restore.sqlite"
TARGET_KEY="${1:-db/sans_finance_latest.sqlite}"

cleanup() {
    rm -f "${TEMP_DB}"
}
trap cleanup EXIT

echo "========================================================"
echo "   SANS FINANCE CLOUDFLARE R2 RESTORE UTILITY"
echo "========================================================"
echo "Target R2 Object: ${TARGET_KEY}"

# 1. Pull from R2
python3 "${PROJECT_DIR}/scripts/pull_cloud_db.py" "${TEMP_DB}" --key "${TARGET_KEY}"

# 2. Verify database validity
EXPENSE_COUNT=$(python3 -c "
import sqlite3, sys
try:
    conn = sqlite3.connect('${TEMP_DB}')
    c = conn.cursor()
    c.execute('SELECT count(*) FROM expenses')
    count = c.fetchone()[0]
    conn.close()
    print(count)
except Exception as e:
    sys.stderr.write(f'Error checking sqlite db: {e}\n')
    sys.exit(1)
")

if [ "${EXPENSE_COUNT}" -le 0 ]; then
    echo "❌ ERROR: Downloaded database has ${EXPENSE_COUNT} expenses. Refusing to restore blank database!"
    echo "💡 You can restore from an archive instead. Run: python3 scripts/pull_cloud_db.py --list"
    exit 1
fi

echo "✅ Verified database integrity: ${EXPENSE_COUNT} transactions found."

# 3. Check for connected ADB device
DEVICE=$(adb devices | grep -w "device" | awk '{print $1}' | head -n 1 || true)

if [ -z "${DEVICE}" ]; then
    echo "⚠️ No ADB device connected."
    echo "💾 Verified database kept at: ${PROJECT_DIR}/sans_finance_recovered.sqlite"
    cp "${TEMP_DB}" "${PROJECT_DIR}/sans_finance_recovered.sqlite"
    exit 0
fi

echo "📱 Connected device found: ${DEVICE}"
echo "🚀 Restoring to com.sans.finance on ${DEVICE}..."

adb -s "${DEVICE}" shell "am force-stop com.sans.finance"
adb -s "${DEVICE}" push "${TEMP_DB}" "/data/local/tmp/restore_db.sqlite" > /dev/null
adb -s "${DEVICE}" shell "run-as com.sans.finance cp /data/local/tmp/restore_db.sqlite /data/data/com.sans.finance/databases/sans_finance_db"
adb -s "${DEVICE}" shell "run-as com.sans.finance rm -f /data/data/com.sans.finance/databases/sans_finance_db-wal /data/data/com.sans.finance/databases/sans_finance_db-shm"
adb -s "${DEVICE}" shell "rm -f /data/local/tmp/restore_db.sqlite"

# Restart app
adb -s "${DEVICE}" shell "monkey -p com.sans.finance -c android.intent.category.LAUNCHER 1" > /dev/null 2>&1

echo "🎉 Database restored successfully to ${DEVICE}!"
echo "========================================================"
