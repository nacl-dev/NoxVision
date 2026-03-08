import SwiftUI

enum HuntingScreenRoute {
    case hub, abschussList, abschussForm, nachsuche, map, calendar, weather
}

struct HuntingHubScreen: View {
    @Binding var isPresented: Bool
    @EnvironmentObject var settings: CameraSettingsStore
    @EnvironmentObject var database: HuntingDatabaseManager
    @State private var currentRoute: HuntingScreenRoute = .hub
    @State private var editingRecord: HuntRecord?

    var body: some View {
        ZStack {
            NoxColors.background.ignoresSafeArea()

            switch currentRoute {
            case .hub:
                hubContent
            case .abschussList:
                AbschussListScreen(
                    onBack: { currentRoute = .hub },
                    onNew: { currentRoute = .abschussForm },
                    onEdit: { record in
                        editingRecord = record
                        currentRoute = .abschussForm
                    }
                )
            case .abschussForm:
                AbschussFormScreen(
                    onBack: {
                        editingRecord = nil
                        currentRoute = .abschussList
                    },
                    editRecord: editingRecord
                )
            case .nachsuche:
                NachsucheScreen(onBack: { currentRoute = .hub })
            case .map:
                MapScreen(onBack: { currentRoute = .hub })
            case .calendar:
                CalendarScreen(onBack: { currentRoute = .hub })
            case .weather:
                WeatherScreen(onBack: { currentRoute = .hub })
            }
        }
        .onAppear {
            database.fetchHuntRecords()
        }
    }

    private var hubContent: some View {
        VStack(spacing: 0) {
            NoxNavigationHeader(
                title: NSLocalizedString("hunting_assistant", comment: ""),
                onBack: { isPresented = false }
            )

            ScrollView {
                VStack(spacing: 12) {
                    // Quick info cards
                    HStack(spacing: 12) {
                        let moonInfo = MoonPhaseCalculator.calculateMoonPhase()
                        NoxQuickInfoCard(
                            icon: "moon.fill",
                            title: moonInfo.phase.germanName,
                            value: moonInfo.phase.icon,
                            iconColor: .yellow
                        )

                        NoxQuickInfoCard(
                            icon: "doc.text",
                            title: NSLocalizedString("hunting_diary", comment: ""),
                            value: "\(database.huntRecords.count)",
                            iconColor: NoxColors.primary
                        )
                    }
                    .padding(.top, 8)

                    NoxSectionHeader(title: NSLocalizedString("main_features", comment: ""))

                    NoxFeatureCard(
                        icon: "scope",
                        title: NSLocalizedString("kill_documentation", comment: ""),
                        subtitle: NSLocalizedString("kill_documentation_subtitle", comment: ""),
                        iconColor: .red
                    ) { currentRoute = .abschussList }

                    NoxFeatureCard(
                        icon: "location.north.line",
                        title: NSLocalizedString("tracking", comment: ""),
                        subtitle: NSLocalizedString("tracking_subtitle", comment: ""),
                        iconColor: .orange
                    ) { currentRoute = .nachsuche }

                    NoxSectionHeader(title: NSLocalizedString("information", comment: ""))

                    NoxFeatureCard(
                        icon: "cloud.sun",
                        title: NSLocalizedString("weather", comment: ""),
                        subtitle: NSLocalizedString("weather_subtitle", comment: ""),
                        iconColor: NoxColors.info
                    ) { currentRoute = .weather }

                    NoxFeatureCard(
                        icon: "calendar",
                        title: NSLocalizedString("hunting_seasons", comment: ""),
                        subtitle: NSLocalizedString("hunting_seasons_subtitle", comment: ""),
                        iconColor: .purple
                    ) { currentRoute = .calendar }

                    NoxFeatureCard(
                        icon: "map",
                        title: NSLocalizedString("map", comment: ""),
                        subtitle: NSLocalizedString("map_subtitle", comment: ""),
                        iconColor: NoxColors.success
                    ) { currentRoute = .map }
                }
                .padding(.horizontal, 16)
                .padding(.bottom, 32)
            }
        }
    }
}
