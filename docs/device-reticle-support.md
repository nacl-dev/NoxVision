# Device Reticle Support Guide

How to check whether your **Guide Sensmart** thermal camera supports a crosshair on its **physical display**, and how NoxVision uses the camera REST API.

NoxVision is an independent companion app for Guide devices. This guide helps you verify hardware reticle support on your model — it is not tied to any other manufacturer's app.

## Device crosshair vs. app crosshair

| | Device crosshair | App crosshair (NoxVision) |
|---|---|---|
| **Where it appears** | Burned into the camera's own display (OSD) | Overlay on the live stream in NoxVision |
| **Control** | Camera REST API (`/api/v1/peripheral/dash*`) | Settings → Camera → Enable app crosshair |
| **Availability** | Only on supported models | Always available |

Both can be active at the same time. NoxVision shows the **device reticle** section in Settings → Camera only when your connected camera reports support.

## Quick answer: TE211M

The **Guide TE211M** does **not** support a hardware crosshair on its display. It is an observation monocular, not a sighting device. The manufacturer confirms there is no optical crosshair option ([Optics Trade](https://www.optics-trade.eu/us/guide-te211m.html), [Guide product page](https://guideir-thermal.com/products/te-211m)).

**Use the app crosshair** in NoxVision instead (Settings → Camera → Enable app crosshair).

## Why the API may respond but the display stays blank

Some Guide cameras share firmware components. The REST endpoints below may return HTTP 200 and store values even when the **physical display** does not render a reticle.

Example (TE211M):

- API identity: `ZG40C_01_HW_0000_0000` (internal platform ID — not a wrong device)
- Reservation `0000` → no hardware reticle pipeline
- `GET dashtype` returns `{"value":"1"}` but the AMOLED screen shows nothing

If curl succeeds but your **camera's own screen** never changes, your model likely does not support device reticle — use the app overlay.

## How to check your device

### Step 1: Connect to the camera WiFi

Join the camera hotspot from your PC or phone. The camera is usually at **`192.168.42.1`**.

### Step 2: Read device identity

```bash
curl -s http://192.168.42.1/api/v1/misc/deviceinfo | python3 -m json.tool
```

Example TE211M response:

```json
{
    "id": "ZG40C_01_HW_0000_0000",
    "value": "ZG40C"
}
```

- **`value`** — project / platform code (e.g. `ZG40C`, `ZG59`)
- **`id`** — full identifier; the **last segment** is often the reservation code (`0000`, `0004`, …)

Some models also expose richer data at `/api/v1/measure/getDeviceInfo`. NoxVision tries both.

### Step 3: Interpret project code and reservation

| Platform prefix | Example models | Hardware reticle |
|---|---|---|
| **ZG40C** + reservation `0000` | TE211M | **No** |
| **ZG40** + reservation `0001` or `0004` | TB Gen2 (sighting variants) | Often yes |
| **ZG59**, **ZG61**, **ZG67**–**ZG70** | TE411, TE421, etc. | Often yes |
| **ZG18**, **ZG30**, **ZG38**, **ZG45**, **ZG51**, **ZG54**, **ZG63**, **ZG66** | TD, TU, TL, TR, TN, TS, DN series | Often yes |
| **ZG02**, PS, C/D/B modules | Various | Usually no |

NoxVision uses project code and reservation to decide whether to show device reticle controls.

### Step 4: Test reticle endpoints (optional)

```bash
# Current state
curl -s http://192.168.42.1/api/v1/peripheral/dashtype
curl -s http://192.168.42.1/api/v1/peripheral/dashlist

# Set type 2, white, brightness 3 (1-based type index!)
curl -X PUT http://192.168.42.1/api/v1/peripheral/dashtype \
  -H 'Content-Type: application/json' -d '{"value":"2"}'
curl -X PUT http://192.168.42.1/api/v1/peripheral/dashcolor \
  -H 'Content-Type: application/json' -d '{"value":"white"}'
curl -X PUT http://192.168.42.1/api/v1/peripheral/dashlight \
  -H 'Content-Type: application/json' -d '{"value":"3"}'
```

**Important:** Reticle type indices are **1-based** (`"1"` … `"5"`). Sending `"0"` typically returns HTTP **417**.

### Step 5: Verify on the physical display

Watch the **camera's built-in screen** while running the commands above — not the NoxVision stream or a PC preview.

- Values change in GET responses **and** crosshair appears on device → supported
- Values change but display unchanged → likely **not supported** on your hardware revision

## NoxVision behavior

When a supported camera is connected, NoxVision can:

- Load current reticle type, color, and brightness
- Apply changes via `dashtype`, `dashcolor`, `dashlight`

When not supported (e.g. TE211M), only the **app crosshair** is offered.

Implementation: `CameraApiClient.kt`, `DeviceInfo.kt`, `SettingsScreen.kt`, `VideoStreamScreen.kt`.

## Troubleshooting

| Symptom | Likely cause |
|---|---|
| HTTP **417** on `dashtype` | Invalid value (e.g. type `"0"` instead of `"1"`–`"5"`) |
| API OK, no display change | Model has no hardware reticle (common on TE211M) |
| `osdgate` with `"on"` fails | That endpoint controls date/time **watermark**, not the reticle |
| USB connected, no API access | USB is usually for charging; use **WiFi hotspot** for REST |
| TE211M shows `ZG40C` | Normal — internal platform ID, not a different product |

## Contributing device data

If you verify reticle support on a model not listed here, please open an issue with:

1. Model name (e.g. TE421)
2. Output of `GET /api/v1/misc/deviceinfo`
3. Whether the physical display showed a crosshair after PUT commands

This helps improve NoxVision capability detection for all Guide users.
