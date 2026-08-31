# Clockity

Clockity is a modern, minimalist clock application for Android built with Jetpack Compose, Kotlin Coroutines, Room Database, and Material 3 design principles. Designed with a pure AMOLED dark aesthetic and Samsung One UI ergonomics, Clockity provides a cohesive, clean, and distraction-free time management experience.

---

## Features

### 1. Alarm Management and Categorized Groups
- **Alarm Groups**: Organize alarms into collapsible named categories (such as Workdays, Weekend, or Medication) to enable or disable related alarms with a single master toggle.
- **Duplicate Prevention**: Comprehensive validation prevents duplicate alarms (identical time, schedule, and group) and prevents duplicate group names across the application.
- **Flexible Scheduling**: Set alarms for recurring days of the week or select a specific calendar date using the integrated date picker.
- **Customization Options**: Configurable snooze durations (with complete disable option), gentle gradual volume wake-up, and tactile vibration patterns (Basic, Heartbeat, Tick-tock, Rapid).
- **Floating Action Button**: Dedicated bottom-right hovering action button for quickly creating new alarms.

### 2. Interactive Wheel and Keypad Time Pickers
- **Haptic Drum Wheels**: Vertical scrollable number wheels with snapping physics, center magnification, and real-time haptic tick feedback on every increment.
- **Dual Input Modes**: Seamlessly switch between scrollable drum wheels and direct numeric keypad entry by tapping on any dial.
- **Smooth Animation**: Programmatic resets (such as clearing a timer) perform a smooth backward scrolling animation with synchronized haptic clicks.

### 3. Multi-Timer and Editable Presets
- **Concurrent Timers**: Run, pause, resume, or add time to multiple countdown timers simultaneously.
- **Prepopulated Presets**: Ships out of the box with standard presets (such as Tea, Boiled Eggs, Power Nap, Pomodoro, and Workout).
- **Fully Editable Presets**: Users can create, modify, or delete any preset directly from the interface.

### 4. World Clock
- **Global Timezones**: Track current local times across major international cities with live daylight indicators and time difference offsets relative to local time.
- **Searchable City Directory**: Search and add cities across multiple continents and timezones.

### 5. Precision Stopwatch
- **High Accuracy**: Millisecond-accurate digital chronometer with continuous runtime tracking.
- **Lap Analysis**: Detailed lap tracking with automatic highlighting of fastest (yellow) and slowest (red) split times.

---

## Design System

Clockity follows strict One UI design guidelines tailored for AMOLED displays:

- **Pure AMOLED Black (`#000000`)**: Deep black backgrounds and popups maximize contrast and conserve battery life on OLED displays.
- **Strict Three-Color Accent Palette**:
  - **One UI Blue (`#3E82F7`)**: Primary actions, active switches, and core interaction points.
  - **One UI Yellow (`#FFD60A`)**: Paused states, daytime indicators, calendar markers, and fastest lap highlights.
  - **One UI Red (`#FF453A`)**: Delete actions, timer completion alerts, and slowest lap highlights.
- **Continuous 4-Corner Outlines**: Popups and dialog cards feature continuous, unclipped rounded borders for clear visual separation against dimmed scrims.

---

## Architecture and Tech Stack

- **UI Framework**: 100% Jetpack Compose with Material 3 components.
- **Architecture Pattern**: MVVM (Model-View-ViewModel) with unidirectional data flow (StateFlow).
- **Local Persistence**: Room Database for offline-first caching of alarms, alarm groups, world cities, and timer presets.
- **Background Scheduling**: Android `AlarmManager` with exact scheduling (`setExactAndAllowWhileIdle`) and `BroadcastReceiver` integration for reliable wakeups.
- **Language**: Kotlin 1.9+ with Coroutines and Flow.

---

## Project Structure

```
com.clockity.app
├── data
│   ├── local        # Room Database, DAOs, TypeConverters
│   └── models       # Data entities (Alarm, AlarmGroup, TimerPreset, WorldCity)
├── ui
│   ├── alarm        # Alarm list, AlarmGroupCard, EditAlarmDialog, EditGroupDialog
│   ├── worldclock   # World clock list, AddCityDialog
│   ├── timer        # Active timers, duration picker, preset dialogs
│   ├── stopwatch    # Chronometer display, lap history
│   ├── components   # OneUIHeader, WheelPicker, OneUISwitch
│   └── theme        # AMOLED Color palette, Type typography, OneUI Theme
├── utils            # AlarmScheduler, TimeUtils, VibrationUtils
└── MainActivity.kt  # Root navigation and tab scaffold
```

---

## Building and Installation

### Prerequisites
- Android Studio Hedgehog (2023.1.1) or newer
- JDK 17
- Android SDK 34

### Building Debug APK
```bash
./gradlew assembleDebug
```
The compiled APK will be located at:
`app/build/outputs/apk/debug/app-debug.apk`

### Running on Device or Emulator
```bash
./gradlew installDebug
adb shell am start -n com.clockity.app.debug/com.clockity.app.MainActivity
```

---

## License

This project is licensed under the Apache License 2.0.
