#include <BLEDevice.h>
#include <BLEUtils.h>
#include <BLEAdvertising.h>
#include "esp_bt.h"
#include "esp_gap_ble_api.h"

// =========================================================================
// CONFIGURATION: Set the Beacon Zone ID here
// Mappings:
// - "HEX_BEACON_01" -> Main Gate
// - "HEX_BEACON_02" -> Registration Desk
// - "HEX_BEACON_03" -> Basketball Court (IT/HECC/HRC Clubs)
// - "HEX_BEACON_04" -> Architect Zone
// =========================================================================
#define BEACON_NAME "HEX_BEACON_03" 

// Unique UUID for the BLE Service
#define SERVICE_UUID        "4faac861-1a49-11ed-861d-0242ac130002"

void setup() {
  Serial.begin(115200);
  delay(1000);
  Serial.println("Initializing ESP32 BLE Broadcast Beacon (HEXplore)...");

  // 1. Init BLE device name with the beacon zone ID
  BLEDevice::init(BEACON_NAME);

  // 2. Configure maximum transmit power levels (+9 dBm) for maximum range
  esp_ble_tx_power_set(ESP_BLE_PWR_TYPE_ADV, ESP_PWR_LVL_P9);
  esp_ble_tx_power_set(ESP_BLE_PWR_TYPE_SCAN, ESP_PWR_LVL_P9);
  esp_ble_tx_power_set(ESP_BLE_PWR_TYPE_DEFAULT, ESP_PWR_LVL_P9);

  // 3. Primary advertisement: name + service UUID
  BLEAdvertisementData advData;
  advData.setFlags(0x06); // General Discoverable & BR_EDR_NOT_SUPPORTED
  advData.setName(BEACON_NAME);
  advData.setCompleteServices(BLEUUID(SERVICE_UUID));

  // 4. Scan response: carries the unique ID in manufacturer data as backup
  BLEAdvertisementData scanResponseData;
  std::string mfgData = "\xFF\xFF"; 
  mfgData += BEACON_NAME;
  scanResponseData.setManufacturerData(mfgData);

  // 5. Configure advertising settings
  BLEAdvertising* pAdvertising = BLEDevice::getAdvertising();
  pAdvertising->setAdvertisementData(advData);
  pAdvertising->setScanResponseData(scanResponseData);
  pAdvertising->setScanResponse(true);

  // Set as Scannable, Non-connectable to prevent devices from locking it up
  pAdvertising->setAdvertisementType(ADV_TYPE_SCAN_IND);

  // Fast, frequent advertising interval (100ms) for high-frequency picks
  pAdvertising->setMinInterval(160); // 160 * 0.625ms = 100ms
  pAdvertising->setMaxInterval(160); // 160 * 0.625ms = 100ms

  // Start broadcasting
  BLEDevice::startAdvertising();

  Serial.println("==================================================");
  Serial.print("BLE Broadcast Beacon Active & Transmitting!\n");
  Serial.print("Broadcasting UID (Name): "); Serial.println(BEACON_NAME);
  Serial.print("TX Power Level: MAX (+9 dBm)\n");
  Serial.println("==================================================");
}

void loop() {
  // Broadcaster does not need a connection loop.
  // It simply sleeps/delays to conserve power.
  delay(5000);
}
