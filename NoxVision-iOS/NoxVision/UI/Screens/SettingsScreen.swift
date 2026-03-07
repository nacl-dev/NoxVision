import SwiftUI

struct SettingsScreen: View {
    @Binding var isPresented: Bool
    @EnvironmentObject var settings: CameraSettingsStore
    @State private var currentPage: SettingsPage = .main
    @State private var showLanguageDialog = false
    @State private var showLogDialog = false
    @State private var showAboutDialog = false
    @State private var showWhatsNewDialog = false
    @State private var showThermalSettings = false

    enum SettingsPage {
        case main, connection, camera, appFeatures
    }

    var body: some View {
        ZStack {
            NoxColors.background.ignoresSafeArea()

            VStack(spacing: 0) {
                NoxNavigationHeader(
                    title: NSLocalizedString("settings", comment: ""),
                    onBack: {
                        if currentPage == .main {
                            isPresented = false
                        } else {
                            currentPage = .main
                        }
                    }
                )

                ScrollView {
                    VStack(spacing: 12) {
                        switch currentPage {
                        case .main:
                            mainPage
                        case .connection:
                            connectionPage
                        case .camera:
                            cameraPage
                        case .appFeatures:
                            appFeaturesPage
                        }
                    }
                    .padding(.horizontal, 16)
                    .padding(.bottom, 32)
                }
            }
        }
        .sheet(isPresented: $showLanguageDialog) {
            LanguageDialog(isPresented: $showLanguageDialog)
        }
        .sheet(isPresented: $showAboutDialog) {
            AboutDialog(isPresented: $showAboutDialog)
        }
        .sheet(isPresented: $showWhatsNewDialog) {
            WhatsNewDialog(isPresented: $showWhatsNewDialog)
        }
        .sheet(isPresented: $showLogDialog) {
            LogDialog(isPresented: $showLogDialog)
        }
    }

    // MARK: - Main Page
    private var mainPage: some View {
        VStack(spacing: 12) {
            NoxFeatureCard(
                icon: "wifi",
                title: NSLocalizedString("connection", comment: ""),
                subtitle: NSLocalizedString("connection_subtitle", comment: ""),
                iconColor: NoxColors.primary
            ) { currentPage = .connection }

            NoxFeatureCard(
                icon: "camera",
                title: NSLocalizedString("camera", comment: ""),
                subtitle: NSLocalizedString("camera_subtitle", comment: ""),
                iconColor: NoxColors.tertiary
            ) { currentPage = .camera }

            NoxFeatureCard(
                icon: "thermometer",
                title: NSLocalizedString("thermal_settings", comment: ""),
                subtitle: NSLocalizedString("thermal_settings_subtitle", comment: ""),
                iconColor: .orange
            ) { showThermalSettings = true }

            NoxFeatureCard(
                icon: "sparkles",
                title: NSLocalizedString("app_features", comment: ""),
                subtitle: NSLocalizedString("app_features_subtitle", comment: ""),
                iconColor: NoxColors.info
            ) { currentPage = .appFeatures }

            NoxFeatureCard(
                icon: "globe",
                title: NSLocalizedString("language", comment: ""),
                subtitle: NSLocalizedString("language_subtitle", comment: ""),
                iconColor: .purple
            ) { showLanguageDialog = true }

            NoxSectionHeader(title: NSLocalizedString("system_info", comment: ""))

            HStack(spacing: 12) {
                Button(action: { showLogDialog = true }) {
                    Label(NSLocalizedString("system_log", comment: ""), systemImage: "doc.text")
                        .font(.system(size: 13))
                        .foregroundColor(NoxColors.onSurfaceVariant)
                        .frame(maxWidth: .infinity)
                        .padding(12)
                        .background(NoxColors.cardBackground)
                        .cornerRadius(12)
                }

                Button(action: { showWhatsNewDialog = true }) {
                    Label(NSLocalizedString("whats_new", comment: ""), systemImage: "sparkles")
                        .font(.system(size: 13))
                        .foregroundColor(NoxColors.onSurfaceVariant)
                        .frame(maxWidth: .infinity)
                        .padding(12)
                        .background(NoxColors.cardBackground)
                        .cornerRadius(12)
                }

                Button(action: { showAboutDialog = true }) {
                    Label(NSLocalizedString("about_noxvision", comment: ""), systemImage: "info.circle")
                        .font(.system(size: 13))
                        .foregroundColor(NoxColors.onSurfaceVariant)
                        .frame(maxWidth: .infinity)
                        .padding(12)
                        .background(NoxColors.cardBackground)
                        .cornerRadius(12)
                }
            }
        }
        .fullScreenCover(isPresented: $showThermalSettings) {
            ThermalSettingsScreen(isPresented: $showThermalSettings)
        }
    }

