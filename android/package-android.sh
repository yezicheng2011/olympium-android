#!/usr/bin/env bash
set -euo pipefail

# Builds universal multi-ABI (arm64-v8a + armeabi-v7a + x86_64 + x86) APK and AAB for Android.

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
DIST_DIR="$SCRIPT_DIR/dist"
mkdir -p "$DIST_DIR"

cd "$SCRIPT_DIR"
GRADLE_BIN="./gradlew"
if [ ! -x "$GRADLE_BIN" ]; then
  GRADLE_BIN="$(command -v gradle || true)"
fi
if [ -z "$GRADLE_BIN" ]; then
  echo "ERROR: Neither ./gradlew nor gradle found. Install Gradle 8.7+ and Android SDK (API 34)." >&2
  exit 1
fi

echo "==> Building universal multi-ABI Android APK (arm64-v8a + armeabi-v7a + x86_64 + x86)..."
"$GRADLE_BIN" assembleRelease

echo "==> Building Android App Bundle (AAB)..."
"$GRADLE_BIN" bundleRelease

cp -f app/build/outputs/apk/release/*.apk "$DIST_DIR/" 2>/dev/null || true
cp -f app/build/outputs/bundle/release/*.aab "$DIST_DIR/" 2>/dev/null || true

echo "Done. Android artifacts written to: $DIST_DIR"
ls -lh "$DIST_DIR"
