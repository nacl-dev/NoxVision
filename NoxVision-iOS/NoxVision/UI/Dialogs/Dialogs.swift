import SwiftUI

// MARK: - Welcome Dialog
struct WelcomeDialog: View {
    @Binding var isPresented: Bool
    @EnvironmentObject var settings: CameraSettingsStore
    @State private var page = 0

    var body: some View {
        ZStack {
            NoxColors.background.ignoresSafeArea()

            VStack(spacing: 24) {
                // Page indicator
                HStack(spacing: 8) {
                    ForEach(0..<4) { i in
                        Circle()
                            .fill(i == page ? NoxColors.primary : NoxColors.outline)
                            .frame(width: 8, height: 8)
                    }
                }
                .padding(.top, 32)

                Spacer()

                switch page {
                case 0:
                    welcomeIntro
                case 1:
                    setupConnection
                case 2:
                    importantFeatures
                default:
                    readyPage
                }

                Spacer()

                // Navigation buttons
                HStack {
                    if page > 0 {
                        Button(NSLocalizedString("back", comment: "")) { page -= 1 }
                            .foregroundColor(NoxColors.onSurfaceVariant)
                    }
                    Spacer()
                    Button(page < 3 ? NSLocalizedString("next", comment: "") : NSLocalizedString("start", comment: "")) {
                        if page < 3 {
                            page += 1
                        } else {
                            settings.setFirstRunCompleted()
                            isPresented = false
                        }
                    }
                    .buttonStyle(NoxButtonStyle())
                }
                .padding(.horizontal, 24)
                .padding(.bottom, 32)
            }
        }
    }

    private var welcomeIntro: some View {
        VStack(spacing: 16) {
            Image(systemName: "eye")
                .font(.system(size: 64))
                .foregroundColor(NoxColors.primary)
            Text(NSLocalizedString("welcome_title", comment: ""))
                .font(.system(size: 24, weight: .bold))
                .foregroundColor(NoxColors.onSurface)
            Text(NSLocalizedString("welcome_intro", comment: ""))
                .font(.system(size: 15))
                .foregroundColor(NoxColors.onSurfaceVariant)
                .multilineTextAlignment(.center)
                .padding(.horizontal, 32)
        }
    }

    private var setupConnection: some View {
        VStack(spacing: 16) {
            Image(systemName: "wifi")
                .font(.system(size: 48))
                .foregroundColor(NoxColors.primary)
            Text(NSLocalizedString("setup_connection", comment: ""))
                .font(.system(size: 20, weight: .semibold))
                .foregroundColor(NoxColors.onSurface)
            Text(NSLocalizedString("welcome_wifi", comment: ""))
                .font(.system(size: 14))
                .foregroundColor(NoxColors.onSurfaceVariant)
                .multilineTextAlignment(.center)
                .padding(.horizontal, 32)
            VStack(alignment: .leading, spacing: 8) {
                Text(NSLocalizedString("default_values", comment: ""))
                    .font(.system(size: 13, weight: .medium))
                    .foregroundColor(NoxColors.onSurface)
                Text(NSLocalizedString("ssid_hint", comment: ""))
                    .font(.system(size: 13, design: .monospaced))
                    .foregroundColor(NoxColors.onSurfaceVariant)
                Text(NSLocalizedString("password_hint", comment: ""))
                    .font(.system(size: 13, design: .monospaced))
                    .foregroundColor(NoxColors.onSurfaceVariant)
            }
            .padding(16)
            .background(NoxColors.surfaceVariant)
            .cornerRadius(12)
            .padding(.horizontal, 32)
        }
    }

