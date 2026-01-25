# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Planned
- Improved AI model
- Temperature measurement display overlay
- Custom color palette support

## [1.2.0] - 2026-01-25

### Added
- 🌡️ **Thermal Measurement Settings Dialog**
  - Emissivity control (0.01-1.0) with material presets (Skin/Fabric, Painted, Metal)
  - Distance compensation slider (0-100m)
  - Humidity settings (0-100%)
  - Reflected temperature adjustment (-40°C to 100°C)
- 🔄 **Shutter/NUC Calibration Button** - Manual sensor calibration
- 📱 **Automatic Camera Detection** - Identifies connected camera model
- 🔌 **REST API Client** (`CameraApiClient.kt`) - Full integration with Guide camera API
- 📊 **Device Info Display** - Shows camera model, resolution, and available features
- 💾 **Settings Persistence** - Thermal settings are saved locally

### Changed
- Extended camera support from TE-Series only to full Guide Sensmart lineup
- Updated Settings dialog with new "Thermische Einstellungen" button
- Improved project structure with separate files for API client and device info

### Supported Camera Models (NEW)
- **C-Series**: C640, C400, C800 (Focus, GPS, Radiometry, Audio)
- **D-Series**: D400, D384, D192, D160 (Focus, Radiometry)
- **B-Series**: B320, B256, B160 (Radiometry)
- **TE-Series**: TE211M, TE-Mini (Radiometry)
- **PS-Series**: PS600, PS400 (Radiometry)

### Technical
- New files: `CameraApiClient.kt`, `DeviceInfo.kt`
- Extended `CameraSettings.kt` with thermal measurement persistence
- REST API endpoints: `/api/v1/measure/*`

## [1.1.0] - 2026-01-24

### Added
- Settings screen with configurable camera IP address
- System log viewer for debugging
- About dialog with app information

### Changed
- Improved connection stability
- Better error handling for WiFi connection

## [1.0.0] - 2026-01-24

### Added
- Live thermal video stream via RTSP (LibVLC)
- YOLO object detection with TensorFlow Lite
- Multiple thermal palettes (Whitehot, Blackhot, Bluehot, Greenhot, Ironred, etc.)
- Screenshot function with gallery storage
- Video recording function
- Integrated media gallery
- Auto-WiFi connect to Guide TE-Series camera
- Real-time detection overlay with bounding boxes
- Dark theme for night use

### Supported Hardware
- Guide TE211M (primarily tested)
- Other Guide TE-Series cameras (compatible)

### Technical Details
- Android 7.0+ (API 24)
- arm64-v8a architecture
- Jetpack Compose UI
- TensorFlow Lite for AI inference
