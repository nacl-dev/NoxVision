import Foundation
import CoreImage
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
    private let iouThreshold: Float = 0.5

    // COCO class labels matching Android's YOLO model
    private let classLabels = [
        "person", "bicycle", "car", "motorcycle", "airplane", "bus",
        "train", "truck", "boat", "traffic light", "fire hydrant",
        "stop sign", "parking meter", "bench", "bird", "cat", "dog",
        "horse", "sheep", "cow", "elephant", "bear", "zebra", "giraffe",
        "backpack", "umbrella", "handbag", "tie", "suitcase", "frisbee",
        "skis", "snowboard", "sports ball", "kite", "baseball bat",
        "baseball glove", "skateboard", "surfboard", "tennis racket",
        "bottle", "wine glass", "cup", "fork", "knife", "spoon", "bowl",
        "banana", "apple", "sandwich", "orange", "broccoli", "carrot",
        "hot dog", "pizza", "donut", "cake", "chair", "couch",
        "potted plant", "bed", "dining table", "toilet", "tv", "laptop",
        "mouse", "remote", "keyboard", "cell phone", "microwave", "oven",
        "toaster", "sink", "refrigerator", "book", "clock", "vase",
        "scissors", "teddy bear", "hair drier", "toothbrush"
    ]

    // Relevant labels for thermal hunting use
    private let relevantLabels = Set(["person", "bicycle", "car", "motorcycle", "bus", "truck", "dog", "cat"])

    // Known heights for distance estimation (pinhole camera model)
    private let knownHeights: [String: Double] = [
        "person": 1.7,
        "bicycle": 1.0,
        "car": 1.5,
        "motorcycle": 1.1,
        "bus": 3.0,
        "truck": 2.5,
        "dog": 0.5,
        "cat": 0.3
    ]

    init() {
        loadModel()
    }

    private func loadModel() {
        // In production, load a .mlmodel file bundled with the app
        // For now, we use Vision's built-in object detection as a placeholder
        // To use a custom YOLO model:
        // 1. Convert your TFLite model to CoreML using coremltools
        // 2. Add the .mlmodel file to the Xcode project
        // 3. Uncomment and modify the code below:
        //
        // guard let modelURL = Bundle.main.url(forResource: "ThermalYOLO", withExtension: "mlmodelc"),
        //       let model = try? MLModel(contentsOf: modelURL) else { return }
        // visionModel = try? VNCoreMLModel(for: model)

        AppLogger.shared.log("ThermalObjectDetector initialized (CoreML)", type: .info)
    }

    func processFrame(_ pixelBuffer: CVPixelBuffer) {
        guard isRunning else { return }

        let handler = VNImageRequestHandler(cvPixelBuffer: pixelBuffer, orientation: .up, options: [:])

        let request: VNRequest
        if let model = visionModel {
            let mlRequest = VNCoreMLRequest(model: model) { [weak self] request, error in
                self?.handleDetectionResults(request.results)
            }
            mlRequest.imageCropAndScaleOption = .scaleFill
            request = mlRequest
        } else {
            // Fallback to Apple's built-in object recognition
            let recognizeRequest = VNRecognizeAnimalsRequest { [weak self] request, error in
                self?.handleAnimalResults(request.results)
            }
            request = recognizeRequest
        }

        do {
            try handler.perform([request])
        } catch {
            AppLogger.shared.log("Detection failed: \(error.localizedDescription)", type: .error)
        }
    }

    private func handleDetectionResults(_ results: [Any]?) {
        guard let observations = results as? [VNRecognizedObjectObservation] else { return }

        let filtered = observations
            .filter { $0.confidence >= confidenceThreshold }
            .filter { obs in
                guard let topLabel = obs.labels.first?.identifier else { return false }
                return relevantLabels.contains(topLabel)
            }
            .map { obs -> DetectedObject in
                let label = obs.labels.first?.identifier ?? "unknown"
                let distance = estimateDistance(label: label, boundingBox: obs.boundingBox)
                return DetectedObject(
                    label: label,
                    confidence: obs.confidence,
                    boundingBox: obs.boundingBox,
                    estimatedDistance: distance
                )
            }

        DispatchQueue.main.async {
            self.detectedObjects = filtered
        }
    }

    private func handleAnimalResults(_ results: [Any]?) {
        guard let observations = results as? [VNRecognizedObjectObservation] else { return }

        let detected = observations
            .filter { $0.confidence >= confidenceThreshold }
            .map { obs -> DetectedObject in
                let label = obs.labels.first?.identifier ?? "animal"
                return DetectedObject(
                    label: label,
                    confidence: obs.confidence,
                    boundingBox: obs.boundingBox,
                    estimatedDistance: nil
                )
            }

        DispatchQueue.main.async {
            self.detectedObjects = detected
        }
    }

    private func estimateDistance(label: String, boundingBox: CGRect) -> Double? {
        guard let knownHeight = knownHeights[label] else { return nil }
        let focalLength = 3.5 // mm (approximate for thermal lens)
        let sensorHeight = 3.0 // mm (approximate sensor size)
        let pixelHeight = boundingBox.height

        guard pixelHeight > 0.01 else { return nil }

        let distance = (knownHeight * focalLength) / (pixelHeight * sensorHeight)
        return min(max(distance, 0.5), 500.0) // Clamp to reasonable range
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
