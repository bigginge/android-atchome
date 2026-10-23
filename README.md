# ATC Home

[![Android CI](https://github.com/bigginge/android-atchome/actions/workflows/android.yml/badge.svg)](https://github.com/bigginge/android-atchome/actions/workflows/android.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
[![API](https://img.shields.io/badge/API-24%2B-brightgreen.svg)](https://android-arsenal.com/api?level=24)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9-7F52FF.svg)](https://kotlinlang.org/)

Home Air Traffic Control (ATC) radar monitor for Android and Amazon Fire tablets.  
Kotlin / Jetpack Compose port of [esp32-atchome](https://github.com/bigginge/esp32-atchome).

Turns an **Amazon Fire 7** (or any Android tablet / phone) into a dedicated, always-on desktop ADS-B radar display.

## Features

- **Tactical radar** — range rings (NM), home-centered crosshair, nearby airport tower markers, heading-aware chevrons, altitude-based coloring, fading trails, tap-to-select
- **Detail panel** — aircraft type, registration, callsign, live distance / altitude / speed / heading, route enrichment, status badge
- **Settings** — home lat/lon (with map picker), search radius, poll interval, keep-screen-on, trail / range-ring / airport toggles
- **Kiosk-friendly** — immersive fullscreen landscape layout with smooth dead-reckoning between ADS-B snapshots

## Requirements

| | |
|---|---|
| **Hardware** | Amazon Fire 7 (5th–12th Gen) or any Android tablet / phone |
| **OS** | Android 7.0+ (API 24) |
| **Layout** | Best in landscape (~1024×600): radar left, info panel right |
| **Data** | [adsb.fi](https://adsb.fi/) (traffic) · [hexdb.io](https://hexdb.io/) (metadata) |

## Quick start

### Android Studio

1. Open the project in **Android Studio** (Hedgehog or newer).
2. Let Gradle sync finish.
3. Run on a device or emulator (`Shift+F10`).

### Command line

```powershell
.\gradlew assembleDebug
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

Debug APK output: `app/build/outputs/apk/debug/app-debug.apk`

## Fire tablet install

1. **Enable USB debugging** — Settings → Device Options → About → tap Serial Number 7× → Developer Options → USB Debugging.
2. Connect via USB and allow the computer when prompted.
3. Install with `adb install -r …` as above, or copy the APK and allow Apps from Unknown Sources.

## Configuration

Default home location is `52.289514, -0.865019` with a 25 NM radius.

1. Tap the **gear** icon.
2. Set home latitude / longitude ([latlong.net](https://www.latlong.net/)) or pick on the map.
3. Adjust radius (15–35 NM is a good starting point) and poll interval.
4. Tap **Save & Apply** — settings persist across restarts.

## Project structure

```
app/src/main/java/com/atchome/atc/
├── MainActivity.kt          # Fullscreen kiosk activity
├── api/                     # adsb.fi, hexdb.io, geocoder, images
├── data/                    # Tracker, SettingsManager, bundled airport catalog
├── geo/                     # Distance / NM offset math
├── model/                   # Aircraft, Airport, TrailPoint
├── ui/                      # Radar, info panel, settings, map picker
└── viewmodel/               # Polling, enrichment, UI state
```

## Development

See [CONTRIBUTING.md](CONTRIBUTING.md) for setup, PR expectations, and issue guidelines.

CI builds a debug APK on every push and pull request to `master` ([workflow](.github/workflows/android.yml)).

## Related projects

- [esp32-atchome](https://github.com/bigginge/esp32-atchome) — original ESP32 firmware
- [esp8266-atchome](https://github.com/bigginge/esp8266-atchome) — ESP8266 variant

## License

Released under the [MIT License](LICENSE).

## Security

See [SECURITY.md](SECURITY.md) for how to report vulnerabilities privately.
