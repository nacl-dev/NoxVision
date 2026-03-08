import Foundation
import SwiftUI

class AppLogger: ObservableObject {
    static let shared = AppLogger()

    enum LogType: String {
        case info = "INFO"
        case warning = "WARNING"
        case error = "ERROR"
        case debug = "DEBUG"

        var color: Color {
            switch self {
            case .info: return .blue
            case .warning: return .yellow
            case .error: return .red
            case .debug: return .gray
            }
        }
    }

    struct LogEntry: Identifiable {
        let id = UUID()
        let timestamp: Date
        let message: String
        let type: LogType

        var formattedTime: String {
            let formatter = DateFormatter()
            formatter.dateFormat = "HH:mm:ss.SSS"
            return formatter.string(from: timestamp)
        }
    }

    @Published var entries: [LogEntry] = []
    private let maxEntries = 500

    func log(_ message: String, type: LogType = .info) {
        let entry = LogEntry(timestamp: Date(), message: message, type: type)
        DispatchQueue.main.async {
            self.entries.insert(entry, at: 0)
            if self.entries.count > self.maxEntries {
                self.entries.removeLast()
            }
        }
        #if DEBUG
        print("[\(type.rawValue)] \(message)")
        #endif
    }

    func clear() {
        DispatchQueue.main.async {
            self.entries.removeAll()
        }
    }
}
