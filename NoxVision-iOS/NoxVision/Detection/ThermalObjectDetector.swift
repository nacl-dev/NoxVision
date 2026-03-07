import Foundation
import CoreImage
import CoreML
import Vision

struct DetectedObject: Identifiable {
    let id = UUID()
    let label: String
    let confidence: Float
    let boundingBox: CGRect
    let estimatedDistance: Double?
}

class ThermalObjectDetector: ObservableObject {
    @Published var detectedObjects: [DetectedObject] = []
    @Published var isRunning = false

    private var visionModel: VNCoreMLModel?
    private let confidenceThreshold: Float = 0.45
    private let iouThreshold: Float = 0.45

    // Labels matching the custom thermal YOLO model
    private let classLabels = ["car", "cat", "dog", "person"]

    // Display label mapping and per-class confidence thresholds (matching Android)
    private let classConfig: [String: (display: String, minConf: Float)] = [
        "person": ("Person", 0.45),
        "car": ("Vehicle", 0.50),
        "truck": ("Vehicle", 0.50),
        "bus": ("Vehicle", 0.50),
        "bicycle": ("Bicycle", 0.50),
        "motorcycle": ("Bicycle", 0.50),
        "dog": ("Animal", 0.45),
        "cat": ("Animal", 0.45),
        "horse": ("Animal", 0.45),
        "cow": ("Animal", 0.45),
        "sheep": ("Animal", 0.45),
        "bear": ("Animal", 0.45),
    ]

    // Known heights for distance estimation (pinhole camera model)
    private let knownHeights: [String: Double] = [
        "person": 1.7,
        "bicycle": 1.0,
        "car": 1.5,
        "motorcycle": 1.2,
        "bus": 3.0,
        "truck": 3.0,
        "dog": 0.6,
        "cat": 0.3,
    ]

    private let focalLengthPixels: Double = 3350.0

    init() {
        loadModel()
    }

    private func loadModel() {
        // Load the CoreML YOLO model bundled with the app
        // The model should be added to the Xcode project as "ThermalYOLO.mlmodel"
        // Convert from TFLite using: python3 convert_model.py
        guard let modelURL = Bundle.main.url(forResource: "ThermalYOLO", withExtension: "mlmodelc") else {
            AppLogger.shared.log("ThermalYOLO.mlmodel not found in bundle — detection disabled", type: .warning)
            AppLogger.shared.log("Run 'python3 scripts/convert_model.py' to convert the TFLite model", type: .info)
            return
        }

        do {
            let model = try MLModel(contentsOf: modelURL)
            visionModel = try VNCoreMLModel(for: model)
            AppLogger.shared.log("ThermalObjectDetector initialized (CoreML)", type: .info)
        } catch {
            AppLogger.shared.log("Failed to load CoreML model: \(error.localizedDescription)", type: .error)
        }
    }

    func processFrame(_ pixelBuffer: CVPixelBuffer) {
        guard isRunning, visionModel != nil else { return }

        let handler = VNImageRequestHandler(cvPixelBuffer: pixelBuffer, orientation: .up, options: [:])

        let mlRequest = VNCoreMLRequest(model: visionModel!) { [weak self] request, error in
            self?.handleDetectionResults(request.results)
        }
        mlRequest.imageCropAndScaleOption = .scaleFill

        do {
            try handler.perform([mlRequest])
        } catch {
            AppLogger.shared.log("Detection failed: \(error.localizedDescription)", type: .error)
        }
    }

    private func handleDetectionResults(_ results: [Any]?) {
        guard let observations = results as? [VNRecognizedObjectObservation] else { return }

        var detections = observations
            .filter { $0.confidence >= confidenceThreshold }
            .compactMap { obs -> DetectedObject? in
                guard let topLabel = obs.labels.first?.identifier.lowercased() else { return nil }
                guard let config = classConfig[topLabel] else { return nil }
                guard obs.confidence >= config.minConf else { return nil }

                let distance = estimateDistance(label: topLabel, boundingBox: obs.boundingBox)
                return DetectedObject(
                    label: config.display,
                    confidence: obs.confidence,
                    boundingBox: obs.boundingBox,
                    estimatedDistance: distance
                )
            }

        // Apply NMS
        detections = applyNMS(detections)

        // Keep top 5
        let top = Array(detections.sorted { $0.confidence > $1.confidence }.prefix(5))

        DispatchQueue.main.async {
            self.detectedObjects = top
        }
    }

    private func applyNMS(_ detections: [DetectedObject]) -> [DetectedObject] {
        guard !detections.isEmpty else { return [] }
        let sorted = detections.sorted { $0.confidence > $1.confidence }
        var keep: [DetectedObject] = []
        var suppressed = Set<Int>()

        for i in sorted.indices {
            if suppressed.contains(i) { continue }
            keep.append(sorted[i])
            for j in (i + 1)..<sorted.count {
                if suppressed.contains(j) { continue }
                if calculateIoU(sorted[i].boundingBox, sorted[j].boundingBox) > iouThreshold {
                    suppressed.insert(j)
                }
            }
        }
        return keep
    }

    private func calculateIoU(_ a: CGRect, _ b: CGRect) -> Float {
        let intersection = a.intersection(b)
        guard !intersection.isNull else { return 0 }
        let intersectionArea = intersection.width * intersection.height
        let unionArea = (a.width * a.height) + (b.width * b.height) - intersectionArea
        return unionArea > 0 ? Float(intersectionArea / unionArea) : 0
    }

    private func estimateDistance(label: String, boundingBox: CGRect) -> Double? {
        guard let knownHeight = knownHeights[label] else { return nil }
        // boundingBox height is normalized (0-1), convert to approximate pixel height
        // assuming 640px input like Android
        let pixelHeight = boundingBox.height * 640.0
        guard pixelHeight > 10 else { return nil }

        let distance = (knownHeight * focalLengthPixels) / pixelHeight
        // Clamp to reasonable range (1-100m like Android)
        return min(max(distance, 1.0), 100.0)
    }

    func start() {
        isRunning = true
        AppLogger.shared.log("Object detection started", type: .info)
    }

    func stop() {
        isRunning = false
        DispatchQueue.main.async {
            self.detectedObjects = []
        }
        AppLogger.shared.log("Object detection stopped", type: .info)
    }
}
