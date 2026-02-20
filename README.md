# NoxVision

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Android](https://img.shields.io/badge/Android-24%2B-green.svg)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.2.10-blue.svg)](https://kotlinlang.org)
[![Version](https://img.shields.io/badge/Version-1.3.0-blue.svg)](../../releases)

NoxVision is an Android app for Guide thermal cameras.
It helps with livestreaming, hunting workflows, and field documentation.

## Demo

<p align="center">
  <img src="docs/preview.gif" alt="NoxVision Demo" width="320">
</p>

## New Here?

If you landed here from Google Play testing: you do not need coding knowledge.
Just follow the steps below.

## Join The Closed Alpha Test

- Tester group: `https://groups.google.com/g/noxvision-closed-testers`
- Opt-in page: `https://play.google.com/apps/testing/com.noxvision.app`
- Install page: `https://play.google.com/store/apps/details?id=com.noxvision.app`
- Feedback: `https://github.com/nacl-dev/NoxVision/issues`

Steps:

1. Join the Google Group.
2. Open the opt-in link with the same Google account.
3. Install the app from the Play Store link.
4. Share feedback and bugs on GitHub Issues.

Full tester guide and invitation template: `docs/closed-alpha-test.md`.

## Features

- Thermal livestream from Guide cameras
- AI object detection on device
- Camera tuning (thermal controls, image enhancement, audio toggle)
- Screenshot and video recording
- Integrated media gallery
- Auto Wi-Fi connection to the camera hotspot
- Hunting Assistant:
  - Shot documentation and journal
  - Weather view (OpenWeather API)
  - Hunting seasons calendar
  - Offline map with waypoints
  - Tracking tools with compass support
- Multi-language UI: `de`, `en`, `fr`, `es`, `it`, `nl`, `pl`, `uk`

## Supported Cameras

Guide Sensmart models are detected automatically:

- TE Series
- C Series
- D Series
- B Series
- PS Series

## Project Status

- Current app version: `1.3.0` (`versionCode 4`)
- Google Play rollout is in progress
- Future public installs should happen through Google Play
- GitHub artifacts are for development and troubleshooting

## Feedback And Support

- Report bugs and ideas: `https://github.com/nacl-dev/NoxVision/issues`
- Privacy policy: `PRIVACY_POLICY.md`

## For Developers

### Requirements

- JDK 17+
- Android SDK API 35
- Gradle Wrapper (`./gradlew`)

### Local Setup

Create or edit `local.properties`:

```properties
sdk.dir=/path/to/Android/Sdk
OPENWEATHER_API_KEY=your_openweather_key

# optional for signed Play release build
UPLOAD_STORE_FILE=keystore/upload-keystore.jks
UPLOAD_STORE_PASSWORD=...
UPLOAD_KEY_ALIAS=upload
UPLOAD_KEY_PASSWORD=...
```

Notes:

- `local.properties` and `keystore/*.jks` are ignored by git.
- Never commit API keys or signing files.

### Build

```bash
# debug APK
JAVA_HOME=/opt/android-studio/jbr PATH=/opt/android-studio/jbr/bin:$PATH ./gradlew :app:assembleDebug

# release AAB for Play Console
JAVA_HOME=/opt/android-studio/jbr PATH=/opt/android-studio/jbr/bin:$PATH ./gradlew :app:bundleRelease
```

### Release Workflow (Play Store)

1. Update `versionCode` and `versionName` in `app/build.gradle.kts`.
2. Build AAB with `:app:bundleRelease`.
3. Upload to Play Console test track.
4. Add release notes and required declarations.
5. Promote to production when approved.

Do not use debug APKs as an official end-user channel.

## Contributing

See `CONTRIBUTING.md`.

## License

MIT (`LICENSE`).
