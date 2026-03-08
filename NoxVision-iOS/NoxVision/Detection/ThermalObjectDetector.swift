import Foundation
import CoreImage
import CoreML
import Vision
import Accelerate

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

    private var model: MLModel?
    private var visionModel: VNCoreMLModel?
    private let confidenceThreshold: Float = 0.45
    private let iouThreshold: Float = 0.45

    private let inputSize = 640
    private let numAnchors = 8400

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
        guard let modelURL = Bundle.main.url(forResource: "ThermalYOLO", withExtension: "mlmodelc") else {
            AppLogger.shared.log("ThermalYOLO.mlmodel not found in bundle — detection disabled", type: .warning)
            AppLogger.shared.log("Run 'python3 scripts/convert_model.py' to convert the TFLite model", type: .info)
            return
        }

        do {
            model = try MLModel(contentsOf: modelURL)
            // Also try Vision wrapper for convenience
            if let m = model {
                visionModel = try? VNCoreMLModel(for: m)
            }
            AppLogger.shared.log("ThermalObjectDetector initialized (CoreML)", type: .info)
        } catch {
            AppLogger.shared.log("Failed to load CoreML model: \(error.localizedDescription)", type: .error)
        }
    }

    func processFrame(_ pixelBuffer: CVPixelBuffer) {
        guard isRunning, model != nil else { return }

        // Try Vision framework first (works if model has ImageType input)
        if let visionModel = visionModel {
            processWithVision(pixelBuffer, visionModel: visionModel)
            return
        }

        // Fallback: manual preprocessing for TensorType input
        processWithCoreML(pixelBuffer)
    }

    // MARK: - Vision Framework Path (ImageType input)

    private func processWithVision(_ pixelBuffer: CVPixelBuffer, visionModel: VNCoreMLModel) {
        let handler = VNImageRequestHandler(cvPixelBuffer: pixelBuffer, orientation: .up, options: [:])

        let mlRequest = VNCoreMLRequest(model: visionModel) { [weak self] request, error in
            guard let self = self else { return }

            // Check if Vision returned recognized objects (ImageType output)
            if let observations = request.results as? [VNRecognizedObjectObservation], !observations.isEmpty {
                self.handleVisionResults(observations)
                return
            }

            // Otherwise parse raw tensor output
            if let featureResults = request.results as? [VNCoreMLFeatureValueObservation] {
                self.handleRawResults(featureResults)
            }
        }
        mlRequest.imageCropAndScaleOption = .scaleFill

        do {
            try handler.perform([mlRequest])
        } catch {
            AppLogger.shared.log("Detection failed: \(error.localizedDescription)", type: .error)
        }
    }

    // MARK: - Direct CoreML Path (TensorType input)

    private func processWithCoreML(_ pixelBuffer: CVPixelBuffer) {
        guard let model = model else { return }

        do {
            let inputArray = try pixelBufferToMLMultiArray(pixelBuffer)
            let inputFeatures = try MLDictionaryFeatureProvider(
                dictionary: ["image": MLFeatureValue(multiArray: inputArray)]
            )
            let output = try model.prediction(from: inputFeatures)
            parseModelOutput(output)
        } catch {
            AppLogger.shared.log("CoreML inference failed: \(error.localizedDescription)", type: .error)
        }
    }

    private func pixelBufferToMLMultiArray(_ pixelBuffer: CVPixelBuffer) throws -> MLMultiArray {
        let width = CVPixelBufferGetWidth(pixelBuffer)
        let height = CVPixelBufferGetHeight(pixelBuffer)

        // Create MLMultiArray with shape [1, 640, 640, 3] (NHWC)
        let array = try MLMultiArray(shape: [1, NSNumber(value: inputSize), NSNumber(value: inputSize), 3], dataType: .float32)

        CVPixelBufferLockBaseAddress(pixelBuffer, .readOnly)
        defer { CVPixelBufferUnlockBaseAddress(pixelBuffer, .readOnly) }

        guard let baseAddress = CVPixelBufferGetBaseAddress(pixelBuffer) else {
            throw NSError(domain: "ThermalDetector", code: 1, userInfo: [NSLocalizedDescriptionKey: "Cannot access pixel buffer"])
        }

        let bytesPerRow = CVPixelBufferGetBytesPerRow(pixelBuffer)
        let pixelFormat = CVPixelBufferGetPixelFormatType(pixelBuffer)

        // Scale to 640x640 and normalize to [0, 1]
        let scaleX = Double(width) / Double(inputSize)
        let scaleY = Double(height) / Double(inputSize)

        let ptr = array.dataPointer.bindMemory(to: Float32.self, capacity: array.count)

        for y in 0..<inputSize {
            let srcY = min(Int(Double(y) * scaleY), height - 1)
            let rowPtr = baseAddress.advanced(by: srcY * bytesPerRow)

            for x in 0..<inputSize {
                let srcX = min(Int(Double(x) * scaleX), width - 1)
                let offset = y * inputSize * 3 + x * 3

                if pixelFormat == kCVPixelFormatType_32BGRA {
                    let pixel = rowPtr.advanced(by: srcX * 4).assumingMemoryBound(to: UInt8.self)
                    ptr[offset + 0] = Float32(pixel[2]) / 255.0  // R
                    ptr[offset + 1] = Float32(pixel[1]) / 255.0  // G
                    ptr[offset + 2] = Float32(pixel[0]) / 255.0  // B
                } else {
                    // RGBA or other format
                    let pixel = rowPtr.advanced(by: srcX * 4).assumingMemoryBound(to: UInt8.self)
                    ptr[offset + 0] = Float32(pixel[0]) / 255.0  // R
                    ptr[offset + 1] = Float32(pixel[1]) / 255.0  // G
                    ptr[offset + 2] = Float32(pixel[2]) / 255.0  // B
                }
            }
        }

        return array
    }

    // MARK: - Output Parsing

    private func handleVisionResults(_ observations: [VNRecognizedObjectObservation]) {
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

        detections = applyNMS(detections)
        let top = Array(detections.sorted { $0.confidence > $1.confidence }.prefix(5))

        DispatchQueue.main.async {
            self.detectedObjects = top
        }
    }

    private func handleRawResults(_ featureResults: [VNCoreMLFeatureValueObservation]) {
        guard let firstResult = featureResults.first,
              let multiArray = firstResult.featureValue.multiArrayValue else { return }
        parseYOLOOutput(multiArray)
    }

    private func parseModelOutput(_ output: MLFeatureProvider) {
        // Find the output multi-array (first available)
        for name in output.featureNames {
            if let multiArray = output.featureValue(for: name)?.multiArrayValue {
                parseYOLOOutput(multiArray)
                return
            }
        }
    }

    /// Parse YOLOv8 output tensor [1, 8, 8400] or [8, 8400]
    /// Format: 4 bbox coords (cx, cy, w, h) + 4 class scores per anchor
    private func parseYOLOOutput(_ output: MLMultiArray) {
        let shape = output.shape.map { $0.intValue }
        let numClasses = classLabels.count

        // Determine dimensions - output may be [1, 8, 8400] or [8, 8400]
        let numElements: Int  // 4 + numClasses
        let anchors: Int
        let dataOffset: Int

        if shape.count == 3 {
            numElements = shape[1]
            anchors = shape[2]
            dataOffset = 0
        } else if shape.count == 2 {
            numElements = shape[0]
            anchors = shape[1]
            dataOffset = 0
        } else {
            return
        }

        guard numElements == 4 + numClasses else { return }
        guard anchors == numAnchors else { return }

        let ptr = output.dataPointer.bindMemory(to: Float32.self, capacity: output.count)

        var detections: [DetectedObject] = []

        // For each anchor, find best class and check threshold
        for i in 0..<anchors {
            var maxScore: Float = 0
            var maxClass = -1

            for c in 0..<numClasses {
                let score = ptr[(4 + c) * anchors + i]
                if score > maxScore {
                    maxScore = score
                    maxClass = c
                }
            }

            guard maxScore >= confidenceThreshold, maxClass >= 0 else { continue }

            let rawLabel = classLabels[maxClass]
            guard let config = classConfig[rawLabel] else { continue }
            guard maxScore >= config.minConf else { continue }

            // Extract bbox (cx, cy, w, h) normalized to input size
            let cx = Double(ptr[0 * anchors + i]) / Double(inputSize)
            let cy = Double(ptr[1 * anchors + i]) / Double(inputSize)
            let w = Double(ptr[2 * anchors + i]) / Double(inputSize)
            let h = Double(ptr[3 * anchors + i]) / Double(inputSize)

            // Convert to CGRect (Vision-style: origin at bottom-left, normalized)
            let x = cx - w / 2.0
            let y = cy - h / 2.0
            let bbox = CGRect(x: x, y: y, width: w, height: h)

            // Filter tiny detections
            guard bbox.width > 0.05 && bbox.height > 0.05 else { continue }

            let distance = estimateDistance(label: rawLabel, boundingBox: bbox)
            detections.append(DetectedObject(
                label: config.display,
                confidence: maxScore,
                boundingBox: bbox,
                estimatedDistance: distance
            ))
        }

        detections = applyNMS(detections)
        let top = Array(detections.sorted { $0.confidence > $1.confidence }.prefix(5))

        DispatchQueue.main.async {
            self.detectedObjects = top
        }
    }

    // MARK: - NMS & Helpers

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
        let pixelHeight = boundingBox.height * Double(inputSize)
        guard pixelHeight > 10 else { return nil }

        let distance = (knownHeight * focalLengthPixels) / pixelHeight
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
