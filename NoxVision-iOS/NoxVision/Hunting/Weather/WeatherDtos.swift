import Foundation

struct WeatherResponse: Codable {
    let main: WeatherMain
    let wind: WeatherWind
    let clouds: WeatherClouds
    let sys: WeatherSys
    let weather: [WeatherCondition]
    let visibility: Int?
}

struct WeatherMain: Codable {
    let temp: Double
    let feelsLike: Double
    let humidity: Int
    let pressure: Int

    enum CodingKeys: String, CodingKey {
        case temp
        case feelsLike = "feels_like"
        case humidity
        case pressure
    }
}

struct WeatherWind: Codable {
    let speed: Double
    let deg: Int?
    let gust: Double?
}

struct WeatherClouds: Codable {
    let all: Int
}

struct WeatherSys: Codable {
    let sunrise: Int
    let sunset: Int
}

struct WeatherCondition: Codable {
    let description: String
    let icon: String
}
