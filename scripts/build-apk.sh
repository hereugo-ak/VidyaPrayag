#!/bin/bash
# Build and install the debug APK on a connected ADB device
# Usage: ./scripts/build-apk.sh

cd "$(dirname "$0")/.."

echo "Checking for connected devices..."
DEVICES=$(adb devices | grep -w "device" | wc -l | tr -d ' ')

if [ "$DEVICES" -eq 0 ]; then
  echo "No ADB device found. Connect a device or start an emulator."
  exit 1
fi

adb devices
echo ""
echo "Building and installing debug APK..."
./gradlew :composeApp:installDebug

if [ $? -eq 0 ]; then
  echo ""
  echo "APK installed successfully."
else
  echo ""
  echo "Build or install failed. Check output above."
  exit 1
fi
