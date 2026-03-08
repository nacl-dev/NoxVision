import SwiftUI

struct ContentView: View {
    @EnvironmentObject var cameraSettings: CameraSettingsStore
    @State private var showWelcome = false
    @State private var showWhatsNew = false

    var body: some View {
        VideoStreamScreen()
            .onAppear {
                if cameraSettings.isFirstRun {
                    showWelcome = true
                } else if cameraSettings.shouldShowWhatsNew() {
                    showWhatsNew = true
                }
            }
            .sheet(isPresented: $showWelcome) {
                WelcomeDialog(isPresented: $showWelcome)
            }
            .sheet(isPresented: $showWhatsNew) {
                WhatsNewDialog(isPresented: $showWhatsNew)
            }
    }
}
