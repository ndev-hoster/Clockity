# Clockity Development & Testing Guidelines

## Waydroid Execution & Deployment Commands

To correctly launch and test Clockity on Waydroid:

```bash
# 1. Full restart and launch (if Waydroid session is stopped or stuck):
waydroid session stop && waydroid session start

# 2. Deploy debug APK and display Waydroid UI:
adb install -r app/build/outputs/apk/debug/app-debug.apk && adb shell am start -n com.clockity.app.debug/com.clockity.app.MainActivity && waydroid show-full-ui

# 3. Deploy signed release APK and display Waydroid UI:
adb install -r /home/kratoes/clockity.apk && adb shell am start -n com.clockity.app/com.clockity.app.MainActivity && waydroid show-full-ui
```

## Build & Release Rules
- Always use JDK 17: `JAVA_HOME=/home/kratoes/.jdk/jdk-17.0.12+7 ./gradlew <task>`
- Release APK target location: `/home/kratoes/clockity.apk`
- Release signing: `signingConfig = signingConfigs.getByName("debug")`
- Maximum active timers: strictly 5
- No emojis in `README.md`