    private var importantFeatures: some View {
        VStack(spacing: 16) {
            Text(NSLocalizedString("important_features", comment: ""))
                .font(.system(size: 20, weight: .semibold))
                .foregroundColor(NoxColors.onSurface)

            VStack(spacing: 12) {
                featureRow(icon: "photo.on.rectangle", title: NSLocalizedString("gallery", comment: ""),
                          desc: NSLocalizedString("feature_gallery_desc", comment: ""))
                featureRow(icon: "thermometer", title: NSLocalizedString("feature_measurements", comment: ""),
                          desc: NSLocalizedString("feature_measurements_desc", comment: ""))
                featureRow(icon: "wifi", title: NSLocalizedString("feature_auto_connect", comment: ""),
                          desc: NSLocalizedString("feature_auto_connect_desc", comment: ""))
                featureRow(icon: "video.fill", title: NSLocalizedString("feature_recording", comment: ""),
                          desc: NSLocalizedString("feature_recording_desc", comment: ""))
            }
            .padding(.horizontal, 24)
        }
    }

    private var readyPage: some View {
        VStack(spacing: 16) {
            Image(systemName: "checkmark.circle.fill")
                .font(.system(size: 64))
                .foregroundColor(NoxColors.success)
            Text(NSLocalizedString("ready", comment: ""))
                .font(.system(size: 24, weight: .bold))
                .foregroundColor(NoxColors.onSurface)
            Text(NSLocalizedString("welcome_outro", comment: ""))
                .font(.system(size: 14))
                .foregroundColor(NoxColors.onSurfaceVariant)
                .multilineTextAlignment(.center)
                .padding(.horizontal, 32)
        }
    }

    private func featureRow(icon: String, title: String, desc: String) -> some View {
        HStack(spacing: 12) {
            Image(systemName: icon)
                .font(.system(size: 20))
                .foregroundColor(NoxColors.primary)
                .frame(width: 36, height: 36)
            VStack(alignment: .leading, spacing: 2) {
                Text(title)
                    .font(.system(size: 14, weight: .medium))
                    .foregroundColor(NoxColors.onSurface)
                Text(desc)
                    .font(.system(size: 12))
                    .foregroundColor(NoxColors.onSurfaceVariant)
            }
            Spacer()
        }
    }
}

// MARK: - What's New Dialog
struct WhatsNewDialog: View {
    @Binding var isPresented: Bool

    var body: some View {
        NavigationView {
            ScrollView {
                VStack(spacing: 12) {
                    ForEach(WhatsNewRepository.features) { note in
                        HStack(spacing: 12) {
                            Image(systemName: "sparkle")
                                .foregroundColor(NoxColors.primary)
                            VStack(alignment: .leading, spacing: 4) {
                                Text(note.title)
                                    .font(.system(size: 15, weight: .medium))
                                    .foregroundColor(NoxColors.onSurface)
                                Text(note.description)
                                    .font(.system(size: 13))
                                    .foregroundColor(NoxColors.onSurfaceVariant)
                            }
                            Spacer()
                        }
                        .padding(12)
                        .background(NoxColors.cardBackground)
                        .cornerRadius(12)
                    }
                }
                .padding(16)
            }
            .background(NoxColors.background)
            .navigationTitle(NSLocalizedString("whats_new_title", comment: ""))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .confirmationAction) {
                    Button(NSLocalizedString("close", comment: "")) {
                        isPresented = false
                    }
                    .foregroundColor(NoxColors.primary)
                }
            }
        }
    }
}

// MARK: - About Dialog
struct AboutDialog: View {
    @Binding var isPresented: Bool
    private let version = Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String ?? "1.3.0"

