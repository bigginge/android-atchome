# android-atchome

Home Air Traffic Control (ATC) radar monitor for Android and Amazon Fire tablets.  
Direct port of [esp32-atchome](https://github.com/example/esp32-atchome) built with **Kotlin** and **Jetpack Compose**.

<p align="center">
  <b>Live ADS-B Radar (Left) &nbsp;•&nbsp; Aircraft Telemetry & Route Panel (Right)</b>
</p>

---

## Overview

`android-atchome` turns an **Amazon Fire 7 tablet** (or any Android tablet / phone) into a dedicated, always-on desktop air traffic control radar display.

| Feature | Details |
|---|---|
| **Target Hardware** | Amazon Fire 7 (5th–12th Gen) / Android 7.0+ (API 24+) |
| **Display Layout** | Landscape 1024×600: Square Radar (Left) + Detail Info Panel (Right) |
| **Data Sources** | Live ADS-B traffic via [adsb.fi](https://adsb.fi/) • Metadata via [hexdb.io](https://hexdb.io/) |
| **Orientation** | Sticky immersive fullscreen kiosk mode with configurable keep-screen-on |
| **Animation** | Smooth dead-reckoning position extrapolation (10 FPS) between snapshots |

---

## Key Features

1. **Tactical Radar View**:
   - Concentric distance range rings with NM markers.
   - Crosshair centered on your home location.
   - Directional aircraft chevrons oriented to magnetic heading (`trackDeg`).
   - **Altitude-aware coloring**: HSV value/saturation scaling matching the original ESP32 firmware (lower altitudes appear darker/saturated, higher altitudes appear brighter).
   - **Historical flight trails**: Multi-point breadcrumbs fading with age and altitude (lower altitude = denser trail).
   - **Interactive touch selection**: Tap any blip on the radar to inspect it. Tap empty space to revert to auto-tracking the nearest aircraft.

2. **Aircraft Detail Panel**:
   - **Aircraft**: Manufacturer, model/type description, and registration.
   - **Flight**: Airline / registered owner and callsign / flight number.
   - **Telemetry matrix**: Live Distance (NM), Altitude (FT), Ground Speed (KTS), and Heading (°).
   - **Route enrichment**: Origin and destination airports with ICAO codes and human-readable names.
   - **Status badge**: Visual indicator for live connection, scanning, or offline state.

3. **In-App Settings Dialog**:
   - Tap the gear icon in the top right to configure:
     - **Home Latitude & Longitude** (decimal degrees)
     - **Search Radius** (5 to 100 NM slider)
     - **Poll Interval** (5 to 60 seconds slider)
     - **Keep Screen On** (toggle for desk stand usage)
     - **Show Trails** / **Show Range Rings**

---

## Project Structure

```
android-atchome/
├── app/
│   ├── src/main/
│   │   ├── AndroidManifest.xml
│   │   ├── java/com/atchome/atc/
│   │   │   ├── MainActivity.kt               # Fullscreen kiosk activity & screen stay-awake
│   │   │   ├── model/
│   │   │   │   ├── Aircraft.kt               # Aircraft telemetry, altitude HSV color math
│   │   │   │   └── TrailPoint.kt             # Historical breadcrumb coordinate
│   │   │   ├── geo/
│   │   │   │   └── GeoCalculations.kt        # Flat-earth NM offsets & haversine distance
│   │   │   ├── api/
│   │   │   │   ├── AdsbApiClient.kt          # adsb.fi live position polling
│   │   │   │   └── HexdbApiClient.kt         # hexdb.io aircraft, route & airport lookup
│   │   │   ├── data/
│   │   │   │   ├── Tracker.kt                # Snapshots, trail management, hit-test & dead reckoning
│   │   │   │   └── SettingsManager.kt        # SharedPreferences persistence
│   │   │   ├── viewmodel/
│   │   │   │   └── AtcViewModel.kt           # Coroutine loops, lazy staged enrichment, state flow
│   │   │   └── ui/
│   │   │       ├── AtcScreen.kt              # Responsive landscape tablet layout
│   │   │       ├── RadarView.kt              # Hardware-accelerated Compose Canvas radar
│   │   │       ├── InfoPanel.kt              # ATC detail card with live telemetry
│   │   │       ├── SettingsDialog.kt         # Configuration dialog
│   │   │       └── theme/                    # ATC dark palette (RadarBg, InfoBg, text styles)
│   │   └── res/                              # Adaptive icons, colors, themes, strings
│   ├── build.gradle.kts                      # App module dependencies & SDK configuration
│   └── proguard-rules.pro
├── gradle/
│   ├── libs.versions.toml                    # Version catalog
│   └── wrapper/                              # Gradle 8.7 wrapper
├── build.gradle.kts                          # Top-level Gradle configuration
├── settings.gradle.kts                       # Root project setup
├── gradlew / gradlew.bat
└── README.md
```

---

## Building the Project

### Option A: Using Android Studio
1. Open **Android Studio** (Hedgehog, Iguana, Jellyfish, or newer).
2. Select **Open** and choose the `c:\repos\android-atchome` folder.
3. Wait for Gradle sync to complete.
4. Connect your Fire 7 tablet via USB (or start an Android emulator).
5. Press **Run** (`Shift + F10`).

### Option B: Command Line (Gradle)
From PowerShell or Command Prompt in the repository folder:

```powershell
# Build debug APK
.\gradlew assembleDebug
```

The compiled APK will be located at:
`app\build\outputs\apk\debug\app-debug.apk`

---

## Installing on Amazon Fire 7 Tablet

### 1. Enable Developer Options & USB Debugging on Fire OS
1. Open **Settings** on your Fire 7 tablet.
2. Tap **Device Options** (or **Device**).
3. Tap **About Fire Tablet**.
4. Tap the **Serial Number** row repeatedly **7 times** until a message appears saying *"You are now a developer!"*.
5. Go back one screen and open the new **Developer Options** menu.
6. Toggle **Developer Options** to **ON**.
7. Scroll down and toggle **USB Debugging** to **ON**.

### 2. Sideload via ADB
Connect the tablet to your computer using a micro-USB (or USB-C on 12th Gen) cable. If prompted on the tablet screen, check **Always allow from this computer** and tap **OK**.

Then run:
```powershell
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

### 3. Alternative (Without ADB): Direct APK Transfer
1. Copy `app-debug.apk` to your Fire 7 tablet via USB file transfer, cloud storage (Google Drive / Dropbox), or local network.
2. In Fire OS Settings, go to **Security & Privacy** -> **Apps from Unknown Sources** -> allow your file manager or browser.
3. Tap `app-debug.apk` on the tablet to install.

---

## Configuration

When first launched, the app defaults to the coordinates specified in your initial config (`52.289514, -0.865019`, radius 25 NM).

To adjust:
1. Tap the **gear icon** in the top right corner.
2. Enter your home latitude and longitude (find yours quickly on [latlong.net](https://www.latlong.net/)).
3. Adjust the search radius (e.g. 15–35 NM recommended for quick performance).
4. Tap **Save & Apply**. Settings are automatically persisted across restarts.
