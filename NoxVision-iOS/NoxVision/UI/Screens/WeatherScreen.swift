import SwiftUI

struct WeatherScreen: View {
    let onBack: () -> Void

    @EnvironmentObject var database: HuntingDatabaseManager
    @StateObject private var locationManager = HuntingLocationManager()
    @State private var weather: CachedWeather?
    @State private var isLoading = false
    @State private var error: String?
    @State private var moonInfo = MoonPhaseCalculator.calculateMoonPhase()

    var body: some View {
        VStack(spacing: 0) {
            NoxNavigationHeader(
                title: NSLocalizedString("weather", comment: ""),
                onBack: onBack,
                trailing: AnyView(
                    Button(action: fetchWeather) {
                        Image(systemName: "arrow.clockwise")
                            .foregroundColor(NoxColors.primary)
                    }
                )
            )

            if isLoading {
                Spacer()
                NoxLoadingOverlay(message: NSLocalizedString("loading", comment: ""))
                Spacer()
            } else if let weather = weather {
                ScrollView {
                    VStack(spacing: 12) {
                        // Main weather card
                        mainWeatherCard(weather)

                        // Wind compass
                        windCard(weather)

                        // Details
                        detailsCard(weather)

                        // Moon phase
                        moonPhaseCard

                        // Last updated
                        if weather.isExpired {
                            Text(NSLocalizedString("data_outdated", comment: ""))
                                .font(.system(size: 12))
                                .foregroundColor(NoxColors.warning)
                        }

                        Text(String(format: NSLocalizedString("last_updated", comment: ""),
                                    weather.timestamp.formatted(date: .abbreviated, time: .shortened)))
                            .font(.system(size: 11))
                            .foregroundColor(NoxColors.outline)
                    }
                    .padding(.horizontal, 16)
                    .padding(.bottom, 32)
                }
            } else {
                Spacer()
                VStack(spacing: 16) {
                    Image(systemName: "cloud.sun")
                        .font(.system(size: 48))
                        .foregroundColor(NoxColors.outline)
                    Text(NSLocalizedString("no_weather_data", comment: ""))
                        .foregroundColor(NoxColors.onSurfaceVariant)
                    Button(action: fetchWeather) {
                        Text(NSLocalizedString("fetch_weather", comment: ""))
                            .buttonStyle(NoxButtonStyle())
                    }
                    .buttonStyle(NoxButtonStyle())
                }
                Spacer()
            }
        }
        .onAppear {
            locationManager.requestPermission()
            weather = database.getCachedWeather()
            if weather == nil || weather?.isExpired == true {
                fetchWeather()
            }
        }
    }

    private func mainWeatherCard(_ w: CachedWeather) -> some View {
        VStack(spacing: 8) {
            Text(String(format: "%.0f°C", w.temperature))
                .font(.system(size: 48, weight: .thin))
                .foregroundColor(NoxColors.onSurface)

            Text(String(format: NSLocalizedString("feels_like", comment: ""), String(format: "%.0f°C", w.feelsLike)))
                .font(.system(size: 14))
                .foregroundColor(NoxColors.onSurfaceVariant)

            Text(w.weatherDescription.capitalized)
                .font(.system(size: 16, weight: .medium))
                .foregroundColor(NoxColors.onSurface)
        }
        .frame(maxWidth: .infinity)
        .noxCard()
    }

    private func windCard(_ w: CachedWeather) -> some View {
        VStack(spacing: 12) {
            NoxSectionHeader(title: NSLocalizedString("wind", comment: ""))

            // Wind compass
            WindCompass(direction: Double(w.windDirection))
                .frame(height: 120)

            HStack(spacing: 24) {
                VStack {
                    Text(String(format: "%.1f m/s", w.windSpeed))
                        .font(.system(size: 16, weight: .bold))
                        .foregroundColor(NoxColors.onSurface)
                    Text(String(format: NSLocalizedString("from_direction", comment: ""), w.windDirectionName))
                        .font(.system(size: 12))
                        .foregroundColor(NoxColors.onSurfaceVariant)
                }

                if let gust = w.windGust {
                    VStack {
                        Text(String(format: "%.1f m/s", gust))
                            .font(.system(size: 16, weight: .bold))
                            .foregroundColor(NoxColors.warning)
                        Text(String(format: NSLocalizedString("gusts", comment: ""), ""))
                            .font(.system(size: 12))
                            .foregroundColor(NoxColors.onSurfaceVariant)
                    }
                }
            }
        }
        .noxCard()
    }

