#!/bin/bash

# Script to push portfolio CSV files to the device for importing
# Usage: ./push_portfolio.sh [filename]

DEFAULT_FILE="2026-05-11_snapshot.json"
FILE="${1:-$DEFAULT_FILE}"
DEST="/sdcard/Download/"

if [ ! -f "$FILE" ]; then
    echo "❌ Error: File '$FILE' not found in the current directory."
    echo "Available snapshot files:"
    ls *.{csv,json} 2>/dev/null
    exit 1
fi

echo "🔄 Preparing to push '$FILE' to device..."

# Push to device
adb push "$FILE" "$DEST"

if [ $? -eq 0 ]; then
    echo "✅ Successfully pushed '$FILE' to '$DEST'"
    echo "📱 You can now import this file from the app's Portfolio screen."
else
    echo "❌ Failed to push file. Is your device connected via ADB?"
    exit 1
fi