    // MARK: - Connection Page
    private var connectionPage: some View {
        VStack(spacing: 16) {
            NoxSectionHeader(title: NSLocalizedString("camera_address", comment: ""))

            VStack(spacing: 8) {
                HStack {
                    Text(NSLocalizedString("ip_address", comment: ""))
                        .font(.system(size: 14))
                        .foregroundColor(NoxColors.onSurfaceVariant)
                    Spacer()
                }
                TextField("192.168.42.1", text: $settings.cameraIp)
                    .textFieldStyle(.plain)
                    .font(.system(size: 15, design: .monospaced))
                    .foregroundColor(NoxColors.onSurface)
                    .padding(12)
                    .background(NoxColors.surfaceVariant)
                    .cornerRadius(12)
                    .keyboardType(.decimalPad)

                if !CameraSettingsStore.isValidIP(settings.cameraIp) && !settings.cameraIp.isEmpty {
                    Text(NSLocalizedString("invalid_ip", comment: ""))
                        .font(.system(size: 12))
                        .foregroundColor(NoxColors.error)
                }

                Button(action: { settings.cameraIp = CameraSettingsStore.defaultIP }) {
                    Text(String(format: NSLocalizedString("reset_to_default", comment: ""), CameraSettingsStore.defaultIP))
                        .font(.system(size: 12))
                        .foregroundColor(NoxColors.primary)
                }
            }
            .noxCard()

            VStack(spacing: 8) {
                HStack {
                    Text(NSLocalizedString("http_api_port", comment: ""))
                        .font(.system(size: 14))
                        .foregroundColor(NoxColors.onSurfaceVariant)
                    Spacer()
                }
                TextField("80", value: $settings.httpPort, format: .number)
                    .textFieldStyle(.plain)
                    .font(.system(size: 15, design: .monospaced))
                    .foregroundColor(NoxColors.onSurface)
                    .padding(12)
                    .background(NoxColors.surfaceVariant)
                    .cornerRadius(12)
                    .keyboardType(.numberPad)
            }
            .noxCard()

            NoxSectionHeader(title: NSLocalizedString("wifi_auto_connect", comment: ""))

            VStack(spacing: 12) {
                NoxToggleRow(title: NSLocalizedString("auto_connect", comment: ""), isOn: $settings.autoConnectEnabled)

                VStack(spacing: 8) {
                    HStack {
                        Text(NSLocalizedString("wifi_ssid", comment: ""))
                            .font(.system(size: 14))
                            .foregroundColor(NoxColors.onSurfaceVariant)
                        Spacer()
                    }
                    TextField(NSLocalizedString("wifi_ssid_hint", comment: ""), text: $settings.wifiSSID)
                        .textFieldStyle(.plain)
                        .font(.system(size: 15))
                        .foregroundColor(NoxColors.onSurface)
                        .padding(12)
                        .background(NoxColors.surfaceVariant)
                        .cornerRadius(12)
                }

                VStack(spacing: 8) {
                    HStack {
                        Text(NSLocalizedString("wifi_password", comment: ""))
                            .font(.system(size: 14))
                            .foregroundColor(NoxColors.onSurfaceVariant)
                        Spacer()
                    }
                    SecureField(NSLocalizedString("wifi_password_hint", comment: ""), text: $settings.wifiPassword)
                        .textFieldStyle(.plain)
                        .font(.system(size: 15))
                        .foregroundColor(NoxColors.onSurface)
                        .padding(12)
                        .background(NoxColors.surfaceVariant)
                        .cornerRadius(12)
                }
            }
            .noxCard()
        }
    }

