# NoxVision

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Android](https://img.shields.io/badge/Android-24%2B-green.svg)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.2.10-blue.svg)](https://kotlinlang.org)
[![Version](https://img.shields.io/badge/Version-1.3.0-blue.svg)](../../releases)

Open-source Android app for Guide thermal cameras with RTSP livestream, on-device object detection, and hunting workflow tools.

## Demo

<p align="center">
  <img src="docs/preview.gif" alt="NoxVision Demo" width="320">
</p>

## Current Status

- App version: `1.3.0` (`versionCode 4`)
- Min SDK: `24`
- Target/Compile SDK: `35`
- ABI: `arm64-v8a`
- Native libraries are 16 KB page-size compatible (required by Google Play for Android 15+ updates).

## Features

- Live RTSP thermal stream via LibVLC
- AI object detection (thermal model, on-device)
- Thermal controls: emissivity, distance, humidity, reflected temperature, NUC/shutter
- Camera settings: brightness, contrast, image enhancement, audio toggle
- Media capture: screenshot + video + integrated gallery
- Auto Wi-Fi connection for Guide hotspot workflow
- Hunting Assistant:
  - shot documentation and journal
  - weather view (OpenWeather API)
  - hunting seasons calendar
  - map with offline tile cache + waypoints
  - tracking/Nachsuche tools with compass support
- Multi-language UI (`de`, `en`, `fr`, `es`, `it`, `nl`, `pl`, `uk`)

## Supported Cameras

Guide Sensmart models are detected by series/profile at runtime.

- TE-Series
- C-Series
- D-Series
- B-Series
- PS-Series

Connection defaults (adjustable in settings):

- Camera IP: `192.168.42.1`
- RTSP: `rtsp://192.168.42.1:8554/video`
- HTTP API: `http://192.168.42.1`

## Build From Source

### Requirements

- JDK 17+ (Android Studio JBR is fine)
- Android SDK (API 35 installed)
- Gradle Wrapper (`./gradlew`)

### Local Configuration

Create/edit `local.properties`:

```properties
sdk.dir=/path/to/Android/Sdk

# required for in-app weather
OPENWEATHER_API_KEY=your_openweather_key

# optional: required for signed Play Store release builds
UPLOAD_STORE_FILE=keystore/upload-keystore.jks
UPLOAD_STORE_PASSWORD=...
UPLOAD_KEY_ALIAS=upload
UPLOAD_KEY_PASSWORD=...
```

Notes:

- `local.properties` and `keystore/*.jks` are intentionally ignored by git.
- Do not commit API keys or keystores.

### Commands

```bash
# debug apk
JAVA_HOME=/opt/android-studio/jbr PATH=/opt/android-studio/jbr/bin:$PATH ./gradlew :app:assembleDebug

# release aab (Play Store)
JAVA_HOME=/opt/android-studio/jbr PATH=/opt/android-studio/jbr/bin:$PATH ./gradlew :app:bundleRelease
```

Artifacts:

- Debug APK: `app/build/outputs/apk/debug/`
- Release AAB: `app/build/outputs/bundle/release/NoxVision-v1.3.0-release.aab`

## Play Store Release Notes

- Upload the generated `.aab` from `app/build/outputs/bundle/release/`.
- Keep upload keystore + passwords backed up securely.
- If an API key was exposed during testing, rotate it before production rollout.

## Troubleshooting

- "No weather data": verify `OPENWEATHER_API_KEY` is set in `local.properties`, then rebuild/reinstall.
- Fish shell variable syntax differs from bash:

```fish
set v (sed -n 's/.*OPENWEATHER_API_KEY = "\\(.*\\)";.*/\\1/p' app/build/generated/source/buildConfig/debug/com/noxvision/app/BuildConfig.java)
echo (string length -- "$v")
```

## Project Structure

```text
app/src/main/java/com/noxvision/app/
├── ui/                 # livestream/settings/hunting screens and dialogs
├── hunting/            # weather, maps, exports, calendar, location
├── detection/          # thermal object detection
├── billing/            # in-app purchase components
├── network/            # Wi-Fi auto connect
└── util/               # logging, locale, media helpers
```

## Contributing

See `CONTRIBUTING.md`.

## License

MIT (`LICENSE`).
