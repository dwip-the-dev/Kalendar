# Kalendar 📅

<div align="center">

[![Android](https://img.shields.io/badge/Platform-Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)](https://android.com)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin%202.0-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/UI-Jetpack%20Compose%20M3-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Skia](https://img.shields.io/badge/Engine-Skia%20GPU%20Canvas-FF5722?style=for-the-badge)](https://skia.org)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue?style=for-the-badge)](LICENSE)

**A high-performance, GPU-accelerated Calendar application for Android crafted with Jetpack Compose, Hardware Skia 2D Canvas, and Modern Android Architecture.**

*Made with ❤️ by Dwip* • **[Message me on Telegram](https://t.me/dwip_thedev)**

</div>

---

## ✨ Features

- ⚡ **120Hz Hardware Skia GPU 2D Canvas Engine**: Pure Skia Canvas rendering for Month grids, Year views, and custom GPU Date & Time pickers with zero node allocation overhead and instantaneous 120 FPS swiping.
- 🗓️ **24-Hour Dynamic Calendar App Icon**: The launcher icon dynamically updates every 24 hours at midnight to show the current day notation and date number. Includes **White Theme** and **Black Theme** switchers.
- 📱 **4 High-DPI Skia Home Screen Widgets**:
  - **Day Widget**: Current day name, giant day number, and month/year with high contrast.
  - **Month Grid Widget**: Full interactive month calendar with Sunday highlights and event indicators.
  - **Next Event Widget**: Dynamic countdown tag (`in 25m`), red vertical accent bar, and time range.
  - **Today's Events Widget**: Formatted live agenda stack for the current date.
  - *All widgets feature direct deep-linking into their respective views on tap.*
- 🔄 **Real-Time Two-Way Google Calendar Sync**: Full integration with Android `CalendarContract.Events` provider for device and Google account calendars, with background WorkManager synchronization.
- 💾 **Offline-First Architecture**: Android Jetpack Room with SQLite, Kotlin Coroutines, and reactive `StateFlow` streams.
- 🎨 **Jetpack Compose, SmoothAnimations, & Orbital**: Fluid shared element transitions, seasonal dynamic imagery headers, and tactile haptic feedback.
- ⏰ **Precise Alarms & Notification Channels**: Heads-up reminder notifications scheduled via AlarmManager exact alarms.
- 🧮 **Date Calculator & Interval Tools**: Calculate target dates forwards or backwards, measure day intervals, and inspect epoch timestamps.

---

## 🛠️ Architecture & Tech Stack

| Layer | Technology |
|---|---|
| **Language** | Kotlin 2.0 (100%) |
| **UI Framework** | Jetpack Compose & Material Design 3 (M3) |
| **Animations** | Jetpack Compose SmoothAnimations & Orbital Dynamics |
| **Graphics Engine** | Hardware-Accelerated Skia GPU 2D Canvas (120 FPS locked) |
| **Local Database** | Android Jetpack Room with SQLite & Flow |
| **Cloud Sync** | Android `CalendarContract.Events` Provider & WorkManager |
| **Widgets** | Android AppWidgets with Skia Bitmap Canvas Rendering |
| **Dynamic Icon** | 62 Android Launcher Activity Aliases with Midnight Alarm Auto-Update |
| **Image Loading** | Coil Image Loader with Curated Seasonal Landscape Engine |
| **Scheduling** | AlarmManager Exact Alarms with Heads-up Notification Channels |
| **Time API** | Desugared Java 8+ `java.time` API (LocalDate, YearMonth, ZoneId) |

---

## 🚀 Getting Started & Building

### Prerequisites
- Android Studio Ladybug / Meerkat or IntelliJ IDEA
- Android SDK (API 35, Build Tools 35.0.0)
- JDK 17 or higher

### Clone the Repository
```bash
git clone https://github.com/dwip-the-dev/Kalendar.git
cd Kalendar
```

### Build Debug APK
```bash
./gradlew assembleDebug
```
The compiled APK will be located at:
```
app/build/outputs/apk/debug/app-debug.apk
```

---

## 👨‍💻 Author

**Dwip**
- **Telegram**: [@dwip_thedev](https://t.me/dwip_thedev)
- **GitHub**: [@dwip-the-dev](https://github.com/dwip-the-dev)

---

## 📄 License

```text
Copyright 2026 Dwip

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
