import Foundation

struct DeviceInfo: Codable, Equatable {
    let deviceName: String
    let cameraName: String
    let videoWidth: Int
    let videoHeight: Int
    let videoFps: Int
    let measureGear: Int
    let cameraLens: String
    let measureRange: String

    enum CodingKeys: String, CodingKey {
        case deviceName = "device_name"
        case cameraName = "camera_name"
        case videoWidth = "video_width"
        case videoHeight = "video_height"
        case videoFps = "video_fps"
        case measureGear = "measure_gear"
        case cameraLens = "camera_lens"
        case measureRange = "measure_range"
    }
}

struct CameraCapabilities {
    let hasFocus: Bool
    let hasGps: Bool
    let hasRadiometry: Bool
    let hasAudio: Bool
    let maxPaletteId: Int
    let supportsRestApi: Bool
}

enum FocusAction: Int {
    case auto = 0
    case near = 1
    case far = 2
    case stop = 3
}

enum ShutterMode: Int {
    case manual = 0
    case auto = 1
}

struct EmissivityPresets {
    static let matteSurface: Float = 0.95
    static let semiGloss: Float = 0.80
    static let oxidizedMetal: Float = 0.60
    static let polishedMetal: Float = 0.30
    static let mirror: Float = 0.05

    static let presets: [(String, Float)] = [
        ("Skin/Fabric", matteSurface),
        ("Painted Surface", semiGloss),
        ("Oxidized Metal", oxidizedMetal),
        ("Polished Metal", polishedMetal),
        ("Mirror/Reflective", mirror)
    ]
}

extension DeviceInfo {
    var capabilities: CameraCapabilities {
        let name = deviceName.lowercased()
        switch true {
        case name.contains("c640"):
            return CameraCapabilities(hasFocus: true, hasGps: true, hasRadiometry: true, hasAudio: true, maxPaletteId: 20, supportsRestApi: true)
        case name.contains("c400"):
            return CameraCapabilities(hasFocus: true, hasGps: true, hasRadiometry: true, hasAudio: true, maxPaletteId: 15, supportsRestApi: true)
        case name.contains("c800"):
            return CameraCapabilities(hasFocus: true, hasGps: true, hasRadiometry: true, hasAudio: true, maxPaletteId: 20, supportsRestApi: true)
        case name.contains("d400"), name.contains("d384"), name.contains("d192"):
            return CameraCapabilities(hasFocus: true, hasGps: false, hasRadiometry: true, hasAudio: false, maxPaletteId: 15, supportsRestApi: true)
        case name.contains("d160"):
            return CameraCapabilities(hasFocus: false, hasGps: false, hasRadiometry: true, hasAudio: false, maxPaletteId: 10, supportsRestApi: true)
        case name.contains("b320"), name.contains("b256"), name.contains("b160"):
            return CameraCapabilities(hasFocus: false, hasGps: false, hasRadiometry: true, hasAudio: false, maxPaletteId: 10, supportsRestApi: true)
        case name.contains("te"):
            return CameraCapabilities(hasFocus: false, hasGps: false, hasRadiometry: true, hasAudio: false, maxPaletteId: 10, supportsRestApi: true)
        case name.contains("ps"):
            return CameraCapabilities(hasFocus: false, hasGps: false, hasRadiometry: true, hasAudio: false, maxPaletteId: 10, supportsRestApi: true)
        case name.hasPrefix("tb"):
            return CameraCapabilities(hasFocus: true, hasGps: true, hasRadiometry: true, hasAudio: true, maxPaletteId: 15, supportsRestApi: true)
        default:
            return CameraCapabilities(hasFocus: false, hasGps: false, hasRadiometry: false, hasAudio: false, maxPaletteId: 10, supportsRestApi: false)
        }
    }

    var seriesName: String {
        let name = deviceName.lowercased()
        switch true {
        case name.contains("c640"): return "Guide C640 Series"
        case name.contains("c400"): return "Guide C400 Series"
        case name.contains("c800"): return "Guide C800 Series"
        case name.contains("d400"): return "Guide D400 Series"
        case name.contains("d384"): return "Guide D384 Series"
        case name.contains("te"): return "Guide TE Series"
        case name.contains("ps"): return "Guide PS Series"
        case name.hasPrefix("tb"): return "Guide TB Series"
        case name.contains("b"): return "Guide B Series"
        default: return "Guide Camera"
        }
    }
}
