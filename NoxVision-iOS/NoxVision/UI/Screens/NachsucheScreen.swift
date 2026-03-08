import SwiftUI
import CoreLocation

struct NachsucheScreen: View {
    let onBack: () -> Void

    @EnvironmentObject var database: HuntingDatabaseManager
    @StateObject private var locationManager = HuntingLocationManager()
    @State private var showAddWaypoint = false
    @State private var selectedWaypointType: WaypointType = .anschuss
    @State private var targetWaypoint: Waypoint?

    var body: some View {
        VStack(spacing: 0) {
            NoxNavigationHeader(
                title: NSLocalizedString("tracking", comment: ""),
                onBack: onBack
            )

            ScrollView {
                VStack(spacing: 16) {
                    // Compass
                    compassSection

                    // Current position
                    positionCard

                    // Target info
                    if let target = targetWaypoint {
                        targetCard(target)
                    }

                    // Waypoints
                    waypointsList
                }
                .padding(.horizontal, 16)
                .padding(.bottom, 100)
            }

            // FAB
            VStack {
                Spacer()
                HStack {
                    Spacer()
                    Button(action: { showAddWaypoint = true }) {
                        Image(systemName: "plus")
                            .font(.system(size: 24, weight: .medium))
                            .foregroundColor(NoxColors.onPrimary)
                            .frame(width: 56, height: 56)
                            .background(NoxColors.primary)
                            .cornerRadius(16)
                            .shadow(color: .black.opacity(0.3), radius: 8, y: 4)
                    }
                    .padding(.trailing, 16)
                    .padding(.bottom, 24)
                }
            }
        }
        .onAppear {
            locationManager.requestPermission()
            locationManager.startUpdating()
            database.fetchAllWaypoints()
        }
        .onDisappear {
            locationManager.stopUpdating()
        }
        .sheet(isPresented: $showAddWaypoint) {
            addWaypointSheet
        }
    }

    // MARK: - Compass
    private var compassSection: some View {
        VStack(spacing: 8) {
            if let heading = locationManager.heading {
                TrackingCompass(
                    heading: heading.magneticHeading,
                    targetBearing: targetBearing
                )
                .frame(height: 200)
            } else {
                VStack {
                    Image(systemName: "location.north.line")
                        .font(.system(size: 48))
                        .foregroundColor(NoxColors.outline)
                    Text(NSLocalizedString("compass_unavailable", comment: ""))
                        .font(.system(size: 13))
                        .foregroundColor(NoxColors.onSurfaceVariant)
                }
                .frame(height: 200)
            }
        }
        .noxCard()
    }

    private var targetBearing: Double? {
        guard let target = targetWaypoint else { return nil }
        return locationManager.bearingTo(lat: target.latitude, lon: target.longitude)
    }

    // MARK: - Position Card
    private var positionCard: some View {
        VStack(spacing: 4) {
            NoxSectionHeader(title: NSLocalizedString("current_position", comment: ""))
            if let loc = locationManager.currentLocation {
                NoxInfoRow(
                    label: "Lat/Lon",
                    value: String(format: "%.6f, %.6f", loc.coordinate.latitude, loc.coordinate.longitude)
                )
                NoxInfoRow(
                    label: NSLocalizedString("accuracy", comment: ""),
                    value: String(format: "%.0fm", loc.horizontalAccuracy),
                    valueColor: loc.horizontalAccuracy < 10 ? NoxColors.success : NoxColors.warning
                )
            } else {
                Text(NSLocalizedString("position_error", comment: ""))
                    .font(.system(size: 13))
                    .foregroundColor(NoxColors.error)
            }
        }
        .noxCard()
    }

    // MARK: - Target Card
    private func targetCard(_ target: Waypoint) -> some View {
        VStack(spacing: 4) {
            HStack {
                Text(String(format: NSLocalizedString("target", comment: ""), target.type.displayName))
                    .font(.system(size: 14, weight: .medium))
                    .foregroundColor(NoxColors.onSurface)
                Spacer()
                if let loc = locationManager.currentLocation {
                    let dist = locationManager.distanceBetween(
                        lat1: loc.coordinate.latitude, lon1: loc.coordinate.longitude,
                        lat2: target.latitude, lon2: target.longitude
                    )
                    Text(String(format: "%.0fm", dist))
                        .font(.system(size: 16, weight: .bold))
                        .foregroundColor(NoxColors.primary)
                }
            }
        }
        .noxCard()
    }