    var body: some View {
        NavigationView {
            ScrollView {
                VStack(spacing: 16) {
                    // App icon placeholder
                    Image(systemName: "eye")
                        .font(.system(size: 48))
                        .foregroundColor(NoxColors.primary)
                        .padding(.top, 16)

                    Text("NoxVision")
                        .font(.system(size: 24, weight: .bold))
                        .foregroundColor(NoxColors.onSurface)

                    Text(String(format: NSLocalizedString("version", comment: ""), version))
                        .font(.system(size: 14))
                        .foregroundColor(NoxColors.onSurfaceVariant)

                    Text(NSLocalizedString("about_description", comment: ""))
                        .font(.system(size: 14))
                        .foregroundColor(NoxColors.onSurfaceVariant)
                        .multilineTextAlignment(.center)
                        .padding(.horizontal, 24)

                    Text(NSLocalizedString("about_long_description", comment: ""))
                        .font(.system(size: 13))
                        .foregroundColor(NoxColors.onSurfaceVariant)
                        .multilineTextAlignment(.center)
                        .padding(.horizontal, 24)

                    Divider().background(NoxColors.outlineVariant)

                    NoxSectionHeader(title: NSLocalizedString("features", comment: ""))

                    VStack(alignment: .leading, spacing: 8) {
                        featureLine(NSLocalizedString("feature_stream", comment: ""))
                        featureLine(NSLocalizedString("feature_ai", comment: ""))
                        featureLine(NSLocalizedString("feature_palettes", comment: ""))
                        featureLine(NSLocalizedString("feature_capture", comment: ""))
                        featureLine(NSLocalizedString("feature_gallery", comment: ""))
                        featureLine(NSLocalizedString("feature_wifi", comment: ""))
                        featureLine(NSLocalizedString("feature_night", comment: ""))
                    }
                    .padding(.horizontal, 24)
                }
                .padding(.bottom, 32)
            }
            .background(NoxColors.background)
            .navigationTitle(NSLocalizedString("about_noxvision", comment: ""))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .confirmationAction) {
                    Button(NSLocalizedString("close", comment: "")) {
                        isPresented = false
                    }
                    .foregroundColor(NoxColors.primary)
                }
            }
        }
    }

    private func featureLine(_ text: String) -> some View {
        HStack(spacing: 8) {
            Image(systemName: "checkmark")
                .font(.system(size: 12))
                .foregroundColor(NoxColors.primary)
            Text(text)
                .font(.system(size: 13))
                .foregroundColor(NoxColors.onSurface)
        }
    }
}

// MARK: - Language Dialog
struct LanguageDialog: View {
    @Binding var isPresented: Bool

    private let languages: [(String, String, String)] = [
        ("system", "Systemsprache", "globe"),
        ("de", "Deutsch", "flag"),
        ("en", "English", "flag"),
        ("fr", "Français", "flag"),
        ("es", "Español", "flag"),
        ("it", "Italiano", "flag"),
        ("nl", "Nederlands", "flag"),
        ("pl", "Polski", "flag"),
        ("uk", "Українська", "flag"),
    ]

    var body: some View {
        NavigationView {
            List {
                ForEach(languages, id: \.0) { lang in
                    Button(action: {
                        // iOS handles language via system settings
                        // This opens the app's language settings
                        if let url = URL(string: UIApplication.openSettingsURLString) {
                            UIApplication.shared.open(url)
                        }
                        isPresented = false
                    }) {
                        HStack {
                            Image(systemName: lang.2)
                                .foregroundColor(NoxColors.primary)
                            Text(lang.1)
                                .foregroundColor(NoxColors.onSurface)
                            Spacer()
                        }
                    }
                }
            }
            .listStyle(.insetGrouped)
            .scrollContentBackground(.hidden)
            .background(NoxColors.background)
            .navigationTitle(NSLocalizedString("select_language", comment: ""))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button(NSLocalizedString("close", comment: "")) {
                        isPresented = false
                    }
                    .foregroundColor(NoxColors.primary)
                }
            }
        }
    }
}

// MARK: - Gallery Dialog
struct GalleryDialog: View {
    @Binding var isPresented: Bool
    @State private var selectedSource: GallerySource = .phone

