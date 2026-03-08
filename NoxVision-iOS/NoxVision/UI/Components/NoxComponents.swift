import SwiftUI

// MARK: - Section Header
struct NoxSectionHeader: View {
    let title: String

    var body: some View {
        Text(title)
            .font(.system(size: 12, weight: .semibold))
            .foregroundColor(NoxColors.primary)
            .tracking(1)
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(.top, 16)
            .padding(.bottom, 4)
    }
}

// MARK: - Feature Card (for settings/hub navigation)
struct NoxFeatureCard: View {
    let icon: String
    let title: String
    let subtitle: String
    var iconColor: Color = NoxColors.primary
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack(spacing: 16) {
                Image(systemName: icon)
                    .font(.system(size: 24))
                    .foregroundColor(iconColor)
                    .frame(width: 44, height: 44)
                    .background(iconColor.opacity(0.12))
                    .cornerRadius(12)

                VStack(alignment: .leading, spacing: 2) {
                    Text(title)
                        .font(.system(size: 16, weight: .medium))
                        .foregroundColor(NoxColors.onSurface)
                    Text(subtitle)
                        .font(.system(size: 13))
                        .foregroundColor(NoxColors.onSurfaceVariant)
                }

                Spacer()

                Image(systemName: "chevron.right")
                    .font(.system(size: 14))
                    .foregroundColor(NoxColors.outline)
            }
            .padding(16)
            .background(NoxColors.cardBackground)
            .cornerRadius(16)
            .overlay(
                RoundedRectangle(cornerRadius: 16)
                    .stroke(NoxColors.outlineVariant, lineWidth: 0.5)
            )
        }
    }
}

// MARK: - Toggle Row
struct NoxToggleRow: View {
    let title: String
    @Binding var isOn: Bool
    var subtitle: String? = nil

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            Toggle(isOn: $isOn) {
                Text(title)
                    .font(.system(size: 15))
                    .foregroundColor(NoxColors.onSurface)
            }
            .tint(NoxColors.primary)

            if let subtitle {
                Text(subtitle)
                    .font(.system(size: 12))
                    .foregroundColor(NoxColors.onSurfaceVariant)
                    .padding(.leading, 2)
            }
        }
        .padding(.vertical, 4)
    }
}

// MARK: - Info Row
struct NoxInfoRow: View {
    let label: String
    let value: String
    var valueColor: Color = NoxColors.onSurfaceVariant

    var body: some View {
        HStack {
            Text(label)
                .font(.system(size: 14))
                .foregroundColor(NoxColors.onSurfaceVariant)
            Spacer()
            Text(value)
                .font(.system(size: 14, weight: .medium))
                .foregroundColor(valueColor)
        }
        .padding(.vertical, 2)
    }
}

// MARK: - Chip
struct NoxChip: View {
    let label: String
    let isSelected: Bool
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Text(label)
                .font(.system(size: 13, weight: .medium))
                .padding(.horizontal, 16)
                .padding(.vertical, 8)
                .background(isSelected ? NoxColors.primary : NoxColors.surfaceVariant)
                .foregroundColor(isSelected ? NoxColors.onPrimary : NoxColors.onSurfaceVariant)
                .cornerRadius(20)
        }
    }
}

// MARK: - Quick Info Card
struct NoxQuickInfoCard: View {
    let icon: String
    let title: String
    let value: String
    var iconColor: Color = NoxColors.primary

    var body: some View {
        VStack(spacing: 8) {
            Image(systemName: icon)
                .font(.system(size: 20))
                .foregroundColor(iconColor)
            Text(value)
                .font(.system(size: 18, weight: .bold))
                .foregroundColor(NoxColors.onSurface)
            Text(title)
                .font(.system(size: 11))
                .foregroundColor(NoxColors.onSurfaceVariant)
        }
        .frame(maxWidth: .infinity)
        .padding(12)
        .background(NoxColors.cardBackground)
        .cornerRadius(12)
        .overlay(
            RoundedRectangle(cornerRadius: 12)
                .stroke(NoxColors.outlineVariant, lineWidth: 0.5)
        )
    }
}

// MARK: - Back Button Header
struct NoxNavigationHeader: View {
    let title: String
    let onBack: () -> Void
    var trailing: AnyView? = nil

    var body: some View {
        HStack {
            Button(action: onBack) {
                HStack(spacing: 4) {
                    Image(systemName: "chevron.left")
                        .font(.system(size: 16, weight: .semibold))
                    Text(NSLocalizedString("back", comment: ""))
                        .font(.system(size: 16))
                }
                .foregroundColor(NoxColors.primary)
            }

            Spacer()

            Text(title)
                .font(.system(size: 18, weight: .semibold))
                .foregroundColor(NoxColors.onSurface)

            Spacer()

            if let trailing {
                trailing
            } else {
                Color.clear.frame(width: 60)
            }
        }
        .padding(.horizontal)
        .padding(.vertical, 8)
    }
}

// MARK: - Loading Overlay
struct NoxLoadingOverlay: View {
    let message: String

    var body: some View {
        VStack(spacing: 16) {
            ProgressView()
                .progressViewStyle(CircularProgressViewStyle(tint: NoxColors.primary))
                .scaleEffect(1.2)
            Text(message)
                .font(.system(size: 14))
                .foregroundColor(NoxColors.onSurfaceVariant)
        }
        .padding(24)
        .background(NoxColors.surface.opacity(0.95))
        .cornerRadius(16)
    }
}
