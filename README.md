<div align="center">
  <img src="docs/preview.gif" alt="NoxVision Demo" width="320" style="border-radius: 12px; margin-bottom: 20px;">

  # 🦉 NoxVision

  **The ultimate Android companion app for Guide™ thermal cameras.**  
  *Livestreaming, intelligent hunting workflows, and seamless field documentation.*

  [![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg?style=flat-square)](https://opensource.org/licenses/MIT)
  [![Android](https://img.shields.io/badge/Android-24%2B-3DDC84.svg?style=flat-square&logo=android)](https://developer.android.com)
  [![Kotlin](https://img.shields.io/badge/Kotlin-2.2.10-7F52FF.svg?style=flat-square&logo=kotlin)](https://kotlinlang.org)
  [![Version](https://img.shields.io/badge/Version-1.3.0-blue.svg?style=flat-square)](../../releases)
</div>

---

## ✨ Features

NoxVision transforms your smartphone into a powerful command center for your thermal camera.

🎥 **Advanced Vision**
- Real-time thermal livestreaming from Guide cameras
- On-device **AI Object Detection** for enhanced situational awareness
- Precise camera tuning (thermal controls, image enhancement, audio toggle)
- One-tap screenshot and video recording with an integrated media gallery

🌲 **Hunting & Field Assistant**
- **Shot Documentation:** Detailed journaling system for every shot
- **Live Weather Data:** Built-in OpenWeather API integration for current conditions
- **Hunting Calendar:** Keep track of hunting seasons effortlessly
- **Offline Maps & Tracking:** Map out waypoints and navigate with compass support, even without cell service

🌐 **Smart Connectivity & Localization**
- Automatic Wi-Fi connection to your camera's hotspot
- Multi-language UI: 🇩🇪 `de`, 🇬🇧 `en`, 🇫🇷 `fr`, 🇪🇸 `es`, 🇮🇹 `it`, 🇳🇱 `nl`, 🇵🇱 `pl`, 🇺🇦 `uk`

---

## 📷 Supported Cameras

NoxVision automatically detects and connects to **Guide Sensmart** models:
- **TE Series** | **C Series** | **D Series** | **B Series** | **PS Series**

---

## 🚀 Get Started

No technical knowledge is required to test NoxVision. Start in just 2 minutes!

1. 👥 **Join the Tester Group** → [Open Google Group](https://groups.google.com/g/noxvision-closed-testers)
2. ✅ **Activate Test Access** → [Open Play Opt-in](https://play.google.com/apps/testing/com.noxvision.app) *(Use the same Google account)*
3. 📥 **Install the App** → [Open Play Store](https://play.google.com/store/apps/details?id=com.noxvision.app)
4. 💬 **Send Feedback** → [Report an Issue on GitHub](https://github.com/nacl-dev/NoxVision/issues)

> **Quick Links:**  
> 📖 [Tester Guide](docs/closed-alpha-test.md) • 🔒 [Privacy Policy](PRIVACY_POLICY.md) • 🐛 [Issue Tracker](https://github.com/nacl-dev/NoxVision/issues)

---

## 📊 Project Status

- **Current Version:** `1.3.0` (`versionCode 4`)
- Google Play rollout is actively in progress
- Public installs will be managed via Google Play; GitHub artifacts are strictly for development and troubleshooting

---

## 🛠️ For Developers

<details>
<summary><strong>Build from Source</strong> <i>(Click to expand)</i></summary>

<br>

### Requirements
- JDK 17+
- Android SDK API 35
- Gradle Wrapper (`./gradlew`)

### Local Setup
Create or edit `local.properties` in the project root:

```properties
sdk.dir=/path/to/Android/Sdk
OPENWEATHER_API_KEY=your_openweather_key

# Optional for signed Play release build
UPLOAD_STORE_FILE=keystore/upload-keystore.jks
UPLOAD_STORE_PASSWORD=...
UPLOAD_KEY_ALIAS=upload
UPLOAD_KEY_PASSWORD=...
```
*⚠️ **Note:** `local.properties` and keystore files must NEVER be committed to version control.*

### Build Commands
```bash
# Debug APK
JAVA_HOME=/opt/android-studio/jbr PATH=/opt/android-studio/jbr/bin:$PATH ./gradlew :app:assembleDebug

# Release AAB for Play Console
JAVA_HOME=/opt/android-studio/jbr PATH=/opt/android-studio/jbr/bin:$PATH ./gradlew :app:bundleRelease
```

### Release Workflow (Play Store)
1. Update `versionCode` and `versionName` in `app/build.gradle.kts`
2. Build AAB with `:app:bundleRelease`
3. Upload to Play Console test track
4. Add release notes and required declarations
5. Promote to production when approved

*Do not distribute debug APKs to end-users.*
</details>

---

## 🤝 Contributing & Support

- **Found a bug or have an idea?** Our [Issue Tracker](https://github.com/nacl-dev/NoxVision/issues) is the best place to share it.
- **Want to contribute?** Check out our [`CONTRIBUTING.md`](CONTRIBUTING.md) guide.

## 📄 License
This project is licensed under the MIT License - see the [`LICENSE`](LICENSE) file for details.
