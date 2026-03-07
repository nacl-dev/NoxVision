import SwiftUI

struct CalendarScreen: View {
    let onBack: () -> Void

    @EnvironmentObject var settings: CameraSettingsStore
    @State private var selectedBundesland = "Nordrhein-Westfalen"
    @State private var showActiveOnly = false

    private let bundeslaender = [
        "Baden-Württemberg", "Bayern", "Berlin", "Brandenburg", "Bremen",
        "Hamburg", "Hessen", "Mecklenburg-Vorpommern", "Niedersachsen",
        "Nordrhein-Westfalen", "Rheinland-Pfalz", "Saarland", "Sachsen",
        "Sachsen-Anhalt", "Schleswig-Holstein", "Thüringen"
    ]

    var body: some View {
        VStack(spacing: 0) {
            NoxNavigationHeader(
                title: NSLocalizedString("hunting_seasons", comment: ""),
                onBack: onBack
            )

            if settings.huntingCountry.supportsGermanSeasons {
                ScrollView {
                    VStack(spacing: 12) {
                        // Bundesland picker
                        VStack(alignment: .leading, spacing: 8) {
                            Text(NSLocalizedString("bundesland", comment: ""))
                                .font(.system(size: 14))
                                .foregroundColor(NoxColors.onSurfaceVariant)
                            Picker("", selection: $selectedBundesland) {
                                ForEach(bundeslaender, id: \.self) { bl in
                                    Text(bl).tag(bl)
                                }
                            }
                            .pickerStyle(.menu)
                            .tint(NoxColors.primary)
                        }
                        .noxCard()

                        // Active only toggle
                        NoxToggleRow(
                            title: NSLocalizedString("show_active_only", comment: ""),
                            isOn: $showActiveOnly
                        )
                        .noxCard()

                        // Summary card
                        let seasons = HuntingSeasonData.getSeasonsForBundesland(selectedBundesland)
                        let active = seasons.filter { $0.isActive }
                        let upcoming = seasons.filter { $0.isUpcoming }

                        HStack(spacing: 12) {
                            NoxQuickInfoCard(
                                icon: "checkmark.circle.fill",
                                title: NSLocalizedString("active_hunting_seasons", comment: ""),
                                value: "\(active.count)",
                                iconColor: NoxColors.success
                            )
                            NoxQuickInfoCard(
                                icon: "clock",
                                title: NSLocalizedString("starting_soon_section", comment: ""),
                                value: "\(upcoming.count)",
                                iconColor: NoxColors.warning
                            )
                        }

                        // Upcoming seasons
                        if !upcoming.isEmpty {
                            NoxSectionHeader(title: NSLocalizedString("starting_soon_section", comment: ""))
                            ForEach(upcoming) { season in
                                SeasonCard(season: season, isActive: false, isUpcoming: true)
                            }
                        }

                        // Seasons list
                        let displayedSeasons = showActiveOnly ? active : seasons
                        let grouped = Dictionary(grouping: displayedSeasons) { $0.wildlifeType }

                        NoxSectionHeader(title: showActiveOnly ?
                            NSLocalizedString("active_hunting_seasons", comment: "") :
                            NSLocalizedString("all_hunting_seasons", comment: "")
                        )

                        ForEach(grouped.keys.sorted(), id: \.self) { type in
                            VStack(alignment: .leading, spacing: 8) {
                                Text(type)
                                    .font(.system(size: 15, weight: .semibold))
                                    .foregroundColor(NoxColors.onSurface)

                                ForEach(grouped[type]!) { season in
                                    SeasonCard(season: season, isActive: season.isActive, isUpcoming: false)
                                }
                            }
                        }

                        // Note
                        Text(NSLocalizedString("hunting_seasons_note", comment: ""))
                            .font(.system(size: 11))
                            .foregroundColor(NoxColors.outline)
                            .padding(.top, 8)
                    }
                    .padding(.horizontal, 16)
                    .padding(.bottom, 32)
                }
            } else {
                Spacer()
                Text(String(format: NSLocalizedString("hunting_seasons_not_available_for_country", comment: ""),
                            settings.huntingCountry.displayName))
                    .font(.system(size: 14))
                    .foregroundColor(NoxColors.onSurfaceVariant)
                    .multilineTextAlignment(.center)
                    .padding()
                Spacer()
            }
        }
    }
}

struct SeasonCard: View {
    let season: HuntingSeason
    let isActive: Bool
    let isUpcoming: Bool

    var body: some View {
        HStack(spacing: 12) {
            Circle()
                .fill(isActive ? NoxColors.success : isUpcoming ? NoxColors.warning : NoxColors.outline)
                .frame(width: 10, height: 10)

            VStack(alignment: .leading, spacing: 2) {
                Text(season.gender ?? NSLocalizedString("all_genders", comment: ""))
                    .font(.system(size: 14, weight: .medium))
                    .foregroundColor(NoxColors.onSurface)
                Text(season.periodDescription)
                    .font(.system(size: 12))
                    .foregroundColor(NoxColors.onSurfaceVariant)
            }

            Spacer()

            if isActive {
                Text("AKTIV")
                    .font(.system(size: 10, weight: .bold))
                    .foregroundColor(NoxColors.success)
                    .padding(.horizontal, 8)
                    .padding(.vertical, 4)
                    .background(NoxColors.success.opacity(0.12))
                    .cornerRadius(4)
            } else if isUpcoming, let days = season.daysUntilStart {
                Text(String(format: NSLocalizedString("in_days", comment: ""), days))
                    .font(.system(size: 11))
                    .foregroundColor(NoxColors.warning)
            }
        }
        .padding(12)
        .background(NoxColors.cardBackground)
        .cornerRadius(10)
    }
}
