import SwiftUI
import PhotosUI
import MapKit

struct AbschussFormScreen: View {
    let onBack: () -> Void
    var editRecord: HuntRecord?

    @EnvironmentObject var database: HuntingDatabaseManager
    @StateObject private var locationManager = HuntingLocationManager()

    @State private var wildlifeType = ""
    @State private var gender = ""
    @State private var estimatedWeight = ""
    @State private var latitude: Double?
    @State private var longitude: Double?
    @State private var bundesland = ""
    @State private var notes = ""
    @State private var selectedPhoto: PhotosPickerItem?
    @State private var photoImage: UIImage?
    @State private var showBundeslandPicker = false

    private let wildlifeTypeNames = WildlifeTypes.allTypes.map { $0.0 }
    private let bundeslaender = [
        "Baden-Württemberg", "Bayern", "Berlin", "Brandenburg", "Bremen",
        "Hamburg", "Hessen", "Mecklenburg-Vorpommern", "Niedersachsen",
        "Nordrhein-Westfalen", "Rheinland-Pfalz", "Saarland", "Sachsen",
        "Sachsen-Anhalt", "Schleswig-Holstein", "Thüringen"
    ]

    var isEditing: Bool { editRecord != nil }

    var body: some View {
        VStack(spacing: 0) {
            NoxNavigationHeader(
                title: isEditing ? NSLocalizedString("edit_kill", comment: "") : NSLocalizedString("new_kill", comment: ""),
                onBack: onBack
            )

            ScrollView {
                VStack(spacing: 16) {
                    // Wildlife section
                    wildlifeSection

                    // Location section
                    locationSection

                    // Photo section
                    photoSection

                    // Notes section
                    notesSection

                    // Save button
                    Button(action: saveRecord) {
                        Text(NSLocalizedString("save", comment: ""))
                            .font(.system(size: 16, weight: .semibold))
                            .foregroundColor(NoxColors.onPrimary)
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 14)
                            .background(wildlifeType.isEmpty ? NoxColors.outline : NoxColors.primary)
                            .cornerRadius(12)
                    }
                    .disabled(wildlifeType.isEmpty)
                }
                .padding(.horizontal, 16)
                .padding(.bottom, 32)
            }
        }
        .onAppear {
            locationManager.requestPermission()
            if let record = editRecord {
                wildlifeType = record.wildlifeType
                gender = record.gender ?? ""
                estimatedWeight = record.estimatedWeight.map { "\($0)" } ?? ""
                latitude = record.latitude
                longitude = record.longitude
                bundesland = record.bundesland ?? ""
                notes = record.notes ?? ""
            }
        }
    }

    private var wildlifeSection: some View {
        VStack(spacing: 12) {
            NoxSectionHeader(title: NSLocalizedString("wildlife", comment: ""))

            VStack(spacing: 12) {
                // Wildlife type picker
                VStack(alignment: .leading, spacing: 4) {
                    Text(NSLocalizedString("wildlife_type", comment: ""))
                        .font(.system(size: 14))
                        .foregroundColor(NoxColors.onSurfaceVariant)
                    Picker("", selection: $wildlifeType) {
                        Text(NSLocalizedString("not_specified", comment: "")).tag("")
                        ForEach(wildlifeTypeNames, id: \.self) { type in
                            Text(type).tag(type)
                        }
                    }
                    .pickerStyle(.menu)
                    .tint(NoxColors.primary)
                }

                // Gender picker
                if !wildlifeType.isEmpty {
                    VStack(alignment: .leading, spacing: 4) {
                        Text(NSLocalizedString("gender_age", comment: ""))
                            .font(.system(size: 14))
                            .foregroundColor(NoxColors.onSurfaceVariant)
                        let genders = WildlifeTypes.gendersForType(wildlifeType)
                        Picker("", selection: $gender) {
                            Text(NSLocalizedString("not_specified", comment: "")).tag("")
                            ForEach(genders, id: \.self) { g in
                                Text(g).tag(g)
                            }
                        }
                        .pickerStyle(.menu)
                        .tint(NoxColors.primary)
                    }
                }

                // Weight
                VStack(alignment: .leading, spacing: 4) {
                    Text(NSLocalizedString("estimated_weight", comment: ""))
                        .font(.system(size: 14))
                        .foregroundColor(NoxColors.onSurfaceVariant)
                    TextField("kg", text: $estimatedWeight)
                        .textFieldStyle(.plain)
                        .font(.system(size: 15))
                        .foregroundColor(NoxColors.onSurface)
                        .padding(12)
                        .background(NoxColors.surfaceVariant)
                        .cornerRadius(12)
                        .keyboardType(.numberPad)
                }
            }
            .noxCard()
        }
    }

    private var locationSection: some View {
        VStack(spacing: 12) {
            NoxSectionHeader(title: NSLocalizedString("position", comment: ""))

            VStack(spacing: 12) {
                // GPS capture button
                Button(action: captureGPS) {
                    HStack {
                        Image(systemName: "location.fill")
                        Text(NSLocalizedString("capture_gps", comment: ""))
                    }
                    .font(.system(size: 14, weight: .medium))
                    .foregroundColor(NoxColors.onPrimary)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 12)
                    .background(NoxColors.primary)
                    .cornerRadius(12)
                }

                if let lat = latitude, let lon = longitude {
                    // Mini map
                    Map(coordinateRegion: .constant(MKCoordinateRegion(
                        center: CLLocationCoordinate2D(latitude: lat, longitude: lon),
                        span: MKCoordinateSpan(latitudeDelta: 0.01, longitudeDelta: 0.01)
                    )), annotationItems: [MapPin(coordinate: CLLocationCoordinate2D(latitude: lat, longitude: lon))]) { pin in
                        MapMarker(coordinate: pin.coordinate, tint: .red)
                    }
                    .frame(height: 150)
                    .cornerRadius(12)

                    Text(String(format: "%.6f, %.6f", lat, lon))
                        .font(.system(size: 12, design: .monospaced))
                        .foregroundColor(NoxColors.onSurfaceVariant)
                }

                // Bundesland picker
                VStack(alignment: .leading, spacing: 4) {
                    Text(NSLocalizedString("bundesland", comment: ""))
                        .font(.system(size: 14))
                        .foregroundColor(NoxColors.onSurfaceVariant)
                    Picker("", selection: $bundesland) {
                        Text(NSLocalizedString("not_specified", comment: "")).tag("")
                        ForEach(bundeslaender, id: \.self) { bl in
                            Text(bl).tag(bl)
                        }
                    }
                    .pickerStyle(.menu)
                    .tint(NoxColors.primary)
                }
            }
            .noxCard()
        }
    }

    private var photoSection: some View {
        VStack(spacing: 12) {
            NoxSectionHeader(title: NSLocalizedString("photo_section", comment: ""))

            VStack(spacing: 12) {
                if let image = photoImage {
                    Image(uiImage: image)
                        .resizable()
                        .scaledToFit()
                        .frame(maxHeight: 200)
                        .cornerRadius(12)

                    Button(action: { photoImage = nil; selectedPhoto = nil }) {
                        Text(NSLocalizedString("remove_photo", comment: ""))
                            .font(.system(size: 13))
                            .foregroundColor(NoxColors.error)
                    }
                }

                PhotosPicker(selection: $selectedPhoto, matching: .images) {
                    HStack {
                        Image(systemName: "photo.badge.plus")
                        Text(NSLocalizedString("add_photo", comment: ""))
                    }
                    .font(.system(size: 14, weight: .medium))
                    .foregroundColor(NoxColors.primary)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 12)
                    .background(NoxColors.primary.opacity(0.12))
                    .cornerRadius(12)
                }
                .onChange(of: selectedPhoto) { newItem in
                    Task {
                        if let data = try? await newItem?.loadTransferable(type: Data.self),
                           let image = UIImage(data: data) {
                            photoImage = image
                        }
                    }
                }
            }
            .noxCard()
        }
    }

    private var notesSection: some View {
        VStack(spacing: 12) {
            NoxSectionHeader(title: NSLocalizedString("notes", comment: ""))

            TextEditor(text: $notes)
                .font(.system(size: 14))
                .foregroundColor(NoxColors.onSurface)
                .scrollContentBackground(.hidden)
                .frame(minHeight: 100)
                .padding(12)
                .background(NoxColors.surfaceVariant)
                .cornerRadius(12)
        }
    }

    private func captureGPS() {
        locationManager.requestSingleLocation()
        if let loc = locationManager.currentLocation {
            latitude = loc.coordinate.latitude
            longitude = loc.coordinate.longitude
        }
    }

    private func saveRecord() {
        if let record = editRecord {
            record.wildlifeType = wildlifeType
            record.gender = gender.isEmpty ? nil : gender
            record.estimatedWeight = Int(estimatedWeight)
            record.latitude = latitude
            record.longitude = longitude
            record.bundesland = bundesland.isEmpty ? nil : bundesland
            record.notes = notes.isEmpty ? nil : notes
            database.updateHuntRecord(record)
        } else {
            let moonInfo = MoonPhaseCalculator.calculateMoonPhase()
            let record = HuntRecord(
                timestamp: Date(),
                latitude: latitude,
                longitude: longitude,
                wildlifeType: wildlifeType,
                gender: gender.isEmpty ? nil : gender,
                estimatedWeight: Int(estimatedWeight),
                notes: notes.isEmpty ? nil : notes,
                moonPhase: moonInfo.phase.germanName,
                bundesland: bundesland.isEmpty ? nil : bundesland
            )
            database.addHuntRecord(record)
        }
        onBack()
    }
}

struct MapPin: Identifiable {
    let id = UUID()
    let coordinate: CLLocationCoordinate2D
}