    // MARK: - Waypoints List
    private var waypointsList: some View {
        VStack(spacing: 8) {
            NoxSectionHeader(title: NSLocalizedString("waypoints", comment: ""))

            if database.waypoints.isEmpty {
                VStack(spacing: 8) {
                    Text(NSLocalizedString("no_waypoints", comment: ""))
                        .font(.system(size: 14))
                        .foregroundColor(NoxColors.onSurfaceVariant)
                    Text(NSLocalizedString("tap_to_add_waypoint", comment: ""))
                        .font(.system(size: 12))
                        .foregroundColor(NoxColors.outline)
                }
                .frame(maxWidth: .infinity)
                .padding(24)
            } else {
                ForEach(database.waypoints, id: \.id) { waypoint in
                    WaypointCard(
                        waypoint: waypoint,
                        isTarget: waypoint.id == targetWaypoint?.id,
                        distance: distanceTo(waypoint),
                        onTap: { targetWaypoint = waypoint }
                    )
                    .contextMenu {
                        Button(role: .destructive) {
                            database.deleteWaypoint(waypoint)
                        } label: {
                            Label(NSLocalizedString("delete", comment: ""), systemImage: "trash")
                        }
                    }
                }
            }
        }
    }

    private func distanceTo(_ waypoint: Waypoint) -> Double? {
        guard let loc = locationManager.currentLocation else { return nil }
        return locationManager.distanceBetween(
            lat1: loc.coordinate.latitude, lon1: loc.coordinate.longitude,
            lat2: waypoint.latitude, lon2: waypoint.longitude
        )
    }

    // MARK: - Add Waypoint Sheet
    private var addWaypointSheet: some View {
        NavigationView {
            VStack(spacing: 20) {
                Text(NSLocalizedString("select_type", comment: ""))
                    .font(.system(size: 16, weight: .medium))
                    .foregroundColor(NoxColors.onSurface)

                ForEach(WaypointType.allCases, id: \.self) { type in
                    Button(action: {
                        addWaypoint(type: type)
                        showAddWaypoint = false
                    }) {
                        HStack {
                            Circle()
                                .fill(Color(red: type.color.red, green: type.color.green, blue: type.color.blue))
                                .frame(width: 12, height: 12)
                            Text(type.displayName)
                                .font(.system(size: 15))
                                .foregroundColor(NoxColors.onSurface)
                            Spacer()
                        }
                        .padding(16)
                        .background(NoxColors.cardBackground)
                        .cornerRadius(12)
                    }
                }

                Spacer()
            }
            .padding(16)
            .background(NoxColors.background)
            .navigationTitle(NSLocalizedString("add_waypoint", comment: ""))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button(NSLocalizedString("cancel", comment: "")) {
                        showAddWaypoint = false
                    }
                }
            }
        }
    }

    private func addWaypoint(type: WaypointType) {
        guard let loc = locationManager.currentLocation else { return }
        let waypoint = Waypoint(
            latitude: loc.coordinate.latitude,
            longitude: loc.coordinate.longitude,
            type: type,
            compassBearing: locationManager.heading.map { Float($0.magneticHeading) }
        )
        database.addWaypoint(waypoint)
    }
}

// MARK: - Waypoint Card
struct WaypointCard: View {
    let waypoint: Waypoint
    let isTarget: Bool
    let distance: Double?
    let onTap: () -> Void

    var body: some View {
        Button(action: onTap) {
            HStack(spacing: 12) {
                Circle()
                    .fill(Color(red: waypoint.type.color.red, green: waypoint.type.color.green, blue: waypoint.type.color.blue))
                    .frame(width: 12, height: 12)

                VStack(alignment: .leading, spacing: 2) {
                    Text(waypoint.type.displayName)
                        .font(.system(size: 14, weight: .medium))
                        .foregroundColor(NoxColors.onSurface)
                    Text(waypoint.timestamp, style: .time)
                        .font(.system(size: 12))
                        .foregroundColor(NoxColors.onSurfaceVariant)
                }

                Spacer()

                if let dist = distance {
                    Text(String(format: "%.0fm", dist))
                        .font(.system(size: 14, weight: .bold))
                        .foregroundColor(NoxColors.primary)
                }

                if isTarget {
                    Image(systemName: "scope")
                        .font(.system(size: 16))
                        .foregroundColor(NoxColors.primary)
                }
            }
            .padding(12)
            .background(isTarget ? NoxColors.primary.opacity(0.1) : NoxColors.cardBackground)
            .cornerRadius(10)
            .overlay(
                RoundedRectangle(cornerRadius: 10)
                    .stroke(isTarget ? NoxColors.primary : .clear, lineWidth: 1)
            )
        }
    }
}

