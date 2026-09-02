# NoxVision

Android companion app for [Guide Sensmart](https://guideir-thermal.com) thermal cameras. Connects over Wi‑Fi via RTSP streaming and the camera HTTP REST API.

![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg?style=flat-square)
![Android 24+](https://img.shields.io/badge/Android-24%2B-3DDC84.svg?style=flat-square&logo=android)
![Kotlin 2.2.10](https://img.shields.io/badge/Kotlin-2.2.10-7F52FF.svg?style=flat-square&logo=kotlin)
![Version 2.0.0](https://img.shields.io/badge/Version-2.0.0-blue.svg?style=flat-square)

**[Google Play](https://play.google.com/store/apps/details?id=com.noxvision.app)** · [FAQ](docs/FAQ.md) · [Device Reticle Support](docs/device-reticle-support.md) · [Privacy Policy](PRIVACY_POLICY.md) · [Issues](https://github.com/nacl-dev/NoxVision/issues)

![NoxVision live stream preview](docs/preview.gif)

## Overview

NoxVision provides live thermal streaming, camera controls, and field tools for Guide devices. It is an independent application — not affiliated with Guide Infrared or their official app — with its own UI, hunting assistant, on-device object detection, and offline maps.

A focus of the project is **reliable camera connection**: one-tap WiFi pairing, stable RTSP streaming, and reconnect when returning to the app — areas where users often report friction with other Guide companion apps.

The app is available on Google Play. End users should install from the store; GitHub builds are intended for development only.

## Features

**Connection**

- In-app WiFi auto-connect to the camera hotspot (Android 10+; SSID and password stored in Settings → Connection)
- Process-bound to the camera network so streaming and REST control use the same link
- RTSP over TCP with tuned buffering for a stable live view in the field
- Stream resumes automatically when the app returns to the foreground

**Streaming & camera control**

- RTSP live view from Guide camera hotspots (default `192.168.42.1`)
- Palette, brightness, contrast, zoom, audio, and hotspot controls
- Thermal measurement settings (emissivity, distance, humidity, NUC shutter)
- Screenshots and video recording with in-app gallery
- On-device AI object detection (optional)

**Hunting assistant**

- Shot documentation and journal entries
- Weather via OpenWeather API
- Hunting calendar and season tracking
- Offline maps, waypoints, and compass navigation

**App crosshair**

- Stream overlay crosshair (all supported models)
- Hardware device reticle on models that support it — see [Device Reticle Support Guide](docs/device-reticle-support.md)

**Localization:** German, English, French, Spanish, Italian, Dutch, Polish, Ukrainian

## Supported cameras

NoxVision targets Guide Sensmart thermal cameras. Compatibility varies by model and firmware.


| Series | Examples                                          | Notes                                      |
| ------ | ------------------------------------------------- | ------------------------------------------ |
| TE     | TE211M (primary test device), TE211, TE411, TE421 | Monoculars; TE211M has no hardware reticle |
| C      | C400, C640, C800                                  | Handheld                                   |
| D      | D160, D192, D384, D400                            | Industrial modules                         |
| B      | B160, B256, B320                                  | Entry-level modules                        |
| PS     | PS series                                         | Compact models                             |


Cameras that expose RTSP at `192.168.42.1:8554` typically work for streaming. Advanced features depend on the specific model. See the [FAQ](docs/FAQ.md) for details.

## Project status


|              |                                                                                             |
| ------------ | ------------------------------------------------------------------------------------------- |
| Version      | 2.0.0 (`versionCode` 5)                                                                     |
| Distribution | [Google Play](https://play.google.com/store/apps/details?id=com.noxvision.app) (production) |




### Documentation

- [FAQ](docs/FAQ.md) — downloads, compatibility, and troubleshooting
- [Device Reticle Support Guide](docs/device-reticle-support.md)



## Contributing

Bug reports and feature requests: [GitHub Issues](https://github.com/nacl-dev/NoxVision/issues)

Contributions: see [CONTRIBUTING.md](CONTRIBUTING.md)

## License

MIT — see [LICENSE](LICENSE).