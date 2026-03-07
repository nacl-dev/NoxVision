import Foundation
import SwiftData
import SwiftUI

@MainActor
class HuntingDatabaseManager: ObservableObject {
    static let shared = HuntingDatabaseManager()

    let modelContainer: ModelContainer
    let modelContext: ModelContext

    @Published var huntRecords: [HuntRecord] = []
    @Published var waypoints: [Waypoint] = []
    @Published var huntingStands: [HuntingStand] = []

    private init() {
        do {
            let schema = Schema([
                HuntRecord.self,
                Waypoint.self,
                HuntingStand.self,
                CachedWeather.self
            ])
            let config = ModelConfiguration(schema: schema, isStoredInMemoryOnly: false)
            modelContainer = try ModelContainer(for: schema, configurations: [config])
            modelContext = modelContainer.mainContext
        } catch {
            fatalError("Failed to create ModelContainer: \(error)")
        }
    }

    // MARK: - Hunt Records
    func fetchHuntRecords() {
        let descriptor = FetchDescriptor<HuntRecord>(sortBy: [SortDescriptor(\.timestamp, order: .reverse)])
        huntRecords = (try? modelContext.fetch(descriptor)) ?? []
    }

    func addHuntRecord(_ record: HuntRecord) {
        modelContext.insert(record)
        try? modelContext.save()
        fetchHuntRecords()
    }

    func deleteHuntRecord(_ record: HuntRecord) {
        modelContext.delete(record)
        try? modelContext.save()
        fetchHuntRecords()
    }

    func updateHuntRecord(_ record: HuntRecord) {
        try? modelContext.save()
        fetchHuntRecords()
    }

    // MARK: - Waypoints
    func fetchWaypoints(for huntRecordId: UUID? = nil) {
        var descriptor = FetchDescriptor<Waypoint>(sortBy: [SortDescriptor(\.timestamp, order: .reverse)])
        if let huntRecordId {
            descriptor.predicate = #Predicate<Waypoint> { $0.huntRecord?.id == huntRecordId }
        }
        waypoints = (try? modelContext.fetch(descriptor)) ?? []
    }

    func fetchAllWaypoints() {
        let descriptor = FetchDescriptor<Waypoint>(sortBy: [SortDescriptor(\.timestamp, order: .reverse)])
        waypoints = (try? modelContext.fetch(descriptor)) ?? []
    }

    func addWaypoint(_ waypoint: Waypoint) {
        modelContext.insert(waypoint)
        try? modelContext.save()
        fetchAllWaypoints()
    }

    func deleteWaypoint(_ waypoint: Waypoint) {
        modelContext.delete(waypoint)
        try? modelContext.save()
        fetchAllWaypoints()
    }

    // MARK: - Hunting Stands
    func fetchHuntingStands() {
        let descriptor = FetchDescriptor<HuntingStand>(sortBy: [SortDescriptor(\.name)])
        huntingStands = (try? modelContext.fetch(descriptor)) ?? []
    }

    func addHuntingStand(_ stand: HuntingStand) {
        modelContext.insert(stand)
        try? modelContext.save()
        fetchHuntingStands()
    }

    func deleteHuntingStand(_ stand: HuntingStand) {
        modelContext.delete(stand)
        try? modelContext.save()
        fetchHuntingStands()
    }

    // MARK: - Cached Weather
    func getCachedWeather() -> CachedWeather? {
        let descriptor = FetchDescriptor<CachedWeather>(sortBy: [SortDescriptor(\.timestamp, order: .reverse)])
        return try? modelContext.fetch(descriptor).first
    }

    func saveWeather(_ weather: CachedWeather) {
        // Remove old entries
        let descriptor = FetchDescriptor<CachedWeather>()
        if let existing = try? modelContext.fetch(descriptor) {
            existing.forEach { modelContext.delete($0) }
        }
        modelContext.insert(weather)
        try? modelContext.save()
    }
}
