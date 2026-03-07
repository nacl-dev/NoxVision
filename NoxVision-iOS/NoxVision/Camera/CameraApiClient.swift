import Foundation

actor CameraApiClient {
    private let baseUrl: String
    private let connectTimeout: TimeInterval = 3.0
    private let readTimeout: TimeInterval = 5.0
    private var restApiAvailable: Bool?

    // API Endpoints
    private enum Endpoint {
        static let deviceInfo = "/api/v1/measure/getDeviceInfo"
        static let baseMeasureParam = "/api/v1/measure/getBaseMeasureParam"
        static let rtspConfig = "/api/v1/measure/getRtspConfigInfo"
        static let gpsInfo = "/api/v1/measure/getGpsInfo"
        static let getPalette = "/api/v1/measure/getPaletteId"
        static let setPalette = "/api/v1/measure/setPaletteId"
        static let getEmission = "/api/v1/measure/getEmission"
        static let setEmission = "/api/v1/measure/setEmission"
        static let getDistance = "/api/v1/measure/getDistance"
        static let setDistance = "/api/v1/measure/setDistance"
        static let getHumidity = "/api/v1/measure/getHumidity"
        static let setHumidity = "/api/v1/measure/setHumidity"
        static let getReflectTemp = "/api/v1/measure/getReflectTemperature"
        static let setReflectTemp = "/api/v1/measure/setReflectTemperature"
        static let getAtmospheric = "/api/v1/measure/getAtmosphericTransmittance"
        static let setAtmospheric = "/api/v1/measure/setAtmosphericTransmittance"
        static let getOptical = "/api/v1/measure/getOpticalTransmittance"
        static let setOptical = "/api/v1/measure/setOpticalTransmittance"
        static let setFocus = "/api/v1/measure/setFocus"
        static let shutter = "/api/v1/measure/shutter"
        static let setRtspServer = "/api/v1/measure/setRtspServerEnable"
    }

    init(baseUrl: String) {
        self.baseUrl = baseUrl
    }

    func isRestApiAvailable() async -> Bool {
        if let available = restApiAvailable { return available }
        let result = await getDeviceInfo()
        restApiAvailable = result != nil
        return restApiAvailable!
    }

    // MARK: - HTTP Helpers

    private func httpGet(_ endpoint: String) async -> [String: Any]? {
        guard let url = URL(string: "\(baseUrl)\(endpoint)") else { return nil }

        var request = URLRequest(url: url)
        request.httpMethod = "GET"
        request.setValue("application/json", forHTTPHeaderField: "Accept")
        request.timeoutInterval = readTimeout

        do {
            let (data, response) = try await URLSession.shared.data(for: request)
            guard let httpResponse = response as? HTTPURLResponse, httpResponse.statusCode == 200 else { return nil }
            return try JSONSerialization.jsonObject(with: data) as? [String: Any]
        } catch {
            AppLogger.shared.log("API GET \(endpoint) failed: \(error.localizedDescription)", type: .error)
            return nil
        }
    }

    private func httpPostJSON(_ endpoint: String, params: [String: Any]) async -> [String: Any]? {
        guard let url = URL(string: "\(baseUrl)\(endpoint)") else { return nil }

        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.setValue("application/json", forHTTPHeaderField: "Accept")
        request.timeoutInterval = readTimeout

        do {
            request.httpBody = try JSONSerialization.data(withJSONObject: params)
            let (data, response) = try await URLSession.shared.data(for: request)
            guard let httpResponse = response as? HTTPURLResponse, httpResponse.statusCode == 200 else { return nil }
            return try JSONSerialization.jsonObject(with: data) as? [String: Any]
        } catch {
            AppLogger.shared.log("API POST \(endpoint) failed: \(error.localizedDescription)", type: .error)
            return nil
        }
    }

    private func isSuccess(_ json: [String: Any]) -> Bool {
        (json["retmsg"] as? String) == "success"
    }

    // MARK: - Device Information

    func getDeviceInfo() async -> DeviceInfo? {
        guard let json = await httpGet(Endpoint.deviceInfo), isSuccess(json) else { return nil }
        return DeviceInfo(
            deviceName: json["device_name"] as? String ?? "Unknown",
            cameraName: json["camera_name"] as? String ?? "Unknown",
            videoWidth: json["video_width"] as? Int ?? 256,
            videoHeight: json["video_height"] as? Int ?? 192,
            videoFps: json["video_fps"] as? Int ?? 25,
            measureGear: json["measure_gear"] as? Int ?? 0,
            cameraLens: json["camera_lens"] as? String ?? "",
            measureRange: json["measure_range"] as? String ?? ""
        )
    }

    func getRtspConfigInfo() async -> [String: Any]? {
        await httpGet(Endpoint.rtspConfig)
    }

    func getGpsInfo() async -> [String: Any]? {
        await httpGet(Endpoint.gpsInfo)
    }

    // MARK: - Palette Control

    func getPaletteId() async -> Int? {
        guard let json = await httpGet(Endpoint.getPalette), isSuccess(json) else { return nil }
        return json["palette_id"] as? Int
    }

    func setPaletteId(_ id: Int) async -> Bool {
        guard let json = await httpPostJSON(Endpoint.setPalette, params: ["palette_id": "\(id)"]) else { return false }
        return isSuccess(json)
    }

    // MARK: - Thermal Measurement Settings

    func getEmission() async -> Float? {
        guard let json = await httpGet(Endpoint.getEmission), isSuccess(json),
              let val = json["emission"] as? Double else { return nil }
        return Float(val)
    }

    func setEmission(_ value: Float, index: Int = 0) async -> Bool {
        guard let json = await httpPostJSON(Endpoint.setEmission, params: [
            "emission": "\(value)", "index": "\(index)"
        ]) else { return false }
        return isSuccess(json)
    }

    func getDistance() async -> Float? {
        guard let json = await httpGet(Endpoint.getDistance), isSuccess(json),
              let val = json["distance"] as? Double else { return nil }
        return Float(val)
    }

    func setDistance(_ meters: Float) async -> Bool {
        guard let json = await httpPostJSON(Endpoint.setDistance, params: ["distance": "\(meters)"]) else { return false }
        return isSuccess(json)
    }

    func getHumidity() async -> Float? {
        guard let json = await httpGet(Endpoint.getHumidity), isSuccess(json),
              let val = json["humidity"] as? Double else { return nil }
        return Float(val)
    }

    func setHumidity(_ percent: Float) async -> Bool {
        guard let json = await httpPostJSON(Endpoint.setHumidity, params: ["humidity": "\(percent)"]) else { return false }
        return isSuccess(json)
    }

    func getReflectTemperature() async -> Float? {
        guard let json = await httpGet(Endpoint.getReflectTemp), isSuccess(json),
              let val = json["reflect_temp"] as? Double else { return nil }
        return Float(val)
    }

    func setReflectTemperature(_ celsius: Float) async -> Bool {
        guard let json = await httpPostJSON(Endpoint.setReflectTemp, params: ["reflect_temp": "\(celsius)"]) else { return false }
        return isSuccess(json)
    }

    func getAtmosphericTransmittance() async -> Float? {
        guard let json = await httpGet(Endpoint.getAtmospheric), isSuccess(json),
              let val = json["atmosphericTransmittance"] as? Double else { return nil }
        return Float(val)
    }

    func setAtmosphericTransmittance(_ value: Float) async -> Bool {
        guard let json = await httpPostJSON(Endpoint.setAtmospheric, params: ["atmosphericTransmittance": "\(value)"]) else { return false }
        return isSuccess(json)
    }

    func getOpticalTransmittance() async -> Float? {
        guard let json = await httpGet(Endpoint.getOptical), isSuccess(json),
              let val = json["opticalTransmittance"] as? Double else { return nil }
        return Float(val)
    }

    func setOpticalTransmittance(_ value: Float) async -> Bool {
        guard let json = await httpPostJSON(Endpoint.setOptical, params: ["opticalTransmittance": "\(value)"]) else { return false }
        return isSuccess(json)
    }

    // MARK: - Camera Control

    func triggerShutter(mode: ShutterMode = .manual) async -> Bool {
        guard let json = await httpPostJSON(Endpoint.shutter, params: ["shutter_mode": "\(mode.rawValue)"]) else { return false }
        return isSuccess(json)
    }

    func setFocus(_ action: FocusAction) async -> Bool {
        guard let json = await httpPostJSON(Endpoint.setFocus, params: ["focus_action": "\(action.rawValue)"]) else { return false }
        return isSuccess(json)
    }

    func setRtspServerEnable(_ enable: Bool) async -> Bool {
        guard let json = await httpPostJSON(Endpoint.setRtspServer, params: ["enable_server": enable ? "1" : "0"]) else { return false }
        return isSuccess(json)
    }
}
