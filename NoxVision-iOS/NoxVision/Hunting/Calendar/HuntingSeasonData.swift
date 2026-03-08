import Foundation

struct HuntingSeason: Identifiable {
    let id = UUID()
    let wildlifeType: String
    let gender: String?
    let startMonth: Int
    let startDay: Int
    let endMonth: Int
    let endDay: Int
    let bundesland: String?

    var isActive: Bool {
        let now = Date()
        let calendar = Calendar.current
        let month = calendar.component(.month, from: now)
        let day = calendar.component(.day, from: now)
        let currentDayOfYear = month * 100 + day
        let startDayOfYear = startMonth * 100 + startDay
        let endDayOfYear = endMonth * 100 + endDay

        if startDayOfYear <= endDayOfYear {
            return currentDayOfYear >= startDayOfYear && currentDayOfYear <= endDayOfYear
        } else {
            return currentDayOfYear >= startDayOfYear || currentDayOfYear <= endDayOfYear
        }
    }

    var isUpcoming: Bool {
        guard !isActive else { return false }
        if let days = daysUntilStart {
            return days > 0 && days <= 30
        }
        return false
    }

    var daysUntilStart: Int? {
        let calendar = Calendar.current
        let now = Date()
        var components = calendar.dateComponents([.year], from: now)
        components.month = startMonth
        components.day = startDay
        guard let startDate = calendar.date(from: components) else { return nil }

        var target = startDate
        if target < now {
            components.year = (components.year ?? 2026) + 1
            target = calendar.date(from: components) ?? startDate
        }

        return calendar.dateComponents([.day], from: now, to: target).day
    }

    var periodDescription: String {
        let months = ["", "Jan", "Feb", "Mär", "Apr", "Mai", "Jun", "Jul", "Aug", "Sep", "Okt", "Nov", "Dez"]
        return "\(startDay). \(months[startMonth]) – \(endDay). \(months[endMonth])"
    }
}

struct HuntingSeasonData {
    static func getSeasonsForBundesland(_ bundesland: String) -> [HuntingSeason] {
        // Federal hunting seasons (Bundesjagdzeiten) - default for all states
        var seasons: [HuntingSeason] = [
            // Rehwild
            HuntingSeason(wildlifeType: "Rehwild", gender: "Bock", startMonth: 5, startDay: 1, endMonth: 10, endDay: 15, bundesland: bundesland),
            HuntingSeason(wildlifeType: "Rehwild", gender: "Ricke", startMonth: 9, startDay: 1, endMonth: 1, endDay: 31, bundesland: bundesland),
            HuntingSeason(wildlifeType: "Rehwild", gender: "Kitz", startMonth: 9, startDay: 1, endMonth: 2, endDay: 28, bundesland: bundesland),

            // Schwarzwild
            HuntingSeason(wildlifeType: "Schwarzwild", gender: "Keiler", startMonth: 6, startDay: 16, endMonth: 1, endDay: 31, bundesland: bundesland),
            HuntingSeason(wildlifeType: "Schwarzwild", gender: "Bache", startMonth: 6, startDay: 16, endMonth: 1, endDay: 31, bundesland: bundesland),
            HuntingSeason(wildlifeType: "Schwarzwild", gender: "Frischling", startMonth: 1, startDay: 1, endMonth: 12, endDay: 31, bundesland: bundesland),

            // Rotwild
            HuntingSeason(wildlifeType: "Rotwild", gender: "Hirsch", startMonth: 8, startDay: 1, endMonth: 1, endDay: 31, bundesland: bundesland),
            HuntingSeason(wildlifeType: "Rotwild", gender: "Tier", startMonth: 8, startDay: 1, endMonth: 1, endDay: 31, bundesland: bundesland),
            HuntingSeason(wildlifeType: "Rotwild", gender: "Kalb", startMonth: 8, startDay: 1, endMonth: 2, endDay: 28, bundesland: bundesland),

            // Damwild
            HuntingSeason(wildlifeType: "Damwild", gender: "Hirsch", startMonth: 9, startDay: 1, endMonth: 1, endDay: 31, bundesland: bundesland),
            HuntingSeason(wildlifeType: "Damwild", gender: "Tier", startMonth: 9, startDay: 1, endMonth: 1, endDay: 31, bundesland: bundesland),
            HuntingSeason(wildlifeType: "Damwild", gender: "Kalb", startMonth: 9, startDay: 1, endMonth: 2, endDay: 28, bundesland: bundesland),

            // Raubwild
            HuntingSeason(wildlifeType: "Raubwild", gender: "Fuchs", startMonth: 7, startDay: 16, endMonth: 2, endDay: 28, bundesland: bundesland),
            HuntingSeason(wildlifeType: "Raubwild", gender: "Dachs", startMonth: 8, startDay: 1, endMonth: 10, endDay: 31, bundesland: bundesland),
            HuntingSeason(wildlifeType: "Raubwild", gender: "Waschbaer", startMonth: 8, startDay: 1, endMonth: 3, endDay: 15, bundesland: bundesland),
            HuntingSeason(wildlifeType: "Raubwild", gender: "Marderhund", startMonth: 9, startDay: 1, endMonth: 2, endDay: 28, bundesland: bundesland),
            HuntingSeason(wildlifeType: "Raubwild", gender: "Marder", startMonth: 10, startDay: 16, endMonth: 2, endDay: 28, bundesland: bundesland),

            // Niederwild
            HuntingSeason(wildlifeType: "Niederwild", gender: "Hase", startMonth: 10, startDay: 1, endMonth: 12, endDay: 31, bundesland: bundesland),
            HuntingSeason(wildlifeType: "Niederwild", gender: "Wildkaninchen", startMonth: 1, startDay: 1, endMonth: 12, endDay: 31, bundesland: bundesland),

            // Federwild
            HuntingSeason(wildlifeType: "Federwild", gender: "Fasan", startMonth: 10, startDay: 1, endMonth: 1, endDay: 15, bundesland: bundesland),
            HuntingSeason(wildlifeType: "Federwild", gender: "Wildente", startMonth: 9, startDay: 1, endMonth: 1, endDay: 15, bundesland: bundesland),
            HuntingSeason(wildlifeType: "Federwild", gender: "Wildgans", startMonth: 11, startDay: 1, endMonth: 1, endDay: 15, bundesland: bundesland),
            HuntingSeason(wildlifeType: "Federwild", gender: "Taube", startMonth: 11, startDay: 1, endMonth: 2, endDay: 20, bundesland: bundesland),
        ]

        return seasons
    }
}
