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

## Distribution Status

- Current app version: `1.3.0` (`versionCode 4`)
- Min SDK: `24`
- Target/Compile SDK: `35`
- ABI: `arm64-v8a`
- Native libraries are 16 KB page-size compatible (required by Google Play for Android 15+ updates).
- Google Play rollout is in progress. Future end-user installs should happen via Google Play.
- GitHub build artifacts are intended for development and troubleshooting only.

## Closed Alpha Test (Google Play)

- Tester group: `https://groups.google.com/g/noxvision-closed-testers`
- Opt-in page: `https://play.google.com/apps/testing/com.noxvision.app`
- Install page: `https://play.google.com/store/apps/details?id=com.noxvision.app`
- Feedback: `https://github.com/nacl-dev/NoxVision/issues`

Testers should first join the Google Group, then open the opt-in link with the same Google account, then install via Play Store.
Full guide and copy/paste invite text: `docs/closed-alpha-test.md`.

## Features

- Live RTSP thermal stream via LibVLC
- On-device object detection
- Thermal controls: emissivity, distance, humidity, reflected temperature, NUC/shutter
- Camera settings: brightness, contrast, image enhancement, audio toggle
- Media capture: screenshot, video recording, integrated gallery
- Auto Wi-Fi connection for Guide hotspot workflow
- Hunting Assistant:
  - Shot documentation and journal
  - Weather view (OpenWeather API)
  - Hunting seasons calendar
  - Offline map with waypoints
  - Tracking tools with compass support
- Multi-language UI (`de`, `en`, `fr`, `es`, `it`, `nl`, `pl`, `uk`)

## Supported Cameras

Guide Sensmart models are detected by series/profile at runtime.

- TE series
- C series
- D series
- B series
- PS series

Connection defaults (adjustable in settings):

- Camera IP: `192.168.42.1`
- RTSP: `rtsp://192.168.42.1:8554/video`
- HTTP API: `http://192.168.42.1`

## Build From Source

### Requirements

- JDK 17+ (Android Studio JBR works)
- Android SDK with API 35 installed
- Gradle Wrapper (`./gradlew`)

### Local Configuration

Create or edit `local.properties`:

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

Security notes:

- `local.properties` and `keystore/*.jks` are ignored by git.
- Never commit API keys or signing files.

### Build Commands

```bash
# debug APK for local testing
JAVA_HOME=/opt/android-studio/jbr PATH=/opt/android-studio/jbr/bin:$PATH ./gradlew :app:assembleDebug

# release AAB for Google Play
JAVA_HOME=/opt/android-studio/jbr PATH=/opt/android-studio/jbr/bin:$PATH ./gradlew :app:bundleRelease
```

Artifacts:

- Debug APK: `app/build/outputs/apk/debug/`
- Release AAB: `app/build/outputs/bundle/release/`

## Release Workflow

1. Bump `versionCode` and `versionName` in `app/build.gradle.kts`.
2. Build AAB with `:app:bundleRelease`.
3. Upload AAB to Play Console test track.
4. Add release notes and policy declarations.
5. Promote from testing track to production when approved.

Do not publish a debug APK as an official user distribution channel. Use Play tracks or a signed release artifact for external users.

## Troubleshooting

- If weather shows "No weather data", verify `OPENWEATHER_API_KEY` in `local.properties`, then rebuild and reinstall.
- Fish shell syntax to check injected key length:

```fish
set v (sed -n 's/.*OPENWEATHER_API_KEY = "\\(.*\\)";.*/\\1/p' app/build/generated/source/buildConfig/debug/com/noxvision/app/BuildConfig.java)
echo (string length -- "$v")
```

## Contributing

See `CONTRIBUTING.md`.

## License

MIT (`LICENSE`).