// MARK: - Tracking Compass
struct TrackingCompass: View {
    let heading: Double
    let targetBearing: Double?

    var body: some View {
        Canvas { context, size in
            let center = CGPoint(x: size.width / 2, y: size.height / 2)
            let radius = min(size.width, size.height) / 2 - 20

            // Draw circle
            let circlePath = Path(ellipseIn: CGRect(
                x: center.x - radius, y: center.y - radius,
                width: radius * 2, height: radius * 2
            ))
            context.stroke(circlePath, with: .color(NoxColors.outline), lineWidth: 1)

            // Draw cardinal directions (rotated by heading)
            let cardinals = [("N", 0.0), ("O", 90.0), ("S", 180.0), ("W", 270.0)]
            for (label, angle) in cardinals {
                let rotated = (angle - heading) * .pi / 180
                let x = center.x + cos(rotated - .pi / 2) * (radius - 12)
                let y = center.y + sin(rotated - .pi / 2) * (radius - 12)

                let isNorth = label == "N"
                context.draw(
                    Text(label)
                        .font(.system(size: isNorth ? 14 : 11, weight: isNorth ? .bold : .regular))
                        .foregroundColor(isNorth ? .red : NoxColors.onSurfaceVariant),
                    at: CGPoint(x: x, y: y)
                )
            }

            // Draw tick marks
            for i in 0..<36 {
                let angle = (Double(i) * 10 - heading) * .pi / 180
                let isMajor = i % 9 == 0
                let innerR = radius - (isMajor ? 8 : 4)
                var tickPath = Path()
                tickPath.move(to: CGPoint(
                    x: center.x + cos(angle - .pi / 2) * innerR,
                    y: center.y + sin(angle - .pi / 2) * innerR
                ))
                tickPath.addLine(to: CGPoint(
                    x: center.x + cos(angle - .pi / 2) * radius,
                    y: center.y + sin(angle - .pi / 2) * radius
                ))
                context.stroke(tickPath, with: .color(NoxColors.outline.opacity(isMajor ? 0.8 : 0.4)), lineWidth: isMajor ? 1.5 : 0.5)
            }

            // Draw target bearing arrow
            if let bearing = targetBearing {
                let arrowAngle = (bearing - heading) * .pi / 180 - .pi / 2
                let arrowEnd = CGPoint(
                    x: center.x + cos(arrowAngle) * (radius - 25),
                    y: center.y + sin(arrowAngle) * (radius - 25)
                )

                var arrowPath = Path()
                arrowPath.move(to: center)
                arrowPath.addLine(to: arrowEnd)
                context.stroke(arrowPath, with: .color(NoxColors.primary), lineWidth: 2.5)

                // Arrowhead
                let headSize: CGFloat = 10
                let ha1 = arrowAngle + .pi * 0.85
                let ha2 = arrowAngle - .pi * 0.85
                var headPath = Path()
                headPath.move(to: arrowEnd)
                headPath.addLine(to: CGPoint(x: arrowEnd.x + cos(ha1) * headSize, y: arrowEnd.y + sin(ha1) * headSize))
                headPath.move(to: arrowEnd)
                headPath.addLine(to: CGPoint(x: arrowEnd.x + cos(ha2) * headSize, y: arrowEnd.y + sin(ha2) * headSize))
                context.stroke(headPath, with: .color(NoxColors.primary), lineWidth: 2.5)
            }

            // Center dot
            let dotPath = Path(ellipseIn: CGRect(x: center.x - 4, y: center.y - 4, width: 8, height: 8))
            context.fill(dotPath, with: .color(NoxColors.onSurface))
        }
    }
}
