import SwiftUI

// Night-optimized color theme matching Android's Material3 dark theme
struct NoxColors {
    // Primary brand colors
    static let primary = Color(red: 0.65, green: 0.85, blue: 0.65)        // Soft green
    static let onPrimary = Color(red: 0.0, green: 0.22, blue: 0.0)
    static let primaryContainer = Color(red: 0.0, green: 0.31, blue: 0.0)
    static let onPrimaryContainer = Color(red: 0.78, green: 0.98, blue: 0.78)

    // Secondary colors
    static let secondary = Color(red: 0.73, green: 0.80, blue: 0.70)
    static let onSecondary = Color(red: 0.14, green: 0.20, blue: 0.12)
    static let secondaryContainer = Color(red: 0.22, green: 0.29, blue: 0.20)
    static let onSecondaryContainer = Color(red: 0.86, green: 0.93, blue: 0.83)

    // Tertiary colors
    static let tertiary = Color(red: 0.63, green: 0.83, blue: 0.85)
    static let onTertiary = Color(red: 0.0, green: 0.22, blue: 0.24)
    static let tertiaryContainer = Color(red: 0.0, green: 0.31, blue: 0.33)
    static let onTertiaryContainer = Color(red: 0.78, green: 0.97, blue: 0.98)

    // Background & Surface
    static let background = Color(red: 0.07, green: 0.07, blue: 0.07)
    static let onBackground = Color(red: 0.89, green: 0.87, blue: 0.82)
    static let surface = Color(red: 0.07, green: 0.07, blue: 0.07)
    static let onSurface = Color(red: 0.89, green: 0.87, blue: 0.82)
    static let surfaceVariant = Color(red: 0.16, green: 0.18, blue: 0.15)
    static let onSurfaceVariant = Color(red: 0.76, green: 0.78, blue: 0.73)

    // Error colors
    static let error = Color(red: 1.0, green: 0.71, blue: 0.67)
    static let onError = Color(red: 0.41, green: 0.0, blue: 0.0)

    // Status colors
    static let success = Color(red: 0.30, green: 0.85, blue: 0.40)
    static let warning = Color(red: 1.0, green: 0.76, blue: 0.03)
    static let info = Color(red: 0.26, green: 0.65, blue: 0.96)

    // Outline
    static let outline = Color(red: 0.57, green: 0.59, blue: 0.54)
    static let outlineVariant = Color(red: 0.27, green: 0.29, blue: 0.26)

    // Card surface
    static let cardBackground = Color(red: 0.11, green: 0.12, blue: 0.10)
}

// MARK: - Custom Button Style
struct NoxButtonStyle: ButtonStyle {
    var isPrimary: Bool = true

    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .font(.system(size: 14, weight: .semibold))
            .padding(.horizontal, 24)
            .padding(.vertical, 12)
            .background(isPrimary ? NoxColors.primary : NoxColors.surfaceVariant)
            .foregroundColor(isPrimary ? NoxColors.onPrimary : NoxColors.onSurface)
            .cornerRadius(12)
            .opacity(configuration.isPressed ? 0.8 : 1.0)
    }
}

// MARK: - Card Modifier
struct NoxCardModifier: ViewModifier {
    func body(content: Content) -> some View {
        content
            .padding(16)
            .background(NoxColors.cardBackground)
            .cornerRadius(16)
            .overlay(
                RoundedRectangle(cornerRadius: 16)
                    .stroke(NoxColors.outlineVariant, lineWidth: 0.5)
            )
    }
}

extension View {
    func noxCard() -> some View {
        modifier(NoxCardModifier())
    }
}
