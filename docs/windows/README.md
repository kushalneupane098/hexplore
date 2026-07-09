# Windows Setup, Build, and USB Install

This guide covers the Windows workflow for setting up HEXplore, building the debug APK, and installing it on a phone over USB.

## What you need

- Windows 10 or Windows 11
- Android Studio installed, or at minimum the Android SDK, platform tools, and build tools
- JDK 17
- USB debugging enabled on the Android phone
- A USB cable that supports data transfer

If you are unsure whether your SDK is configured correctly, open Android Studio once and let it install the missing components through SDK Manager.

## First-time setup

1. Clone the repository and open a PowerShell window in the repo root.
2. Run the setup wrapper:

```powershell
.\scripts\windows\setup.bat
```

The setup command will:

- copy `.env.example` to `.env` if `.env` is missing
- resolve the Android SDK path from your environment or the default Android Studio location
- write the Windows `local.properties` file with the detected SDK path
- verify that Java and ADB are available

If the script cannot find your SDK, set `ANDROID_SDK_ROOT` to the SDK directory and run setup again.

## Build the app

To build a debug APK without installing it on a device:

```powershell
.\scripts\windows\build.bat
```

This produces:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Install over USB

1. Enable Developer Options on the phone.
2. Turn on USB debugging.
3. Connect the phone to the PC with a data cable.
4. Accept the USB debugging prompt on the phone.
5. Verify that ADB sees the device:

```powershell
.\scripts\windows\devices.bat
```

6. Install the app:

```powershell
.\scripts\windows\install.bat
```

If you have more than one device connected, use the PowerShell helper directly and pass the serial:

```powershell
.\scripts\windows\hexplore.ps1 install -DeviceSerial <serial-from-adb-devices>
```

## Manual Gradle commands

If you prefer Gradle directly, these are the equivalent commands:

```powershell
.\gradlew.bat assembleDebug
.\gradlew.bat installDebug
```

## Troubleshooting

If the device shows as `unauthorized`, unlock the phone and accept the debugging prompt again.

If `adb` is not found, install Android SDK Platform-Tools or re-run the setup script after setting `ANDROID_SDK_ROOT`.

If Gradle cannot find the SDK, make sure `local.properties` points to the Windows SDK location and not a Linux path from another machine.
