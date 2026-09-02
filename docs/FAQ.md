# NoxVision FAQ

Frequently asked questions about NoxVision and Guide thermal cameras.

## Is NoxVision a copy of another app?

**No.** NoxVision is an **independent companion app** for Guide Sensmart thermal cameras. It connects over WiFi using standard RTSP streaming and the camera's HTTP REST API. NoxVision adds its own UI, hunting assistant tools, on-device AI detection, offline maps, and multi-language support — it is not a reverse-engineered clone of any other application.

## Can I show a crosshair on the camera display?

**It depends on your camera model.** NoxVision offers two separate options under **Settings → Camera**:

| | App crosshair | Device crosshair |
|---|---|---|
| **Where** | Overlay on the NoxVision live stream | Camera's physical display (OSD) |
| **TE211M** | Yes — always available | No — not supported |
| **Other models** | Yes — always available | Model-dependent (see below) |

### App crosshair

Works on **all** supported Guide cameras. Enable **Enable app crosshair** in Settings → Camera. This is the recommended option for the **TE211M**.

### Device crosshair (hardware OSD)

The device reticle section is **always visible** in Settings → Camera. How it behaves depends on your connected camera:

**TE211M (and similar models without hardware reticle)**
- The toggle is **grayed out** and cannot be enabled.
- NoxVision shows: *Not supported on TE211M — no hardware crosshair. Use the app crosshair above.*
- This model is an observation monocular — the REST API may respond, but nothing appears on the camera display.

**Officially supported models** (e.g. TE411, TE421, TB Gen2 sighting variants, many ZG59/ZG67 series)
- The toggle becomes **active** once the camera is connected.
- NoxVision shows an **experimental** notice: *Not tested on this model. Verify on the camera display.*
- You can set reticle type, color, and brightness — but NoxVision has **not** fully verified these controls on every supported model. Always check the **physical camera screen**, not just the app stream.

**Not connected**
- The toggle stays disabled until you start a live stream to the camera.
- Connect first, then check whether your model is supported or grayed out.

See the [Device Reticle Support Guide](device-reticle-support.md) for step-by-step checks with curl.

## What is the difference between app crosshair and device crosshair?

| | App crosshair | Device crosshair |
|---|---|---|
| **Where** | Overlay on the NoxVision live stream | Camera's physical display |
| **TE211M** | Yes | No (grayed out in settings) |
| **Supported models** | Yes | Yes, when connected (experimental) |
| **Settings path** | Settings → Camera → Enable app crosshair | Settings → Camera → Enable device reticle |

Use the **app crosshair** when you want a crosshair on the phone screen. Use the **device reticle** only if your camera model supports hardware OSD and you need the crosshair on the camera's own display.

## Which cameras does NoxVision support?

NoxVision is built for **Guide Sensmart** thermal cameras, including:

| Series | Examples | Notes |
|---|---|---|
| **TE** | TE211M (primary tested), TE211, TE411, TE421 | Monoculars; TE211M most tested |
| **C** | C400, C640, C800 | Full-featured handheld |
| **D** | D160, D192, D384, D400 | Industrial modules |
| **B** | B160, B256, B320 | Budget modules |
| **PS** | PS series | Compact models |
| **TB** | TB Gen2 | Some variants support device reticle |

Any Guide camera that exposes RTSP at **`192.168.42.1:8554`** (default) will typically work for **live streaming**. Advanced features (focus, radiometry, device reticle) depend on the specific model.

## My TE211M reports `ZG40C` in the API — is that wrong?

**No, that's normal.** Guide cameras expose an internal platform ID (e.g. `ZG40C`) that may differ from the marketing name (TE211M). Your device is still a TE211M if that's what's printed on the housing and what the official Guide app shows.

## How do I connect NoxVision to my camera?

1. Power on the camera and enable its WiFi hotspot.
2. In NoxVision, open **Settings → Connection** and enter the camera SSID and password (from the sticker on the device).
3. Enable **Auto connect** if you want the app to join the hotspot when starting a stream.
4. Tap play on the live stream screen — the app connects to `192.168.42.1` by default (IP configurable in Settings → Connection).

### Why is connection a focus in NoxVision?

Reliable pairing and a stable stream matter in the field. NoxVision is built around that workflow:

- **WiFi auto-connect** — on Android 10+, the app can join the camera hotspot without leaving NoxVision or switching manually in system settings.
- **Dedicated camera network** — traffic is routed to the camera WiFi link so RTSP and REST control stay on the same connection.
- **Stable streaming** — RTSP over TCP with adjusted buffering, rather than a fragile default setup.
- **Resume on return** — if you leave the app briefly, the stream reconnects when you come back.

This is one of the most noticeable improvements over the official Guide app for many users, especially on TE-series monoculars. You still configure SSID and password once; after that, connecting is typically a single tap.

## Does NoxVision work over USB?

USB on most Guide monoculars (including TE211M) is primarily for **charging**. Live control and streaming use **WiFi**. Connecting the camera to a PC via USB does not replace the WiFi connection for NoxVision.

## Where is the Hunting Assistant?

The **Hunting & Field Assistant** (hunt log, weather, maps, waypoints, and more) is an **optional feature** and is **disabled by default**.

There is **no Hunting Assistant button on the home screen** anymore. To use it:

1. Open **Settings** (gear icon on the live stream screen)
2. Go to **App Features**
3. Turn on **Enable Hunting Assistant**
4. Tap the **Hunting Assistant** card to open the hub
5. Optionally set your **country profile** for hunting season data

### The app says hunting features are available, but I can't find them / a button is grayed out

If the [Play Store listing](https://play.google.com/store/apps/details?id=com.noxvision.app) or in-app text mentions hunting tools but you don't see a button on the main screen, that's expected: the feature must be **enabled first** under **Settings → App Features**.

After you turn on **Enable Hunting Assistant**, the hub card and country profile appear in that same section. Until then, hunting features are simply not available — not because your camera is unsupported.

**New installation:** Hunting Assistant is off by default. You can also opt in during the first-launch welcome dialog.

**Existing users** who had the feature enabled before this change keep it active, but open it only through **Settings → App Features** (no longer from the home screen).

## Where can I download NoxVision?

NoxVision is available on [Google Play](https://play.google.com/store/apps/details?id=com.noxvision.app).

Install or update the app from the Play Store using the same Google account you use on your Android device.

## Where can I report bugs or request features?

[GitHub Issues](https://github.com/nacl-dev/NoxVision/issues)

When reporting a bug, please include your device model, Android version, app version, and steps to reproduce the issue.

## Related documentation

- [Device Reticle Support Guide](device-reticle-support.md) — verify hardware crosshair support
- [README](../README.md) — features and build instructions
