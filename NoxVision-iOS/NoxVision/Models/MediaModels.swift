import Foundation

struct CameraFile: Identifiable {
    let id = UUID()
    let name: String
    let size: Int64
    let date: String
    let type: String
}

enum GallerySource: String, CaseIterable {
    case cameraDevice = "Thermal"
    case phone = "Phone"
}

enum PhoneFolder: String, CaseIterable {
    case camera = "Camera"
    case pictures = "Pictures"
    case noxvision = "NoxVision"
}

struct PhoneMediaFile: Identifiable {
    let id = UUID()
    let url: URL
    let name: String
    let size: Int64
    let dateAdded: Date
    let mimeType: String
    let isVideo: Bool
}
