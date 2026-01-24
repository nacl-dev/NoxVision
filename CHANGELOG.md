# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Planned
- Settings screen for camera IP
- Support for additional camera models
- Improved AI model

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
