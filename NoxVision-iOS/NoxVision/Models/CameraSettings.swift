import Foundation
import SwiftUI
import Combine

enum CrosshairStyle: Int, CaseIterable, Identifiable {
    case simple = 0
    case gap = 1
    case circleDot = 2
    case chevron = 3

    var id: Int { rawValue }

    var displayName: String {
        switch self {
        case .simple: return NSLocalizedString("crosshair_simple", comment: "")
        case .gap: return NSLocalizedString("crosshair_gap", comment: "")
        case .circleDot: return NSLocalizedString("crosshair_circle_dot", comment: "")
        case .chevron: return NSLocalizedString("crosshair_chevron", comment: "")
        }
    }
}

enum HuntingAssistantCountry: String, CaseIterable, Identifiable {
    case germany = "DE"
    case france = "FR"
    case spain = "ES"
    case italy = "IT"
    case netherlands = "NL"
    case poland = "PL"
    case ukraine = "UA"
    case international = "INTL"

    var id: String { rawValue }

    var displayName: String {
        switch self {
        case .germany: return NSLocalizedString("hunting_country_germany", comment: "")
        case .france: return NSLocalizedString("hunting_country_france", comment: "")
        case .spain: return NSLocalizedString("hunting_country_spain", comment: "")
        case .italy: return NSLocalizedString("hunting_country_italy", comment: "")
        case .netherlands: return NSLocalizedString("hunting_country_netherlands", comment: "")
        case .poland: return NSLocalizedString("hunting_country_poland", comment: "")
        case .ukraine: return NSLocalizedString("hunting_country_ukraine", comment: "")
        case .international: return NSLocalizedString("hunting_country_international", comment: "")
        }
    }

    var supportsGermanSeasons: Bool {
        self == .germany
    }

    static func fromCode(_ code: String?) -> HuntingAssistantCountry {
        guard let code = code else { return .germany }
        return HuntingAssistantCountry(rawValue: code) ?? .germany
    }
}

class CameraSettingsStore: ObservableObject {
    private let defaults = UserDefaults.standard
    private let prefsPrefix = "noxvision_"

    // MARK: - Default Values
    static let defaultIP = "192.168.42.1"
    static let defaultWifiSSID = "TE Mini-089F"
    static let defaultWifiPassword = "12345678"
    static let defaultHttpPort = 80
    static let defaultEmissivity: Float = 0.95
    static let defaultDistance: Float = 1.0
    static let defaultHumidity: Float = 50.0
    static let defaultReflectTemp: Float = 23.0

    // MARK: - Published Properties
    @Published var cameraIp: String {
        didSet { defaults.set(cameraIp, forKey: key("camera_ip")) }
    }
    @Published var wifiSSID: String {
        didSet { defaults.set(wifiSSID, forKey: key("wifi_ssid")) }
    }
    @Published var wifiPassword: String {
        didSet { defaults.set(wifiPassword, forKey: key("wifi_password")) }
    }
    @Published var httpPort: Int {
        didSet { defaults.set(httpPort, forKey: key("http_port")) }
    }
    @Published var autoConnectEnabled: Bool {
        didSet { defaults.set(autoConnectEnabled, forKey: key("autoconnect_enabled")) }
    }
    @Published var crosshairEnabled: Bool {
        didSet { defaults.set(crosshairEnabled, forKey: key("crosshair_enabled")) }
    }
    @Published var crosshairStyle: CrosshairStyle {
        didSet { defaults.set(crosshairStyle.rawValue, forKey: key("crosshair_style")) }
    }
    @Published var huntingAssistantHomeEnabled: Bool {
        didSet { defaults.set(huntingAssistantHomeEnabled, forKey: key("hunting_assistant_home_enabled")) }
    }
    @Published var huntingCountry: HuntingAssistantCountry {
        didSet { defaults.set(huntingCountry.rawValue, forKey: key("hunting_country")) }
    }
    @Published var emissivity: Float {
        didSet { defaults.set(emissivity, forKey: key("emissivity")) }
    }
    @Published var distance: Float {
        didSet { defaults.set(distance, forKey: key("distance")) }
    }
    @Published var humidity: Float {
        didSet { defaults.set(humidity, forKey: key("humidity")) }
    }
    @Published var reflectTemperature: Float {
        didSet { defaults.set(reflectTemperature, forKey: key("reflect_temp")) }
    }
    @Published var audioEnabled: Bool {
        didSet { defaults.set(audioEnabled, forKey: key("audio_enabled")) }
    }
    @Published var hotspotEnabled: Bool {
        didSet { defaults.set(hotspotEnabled, forKey: key("hotspot_enabled")) }
    }
    @Published var aiDetectionEnabled: Bool {
        didSet { defaults.set(aiDetectionEnabled, forKey: key("ai_detection_enabled")) }
    }
    @Published var imageEnhancementEnabled: Bool {
        didSet { defaults.set(imageEnhancementEnabled, forKey: key("image_enhancement_enabled")) }
    }

