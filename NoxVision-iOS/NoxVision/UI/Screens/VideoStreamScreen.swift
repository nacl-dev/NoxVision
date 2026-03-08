import SwiftUI
import AVKit
import VLCKitSPM

struct VideoStreamScreen: View {
    @EnvironmentObject var settings: CameraSettingsStore
    @StateObject private var detector = ThermalObjectDetector()
    @StateObject private var wifiManager = WiFiAutoConnect()

    @State private var isConnected = false
    @State private var isBuffering = false
    @State private var bufferPercent = 0
    @State private var isRecording = false
    @State private var recordingDuration = ""
    @State private var currentZoom: Float = 1.0
    @State private var currentPaletteIndex = 0
    @State private var showSettings = false
    @State private var showGallery = false
    @State private var showHuntingHub = false
    @State private var showThermalSettings = false
    @State private var showPreview = false
    @State private var previewImage: UIImage?
    @State private var toastMessage: String?
    @State private var showControls = true

    private let palettes = [
        ("Whitehot", 0), ("Iron", 2), ("Bluehot", 4),
        ("Greenhot", 5), ("Blackhot", 9), ("Redhot", 11)
    ]

    var body: some View {
        ZStack {
            NoxColors.background.ignoresSafeArea()

            // Video stream area
            videoArea

            // Detection overlay
            if settings.aiDetectionEnabled {
                detectionOverlay
            }

            // Crosshair overlay
            if settings.crosshairEnabled {
                CrosshairOverlay(style: settings.crosshairStyle, color: .white.opacity(0.8))
            }

            // Controls overlay
            if showControls {
                controlsOverlay
            }

            // Status bar
            statusBar

            // Toast
            if let message = toastMessage {
                toastView(message)
            }
        }
        .onTapGesture {
            withAnimation(.easeInOut(duration: 0.2)) {
                showControls.toggle()
            }
        }
        .onAppear {
            if settings.autoConnectEnabled {
                wifiManager.connectToCamera(ssid: settings.wifiSSID, password: settings.wifiPassword)
            }
        }
        .fullScreenCover(isPresented: $showSettings) {
            SettingsScreen(isPresented: $showSettings)
        }
        .fullScreenCover(isPresented: $showHuntingHub) {
            HuntingHubScreen(isPresented: $showHuntingHub)
        }
        .sheet(isPresented: $showGallery) {
            GalleryDialog(isPresented: $showGallery)
        }
        .sheet(isPresented: $showThermalSettings) {
            ThermalSettingsScreen(isPresented: $showThermalSettings)
        }
        .sheet(isPresented: $showPreview) {
            if let image = previewImage {
                PreviewDialog(image: image, isPresented: $showPreview)
            }
        }
    }

    // MARK: - Video Area
    private var videoArea: some View {
        GeometryReader { geometry in
            ZStack {
                // Placeholder / stream view
                if isConnected {
                    // In production, use VLCKit or a custom RTSP player
                    // For now, show a placeholder that indicates the stream URL
                    RTSPPlayerView(url: settings.rtspUrl, isConnected: $isConnected, isBuffering: $isBuffering)
                        .frame(maxWidth: .infinity, maxHeight: .infinity)
                } else {
                    VStack(spacing: 16) {
                        Image(systemName: "video.slash")
                            .font(.system(size: 48))
                            .foregroundColor(NoxColors.outline)
                        Text(NSLocalizedString("connecting_to_camera", comment: ""))
                            .font(.system(size: 14))
                            .foregroundColor(NoxColors.onSurfaceVariant)
                        Text(settings.rtspUrl)
                            .font(.system(size: 12, design: .monospaced))
                            .foregroundColor(NoxColors.outline)
                    }
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
                    .background(Color.black)
                }
            }
        }
    }

    // MARK: - Detection Overlay
    private var detectionOverlay: some View {
        GeometryReader { geometry in
            ForEach(detector.detectedObjects) { obj in
                let rect = CGRect(
                    x: obj.boundingBox.minX * geometry.size.width,
                    y: (1 - obj.boundingBox.maxY) * geometry.size.height,
                    width: obj.boundingBox.width * geometry.size.width,
                    height: obj.boundingBox.height * geometry.size.height
                )

                ZStack(alignment: .topLeading) {
                    Rectangle()
                        .stroke(NoxColors.primary, lineWidth: 2)
                        .frame(width: rect.width, height: rect.height)

                    VStack(alignment: .leading, spacing: 1) {
                        Text(obj.label.capitalized)
                            .font(.system(size: 10, weight: .bold))
                        Text(String(format: "%.0f%%", obj.confidence * 100))
                            .font(.system(size: 9))
                        if let dist = obj.estimatedDistance {
                            Text(String(format: "~%.0fm", dist))
                                .font(.system(size: 9))
                        }
                    }
                    .foregroundColor(.white)
                    .padding(3)
                    .background(NoxColors.primary.opacity(0.7))
                    .cornerRadius(4)
                    .offset(y: -20)
                }
                .position(x: rect.midX, y: rect.midY)
            }
        }
        .allowsHitTesting(false)
    }

