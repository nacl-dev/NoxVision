import SwiftUI
import MapKit

struct MapScreen: View {
    let onBack: () -> Void

    @EnvironmentObject var database: HuntingDatabaseManager
    @StateObject private var locationManager = HuntingLocationManager()
    @State private var region = MKCoordinateRegion(
        center: CLLocationCoordinate2D(latitude: 51.1657, longitude: 10.4515),
        span: MKCoordinateSpan(latitudeDelta: 0.1, longitudeDelta: 0.1)
    )
    @State private var showAddStand = false
    @State private var longPressLocation: CLLocationCoordinate2D?
    @State private var newStandName = ""
    @State private var newStandType: HuntingStandType = .hochsitz

    var body: some View {
        ZStack {
            VStack(spacing: 0) {
                NoxNavigationHeader(
                    title: NSLocalizedString("map", comment: ""),
                    onBack: onBack
                )

                // Map
                Map(coordinateRegion: $region,
                    showsUserLocation: true,
                    annotationItems: mapAnnotations
                ) { item in
                    MapAnnotation(coordinate: item.coordinate) {
                        VStack(spacing: 2) {
                            Image(systemName: item.isStand ? "house.fill" : "mappin.circle.fill")
                                .font(.system(size: 20))
                                .foregroundColor(item.isStand ? .orange : .blue)
                            Text(item.name)
                                .font(.system(size: 9))
                                .foregroundColor(.white)
                                .padding(.horizontal, 4)
                                .padding(.vertical, 2)
                                .background(Color.black.opacity(0.7))
                                .cornerRadius(4)
                        }
                    }
                }
                .ignoresSafeArea(edges: .bottom)
                .onLongPressGesture(minimumDuration: 0.5) {
                    // Use center of map for long press
                    longPressLocation = region.center
                    showAddStand = true
                }
            }

            // Overlay controls
            VStack {
                Spacer()

                HStack {
                    // Info card
                    VStack(alignment: .leading, spacing: 4) {
                        Text(String(format: NSLocalizedString("hunting_stands", comment: ""), database.huntingStands.count))
                            .font(.system(size: 11))
                        Text(String(format: NSLocalizedString("waypoints_count", comment: ""), database.waypoints.count))
                            .font(.system(size: 11))
                    }
                    .foregroundColor(NoxColors.onSurface)
                    .padding(8)
                    .background(NoxColors.surface.opacity(0.9))
                    .cornerRadius(8)

                    Spacer()

                    // Zoom controls
                    VStack(spacing: 8) {
                        Button(action: { centerOnUser() }) {
                            Image(systemName: "location.fill")
                                .font(.system(size: 18))
                                .foregroundColor(NoxColors.primary)
                                .frame(width: 44, height: 44)
                                .background(NoxColors.surface.opacity(0.9))
                                .cornerRadius(22)
                        }

                        Button(action: zoomIn) {
                            Image(systemName: "plus")
                                .font(.system(size: 18))
                                .foregroundColor(NoxColors.onSurface)
                                .frame(width: 44, height: 44)
                                .background(NoxColors.surface.opacity(0.9))
                                .cornerRadius(22)
                        }

                        Button(action: zoomOut) {
                            Image(systemName: "minus")
                                .font(.system(size: 18))
                                .foregroundColor(NoxColors.onSurface)
                                .frame(width: 44, height: 44)
                                .background(NoxColors.surface.opacity(0.9))
                                .cornerRadius(22)
                        }
                    }
                }
                .padding(16)

                // Long press hint
                Text(NSLocalizedString("long_press_hint", comment: ""))
                    .font(.system(size: 11))
                    .foregroundColor(NoxColors.onSurfaceVariant)
                    .padding(.bottom, 8)
            }
        }
        .onAppear {
            locationManager.requestPermission()
            database.fetchHuntingStands()
            database.fetchAllWaypoints()
            centerOnUser()
        }
        .alert(NSLocalizedString("add_hunting_stand", comment: ""), isPresented: $showAddStand) {
            TextField(NSLocalizedString("stand_name", comment: ""), text: $newStandName)
            Button(NSLocalizedString("cancel", comment: ""), role: .cancel) {
                newStandName = ""
            }
            Button(NSLocalizedString("save", comment: "")) {
                if let loc = longPressLocation, !newStandName.isEmpty {
                    let stand = HuntingStand(
                        name: newStandName,
                        latitude: loc.latitude,
                        longitude: loc.longitude,
                        type: newStandType
                    )
                    database.addHuntingStand(stand)
                    newStandName = ""
                }
            }
        }
    }

    private var mapAnnotations: [MapAnnotationItem] {
        var items: [MapAnnotationItem] = []
        for stand in database.huntingStands {
            items.append(MapAnnotationItem(
                coordinate: CLLocationCoordinate2D(latitude: stand.latitude, longitude: stand.longitude),
                name: stand.name,
                isStand: true
            ))
        }
        for wp in database.waypoints {
            items.append(MapAnnotationItem(
                coordinate: CLLocationCoordinate2D(latitude: wp.latitude, longitude: wp.longitude),
                name: wp.type.displayName,
                isStand: false
            ))
        }
        return items
    }

    private func centerOnUser() {
        if let loc = locationManager.currentLocation {
            region.center = loc.coordinate
            region.span = MKCoordinateSpan(latitudeDelta: 0.02, longitudeDelta: 0.02)
        }
    }

    private func zoomIn() {
        region.span.latitudeDelta = max(region.span.latitudeDelta / 2, 0.001)
        region.span.longitudeDelta = max(region.span.longitudeDelta / 2, 0.001)
    }

    private func zoomOut() {
        region.span.latitudeDelta = min(region.span.latitudeDelta * 2, 90)
        region.span.longitudeDelta = min(region.span.longitudeDelta * 2, 180)
    }
}

struct MapAnnotationItem: Identifiable {
    let id = UUID()
    let coordinate: CLLocationCoordinate2D
    let name: String
    let isStand: Bool
}