    var body: some View {
        NavigationView {
            VStack {
                Picker("", selection: $selectedSource) {
                    ForEach(GallerySource.allCases, id: \.self) { source in
                        Text(source.rawValue).tag(source)
                    }
                }
                .pickerStyle(.segmented)
                .padding()

                if selectedSource == .phone {
                    // Show photo library
                    Text(NSLocalizedString("no_files", comment: ""))
                        .foregroundColor(NoxColors.onSurfaceVariant)
                        .frame(maxWidth: .infinity, maxHeight: .infinity)
                } else {
                    Text(NSLocalizedString("connecting_to_camera", comment: ""))
                        .foregroundColor(NoxColors.onSurfaceVariant)
                        .frame(maxWidth: .infinity, maxHeight: .infinity)
                }
            }
            .background(NoxColors.background)
            .navigationTitle(NSLocalizedString("gallery", comment: ""))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button(NSLocalizedString("close", comment: "")) {
                        isPresented = false
                    }
                    .foregroundColor(NoxColors.primary)
                }
            }
        }
    }
}

// MARK: - Preview Dialog
struct PreviewDialog: View {
    let image: UIImage
    @Binding var isPresented: Bool

    var body: some View {
        ZStack {
            Color.black.ignoresSafeArea()

            Image(uiImage: image)
                .resizable()
                .scaledToFit()

            VStack {
                HStack {
                    Spacer()
                    Button(action: { isPresented = false }) {
                        Image(systemName: "xmark.circle.fill")
                            .font(.system(size: 28))
                            .foregroundColor(.white.opacity(0.8))
                    }
                    .padding()
                }
                Spacer()

                HStack(spacing: 24) {
                    Button(action: shareImage) {
                        VStack {
                            Image(systemName: "square.and.arrow.up")
                                .font(.system(size: 24))
                            Text("Share")
                                .font(.system(size: 12))
                        }
                        .foregroundColor(.white)
                    }

                    Button(action: saveImage) {
                        VStack {
                            Image(systemName: "square.and.arrow.down")
                                .font(.system(size: 24))
                            Text(NSLocalizedString("save", comment: ""))
                                .font(.system(size: 12))
                        }
                        .foregroundColor(.white)
                    }
                }
                .padding(.bottom, 32)
            }
        }
    }

    private func shareImage() {
        // Implement share sheet
    }

    private func saveImage() {
        UIImageWriteToSavedPhotosAlbum(image, nil, nil, nil)
    }
}

// MARK: - Log Dialog
struct LogDialog: View {
    @Binding var isPresented: Bool
    @EnvironmentObject var logger: AppLogger

    var body: some View {
        NavigationView {
            Group {
                if logger.entries.isEmpty {
                    Text(NSLocalizedString("no_log_entries", comment: ""))
                        .foregroundColor(NoxColors.onSurfaceVariant)
                        .frame(maxWidth: .infinity, maxHeight: .infinity)
                } else {
                    ScrollView {
                        LazyVStack(alignment: .leading, spacing: 4) {
                            ForEach(logger.entries) { entry in
                                HStack(alignment: .top, spacing: 8) {
                                    Text(entry.formattedTime)
                                        .font(.system(size: 10, design: .monospaced))
                                        .foregroundColor(NoxColors.outline)
                                    Text("[\(entry.type.rawValue)]")
                                        .font(.system(size: 10, weight: .bold, design: .monospaced))
                                        .foregroundColor(entry.type.color)
                                    Text(entry.message)
                                        .font(.system(size: 10, design: .monospaced))
                                        .foregroundColor(NoxColors.onSurface)
                                }
                            }
                        }
                        .padding(8)
                    }
                }
            }
            .background(NoxColors.background)
            .navigationTitle(NSLocalizedString("system_log", comment: ""))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button(NSLocalizedString("close", comment: "")) {
                        isPresented = false
                    }
                    .foregroundColor(NoxColors.primary)
                }
                ToolbarItem(placement: .destructiveAction) {
                    Button(NSLocalizedString("clear", comment: "")) {
                        logger.clear()
                    }
                    .foregroundColor(NoxColors.error)
                }
            }
        }
    }
}