    // MARK: - Non-Published Properties
    var isFirstRun: Bool {
        get { defaults.object(forKey: key("first_run")) == nil ? true : defaults.bool(forKey: key("first_run")) }
        set { defaults.set(newValue, forKey: key("first_run")) }
    }

    var lastVersionCode: Int {
        get { defaults.integer(forKey: key("last_version_code")) }
        set { defaults.set(newValue, forKey: key("last_version_code")) }
    }

    // MARK: - Cached Device Info
    var cachedDeviceInfo: DeviceInfo? {
        get {
            guard let name = defaults.string(forKey: key("device_name")) else { return nil }
            return DeviceInfo(
                deviceName: name,
                cameraName: defaults.string(forKey: key("camera_name")) ?? "",
                videoWidth: defaults.integer(forKey: key("video_width")),
                videoHeight: defaults.integer(forKey: key("video_height")),
                videoFps: 25,
                measureGear: 0,
                cameraLens: "",
                measureRange: ""
            )
        }
        set {
            if let info = newValue {
                defaults.set(info.deviceName, forKey: key("device_name"))
                defaults.set(info.cameraName, forKey: key("camera_name"))
                defaults.set(info.videoWidth, forKey: key("video_width"))
                defaults.set(info.videoHeight, forKey: key("video_height"))
            } else {
                defaults.removeObject(forKey: key("device_name"))
                defaults.removeObject(forKey: key("camera_name"))
                defaults.removeObject(forKey: key("video_width"))
                defaults.removeObject(forKey: key("video_height"))
            }
        }
    }

    // MARK: - Init
    init() {
        let d = UserDefaults.standard
        let p = "noxvision_"
        self.cameraIp = d.string(forKey: "\(p)camera_ip") ?? Self.defaultIP
        self.wifiSSID = d.string(forKey: "\(p)wifi_ssid") ?? Self.defaultWifiSSID
        self.wifiPassword = d.string(forKey: "\(p)wifi_password") ?? Self.defaultWifiPassword
        self.httpPort = d.object(forKey: "\(p)http_port") != nil ? d.integer(forKey: "\(p)http_port") : Self.defaultHttpPort
        self.autoConnectEnabled = d.object(forKey: "\(p)autoconnect_enabled") != nil ? d.bool(forKey: "\(p)autoconnect_enabled") : true
        self.crosshairEnabled = d.bool(forKey: "\(p)crosshair_enabled")
        self.crosshairStyle = CrosshairStyle(rawValue: d.integer(forKey: "\(p)crosshair_style")) ?? .simple
        self.huntingAssistantHomeEnabled = d.object(forKey: "\(p)hunting_assistant_home_enabled") != nil ? d.bool(forKey: "\(p)hunting_assistant_home_enabled") : true
        self.huntingCountry = HuntingAssistantCountry(rawValue: d.string(forKey: "\(p)hunting_country") ?? "DE") ?? .germany
        self.emissivity = d.object(forKey: "\(p)emissivity") != nil ? d.float(forKey: "\(p)emissivity") : Self.defaultEmissivity
        self.distance = d.object(forKey: "\(p)distance") != nil ? d.float(forKey: "\(p)distance") : Self.defaultDistance
        self.humidity = d.object(forKey: "\(p)humidity") != nil ? d.float(forKey: "\(p)humidity") : Self.defaultHumidity
        self.reflectTemperature = d.object(forKey: "\(p)reflect_temp") != nil ? d.float(forKey: "\(p)reflect_temp") : Self.defaultReflectTemp
        self.audioEnabled = d.bool(forKey: "\(p)audio_enabled")
        self.hotspotEnabled = d.bool(forKey: "\(p)hotspot_enabled")
        self.aiDetectionEnabled = d.bool(forKey: "\(p)ai_detection_enabled")
        self.imageEnhancementEnabled = d.bool(forKey: "\(p)image_enhancement_enabled")
    }

    // MARK: - Helpers
    private func key(_ name: String) -> String { "\(prefsPrefix)\(name)" }

    var rtspUrl: String { "rtsp://\(cameraIp):8554/video" }

    var baseUrl: String {
        httpPort == 80 ? "http://\(cameraIp)" : "http://\(cameraIp):\(httpPort)"
    }

    static func isValidIP(_ ip: String) -> Bool {
        let pattern = "^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$"
        return ip.range(of: pattern, options: .regularExpression) != nil
    }

    func setFirstRunCompleted() {
        isFirstRun = false
    }

    func shouldShowWhatsNew() -> Bool {
        let currentVersion = 4
        if lastVersionCode < currentVersion {
            lastVersionCode = currentVersion
            return true
        }
        return false
    }
}