    private func detailsCard(_ w: CachedWeather) -> some View {
        VStack(spacing: 8) {
            NoxSectionHeader(title: NSLocalizedString("details", comment: ""))
            NoxInfoRow(label: NSLocalizedString("air_humidity", comment: ""), value: "\(w.humidity)%")
            NoxInfoRow(label: NSLocalizedString("air_pressure", comment: ""), value: "\(w.pressure) hPa")
            NoxInfoRow(label: NSLocalizedString("cloud_cover", comment: ""), value: "\(w.cloudiness)%")
            NoxInfoRow(label: NSLocalizedString("visibility", comment: ""), value: String(format: "%.1f km", Double(w.visibility) / 1000))
            Divider().background(NoxColors.outlineVariant)
            NoxInfoRow(label: NSLocalizedString("sunrise", comment: ""), value: w.sunrise.formatted(date: .omitted, time: .shortened))
            NoxInfoRow(label: NSLocalizedString("sunset", comment: ""), value: w.sunset.formatted(date: .omitted, time: .shortened))
        }
        .noxCard()
    }

    private var moonPhaseCard: some View {
        VStack(spacing: 8) {
            NoxSectionHeader(title: NSLocalizedString("moon_phase", comment: ""))
            HStack(spacing: 16) {
                Text(moonInfo.phase.icon)
                    .font(.system(size: 36))
                VStack(alignment: .leading, spacing: 4) {
                    Text(moonInfo.phase.germanName)
                        .font(.system(size: 16, weight: .medium))
                        .foregroundColor(NoxColors.onSurface)
                    Text(String(format: NSLocalizedString("illumination", comment: ""), Int(moonInfo.illuminationPercent)))
                        .font(.system(size: 13))
                        .foregroundColor(NoxColors.onSurfaceVariant)
                    Text(moonInfo.activityPrediction.germanText)
                        .font(.system(size: 13))
                        .foregroundColor(activityColor)
                }
                Spacer()
            }
        }
        .noxCard()
    }

    private var activityColor: Color {
        switch moonInfo.activityPrediction {
        case .veryHigh: return NoxColors.success
        case .high: return NoxColors.primary
        case .medium: return NoxColors.warning
        case .low: return .orange
        case .veryLow: return NoxColors.error
        }
    }

    private func fetchWeather() {
        isLoading = true
        locationManager.requestPermission()

        Task {
            // Wait briefly for location
            try? await Task.sleep(nanoseconds: 1_000_000_000)

            let lat = locationManager.currentLocation?.coordinate.latitude ?? 51.1657
            let lon = locationManager.currentLocation?.coordinate.longitude ?? 10.4515

            let client = WeatherApiClient()
            let result = await client.fetchWeather(latitude: lat, longitude: lon)

            await MainActor.run {
                isLoading = false
                switch result {
                case .success(let w):
                    weather = w
                    database.saveWeather(w)
                case .failure(let err):
                    error = err.localizedDescription
                    AppLogger.shared.log("Weather fetch failed: \(err.localizedDescription)", type: .error)
                }
            }
        }
    }
}

// MARK: - Wind Compass
struct WindCompass: View {
    let direction: Double

    var body: some View {
        Canvas { context, size in
            let center = CGPoint(x: size.width / 2, y: size.height / 2)
            let radius = min(size.width, size.height) / 2 - 10

            // Draw circle
            let circlePath = Path(ellipseIn: CGRect(
                x: center.x - radius, y: center.y - radius,
                width: radius * 2, height: radius * 2
            ))
            context.stroke(circlePath, with: .color(NoxColors.outline), lineWidth: 1)

            // Draw cardinal directions
            let cardinals = ["N", "O", "S", "W"]
            for (i, label) in cardinals.enumerated() {
                let angle = Double(i) * .pi / 2 - .pi / 2
                let x = center.x + cos(angle) * (radius + 8)
                let y = center.y + sin(angle) * (radius + 8)
                context.draw(
                    Text(label).font(.system(size: 10, weight: .bold)).foregroundColor(NoxColors.onSurfaceVariant),
                    at: CGPoint(x: x, y: y)
                )
            }

            // Draw wind direction arrow
            let windAngle = direction * .pi / 180 - .pi / 2
            let arrowEnd = CGPoint(
                x: center.x + cos(windAngle) * (radius - 15),
                y: center.y + sin(windAngle) * (radius - 15)
            )

            var arrowPath = Path()
            arrowPath.move(to: center)
            arrowPath.addLine(to: arrowEnd)
            context.stroke(arrowPath, with: .color(NoxColors.primary), lineWidth: 2)

            // Arrowhead
            let headSize: CGFloat = 8
            let headAngle1 = windAngle + .pi * 0.85
            let headAngle2 = windAngle - .pi * 0.85
            var headPath = Path()
            headPath.move(to: arrowEnd)
            headPath.addLine(to: CGPoint(
                x: arrowEnd.x + cos(headAngle1) * headSize,
                y: arrowEnd.y + sin(headAngle1) * headSize
            ))
            headPath.move(to: arrowEnd)
            headPath.addLine(to: CGPoint(
                x: arrowEnd.x + cos(headAngle2) * headSize,
                y: arrowEnd.y + sin(headAngle2) * headSize
            ))
            context.stroke(headPath, with: .color(NoxColors.primary), lineWidth: 2)
        }
    }
}
