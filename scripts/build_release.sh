#!/bin/bash
cd "$(dirname "$0")/.." || exit
# 🚀 Build Release APK
set -e

echo "🧹 Cleaning project..."
./gradlew clean

echo "📦 Building Release APK..."
./gradlew assembleRelease

# Find the APK
APK_PATH=$(find app/build/outputs/apk/release -name "*.apk" | head -n 1)

if [ -z "$APK_PATH" ]; then
    echo "❌ Error: Could not find release APK!"
    exit 1
fi

echo "✅ Build successful!"
echo "📂 APK location: $APK_PATH"

# Create release directory if not exists
mkdir -p release
cp "$APK_PATH" release/sans-finance-release.apk

echo "📁 Copied to: release/sans-finance-release.apk"
