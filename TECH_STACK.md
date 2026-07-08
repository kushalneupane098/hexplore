# HEXplore — Technical Architecture

> **HEX 2083 Spatial BLE Navigation Companion**
> Himalaya College of Engineering (HCOE), Lalitpur, Nepal

---

## Table of Contents

1. [System Overview](#1-system-overview)
2. [Hardware Layer — ESP32 BLE Beacons](#2-hardware-layer--esp32-ble-beacons)
3. [Android App Architecture](#3-android-app-architecture)
4. [BLE Scanning & Signal Processing](#4-ble-scanning--signal-processing)
5. [Data Layer — Room Database](#5-data-layer--room-database)
6. [UI Architecture — Jetpack Compose](#6-ui-architecture--jetpack-compose)
7. [Spatial Radar Engine](#7-spatial-radar-engine)
8. [Zone Data Format](#8-zone-data-format)
9. [Dependency Catalog](#9-dependency-catalog)
10. [Build Configuration](#10-build-configuration)

---

## 1. System Overview

HEXplore is a **proximity-aware indoor navigation system** consisting of two tightly coupled components:

```
┌─────────────────────────────────────────────────────────────────┐
│                        PHYSICAL LAYER                           │
│                                                                 │
│   [ESP32 Beacon #01]  [ESP32 Beacon #02]  [ESP32 Beacon #03]   │
│   Main Gate           Registration Desk   Basketball Court      │
│   HEX_BEACON_01       HEX_BEACON_02       HEX_BEACON_03         │
│        │                    │                    │              │
│        └────────────────────┴────────────────────┘             │
│                    BLE Advertisement Packets                     │
│                    (ADV_TYPE_SCAN_IND, +9dBm)                   │
└────────────────────────────────┬────────────────────────────────┘
                                 │
                        Bluetooth Radio
                                 │
┌────────────────────────────────▼────────────────────────────────┐
│                     ANDROID APPLICATION                         │
│                                                                 │
│   BleScannerService (Foreground) → BleSignalTracker (Global)   │
│                 ↓                              ↓                │
│          BleViewModel ←──────── PlaceRepository                │
│                 ↓                              ↓                │
│        Jetpack Compose UI             Room SQLite DB            │
│   (Radar + Zones + Zone Detail)     (Places, Showcases, Games)  │
└─────────────────────────────────────────────────────────────────┘
```

---

## 2. Hardware Layer — ESP32 BLE Beacons

### Microcontroller
- **Board**: ESP32 (any variant: ESP32 DevKit, WROOM-32, etc.)
- **Firmware**: Custom Arduino sketch (`esp32_beacon.ino`)
- **SDK**: ESP-IDF via Arduino-ESP32 core v3.x

### BLE Advertising Configuration

| Parameter | Value | Rationale |
|---|---|---|
| Advertisement Type | `ADV_TYPE_SCAN_IND` | Scannable but **non-connectable** — prevents Bluetooth devices from establishing a connection and suspending advertisement |
| TX Power | `ESP_PWR_LVL_P9` (+9 dBm) | Maximum transmit power for range; critical in outdoor exhibition environments with interference |
| Advertising Interval | 100ms (160 × 0.625ms) | Frequent packets ensure rapid phone detection when approaching a zone |
| Primary Payload | Device Name + Service UUID | Android scanner filters by device name prefix `HEX_BEACON_` |
| Scan Response | Manufacturer-specific data | Redundant identifier in a secondary packet for robustness |

### Why Non-Connectable Mode?

If an ESP32 is set to connectable advertising (`ADV_TYPE_IND`), any Bluetooth Central device (phones, laptops) in the vicinity can establish a connection. While connected, the ESP32's BT controller **suspends advertisement packets**, making the beacon invisible to all scanners. By using `ADV_TYPE_SCAN_IND`, the device permanently broadcasts without accepting connections.

### Beacon Identity Scheme

Each beacon is identified by its **BLE Device Name**, which doubles as the zone's unique identifier:

```
HEX_BEACON_01  →  Main Gate
HEX_BEACON_02  →  Registration Desk
HEX_BEACON_03  →  Basketball Court
HEX_BEACON_04  →  Architect Zone
```

---

## 3. Android App Architecture

### Pattern: MVVM + Repository + Foreground Service

```
MainActivity (Compose Root)
    │
    ├── BleViewModel (AndroidViewModel)
    │       ├── Observes: BleSignalTracker.activeBeacon (StateFlow)
    │       ├── Triggers: XP updates, visited zone marking
    │       └── Reads: PlaceRepository.allPlaces (Flow<List<PlaceWithDetails>>)
    │
    ├── PlaceRepository
    │       ├── Pre-populates Room from hex_data.json on first launch
    │       └── Exposes: Flow<List<PlaceWithDetails>> via PlaceDao
    │
    └── BleScannerService (Foreground Service)
            ├── Runs independently of UI lifecycle
            ├── Manages: BluetoothLeScanner, ScanCallback
            ├── Publishes: BleSignalTracker.activeBeacon (StateFlow)
            └── Schedules: Periodic restart every 90 seconds
```

### Key Design Decisions

**Foreground Service for BLE**: Android aggressively restricts background work. A `Service` with `FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE` keeps the BLE scanner alive even when the app is backgrounded or the screen is off.

**BleSignalTracker as Global State**: A singleton object holds the current active beacon in a `StateFlow`. This decouples the scanner (Service) from the UI (ViewModel), allowing either to restart independently without losing state.

**Room DB Prepopulation**: Rather than hardcoding UI data or fetching from a server, all exhibition content is bundled as `hex_data.json` in the app assets. The repository reads this file once (when the database is empty) and writes all zone data into Room. Subsequent launches read directly from SQLite — zero network dependency.

---

## 4. BLE Scanning & Signal Processing

### Scan Configuration

```kotlin
ScanSettings.Builder()
    .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)  // Maximum frequency
    .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
    .build()
```

### Beacon Detection Filter

The scanner filters only for devices whose **name starts with** `HEX_BEACON_`:

```kotlin
ScanFilter.Builder()
    .setDeviceName("HEX_BEACON_")  // prefix match
    .build()
```

### RSSI Smoothing

Raw RSSI readings are highly volatile due to multipath interference, reflections, and body blocking. HEXplore uses a **rolling average over the last 5 readings** per beacon:

```
beaconRssiHistory: Map<String, ArrayList<Int>>
    → Keep last 5 RSSI values per beacon UID
    → computeAverage() → smoothed RSSI score
```

### Active Beacon Selection

Every **1.5 seconds**, the evaluator:
1. Prunes beacons not seen within the last **12 seconds**
2. Computes the average RSSI for each remaining beacon
3. Selects the beacon with the **strongest (highest)** average RSSI
4. Publishes to `BleSignalTracker.activeBeacon` — the global `StateFlow`

### Android BLE Scan Throttling Mitigation

Android OS automatically throttles continuous BLE scans after approximately 30 minutes of `SCAN_MODE_LOW_LATENCY`. The scanner enters a lower-frequency "opportunistic" mode, dramatically reducing packet reception rate.

**Solution**: A `restartScanRunnable` fires every **90 seconds** and performs:
1. `stopScanning()` — releases the hardware scanner
2. 1-second delay
3. `startScanning()` — acquires a fresh scanner with full `LOW_LATENCY` priority

This resets Android's internal throttle counter, maintaining full scanning performance indefinitely.

---

## 5. Data Layer — Room Database

### Entity Schema

```
places
├── uid (PK, TEXT)              ← "HEX_BEACON_03"
├── locationName (TEXT)
├── shortDescription (TEXT)
├── imageAsset (TEXT)           ← drawable resource name
├── major (INT)
├── minor (INT)
├── about (TEXT)
├── imageUrl (TEXT, nullable)
├── isVisited (BOOLEAN)
└── aboutNavigationJson (TEXT)  ← JSON-serialized List<JsonPathway>

showcases
├── id (PK, auto)
├── placeUid (FK → places.uid)
├── title (TEXT)
├── desc (TEXT)
├── imageUrl (TEXT, nullable)
└── category (TEXT, nullable)

games
├── id (PK, auto)
├── placeUid (FK → places.uid)
├── title (TEXT)
├── desc (TEXT)
└── imageUrl (TEXT, nullable)

navigation_entries
├── id (PK, auto)
├── placeUid (FK → places.uid)
├── direction (TEXT)            ← "left", "right", "in_front", "behind"
└── targetUid (TEXT)
```

### PlaceWithDetails

Room's `@Relation` queries join all four tables into a single `PlaceWithDetails` object for the UI:

```kotlin
data class PlaceWithDetails(
    @Embedded val place: PlaceEntity,
    @Relation(parentColumn = "uid", entityColumn = "placeUid")
    val showcases: List<ShowcaseEntity>,
    @Relation(parentColumn = "uid", entityColumn = "placeUid")
    val games: List<GameEntity>,
    @Relation(parentColumn = "uid", entityColumn = "placeUid")
    val navigationEntries: List<NavigationEntity>
)
```

### Database Version History

| Version | Change |
|---|---|
| 1 | Initial schema |
| 2 | Added `imageUrl` to places |
| 3 | Added `isVisited` boolean |
| 4 | Bumped for fresh prepopulation |
| 5 | Added `aboutNavigationJson` column |
| 6 | Refreshed with Robo Soccer game data |

> The database uses `fallbackToDestructiveMigration()` — on a version upgrade, all tables are dropped and re-populated from `hex_data.json`.

---

## 6. UI Architecture — Jetpack Compose

### Screen Graph

```
MainActivity
    │
    ├── LoadingOverlay (if allPlaces.isEmpty())
    │
    └── Scaffold
            ├── TopBar (HeaderSection)
            └── Content (based on selected tab)
                    ├── tab 0: MainExploreScreen (Radar + Zone Card)
                    ├── tab 1: ZonesScreen (Zone List Grid)
                    └── tab 2: MeScreen (Profile + XP + Rank)

ZoneDetailScreen (overlay)
    ├── Tab 0: TabAboutContent (About text + Visual Pathway Guide)
    ├── Tab 1: TabShowcasesContent (Project cards by category)
    └── Tab 2: TabGamesContent (Game cards)
```

### Theme System

| Token | Value | Usage |
|---|---|---|
| `DeepNavy` | `#050D18` | Background |
| `ElectricCyan` | `#00E5FF` | Primary accent, labels, borders |
| `NeonTeal` | `#00BFA5` | Secondary highlights |
| `TranslucentSurface` | `#FFFFFF` at 8% alpha | Glassmorphic card backgrounds |
| `WhiteTranslucent` | `#FFFFFF` at 60% alpha | Subtitle and secondary text |

### Glassmorphic Card Component

All content cards use a shared `GlassmorphicCard` composable:
- `Box` with `Canvas`-drawn background gradient
- Thin 1dp border with configurable border color (typically `ElectricCyan` at 15–35% alpha)
- `RoundedCornerShape(20–24.dp)` corners
- Optional `onClick` lambda for tappable cards

---

## 7. Spatial Radar Engine

The radar is the centerpiece of the Explore screen. It is implemented as a custom `Canvas`-based Compose component in `MainRadarScreen.kt`.

### Components

1. **Concentric Rings**: 3 rings drawn with `drawCircle()` at 33%, 66%, and 100% of the radar radius, with decreasing alpha from inner to outer
2. **Scan Animation**: `infiniteTransition` animates a gradient arc rotating from 0° to 360°, creating a "sweep" effect
3. **Center Zone**: The active zone's image is drawn as a circular cropped image at the center
4. **Orbit Bubbles**: Adjacent zones are rendered as small circular thumbnails orbiting at a fixed radius from center
5. **Static Layout**: Orbit positions are computed from the zone's relative bearing in **fixed compass coordinates** (North = top, East = right) — not compass-rotation-adjusted, ensuring stable, non-spinning bubbles

### Position Calculation

```kotlin
val angleDeg = when (place.place.uid) {
    "HEX_BEACON_01" -> 270f  // West
    "HEX_BEACON_02" -> 180f  // South
    "HEX_BEACON_04" -> 90f   // East
    else -> 0f
}
val angleRad = Math.toRadians(angleDeg.toDouble())
val bubbleX = centerX + orbitRadius * cos(angleRad)
val bubbleY = centerY + orbitRadius * sin(angleRad)
```

---

## 8. Zone Data Format

The complete exhibition content catalog is stored in `app/src/main/assets/hex_data.json`. The top-level structure:

```json
{
  "beacons": {
    "HEX_BEACON_03": {
      "location_name": "Basketball Court",
      "short_description": "...",
      "image_asset": "img_basketball_court",
      "major": 1,
      "minor": 3,
      "image_url": "img_basketball_court",
      "detailed_info": {
        "about": "Zone description...",
        "showcases": [
          { "title": "...", "desc": "...", "image_url": "...", "category": "Right Side (HECC Club)" }
        ],
        "games": [
          { "title": "Robo Soccer Challenge", "desc": "...", "image_url": "img_robo_soccer_game" }
        ],
        "about_navigation": [
          { "title": "Adjacent Side: Architecture Zone", "desc": "...", "images": ["img_architect_zone"] }
        ]
      },
      "navigation": {
        "in_front": null,
        "behind": null,
        "left": null,
        "right": { "target_uid": "HEX_BEACON_01", "name": "Main Gate" }
      }
    }
  }
}
```

The `about_navigation` array drives the **Visual Pathway Guide** cards at the bottom of each zone's About tab. Each entry supports 1 (full-width) or 2 (split side-by-side) images with `RoundedCornerShape(16.dp)` styling.

---

## 9. Dependency Catalog

| Library | Version | Role |
|---|---|---|
| **Kotlin** | 2.0.x | Primary language |
| **Jetpack Compose BOM** | Latest | UI framework |
| **Compose Material 3** | Via BOM | Material Design components |
| **Compose Material Icons Extended** | Via BOM | Icon set |
| **AndroidX Activity Compose** | — | `setContent{}` integration |
| **AndroidX Lifecycle ViewModel Compose** | — | `viewModel()` Compose integration |
| **AndroidX Lifecycle Runtime Compose** | — | `collectAsStateWithLifecycle()` |
| **Room Runtime + KTX** | — | Local SQLite ORM |
| **Room KSP Compiler** | — | Code generation for DAO |
| **Moshi Kotlin** | — | JSON parsing |
| **Moshi KSP Codegen** | — | Adapter code generation |
| **Coil Compose** | — | Async image loading |
| **Accompanist Permissions** | — | Runtime permission handling |
| **Kotlinx Coroutines** | — | Async/Flow |
| **OkHttp + Retrofit** | — | HTTP client (available for future use) |

---

## 10. Build Configuration

### Requirements

| Requirement | Minimum Version |
|---|---|
| Android Studio | Hedgehog 2023.1.1 |
| JDK | 17 |
| Android SDK | API 36 (compile), API 24 (min) |
| Kotlin | 2.0.x |
| Gradle | 8.x |

### Environment Variables (`.env`)

| Variable | Purpose |
|---|---|
| `KEYSTORE_PATH` | Path to release keystore JKS file |
| `STORE_PASSWORD` | Keystore password |
| `KEY_PASSWORD` | Key alias password |

Debug builds use `debug.keystore` with default Android debug credentials — no configuration needed.

### Important Build Notes

- The `applicationId` is `com.aistudio.hexplore.nvfpxe` — this must match any installed APK for upgrade installs
- Database version is defined in `AppDatabase.kt` — increment this whenever `hex_data.json` content changes
- `fallbackToDestructiveMigration()` is used — **upgrading the DB version will wipe and reload all data**
- Resource images must be placed in `app/src/main/res/drawable/` with lowercase names matching the `image_url` / `image_asset` field in `hex_data.json`

---

*Documentation for HEXplore — HEX 2083, Himalaya College of Engineering*
