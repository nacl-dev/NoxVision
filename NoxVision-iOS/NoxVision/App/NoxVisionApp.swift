import SwiftUI

@main
struct NoxVisionApp: App {
    @StateObject private var cameraSettings = CameraSettingsStore()
    @StateObject private var appLogger = AppLogger.shared
    @StateObject private var huntingDatabase = HuntingDatabaseManager.shared

    var body: some Scene {
        WindowGroup {
            ContentView()
                .environmentObject(cameraSettings)
                .environmentObject(appLogger)
                .environmentObject(huntingDatabase)
                .preferredColorScheme(.dark)
        }
    }
}
