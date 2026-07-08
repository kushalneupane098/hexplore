#include <BLEDevice.h>
#include <BLEUtils.h>
#include <BLEServer.h>
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

// Unique UUIDs for the BLE Service and Characteristic
#define SERVICE_UUID        "4faac861-1a49-11ed-861d-0242ac130002"
#define CHARACTERISTIC_UUID "5a229b62-1a49-11ed-861d-0242ac130002"

bool deviceConnected = false;

BLEServer* pServer = NULL;
BLECharacteristic* pCharacteristic = NULL;

class MyServerCallbacks: public BLEServerCallbacks {
    void onConnect(BLEServer* pServer) {
      deviceConnected = true;
      Serial.println("Device connected successfully!");
    };
    void onDisconnect(BLEServer* pServer) {
      deviceConnected = false;
      Serial.println("Device disconnected. Restarting advertising...");
      pServer->startAdvertising();
    }
};

void setup() {
  Serial.begin(115200);
  delay(1000);
  Serial.println("Initializing ESP32 BLE Transmitter (HEXplore Beacon)...");

  // 1. Init BLE device name with the beacon zone ID
  BLEDevice::init(BEACON_NAME);

  // 2. MAX TX POWER — configure maximum transmit power levels (+9 dBm)
  esp_ble_tx_power_set(ESP_BLE_PWR_TYPE_ADV, ESP_PWR_LVL_P9);
  esp_ble_tx_power_set(ESP_BLE_PWR_TYPE_SCAN, ESP_PWR_LVL_P9);
  esp_ble_tx_power_set(ESP_BLE_PWR_TYPE_DEFAULT, ESP_PWR_LVL_P9);
  esp_ble_tx_power_set(ESP_BLE_PWR_TYPE_CONN_HDL0, ESP_PWR_LVL_P9);

  // 3. Create the server
  pServer = BLEDevice::createServer();
  pServer->setCallbacks(new MyServerCallbacks());

  // 4. Create the service
  BLEService* pService = pServer->createService(SERVICE_UUID);

  // 5. Create the characteristic containing the unique ID
  pCharacteristic = pService->createCharacteristic(
                      CHARACTERISTIC_UUID,
                      BLECharacteristic::PROPERTY_READ |
                      BLECharacteristic::PROPERTY_NOTIFY
                    );
  pCharacteristic->setValue(BEACON_NAME);
  pService->start();

  // 6. Primary advertisement: name + service UUID
  BLEAdvertisementData advData;
  advData.setFlags(0x06); // General Discoverable & BR_EDR_NOT_SUPPORTED
  advData.setName(BEACON_NAME);
  advData.setCompleteServices(BLEUUID(SERVICE_UUID));

  // 7. Scan response: carries the unique ID in manufacturer data as backup
  BLEAdvertisementData scanResponseData;
  std::string mfgData = "\xFF\xFF"; 
  mfgData += BEACON_NAME;
  scanResponseData.setManufacturerData(mfgData);

  BLEAdvertising* pAdvertising = BLEDevice::getAdvertising();
  pAdvertising->setAdvertisementData(advData);
  pAdvertising->setScanResponseData(scanResponseData);
  pAdvertising->setScanResponse(true);

  // 8. Fast, frequent advertising interval (100ms) for high-frequency scanner picks
  pAdvertising->setMinInterval(160); // 160 * 0.625ms = 100ms
  pAdvertising->setMaxInterval(160); // 160 * 0.625ms = 100ms

  pAdvertising->setMinPreferred(0x06);
  pAdvertising->setMinPreferred(0x12);

  BLEDevice::startAdvertising();

  Serial.println("==================================================");
  Serial.print("BLE Beacon Active & Transmitting!\n");
  Serial.print("Broadcasting UID (Name): "); Serial.println(BEACON_NAME);
  Serial.print("TX Power Level: MAX (+9 dBm)\n");
  Serial.println("==================================================");
}

void loop() {
  if (deviceConnected) {
    pCharacteristic->setValue(BEACON_NAME);
    pCharacteristic->notify();
  }
  delay(2000);
}
