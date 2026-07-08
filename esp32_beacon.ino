/*
 * HEXplore - ESP32 BLE Beacon Transmitter Program
 * Compatible with the HEXplore Android Proximity Radar.
 * 
 * Flashing Instructions:
 * 1. Open this file in the Arduino IDE.
 * 2. Select your ESP32 Board (Tools > Board) and configure the Port.
 * 3. Change the BEACON_NAME definition below to match your target zone UID (e.g. "HEX_BEACON_01" to "HEX_BEACON_04").
 * 4. Upload the code to your ESP32.
 */

#include <BLEDevice.h>
#include <BLEUtils.h>
#include <BLEAdvertising.h>

// =========================================================================
// CONFIGURATION: Set the Beacon Zone ID here
// Mappings:
// - "HEX_BEACON_01" -> Main Gate
// - "HEX_BEACON_02" -> Registration Desk
// - "HEX_BEACON_03" -> Basketball Court (IT/HECC/HRC Clubs)
// - "HEX_BEACON_04" -> Architect Zone
// =========================================================================
#define BEACON_NAME "HEX_BEACON_03" 

// Standard iBeacon Manufacturer Data payload definition
// Apple Manufacturer ID: 0x004C
// iBeacon type: 0x02, Length: 0x15 (21 bytes)
// Major: 1, Minor: Matches the beacon number (e.g. 1 for HEX_BEACON_01, 3 for HEX_BEACON_03)
void setBeaconPayload(BLEAdvertising* pAdvertising, uint16_t minorVal) {
    BLEBeacon oBeacon = BLEBeacon();
    oBeacon.setManufacturerId(0x004C); // Apple Company Code
    oBeacon.setProximityUUID(BLEUUID("74278bda-b644-4520-8f0c-720eaf059935")); // standard dummy UUID
    oBeacon.setMajor(1);      // Major = 1 (exhibition network)
    oBeacon.setMinor(minorVal); // Minor = Zone number (e.g., 3 for HEX_BEACON_03)
    oBeacon.setSignalPower(-59); // Measured RSSI at 1 meter (used for distance calculation)

    BLEAdvertisementData oAdvertisementData = BLEAdvertisementData();
    BLEAdvertisementData oScanResponseData = BLEAdvertisementData();
    
    // Add flags
    oAdvertisementData.setFlags(0x04 | 0x02); // BR_EDR_NOT_SUPPORTED | GENERAL_DISCOVERABLE
    
    // Set manufacturer data string
    std::string strServiceData = "";
    strServiceData += (char)26;     // Length of structure
    strServiceData += (char)0xFF;   // Manufacturer Specific Data Type
    strServiceData += oBeacon.getData();
    oAdvertisementData.addData(strServiceData);
    
    // Set device name in scan response so the scanner can pick it up both ways
    oScanResponseData.setName(BEACON_NAME);
    
    pAdvertising->setAdvertisementData(oAdvertisementData);
    pAdvertising->setScanResponseData(oScanResponseData);
}

void setup() {
  Serial.begin(115200);
  delay(1000);
  Serial.println("Initializing ESP32 BLE Transmitter...");

  // Extract minor value number from string for iBeacon parsing backup
  int minorNumber = 3; // Default
  String nameStr = String(BEACON_NAME);
  int lastUnderscore = nameStr.lastIndexOf('_');
  if (lastUnderscore != -1) {
    minorNumber = nameStr.substring(lastUnderscore + 1).toInt();
  }

  // Initialize BLE Device name
  BLEDevice::init(BEACON_NAME);
  
  // Create Server
  BLEServer *pServer = BLEDevice::createServer();

  // Get advertising pointer
  BLEAdvertising *pAdvertising = BLEDevice::getAdvertising();

  // Set dual-mode compatibility: iBeacon standard manufacturer payload + Device name in scan response
  setBeaconPayload(pAdvertising, (uint16_t)minorNumber);
  
  // Enable advertising parameters
  pAdvertising->setScanResponse(true);
  pAdvertising->setMinPreferred(0x06); // connection parameter optimizations
  pAdvertising->setMinPreferred(0x12);

  // Start Advertising
  BLEDevice::startAdvertising();
  
  Serial.println("==================================================");
  Serial.print("BLE Beacon Active & Transmitting!\n");
  Serial.print("Device Name: "); Serial.println(BEACON_NAME);
  Serial.print("iBeacon Minor: "); Serial.println(minorNumber);
  Serial.println("==================================================");
}

void loop() {
  // LED blink indicator showing heartbeats
  #ifdef LED_BUILTIN
    pinMode(LED_BUILTIN, OUTPUT);
    digitalWrite(LED_BUILTIN, HIGH);
    delay(100);
    digitalWrite(LED_BUILTIN, LOW);
    delay(1900);
  #else
    delay(2000);
  #endif
}
