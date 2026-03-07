import Foundation
import NetworkExtension
import Network

class WiFiAutoConnect: ObservableObject {
    @Published var isConnected = false
    @Published var currentSSID: String?
    @Published var connectionStatus: String = ""

    func connectToCamera(ssid: String, password: String) {
        let config = NEHotspotConfiguration(ssid: ssid, passphrase: password, isWEP: false)
        config.joinOnce = false

        connectionStatus = NSLocalizedString("connecting", comment: "")

        NEHotspotConfigurationManager.shared.apply(config) { [weak self] error in
            DispatchQueue.main.async {
                if let error = error as NSError? {
                    if error.domain == NEHotspotConfigurationErrorDomain &&
                       error.code == NEHotspotConfigurationError.alreadyAssociated.rawValue {
                        self?.isConnected = true
                        self?.currentSSID = ssid
                        self?.connectionStatus = ""
                        AppLogger.shared.log("Already connected to \(ssid)", type: .info)
                    } else {
                        self?.isConnected = false
                        self?.connectionStatus = NSLocalizedString("wifi_connection_failed", comment: "")
                        AppLogger.shared.log("WiFi connection failed: \(error.localizedDescription)", type: .error)
                    }
                } else {
                    self?.isConnected = true
                    self?.currentSSID = ssid
                    self?.connectionStatus = ""
                    AppLogger.shared.log("Connected to \(ssid)", type: .info)
                }
            }
        }
    }

    func disconnect(ssid: String) {
        NEHotspotConfigurationManager.shared.removeConfiguration(forSSID: ssid)
        isConnected = false
        currentSSID = nil
        AppLogger.shared.log("Disconnected from \(ssid)", type: .info)
    }

    func checkCurrentConnection(expectedSSID: String) {
        NEHotspotNetwork.fetchCurrent { [weak self] network in
            DispatchQueue.main.async {
                if let network = network {
                    self?.currentSSID = network.ssid
                    self?.isConnected = network.ssid == expectedSSID
                } else {
                    self?.isConnected = false
                    self?.currentSSID = nil
                }
            }
        }
    }
}
