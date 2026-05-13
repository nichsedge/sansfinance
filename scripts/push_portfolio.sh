#!/bin/bash
cd "$(dirname "$0")/.." || exit

# Script to push portfolio JSON snapshots to the device for importing
# Usage: ./push_portfolio.sh [filename]

DEFAULT_PATTERN="*.json"
PATTERN="${1:-$DEFAULT_PATTERN}"
DEST="/sdcard/Download/"

# Use nullglob to handle no matches gracefully
shopt -s nullglob
FILES=($PATTERN)

if [ ${#FILES[@]} -eq 0 ]; then
    echo "❌ Error: No files matching '$PATTERN' found in the current directory."
    echo "Available snapshot files:"
    ls *.json 2>/dev/null
    exit 1
fi

echo "🔄 Preparing to push ${#FILES[@]} file(s) to device..."

# Push to device
adb push "${FILES[@]}" "$DEST"

if [ $? -eq 0 ]; then
    echo "✅ Successfully pushed file(s) to '$DEST'"
    echo "📱 You can now import them from the app's Portfolio screen."
else
    echo "❌ Failed to push file(s). Is your device connected via ADB?"
    exit 1
fi