    // MARK: - Controls Overlay
    private var controlsOverlay: some View {
        VStack {
            Spacer()

            // Bottom controls
            HStack(spacing: 0) {
                // Left column - Zoom & Palette
                VStack(spacing: 12) {
                    controlButton(icon: "plus.magnifyingglass", label: NSLocalizedString("zoom", comment: "")) {
                        currentZoom = currentZoom >= 4.0 ? 1.0 : currentZoom + 0.5
                    }
                    controlButton(icon: "paintpalette", label: NSLocalizedString("palette", comment: "")) {
                        currentPaletteIndex = (currentPaletteIndex + 1) % palettes.count
                        Task {
                            let client = CameraApiClient(baseUrl: settings.baseUrl)
                            _ = await client.setPaletteId(palettes[currentPaletteIndex].1)
                        }
                    }
                }

                Spacer()

                // Center - Main actions
                VStack(spacing: 16) {
                    // Connect/Disconnect
                    Button(action: toggleConnection) {
                        VStack(spacing: 4) {
                            Image(systemName: isConnected ? "wifi.slash" : "wifi")
                                .font(.system(size: 28))
                            Text(isConnected ? NSLocalizedString("disconnect", comment: "") : NSLocalizedString("connect", comment: ""))
                                .font(.system(size: 10))
                        }
                        .foregroundColor(isConnected ? NoxColors.error : NoxColors.primary)
                    }

                    HStack(spacing: 24) {
                        // Record
                        Button(action: toggleRecording) {
                            VStack(spacing: 4) {
                                Circle()
                                    .fill(isRecording ? NoxColors.error : .red)
                                    .frame(width: 36, height: 36)
                                    .overlay(
                                        isRecording ?
                                        RoundedRectangle(cornerRadius: 4).fill(.white).frame(width: 14, height: 14) :
                                        nil
                                    )
                                Text(isRecording ? NSLocalizedString("stop_rec", comment: "") : NSLocalizedString("record", comment: ""))
                                    .font(.system(size: 10))
                                    .foregroundColor(NoxColors.onSurface)
                            }
                        }

                        // Photo
                        Button(action: takeScreenshot) {
                            VStack(spacing: 4) {
                                Image(systemName: "camera.fill")
                                    .font(.system(size: 28))
                                Text(NSLocalizedString("photo", comment: ""))
                                    .font(.system(size: 10))
                            }
                            .foregroundColor(NoxColors.onSurface)
                        }
                    }
                }

                Spacer()

                // Right column - Navigation
                VStack(spacing: 12) {
                    controlButton(icon: "photo.on.rectangle", label: NSLocalizedString("gallery", comment: "")) {
                        showGallery = true
                    }
                    controlButton(icon: "gearshape", label: NSLocalizedString("settings", comment: "")) {
                        showSettings = true
                    }
                    if settings.huntingAssistantHomeEnabled {
                        controlButton(icon: "leaf", label: NSLocalizedString("hunting", comment: "")) {
                            showHuntingHub = true
                        }
                    }
                }
            }
            .padding(.horizontal, 16)
            .padding(.bottom, 24)
            .background(
                LinearGradient(
                    gradient: Gradient(colors: [.clear, .black.opacity(0.7)]),
                    startPoint: .top,
                    endPoint: .bottom
                )
            )
        }
    }