    // MARK: - Camera Page
    private var cameraPage: some View {
        VStack(spacing: 16) {
            NoxSectionHeader(title: NSLocalizedString("audio_display", comment: ""))

            VStack(spacing: 8) {
                NoxToggleRow(title: NSLocalizedString("enable_audio", comment: ""), isOn: $settings.audioEnabled)
                NoxToggleRow(title: NSLocalizedString("show_hotspot", comment: ""), isOn: $settings.hotspotEnabled)
            }
            .noxCard()

            NoxSectionHeader(title: NSLocalizedString("crosshair_settings", comment: ""))

            VStack(spacing: 12) {
                NoxToggleRow(title: NSLocalizedString("enable_crosshair", comment: ""), isOn: $settings.crosshairEnabled)

                if settings.crosshairEnabled {
                    VStack(alignment: .leading, spacing: 8) {
                        Text(NSLocalizedString("crosshair_style", comment: ""))
                            .font(.system(size: 14))
                            .foregroundColor(NoxColors.onSurfaceVariant)

                        HStack(spacing: 8) {
                            ForEach(CrosshairStyle.allCases) { style in
                                NoxChip(
                                    label: style.displayName,
                                    isSelected: settings.crosshairStyle == style
                                ) {
                                    settings.crosshairStyle = style
                                }
                            }
                        }
                    }
                }
            }
            .noxCard()

            NoxSectionHeader(title: NSLocalizedString("image_settings", comment: ""))

            VStack(spacing: 12) {
                VStack(alignment: .leading, spacing: 4) {
                    Text(NSLocalizedString("brightness", comment: ""))
                        .font(.system(size: 14))
                        .foregroundColor(NoxColors.onSurfaceVariant)
                    HStack(spacing: 8) {
                        ForEach([-2, -1, 0, 1, 2], id: \.self) { val in
                            NoxChip(label: val > 0 ? "+\(val)" : "\(val)", isSelected: val == 0) {}
                        }
                    }
                }

                VStack(alignment: .leading, spacing: 4) {
                    Text(NSLocalizedString("contrast", comment: ""))
                        .font(.system(size: 14))
                        .foregroundColor(NoxColors.onSurfaceVariant)
                    HStack(spacing: 8) {
                        ForEach([-2, -1, 0, 1, 2], id: \.self) { val in
                            NoxChip(label: val > 0 ? "+\(val)" : "\(val)", isSelected: val == 0) {}
                        }
                    }
                }
            }
            .noxCard()
        }
    }

    // MARK: - App Features Page
    private var appFeaturesPage: some View {
        VStack(spacing: 16) {
            NoxSectionHeader(title: NSLocalizedString("hunting", comment: ""))

            VStack(spacing: 12) {
                NoxToggleRow(
                    title: NSLocalizedString("hunting_assistant", comment: ""),
                    isOn: .constant(true),
                    subtitle: NSLocalizedString("hunting_assistant_subtitle", comment: "")
                )

                NoxToggleRow(
                    title: NSLocalizedString("show_hunting_assistant_home", comment: ""),
                    isOn: $settings.huntingAssistantHomeEnabled
                )

                VStack(alignment: .leading, spacing: 8) {
                    Text(NSLocalizedString("hunting_country", comment: ""))
                        .font(.system(size: 14))
                        .foregroundColor(NoxColors.onSurfaceVariant)

                    Picker("", selection: $settings.huntingCountry) {
                        ForEach(HuntingAssistantCountry.allCases) { country in
                            Text(country.displayName).tag(country)
                        }
                    }
                    .pickerStyle(.menu)
                    .tint(NoxColors.primary)
                }
            }
            .noxCard()

            NoxSectionHeader(title: NSLocalizedString("ai_features", comment: ""))

            VStack(spacing: 8) {
                NoxToggleRow(
                    title: NSLocalizedString("ai_object_detection", comment: ""),
                    isOn: $settings.aiDetectionEnabled,
                    subtitle: NSLocalizedString("ai_detection_desc", comment: "")
                )
            }
            .noxCard()

            NoxSectionHeader(title: NSLocalizedString("image_enhancement", comment: ""))

            VStack(spacing: 8) {
                NoxToggleRow(
                    title: NSLocalizedString("enable_image_enhancement", comment: ""),
                    isOn: $settings.imageEnhancementEnabled,
                    subtitle: NSLocalizedString("image_enhancement_desc", comment: "")
                )
            }
            .noxCard()
        }
    }
}
