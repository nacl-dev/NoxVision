import SwiftUI

struct ThermalSettingsScreen: View {
    @Binding var isPresented: Bool
    @EnvironmentObject var settings: CameraSettingsStore
    @State private var isCalibrating = false
    @State private var localEmissivity: Float = 0.95
    @State private var localDistance: Float = 1.0
    @State private var localHumidity: Float = 50.0
    @State private var localReflectTemp: Float = 23.0

    var body: some View {
        ZStack {
            NoxColors.background.ignoresSafeArea()

            VStack(spacing: 0) {
                NoxNavigationHeader(
                    title: NSLocalizedString("thermal_settings", comment: ""),
                    onBack: { isPresented = false }
                )

                ScrollView {
                    VStack(spacing: 16) {
                        // Device info
                        if let device = settings.cachedDeviceInfo {
                            deviceInfoSection(device)
                        }

                        // Shutter / NUC
                        shutterSection

                        // Emissivity
                        emissivitySection

                        // Distance
                        distanceSection

                        // Humidity
                        humiditySection

                        // Reflect temperature
                        reflectTempSection

                        // Apply button
                        Button(action: applySettings) {
                            Text(NSLocalizedString("apply_settings", comment: ""))
                                .font(.system(size: 16, weight: .semibold))
                                .foregroundColor(NoxColors.onPrimary)
                                .frame(maxWidth: .infinity)
                                .padding(.vertical, 14)
                                .background(NoxColors.primary)
                                .cornerRadius(12)
                        }
                        .padding(.top, 8)
                    }
                    .padding(.horizontal, 16)
                    .padding(.bottom, 32)
                }
            }
        }
        .onAppear {
            localEmissivity = settings.emissivity
            localDistance = settings.distance
            localHumidity = settings.humidity
            localReflectTemp = settings.reflectTemperature
        }
    }

    private func deviceInfoSection(_ device: DeviceInfo) -> some View {
        VStack(spacing: 8) {
            NoxSectionHeader(title: NSLocalizedString("camera_features", comment: ""))
            let caps = device.capabilities
            HStack(spacing: 16) {
                featureBadge(NSLocalizedString("radiometry", comment: ""), enabled: caps.hasRadiometry)
                featureBadge(NSLocalizedString("focus", comment: ""), enabled: caps.hasFocus)
                featureBadge(NSLocalizedString("gps", comment: ""), enabled: caps.hasGps)
            }
            Text("\(device.videoWidth)x\(device.videoHeight) @ \(device.videoFps)fps")
                .font(.system(size: 12, design: .monospaced))
                .foregroundColor(NoxColors.onSurfaceVariant)
        }
        .noxCard()
    }

    private func featureBadge(_ label: String, enabled: Bool) -> some View {
        HStack(spacing: 4) {
            Image(systemName: enabled ? "checkmark.circle.fill" : "xmark.circle")
                .font(.system(size: 12))
                .foregroundColor(enabled ? NoxColors.success : NoxColors.outline)
            Text(label)
                .font(.system(size: 12))
                .foregroundColor(enabled ? NoxColors.onSurface : NoxColors.outline)
        }
    }

    private var shutterSection: some View {
        VStack(spacing: 8) {
            NoxSectionHeader(title: NSLocalizedString("shutter_nuc", comment: ""))
            Button(action: triggerShutter) {
                HStack {
                    Image(systemName: "camera.shutter.button")
                    Text(isCalibrating ? NSLocalizedString("calibrating", comment: "") : NSLocalizedString("shutter_nuc", comment: ""))
                }
                .font(.system(size: 14, weight: .medium))
                .foregroundColor(NoxColors.onPrimary)
                .frame(maxWidth: .infinity)
                .padding(.vertical, 12)
                .background(isCalibrating ? NoxColors.outline : NoxColors.primary)
                .cornerRadius(12)
            }
            .disabled(isCalibrating)
        }
        .noxCard()
    }

    private var emissivitySection: some View {
        VStack(spacing: 8) {
            HStack {
                Text(String(format: NSLocalizedString("emissivity", comment: ""), localEmissivity))
                    .font(.system(size: 14))
                    .foregroundColor(NoxColors.onSurface)
                Spacer()
            }

            Slider(value: $localEmissivity, in: 0.01...1.0, step: 0.01)
                .tint(NoxColors.primary)

            HStack(spacing: 8) {
                Text(NSLocalizedString("presets", comment: ""))
                    .font(.system(size: 12))
                    .foregroundColor(NoxColors.onSurfaceVariant)
                Spacer()
                ForEach([
                    (NSLocalizedString("skin", comment: ""), Float(0.95)),
                    (NSLocalizedString("wood", comment: ""), Float(0.90)),
                    (NSLocalizedString("steel", comment: ""), Float(0.60)),
                    (NSLocalizedString("aluminum", comment: ""), Float(0.30))
                ], id: \.0) { preset in
                    NoxChip(
                        label: preset.0,
                        isSelected: abs(localEmissivity - preset.1) < 0.01
                    ) {
                        localEmissivity = preset.1
                    }
                }
            }
        }
        .noxCard()
    }

    private var distanceSection: some View {
        VStack(spacing: 8) {
            HStack {
                Text(String(format: NSLocalizedString("distance", comment: ""), localDistance))
                    .font(.system(size: 14))
                    .foregroundColor(NoxColors.onSurface)
                Spacer()
            }
            Slider(value: $localDistance, in: 1...100, step: 0.5)
                .tint(NoxColors.primary)
        }
        .noxCard()
    }

    private var humiditySection: some View {
        VStack(spacing: 8) {
            HStack {
                Text(String(format: NSLocalizedString("humidity", comment: ""), localHumidity))
                    .font(.system(size: 14))
                    .foregroundColor(NoxColors.onSurface)
                Spacer()
            }
            Slider(value: $localHumidity, in: 0...100, step: 1)
                .tint(NoxColors.primary)
        }
        .noxCard()
    }

    private var reflectTempSection: some View {
        VStack(spacing: 8) {
            HStack {
                Text(String(format: NSLocalizedString("reflect_temperature", comment: ""), localReflectTemp))
                    .font(.system(size: 14))
                    .foregroundColor(NoxColors.onSurface)
                Spacer()
            }
            Slider(value: $localReflectTemp, in: -20...120, step: 0.5)
                .tint(NoxColors.primary)
        }
        .noxCard()
    }

    private func triggerShutter() {
        isCalibrating = true
        Task {
            let client = CameraApiClient(baseUrl: settings.baseUrl)
            _ = await client.triggerShutter()
            try? await Task.sleep(nanoseconds: 2_000_000_000)
            await MainActor.run { isCalibrating = false }
        }
    }

    private func applySettings() {
        settings.emissivity = localEmissivity
        settings.distance = localDistance
        settings.humidity = localHumidity
        settings.reflectTemperature = localReflectTemp

        Task {
            let client = CameraApiClient(baseUrl: settings.baseUrl)
            _ = await client.setEmission(localEmissivity)
            _ = await client.setDistance(localDistance)
            _ = await client.setHumidity(localHumidity)
            _ = await client.setReflectTemperature(localReflectTemp)
        }
    }
}