    // MARK: - Status Bar
    private var statusBar: some View {
        VStack {
            HStack(spacing: 8) {
                // Connection status
                HStack(spacing: 4) {
                    Circle()
                        .fill(isConnected ? NoxColors.success : NoxColors.error)
                        .frame(width: 8, height: 8)
                    Text(isConnected ? NSLocalizedString("live", comment: "") : NSLocalizedString("offline", comment: ""))
                        .font(.system(size: 11, weight: .bold, design: .monospaced))
                        .foregroundColor(NoxColors.onSurface)
                }
                .padding(.horizontal, 8)
                .padding(.vertical, 4)
                .background(Color.black.opacity(0.6))
                .cornerRadius(8)

                if isBuffering {
                    Text(String(format: NSLocalizedString("buffering", comment: ""), bufferPercent))
                        .font(.system(size: 11, design: .monospaced))
                        .foregroundColor(NoxColors.warning)
                        .padding(.horizontal, 8)
                        .padding(.vertical, 4)
                        .background(Color.black.opacity(0.6))
                        .cornerRadius(8)
                }

                if isRecording {
                    HStack(spacing: 4) {
                        Circle().fill(.red).frame(width: 8, height: 8)
                        Text(String(format: NSLocalizedString("rec", comment: ""), recordingDuration))
                            .font(.system(size: 11, weight: .bold, design: .monospaced))
                            .foregroundColor(.red)
                    }
                    .padding(.horizontal, 8)
                    .padding(.vertical, 4)
                    .background(Color.black.opacity(0.6))
                    .cornerRadius(8)
                }

                Spacer()

                // Zoom indicator
                if currentZoom > 1.0 {
                    Text(String(format: "%.1fx", currentZoom))
                        .font(.system(size: 11, weight: .bold, design: .monospaced))
                        .foregroundColor(NoxColors.onSurface)
                        .padding(.horizontal, 8)
                        .padding(.vertical, 4)
                        .background(Color.black.opacity(0.6))
                        .cornerRadius(8)
                }

                // Palette name
                Text(palettes[currentPaletteIndex].0)
                    .font(.system(size: 11, weight: .medium))
                    .foregroundColor(NoxColors.onSurface)
                    .padding(.horizontal, 8)
                    .padding(.vertical, 4)
                    .background(Color.black.opacity(0.6))
                    .cornerRadius(8)
            }
            .padding(.horizontal, 16)
            .padding(.top, 8)

            Spacer()
        }
    }

    // MARK: - Helpers

    private func controlButton(icon: String, label: String, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            VStack(spacing: 4) {
                Image(systemName: icon)
                    .font(.system(size: 22))
                Text(label)
                    .font(.system(size: 10))
            }
            .foregroundColor(NoxColors.onSurface)
            .frame(width: 60, height: 50)
        }
    }

    private func toggleConnection() {
        if isConnected {
            isConnected = false
        } else {
            isBuffering = true
            isConnected = true
        }
    }

    private func toggleRecording() {
        isRecording.toggle()
        if !isRecording {
            showToast(NSLocalizedString("video_saved", comment: ""))
        }
    }

    private func takeScreenshot() {
        // In production, capture frame from video stream
        showToast(NSLocalizedString("photo_saved", comment: ""))
    }

    private func showToast(_ message: String) {
        toastMessage = message
        DispatchQueue.main.asyncAfter(deadline: .now() + 2) {
            toastMessage = nil
        }
    }

    private func toastView(_ message: String) -> some View {
        VStack {
            Spacer()
            Text(message)
                .font(.system(size: 14))
                .foregroundColor(NoxColors.onSurface)
                .padding(.horizontal, 20)
                .padding(.vertical, 10)
                .background(NoxColors.surfaceVariant.opacity(0.95))
                .cornerRadius(24)
                .padding(.bottom, 100)
        }
        .transition(.opacity)
        .animation(.easeInOut, value: toastMessage)
    }
}

// MARK: - RTSP Player View (VLCKit)
struct RTSPPlayerView: UIViewRepresentable {
    let url: String
    @Binding var isConnected: Bool
    @Binding var isBuffering: Bool

    func makeCoordinator() -> Coordinator {
        Coordinator(parent: self)
    }

    func makeUIView(context: Context) -> UIView {
        let videoView = UIView()
        videoView.backgroundColor = .black

        let mediaPlayer = VLCMediaPlayer()
        mediaPlayer.delegate = context.coordinator
        mediaPlayer.drawable = videoView

        if let streamURL = URL(string: url) {
            let media = VLCMedia(url: streamURL)
            media.addOptions([
                "network-caching": 300,
                "rtsp-tcp": true,
                "clock-jitter": 0,
                "clock-synchro": 0,
            ])
            mediaPlayer.media = media
            mediaPlayer.play()
        }

        context.coordinator.mediaPlayer = mediaPlayer
        return videoView
    }

    func updateUIView(_ uiView: UIView, context: Context) {}

    static func dismantleUIView(_ uiView: UIView, coordinator: Coordinator) {
        coordinator.mediaPlayer?.stop()
        coordinator.mediaPlayer = nil
    }

    class Coordinator: NSObject, VLCMediaPlayerDelegate {
        let parent: RTSPPlayerView
        var mediaPlayer: VLCMediaPlayer?

        init(parent: RTSPPlayerView) {
            self.parent = parent
        }

        func mediaPlayerStateChanged(_ notification: Notification) {
            guard let player = mediaPlayer else { return }
            DispatchQueue.main.async {
                switch player.state {
                case .playing:
                    self.parent.isConnected = true
                    self.parent.isBuffering = false
                case .buffering:
                    self.parent.isBuffering = true
                case .stopped, .error:
                    self.parent.isConnected = false
                    self.parent.isBuffering = false
                default:
                    break
                }
            }
        }
    }
}
