# HEXplore - HEX 2083 Proximity Companion

**HEXplore** is an offline-first Indoor Positioning, Navigation, and Gamification companion app built for the **Himalaya Exhibition (HEX 2083)** at **Himalaya College of Engineering (HCOE)**. 

The app turns a user's mobile device into a spatial scanner, utilizing Bluetooth Low Energy (BLE) transmissions from distributed ESP32 microcontrollers to automatically detect the user's current department zone, unlock student project showcases, and enable interactive mini-games.

---

## 🌟 Key Features

1. **Spatial Radar UI**: Concentric circles scan dynamically and render the user's current central zone. Adjacent physical paths (e.g. going from the Main Gate to the Basketball Court) are drawn as orbiting bubbles that rotate in real-time matching the device's physical compass hardware orientation.
2. **Proximity-Based Unlocks**: To see technical specifications of projects or complete live quests, the user must be physically present at the stall. If the user walks away or views a zone remotely, a **Proximity Lock** is applied.
3. **Decoupled Debouncing & RSSI Tracking**: 
   * BLE scans are processed using a rolling-average of the last 5 RSSI readings to smooth out wireless interference.
   * A periodic evaluator runs every 1.5 seconds to select the single strongest signal.
   * Stale beacons are pruned after 6 seconds of absence, clearing the proximity view automatically when walking away.
4. **Offline-First Room DB**: All layout details (projects, structural diagrams, navigation maps, challenges) are cached locally in a Room SQLite database on startup, ensuring zero dependence on network connectivity.
5. **Gamified XP Ranking**: Earning XP by clearing live arcade challenges at stalls helps users rank up (e.g., from *Exhibition Guest* to *Exhibition Grandmaster*).

---

## 🗺️ Zone Catalog Configuration

The app maps physical BLE signals (Major: `1`, Minor: `1` to `4`) to the following exhibition zones:
* **Main Gate** (`HEX_BEACON_01`): Entry point. Connected on its right side to the Basketball Court.
* **Registration Desk** (`HEX_BEACON_02`): Visitors register, get passes, and download layout guidelines. Hosts Civil Department projects.
* **Basketball Court** (`HEX_BEACON_03`): Active technology arena hosting three sub-zones:
  * **Right Side (HECC Club)**: Virtual Lens Try-On (AI), 2-Player Quiz, Aero Drum (FSR), Smart Shopping Cart.
  * **Middle Zone (IT Club)**: Smart Home Security, IoT Smart Irrigation, Bluetooth RC Car.
  * **Left Side (HRC Club)**: Balloon Popping Robot, Line Follower, UAV Telemetry Drone, CV Gesture Robot Arm.
  * **Live games**: *Buzz Wire Challenge*, *Ring Toss*, and *Stop Time Precision*.
* **Architect Zone** (`HEX_BEACON_04`): Design scale models and layouts. Hosts Architecture projects and the *Rat Trap* logic game.

---

## 🔌 ESP32 Beacon Setup (Hardware)

To set up the physical transmitters for the exhibition booths, use the provided Arduino IDE script: [esp32_beacon.ino](file:///home/nischal/Downloads/hexplore/esp32_beacon.ino).

### Flashing Steps:
1. Open the [esp32_beacon.ino](file:///home/nischal/Downloads/hexplore/esp32_beacon.ino) script in the Arduino IDE.
2. Ensure you have the `esp32` board manager package installed.
3. Configure the `BEACON_NAME` parameter in the code:
   ```cpp
   #define BEACON_NAME "HEX_BEACON_03" // Change to 01, 02, 03, or 04 depending on the booth
   ```
4. Upload/Flash the sketch to your ESP32 board. The microcontroller will immediately begin broadcasting dual-compatibility packets (Apple iBeacon format with minor value matching the zone, and standard BLE scan response containing the matching device name).

---

## 🛠️ Build and Deploy Locally

Follow these commands to compile and install the app directly on your mobile device:

### Prerequisites:
Enable **USB Debugging** on your phone (under Settings > Developer Options) and connect it via USB.

### Commands:
1. **Initialize the local secrets file**:
   ```bash
   cp .env.example .env
   ```
2. **Build and install directly to connected phone**:
   ```bash
   ./gradlew installDebug
   ```
   *(Note: The build is pre-configured to use the system Java JDK 17 /usr/lib/jvm/java-17-openjdk-amd64 and global Android SDK.)*

3. **Verify running devices**:
   ```bash
   adb devices
   ```
