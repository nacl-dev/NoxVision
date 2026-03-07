import Foundation

class WeatherApiClient {
    // In production, store this in a secure config or keychain
    private let apiKey: String
    private static let baseURL = "https://api.openweathermap.org/data/2.5/weather"

    init(apiKey: String = "") {
        // Load from Info.plist or environment
        if apiKey.isEmpty {
            self.apiKey = Bundle.main.object(forInfoDictionaryKey: "OPENWEATHER_API_KEY") as? String ?? ""
        } else {
            self.apiKey = apiKey
        }
    }

    func fetchWeather(latitude: Double, longitude: Double) async -> Result<CachedWeather, Error> {
        let sanitizedKey = apiKey.trimmingCharacters(in: .whitespacesAndNewlines).replacingOccurrences(of: "\"", with: "")
        guard !sanitizedKey.isEmpty else {
            return .failure(NSError(domain: "WeatherAPI", code: -1, userInfo: [NSLocalizedDescriptionKey: "OPENWEATHER_API_KEY is missing"]))
        }

        guard let url = URL(string: "\(Self.baseURL)?lat=\(latitude)&lon=\(longitude)&appid=\(sanitizedKey)&units=metric&lang=de") else {
            return .failure(NSError(domain: "WeatherAPI", code: -2, userInfo: [NSLocalizedDescriptionKey: "Invalid URL"]))
        }

        var request = URLRequest(url: url)
        request.timeoutInterval = 10.0

        do {
            let (data, response) = try await URLSession.shared.data(for: request)

            guard let httpResponse = response as? HTTPURLResponse else {
                return .failure(NSError(domain: "WeatherAPI", code: -3, userInfo: [NSLocalizedDescriptionKey: "Invalid response"]))
            }

            guard httpResponse.statusCode == 200 else {
                let errorBody = String(data: data, encoding: .utf8)?.prefix(200) ?? ""
                return .failure(NSError(domain: "WeatherAPI", code: httpResponse.statusCode,
                    userInfo: [NSLocalizedDescriptionKey: "HTTP Error: \(httpResponse.statusCode) (\(errorBody))"]))
            }

            let weather = try parseWeatherResponse(data, latitude: latitude, longitude: longitude)
            return .success(weather)
        } catch {
            return .failure(error)
        }
    }

    func parseWeatherResponse(_ data: Data, latitude: Double, longitude: Double) throws -> CachedWeather {
        let decoder = JSONDecoder()
        let response = try decoder.decode(WeatherResponse.self, from: data)

        let main = response.main
        let wind = response.wind
        let clouds = response.clouds
        let sys = response.sys
        let condition = response.weather.first ?? WeatherCondition(description: "", icon: "")

        return CachedWeather(
            timestamp: Date(),
            latitude: latitude,
            longitude: longitude,
            temperature: main.temp,
            feelsLike: main.feelsLike,
            humidity: main.humidity,
            pressure: main.pressure,
            windSpeed: wind.speed,
            windDirection: wind.deg ?? 0,
            windGust: wind.gust.flatMap { $0 > 0 ? $0 : nil },
            cloudiness: clouds.all,
            visibility: response.visibility ?? 10000,
            weatherDescription: condition.description,
            icon: condition.icon,
            sunrise: Date(timeIntervalSince1970: TimeInterval(sys.sunrise)),
            sunset: Date(timeIntervalSince1970: TimeInterval(sys.sunset))
        )
    }
}
