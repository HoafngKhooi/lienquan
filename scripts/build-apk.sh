#!/usr/bin/env bash
set -euo pipefail

if [ ! -f "./gradlew" ]; then
  echo "ERROR: ./gradlew không tồn tại. Chạy trước:"
  echo "  pkg install gradle && gradle wrapper --gradle-version 8.7"
  exit 1
fi

SDK="$HOME/android-sdk"
export ANDROID_HOME="$SDK"
export ANDROID_SDK_ROOT="$SDK"
export PATH="$PATH:$SDK/cmdline-tools/latest/bin:$SDK/platform-tools"

if [ ! -f "$SDK/platforms/android-34/android.jar" ]; then
  echo "==> Setup Android SDK lần đầu (~2.5GB, 15-20p)"
  mkdir -p "$SDK/cmdline-tools"
  cd "$SDK"
  wget -q https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip -O cmdline.zip
  unzip -q cmdline.zip -d cmdline-tools/
  mv cmdline-tools/cmdline-tools cmdline-tools/latest 2>/dev/null || true
  rm cmdline.zip
  cd - >/dev/null
  yes | sdkmanager --licenses >/dev/null 2>&1 || true
  sdkmanager "platform-tools" "platforms;android-34" "build-tools;34.0.0"
fi

echo "==> Build APK debug..."
./gradlew :app:assembleDebug

APK="app/build/outputs/apk/debug/app-debug.apk"
if [ -f "$APK" ]; then
  cp "$APK" "$HOME/lq-ban-assistant.apk"
  echo ""
  echo "✅ APK: $APK"
  echo "✅ Copy ra: $HOME/lq-ban-assistant.apk"
  echo "Cài: termux-open $HOME/lq-ban-assistant.apk"
else
  echo "❌ Build fail — không thấy $APK"
  exit 1
fi
