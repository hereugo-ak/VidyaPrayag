#!/bin/bash
# Build once, install and launch on ALL connected ADB devices (USB + wireless)
# Usage: ./scripts/build-apk.sh

cd "$(dirname "$0")/.."

PACKAGE="com.littlebridge.enrollplus"

echo "Checking for connected devices..."
DEVICE_IDS=$(adb devices | grep -w "device" | awk '{print $1}')

if [ -z "$DEVICE_IDS" ]; then
  echo "No ADB device found. Connect a device or start an emulator."
  exit 1
fi

DEVICE_COUNT=$(echo "$DEVICE_IDS" | wc -l | tr -d ' ')
echo "Found $DEVICE_COUNT device(s):"
echo "$DEVICE_IDS" | sed 's/^/  - /'
echo ""

echo "Building debug APK (single build for all devices)..."
./gradlew :composeApp:assembleDevDebug

if [ $? -ne 0 ]; then
  echo ""
  echo "Build failed. Check output above."
  exit 1
fi

APK="composeApp/build/outputs/apk/dev/debug/composeApp-dev-debug.apk"

if [ ! -f "$APK" ]; then
  echo "APK not found at expected path: $APK"
  exit 1
fi

echo ""
echo "Installing and launching on all $DEVICE_COUNT device(s)..."
echo ""

FAILED=0
for SERIAL in $DEVICE_IDS; do
  echo "── Device: $SERIAL ──"
  adb -s "$SERIAL" install -r "$APK"
  if [ $? -ne 0 ]; then
    echo "  Install failed on $SERIAL"
    FAILED=$((FAILED + 1))
    continue
  fi
  adb -s "$SERIAL" shell monkey -p "$PACKAGE" -c android.intent.category.LAUNCHER 1 >/dev/null 2>&1
  echo "  Installed and launched."
  echo ""
done

if [ "$FAILED" -gt 0 ]; then
  echo "$FAILED device(s) failed. Check output above."
  exit 1
fi

echo "All done — app deployed to $DEVICE_COUNT device(s)."
