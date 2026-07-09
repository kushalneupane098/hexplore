<div align="center">

<img src="assets/2.png" alt="HEXplore Logo" width="120"/>

# HEXplore

### Spatial BLE Navigation Companion for HEX 2083

**A proximity-aware indoor navigation and gamification app for the Himalaya Engineering Exhibition**

[![Android](https://img.shields.io/badge/Platform-Android%208.0%2B-3DDC84?logo=android&logoColor=white)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4?logo=jetpackcompose&logoColor=white)](https://developer.android.com/compose)
[![BLE](https://img.shields.io/badge/Hardware-ESP32%20BLE-00B4D8)](https://www.espressif.com/en/products/socs/esp32)
[![License](https://img.shields.io/badge/License-MIT-green)](LICENSE)

</div>

---

## 📖 Overview

**HEXplore** is an offline-first Android application purpose-built for the **Himalaya Engineering Exhibition (HEX 2083)** at **Himalaya College of Engineering (HCOE), Lalitpur, Nepal**.

The app transforms a visitor's smartphone into an active spatial compass. Distributed **ESP32 BLE beacons** placed at each exhibition zone broadcast unique identifiers. The app continuously scans for these signals, determines the visitor's current location using **RSSI-based proximity estimation**, unlocks the nearest zone's content, and renders a cyberpunk-aesthetic **spatial radar** centered on the user.

Visitors can explore department showcases, engage with gamified challenges, collect XP points, and navigate between zones using the in-app visual pathway guide — all without any internet connection.

---

## ✨ Features

| Feature | Description |
|---|---|
| 🛰️ **Spatial BLE Radar** | Concentric animated radar UI showing the detected zone at the center and adjacent zones as orbiting bubbles |
| 🔓 **Proximity Zone Unlocking** | Zones auto-unlock when the visitor is physically near the ESP32 beacon; a lock screen prevents remote viewing |
| 📡 **Robust BLE Scanning** | Periodic 90-second scanner restarts bypass Android's BLE scan throttling for continuous, indefinite operation |
| 🗺️ **Visual Pathway Guide** | Zone detail pages include a "Nearby Destinations & Pathways" section with real photographs of connecting routes |
| 🎮 **Gamified XP System** | Completing zone challenges earns XP; a ranking ladder promotes from *Exhibition Guest* to *Exhibition Grandmaster* |
| ✅ **Persistent Visit Tracking** | Zones marked as visited are stored in Room SQLite DB and persist across app restarts |
| 📴 **Offline-First Architecture** | All zone data (projects, games, navigation, photos) is bundled in the APK — no internet required |
| 🌑 **Cyberpunk Dark UI** | Deep navy background, electric cyan accents, glassmorphic cards, and smooth animations |

---

## 🗺️ Zone Map

```
[MAIN GATE]  ──────────────►  [BASKETBALL COURT]
                                      │
                              ┌───────┴───────────┐
                         [HECC Club]   [IT Club]   [HRC Club]
                                      │
                              [ARCHITECT ZONE]
                                      │
                               [REGISTRATION DESK]
```

| Beacon ID | Zone | Hosted By |
|---|---|---|
| `HEX_BEACON_01` | Main Gate | Entry / Welcome |
| `HEX_BEACON_02` | Registration Desk | Civil Dept. Club |
| `HEX_BEACON_03` | Basketball Court | HECC + IT + HRC Clubs |
| `HEX_BEACON_04` | Architect Zone | Architecture Dept. |

---

## 🏗️ Tech Stack

See [`TECH_STACK.md`](TECH_STACK.md) for the full deep-dive technical architecture documentation.

**Quick Summary:**
- **Android App**: Kotlin + Jetpack Compose + Room + Moshi
- **BLE Hardware**: ESP32 microcontrollers running custom Arduino firmware
- **Architecture**: MVVM + Repository + Foreground Service

## 🤖 GitHub CI

The repository includes [`.github/workflows/android-ci.yml`](.github/workflows/android-ci.yml), which builds the debug APK on every push and pull request using GitHub Actions.

---

## 📋 Prerequisites

Before building, ensure you have the following installed:

- **Android Studio** (Hedgehog 2023.1.1 or later) — or just the Android SDK CLI tools
- **JDK 17** (`java -version` should show 17.x)
- **Android SDK** with API Level 24+ installed
- **ADB** (`adb --version`) — comes with Android SDK platform tools
- **Arduino IDE 2.x** (for flashing ESP32 beacons)
  - Install `esp32` board package via Boards Manager

If you are setting up on Windows, use the dedicated guide in [docs/windows/README.md](docs/windows/README.md) and the helper scripts in [scripts/windows/hexplore.ps1](scripts/windows/hexplore.ps1).

---

## 🚀 Getting Started

### 1. Clone the Repository

```bash
git clone https://github.com/nischal-hcoe/hexplore.git
cd hexplore
```

### 2. Configure Environment

- **Windows:** run [scripts/windows/setup.bat](scripts/windows/setup.bat) or [scripts/windows/hexplore.ps1](scripts/windows/hexplore.ps1)
- **macOS/Linux:** copy `.env.example` to `.env` if the file does not already exist

### 3. Connect Your Android Device

Enable **USB Debugging** on your phone:
> Settings → About Phone → tap **Build Number** 7 times → Developer Options → USB Debugging ✅

Connect via USB, then verify:

```bash
adb devices
```

### 4. Build & Install

- **Windows:** run [scripts/windows/build.bat](scripts/windows/build.bat) to compile the debug APK, or [scripts/windows/install.bat](scripts/windows/install.bat) to build and install to the connected device
- **Other platforms:** run `./gradlew assembleDebug` or `./gradlew installDebug`

The generated APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

---

## 🔌 ESP32 Beacon Firmware

Each physical exhibition zone requires one ESP32 microcontroller running the beacon firmware.

### Flashing Steps

1. Open `esp32_beacon.ino` in **Arduino IDE**
2. Install the ESP32 board package if not already present:
   - Boards Manager → search **esp32** by Espressif → Install
3. Select your board: `Tools → Board → ESP32 Dev Module`
4. Set the beacon ID for the target zone:
   ```cpp
   // At the top of esp32_beacon.ino:
   #define BEACON_NAME "HEX_BEACON_03"  // Change to 01, 02, 03, or 04
   ```
   | Value | Zone |
   |---|---|
   | `HEX_BEACON_01` | Main Gate |
   | `HEX_BEACON_02` | Registration Desk |
   | `HEX_BEACON_03` | Basketball Court |
   | `HEX_BEACON_04` | Architect Zone |

5. Upload to the ESP32 board (`Ctrl+U`)
6. The ESP32 will begin broadcasting immediately at maximum TX power (+9 dBm) in **non-connectable scannable mode**, ensuring no interference from nearby Bluetooth devices attempting to connect.

---

## 📁 Project Structure

```
hexplore/
├── .github/
│   └── workflows/
│       └── android-ci.yml            # GitHub Actions workflow for debug APK builds
├── docs/
│   └── windows/
│       └── README.md                # Windows setup, build, and USB install guide
├── app/
│   └── src/main/
│       ├── assets/
│       │   └── hex_data.json          # Zone catalog (beacons, showcases, games, nav)
│       ├── java/com/example/
│       │   ├── ble/
│       │   │   ├── BleScannerService.kt    # Foreground BLE scanning service
│       │   │   ├── BleViewModel.kt         # ViewModel: beacon state & XP logic
│       │   │   └── BleSignalTracker.kt     # Global RSSI state holder
│       │   ├── data/
│       │   │   ├── AppDatabase.kt          # Room database configuration
│       │   │   ├── Models.kt               # Room entities + Moshi JSON models
│       │   │   ├── PlaceDao.kt             # Database queries
│       │   │   └── PlaceRepository.kt      # Data layer: JSON → Room prepopulator
│       │   ├── ui/
│       │   │   ├── MainRadarScreen.kt      # Spatial radar Compose component
│       │   │   └── theme/                  # Color palette, typography, shapes
│       │   └── MainActivity.kt             # Root Compose UI: screens & navigation
│       └── res/
│           └── drawable/                   # Zone + project photographs
├── esp32_beacon.ino                        # ESP32 Arduino beacon firmware
├── scripts/
│   └── windows/
│       ├── hexplore.ps1                  # Windows helper for setup/build/install/device checks
│       ├── setup.bat                     # One-click setup wrapper
│       ├── build.bat                     # Build debug APK wrapper
│       ├── install.bat                   # Build + install over USB wrapper
│       └── devices.bat                   # adb devices wrapper
├── TECH_STACK.md                           # Technical architecture documentation
└── README.md                               # This file
```

---

## 🔧 Configuration

### Zone Data (`hex_data.json`)

All exhibition content is driven by `app/src/main/assets/hex_data.json`. Each beacon entry follows this schema:

```json
{
  "beacons": {
    "HEX_BEACON_03": {
      "location_name": "Basketball Court",
      "short_description": "...",
      "image_asset": "img_basketball_court",
      "major": 1,
      "minor": 3,
      "detailed_info": {
        "about": "Zone description text...",
        "showcases": [ { "title": "...", "desc": "...", "image_url": "...", "category": "..." } ],
        "games": [ { "title": "...", "desc": "...", "image_url": "..." } ],
        "about_navigation": [
          { "title": "Adjacent Side: ...", "desc": "...", "images": ["img_architect_zone"] }
        ]
      }
    }
  }
}
```

> **Note:** After modifying `hex_data.json`, increment the `version` number in `AppDatabase.kt` to trigger a database reload on next app launch.

---

## 📜 License

This project is licensed under the **MIT License**. See [LICENSE](LICENSE) for details.

---

## 🏫 About

Built for **HEX 2083** — the annual engineering exhibition of **Himalaya College of Engineering (HCOE)**, Chyasal, Lalitpur, Nepal.
