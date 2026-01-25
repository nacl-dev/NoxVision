# NoxVision

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Android](https://img.shields.io/badge/Android-24%2B-green.svg)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0-blue.svg)](https://kotlinlang.org)
[![Version](https://img.shields.io/badge/Version-1.2-blue.svg)](../../releases)

**Open Source Android App for Guide Thermal Cameras with AI Object Detection**

A powerful alternative to the official Guide app, supporting multiple Guide Sensmart thermal camera models including the [Guide TE211M](https://de.guideoutdoor.com/produkt/wärmebild-monokulare/te-serie/te211m) Thermal Monocular.

---

## 📺 Demo

<p align="center">
  <img src="docs/preview.gif" alt="NoxVision Demo" width="300">
</p>

---

## ✨ Features

### Core Features
- 🎥 **Live Thermal Video Stream** via RTSP (LibVLC)
- 🤖 **YOLO Object Detection** with TensorFlow Lite
- 🎨 **Multiple Palettes**: Whitehot, Blackhot, Bluehot, Greenhot, Ironred, and more
- 📸 **Screenshot & Video Recording** with gallery storage
- 📁 **Integrated Gallery** for captured media
- 📶 **Auto-WiFi Connect** to camera
- ⚡ **Real-time Overlay** with bounding boxes and labels
- 🌙 **Dark Theme** optimized for night use

### 🆕 Thermal Measurement (v1.2)
- 🌡️ **Emissivity Control** (0.01-1.0) with material presets
- 📏 **Distance Compensation** for accurate temperature readings
- 💧 **Humidity Settings** for atmospheric correction
- ♨️ **Reflected Temperature** adjustment
- 🔄 **Shutter/NUC Calibration** button
- 📱 **Automatic Camera Detection** - identifies model and enables features
- 🔌 **REST API Integration** for professional thermal settings

## 📱 Supported Devices

### Thermal Cameras

| Series | Models | Features |
|--------|--------|----------|
| **TE-Series** | TE211M, TE-Mini | Radiometry ✓ |
| **C-Series** | C640, C400, C800 | Focus ✓, GPS ✓, Radiometry ✓, Audio ✓ |
| **D-Series** | D400, D384, D192, D160 | Focus ✓, Radiometry ✓ |
| **B-Series** | B320, B256, B160 | Radiometry ✓ |
| **PS-Series** | PS600, PS400 | Radiometry ✓ |

> **Note**: Features are automatically detected based on the connected camera model.

### Android
- **Minimum**: Android 7.0 (API 24)
- **Target**: Android 15 (API 35)
- **Architecture**: arm64-v8a

## 🔧 Camera Connection

The app automatically connects to the camera via WiFi:

| Parameter | Default Value | Description |
|-----------|---------------|-------------|
| **SSID** | `TE Mini-XXXX` | Camera's WiFi hotspot |
| **Password** | `12345678` | Default password for all Guide cameras |
| **IP** | `192.168.42.1` | Fixed IP of camera in hotspot mode |
| **RTSP** | `rtsp://192.168.42.1:8554/video` | Video stream URL |
| **HTTP API** | `http://192.168.42.1` | Camera control |

> **Note**: The IP can be changed in Settings if your camera uses a different address.

## 📥 Installation

### Option 1: APK Download (Recommended)
1. Download the latest APK from [Releases](../../releases)
2. Install on your Android device
3. Allow installation from unknown sources if prompted

### Option 2: Build from Source

**Prerequisites:**
- JDK 17 or higher
- Android SDK (API 35)
- Git

```bash
# Clone repository
git clone https://github.com/nacl-dev/NoxVision.git
cd NoxVision

# Set SDK path (if not auto-detected)
echo "sdk.dir=/path/to/Android/Sdk" > local.properties

# Build debug APK
./gradlew assembleDebug

# APK location:
# app/build/outputs/apk/debug/NoxVision-v1.2-debug.apk
```

## 🎯 Usage

1. **Turn on camera** and enable WiFi hotspot
2. **Launch app** - automatically connects to camera
3. **Live stream** displays with object detection
4. **Controls**:
   - 📷 Take screenshot
   - 🎬 Record video
   - 🎨 Change palette
   - 📁 Open gallery

### 🌡️ Thermal Settings (NEW in v1.2)

1. Open **Settings** ⚙️
2. Tap **"Thermische Einstellungen"**
3. Adjust:
   - **Emissivity** - Match the material you're measuring
   - **Distance** - Set the distance to your target
   - **Humidity** - Current ambient humidity
   - **Reflected Temperature** - Ambient temperature
4. Tap **"An Kamera senden"** to apply

## 🧠 Object Detection

The app uses a trained TensorFlow Lite model for thermal detection:

| File | Description |
|------|-------------|
| `detect.tflite` | TFLite model (YOLO-based) |
| `labelmap.txt` | Class labels (person, vehicle, etc.) |

The model is specifically trained for thermal images and detects people and vehicles in thermal imagery.

## 🛠️ Development

### Project Structure
```
app/src/main/
├── java/com/noxvision/app/
│   ├── MainActivity.kt       # Main app logic
│   ├── CameraApiClient.kt    # REST API client (NEW)
│   ├── CameraSettings.kt     # Settings persistence
│   ├── DeviceInfo.kt         # Camera detection (NEW)
│   └── ui/theme/             # Compose Theme
├── assets/
│   ├── detect.tflite         # AI model
│   └── labelmap.txt          # Classes
└── res/
    ├── drawable/             # Palette images
    └── values/               # Strings, Colors
```

### Tech Stack
- **Language**: Kotlin 2.0
- **UI**: Jetpack Compose
- **Video**: LibVLC
- **AI**: TensorFlow Lite
- **HTTP**: OkHttp / HttpURLConnection
- **Images**: Coil

### Build Variants
```bash
# Debug (for testing)
./gradlew assembleDebug

# Release (for distribution)
./gradlew assembleRelease
```

## 🤝 Contributing

Contributions are welcome! See [CONTRIBUTING.md](CONTRIBUTING.md) for details.

### Branches
- `main` - Stable version
- `beta` - Development & new features

## 📋 Roadmap

- [x] Settings screen for camera IP
- [x] Support for additional camera models ✅ **v1.2**
- [x] Thermal measurement settings ✅ **v1.2**
- [x] Export to video formats
- [ ] Improved AI model
- [ ] Temperature measurement display
- [ ] Custom color palettes

## ❓ FAQ

**Q: Why this app instead of the official Guide app?**
> The official app has performance issues and missing features. This open-source alternative offers better performance, AI detection, and professional thermal settings.

**Q: Does the app work with other thermal cameras?**
> Yes! NoxVision supports the full Guide Sensmart lineup including C-Series, D-Series, B-Series, TE-Series, and PS-Series cameras.

**Q: What is emissivity and why does it matter?**
> Emissivity is how well a surface emits thermal radiation. Different materials have different values (human skin ≈ 0.95, polished metal ≈ 0.3). Setting it correctly improves temperature accuracy.

**Q: Do I need Android Studio?**
> No! You can build the APK with VS Code and Gradle. See Installation above.

## 📄 License

[MIT License](LICENSE) - You can freely use, modify, and distribute the app.

## 🙏 Credits

- [Guide Sensmart](https://de.guideoutdoor.com) for the hardware
- [LibVLC](https://www.videolan.org/vlc/libvlc.html) for video streaming
- [TensorFlow Lite](https://www.tensorflow.org/lite) for AI inference

---

**⭐ If you like this project, give it a star!**
