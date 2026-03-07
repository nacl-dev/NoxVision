import Foundation
import SwiftData

@Model
final class HuntRecord {
    @Attribute(.unique) var id: UUID
    var timestamp: Date
    var latitude: Double?
    var longitude: Double?
    var wildlifeType: String
    var gender: String?
    var estimatedWeight: Int?
    var notes: String?
    var thermalImagePath: String?
    var moonPhase: String?
    var weatherSnapshot: String?
    var bundesland: String?

    @Relationship(deleteRule: .cascade, inverse: \Waypoint.huntRecord)
    var waypoints: [Waypoint]?

    init(
        timestamp: Date = Date(),
        latitude: Double? = nil,
        longitude: Double? = nil,
        wildlifeType: String = "",
        gender: String? = nil,
        estimatedWeight: Int? = nil,
        notes: String? = nil,
        thermalImagePath: String? = nil,
        moonPhase: String? = nil,
        weatherSnapshot: String? = nil,
        bundesland: String? = nil
    ) {
        self.id = UUID()
        self.timestamp = timestamp
        self.latitude = latitude
        self.longitude = longitude
        self.wildlifeType = wildlifeType
        self.gender = gender
        self.estimatedWeight = estimatedWeight
        self.notes = notes
        self.thermalImagePath = thermalImagePath
        self.moonPhase = moonPhase
        self.weatherSnapshot = weatherSnapshot
        self.bundesland = bundesland
    }
}

struct WildlifeTypes {
    static let rehwild = ["Bock", "Ricke", "Kitz"]
    static let schwarzwild = ["Keiler", "Bache", "Frischling", "Ueberlaeufer"]
    static let rotwild = ["Hirsch", "Tier", "Kalb"]
    static let damwild = ["Hirsch", "Tier", "Kalb"]
    static let muffelwild = ["Widder", "Schaf", "Lamm"]
    static let raubwild = ["Fuchs", "Dachs", "Waschbaer", "Marderhund", "Marder"]
    static let niederwild = ["Hase", "Wildkaninchen"]
    static let federwild = ["Fasan", "Wildente", "Wildgans", "Taube", "Kraehe"]

    static let allTypes: [(String, [String])] = [
        ("Rehwild", rehwild),
        ("Schwarzwild", schwarzwild),
        ("Rotwild", rotwild),
        ("Damwild", damwild),
        ("Muffelwild", muffelwild),
        ("Raubwild", raubwild),
        ("Niederwild", niederwild),
        ("Federwild", federwild)
    ]

    static func gendersForType(_ type: String) -> [String] {
        allTypes.first(where: { $0.0 == type })?.1 ?? []
    }
}

enum WaypointType: String, Codable, CaseIterable {
    case lastSeen = "LAST_SEEN"
    case bloodTrail = "BLOOD_TRAIL"
    case recovery = "RECOVERY"
    case anschuss = "ANSCHUSS"
    case custom = "CUSTOM"

    var displayName: String {
        switch self {
        case .lastSeen: return NSLocalizedString("waypoint_last_seen", comment: "")
        case .bloodTrail: return NSLocalizedString("waypoint_blood_trail", comment: "")
        case .recovery: return NSLocalizedString("waypoint_recovery", comment: "")
        case .anschuss: return NSLocalizedString("waypoint_anschuss", comment: "")
        case .custom: return NSLocalizedString("waypoint_custom", comment: "")
        }
    }

    var color: (red: Double, green: Double, blue: Double) {
        switch self {
        case .anschuss: return (1.0, 0.3, 0.3)
        case .lastSeen: return (1.0, 0.8, 0.0)
        case .bloodTrail: return (0.8, 0.0, 0.0)
        case .recovery: return (0.3, 0.8, 0.3)
        case .custom: return (0.5, 0.5, 1.0)
        }
    }
}

@Model
final class Waypoint {
    @Attribute(.unique) var id: UUID
    var huntRecord: HuntRecord?
    var latitude: Double
    var longitude: Double
    var type: WaypointType
    var timestamp: Date
    var compassBearing: Float?
    var notes: String?

    init(
        latitude: Double,
        longitude: Double,
        type: WaypointType,
        timestamp: Date = Date(),
        compassBearing: Float? = nil,
        notes: String? = nil,
        huntRecord: HuntRecord? = nil
    ) {
        self.id = UUID()
        self.latitude = latitude
        self.longitude = longitude
        self.type = type
        self.timestamp = timestamp
        self.compassBearing = compassBearing
        self.notes = notes
        self.huntRecord = huntRecord
    }
}

enum HuntingStandType: String, Codable, CaseIterable {
    case hochsitz = "HOCHSITZ"
    case kanzel = "KANZEL"
    case druckjagd = "DRUCKJAGD"
    case ansitz = "ANSITZ"
    case custom = "CUSTOM"

    var displayName: String {
        switch self {
        case .hochsitz: return NSLocalizedString("stand_hochsitz", comment: "")
        case .kanzel: return NSLocalizedString("stand_kanzel", comment: "")
        case .druckjagd: return NSLocalizedString("stand_druckjagd", comment: "")
        case .ansitz: return NSLocalizedString("stand_ansitz", comment: "")
        case .custom: return NSLocalizedString("stand_custom", comment: "")
        }
    }
}

@Model
final class HuntingStand {
    @Attribute(.unique) var id: UUID
    var name: String
    var latitude: Double
    var longitude: Double
    var type: HuntingStandType
    var notes: String?

    init(
        name: String,
        latitude: Double,
        longitude: Double,
        type: HuntingStandType,
        notes: String? = nil
    ) {
        self.id = UUID()
        self.name = name
        self.latitude = latitude
        self.longitude = longitude
        self.type = type
        self.notes = notes
    }
}

@Model
final class CachedWeather {
    @Attribute(.unique) var id: UUID
    var timestamp: Date
    var latitude: Double
    var longitude: Double
    var temperature: Double
    var feelsLike: Double
    var humidity: Int
    var pressure: Int
    var windSpeed: Double
    var windDirection: Int
    var windGust: Double?
    var cloudiness: Int
    var visibility: Int
    var weatherDescription: String
    var icon: String
    var sunrise: Date
    var sunset: Date

    init(
        timestamp: Date = Date(),
        latitude: Double,
        longitude: Double,
        temperature: Double,
        feelsLike: Double,
        humidity: Int,
        pressure: Int,
        windSpeed: Double,
        windDirection: Int,
        windGust: Double? = nil,
        cloudiness: Int,
        visibility: Int,
        weatherDescription: String,
        icon: String,
        sunrise: Date,
        sunset: Date
    ) {
        self.id = UUID()
        self.timestamp = timestamp
        self.latitude = latitude
        self.longitude = longitude
        self.temperature = temperature
        self.feelsLike = feelsLike
        self.humidity = humidity
        self.pressure = pressure
        self.windSpeed = windSpeed
        self.windDirection = windDirection
        self.windGust = windGust
        self.cloudiness = cloudiness
        self.visibility = visibility
        self.weatherDescription = weatherDescription
        self.icon = icon
        self.sunrise = sunrise
        self.sunset = sunset
    }

    var isExpired: Bool {
        Date().timeIntervalSince(timestamp) > 30 * 60
    }

    var windDirectionName: String {
        switch windDirection {
        case 0..<23: return "N"
        case 23..<68: return "NO"
        case 68..<113: return "O"
        case 113..<158: return "SO"
        case 158..<203: return "S"
        case 203..<248: return "SW"
        case 248..<293: return "W"
        case 293..<338: return "NW"
        default: return "N"
        }
    }
}
