import Foundation

struct MoonPhaseCalculator {
    static let synodicMonth = 29.530588853
    static let referenceNewMoonJD = 2451550.2597222

    enum MoonPhase: String, CaseIterable {
        case newMoon
        case waxingCrescent
        case firstQuarter
        case waxingGibbous
        case fullMoon
        case waningGibbous
        case lastQuarter
        case waningCrescent

        var germanName: String {
            switch self {
            case .newMoon: return "Neumond"
            case .waxingCrescent: return "Zunehmender Sichelmond"
            case .firstQuarter: return "Erstes Viertel"
            case .waxingGibbous: return "Zunehmender Dreiviertelmond"
            case .fullMoon: return "Vollmond"
            case .waningGibbous: return "Abnehmender Dreiviertelmond"
            case .lastQuarter: return "Letztes Viertel"
            case .waningCrescent: return "Abnehmender Sichelmond"
            }
        }

        var icon: String {
            switch self {
            case .newMoon: return "\u{1F311}"
            case .waxingCrescent: return "\u{1F312}"
            case .firstQuarter: return "\u{1F313}"
            case .waxingGibbous: return "\u{1F314}"
            case .fullMoon: return "\u{1F315}"
            case .waningGibbous: return "\u{1F316}"
            case .lastQuarter: return "\u{1F317}"
            case .waningCrescent: return "\u{1F318}"
            }
        }

        var illumination: String {
            switch self {
            case .newMoon: return "0%"
            case .waxingCrescent: return "1-49%"
            case .firstQuarter: return "50%"
            case .waxingGibbous: return "51-99%"
            case .fullMoon: return "100%"
            case .waningGibbous: return "99-51%"
            case .lastQuarter: return "50%"
            case .waningCrescent: return "49-1%"
            }
        }
    }

    struct MoonInfo {
        let phase: MoonPhase
        let daysSinceNewMoon: Double
        let daysUntilNextNewMoon: Double
        let illuminationPercent: Double
        let activityPrediction: WildlifeActivityPrediction
    }

    enum WildlifeActivityPrediction: Int {
        case veryLow = 1
        case low = 2
        case medium = 3
        case high = 4
        case veryHigh = 5

        var germanText: String {
            switch self {
            case .veryHigh: return "Sehr hohe Aktivitaet"
            case .high: return "Hohe Aktivitaet"
            case .medium: return "Mittlere Aktivitaet"
            case .low: return "Geringe Aktivitaet"
            case .veryLow: return "Sehr geringe Aktivitaet"
            }
        }
    }

    static func calculateMoonPhase(timestamp: Date = Date()) -> MoonInfo {
        let julianDate = toJulianDate(timestamp)
        let daysSinceRef = julianDate - referenceNewMoonJD
        let lunarCycles = daysSinceRef / synodicMonth
        let currentCycleProgress = lunarCycles - floor(lunarCycles)
        let daysSinceNewMoon = currentCycleProgress * synodicMonth
        let daysUntilNextNewMoon = synodicMonth - daysSinceNewMoon

        let phase = getPhaseFromCycleProgress(currentCycleProgress)
        let illumination = calculateIllumination(currentCycleProgress)
        let activity = predictWildlifeActivity(phase)

        return MoonInfo(
            phase: phase,
            daysSinceNewMoon: daysSinceNewMoon,
            daysUntilNextNewMoon: daysUntilNextNewMoon,
            illuminationPercent: illumination,
            activityPrediction: activity
        )
    }

    private static func toJulianDate(_ date: Date) -> Double {
        var calendar = Calendar(identifier: .gregorian)
        calendar.timeZone = TimeZone(identifier: "UTC")!
        let components = calendar.dateComponents([.year, .month, .day, .hour, .minute, .second], from: date)

        let year = components.year!
        let month = components.month!
        let day = components.day!
        let hour = components.hour!
        let minute = components.minute!
        let second = components.second!

        let dayFraction = (Double(hour) + Double(minute) / 60.0 + Double(second) / 3600.0) / 24.0
        let a = (14 - month) / 12
        let y = year + 4800 - a
        let m = month + 12 * a - 3
        let jdn = day + (153 * m + 2) / 5 + 365 * y + y / 4 - y / 100 + y / 400 - 32045

        return Double(jdn) + dayFraction - 0.5
    }

    private static func getPhaseFromCycleProgress(_ progress: Double) -> MoonPhase {
        switch progress {
        case ..<0.0625: return .newMoon
        case ..<0.1875: return .waxingCrescent
        case ..<0.3125: return .firstQuarter
        case ..<0.4375: return .waxingGibbous
        case ..<0.5625: return .fullMoon
        case ..<0.6875: return .waningGibbous
        case ..<0.8125: return .lastQuarter
        case ..<0.9375: return .waningCrescent
        default: return .newMoon
        }
    }

    private static func calculateIllumination(_ progress: Double) -> Double {
        let angle = progress * 2 * .pi
        return ((1 - cos(angle)) / 2) * 100
    }

    private static func predictWildlifeActivity(_ phase: MoonPhase) -> WildlifeActivityPrediction {
        switch phase {
        case .fullMoon: return .veryHigh
        case .waxingGibbous, .waningGibbous: return .high
        case .firstQuarter, .lastQuarter: return .medium
        case .waxingCrescent, .waningCrescent: return .low
        case .newMoon: return .veryLow
        }
    }

    static func getNextFullMoon(from date: Date = Date()) -> Date {
        let info = calculateMoonPhase(timestamp: date)
        let daysUntilFull: Double
        if info.daysSinceNewMoon < synodicMonth / 2 {
            daysUntilFull = (synodicMonth / 2) - info.daysSinceNewMoon
        } else {
            daysUntilFull = synodicMonth - info.daysSinceNewMoon + (synodicMonth / 2)
        }
        return date.addingTimeInterval(daysUntilFull * 24 * 60 * 60)
    }

    static func getNextNewMoon(from date: Date = Date()) -> Date {
        let info = calculateMoonPhase(timestamp: date)
        return date.addingTimeInterval(info.daysUntilNextNewMoon * 24 * 60 * 60)
    }
}
