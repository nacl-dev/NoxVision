#!/usr/bin/env python3
"""
Convert the TFLite YOLOv8 model (detect.tflite) to CoreML format for iOS.

Requirements:
    pip install coremltools tensorflow numpy

Usage:
    python3 scripts/convert_model.py

Output:
    NoxVision-iOS/NoxVision/Detection/ThermalYOLO.mlpackage/
    (Xcode will compile this to .mlmodelc automatically when added to the project)
"""

import sys
import os
import numpy as np

try:
    import coremltools as ct
    import tensorflow as tf
except ImportError:
    print("Missing dependencies. Install them with:")
    print("  pip install coremltools tensorflow numpy")
    sys.exit(1)


def convert_tflite_to_coreml():
    # Paths
    script_dir = os.path.dirname(os.path.abspath(__file__))
    project_root = os.path.dirname(script_dir)
    tflite_path = os.path.join(project_root, "app", "src", "main", "assets", "detect.tflite")
    output_path = os.path.join(project_root, "NoxVision-iOS", "NoxVision", "Detection", "ThermalYOLO.mlpackage")

    if not os.path.exists(tflite_path):
        print(f"Error: TFLite model not found at {tflite_path}")
        sys.exit(1)

    print(f"Loading TFLite model from: {tflite_path}")
    print(f"Model size: {os.path.getsize(tflite_path) / 1024 / 1024:.1f} MB")

    # Load the TFLite model to inspect its shape
    interpreter = tf.lite.Interpreter(model_path=tflite_path)
    interpreter.allocate_tensors()

    input_details = interpreter.get_input_details()
    output_details = interpreter.get_output_details()

    print(f"\nInput shape:  {input_details[0]['shape']} ({input_details[0]['dtype']})")
    print(f"Output shape: {output_details[0]['shape']} ({output_details[0]['dtype']})")

    # Class labels matching labelmap.txt
    class_labels = ["car", "cat", "dog", "person"]

    # Convert TFLite -> CoreML
    print("\nConverting to CoreML...")
    mlmodel = ct.convert(
        tflite_path,
        source="tensorflow",
        inputs=[
            ct.ImageType(
                name="image",
                shape=input_details[0]['shape'],  # [1, 640, 640, 3]
                scale=1.0 / 255.0,
                bias=[0, 0, 0],
                color_layout="RGB",
            )
        ],
        minimum_deployment_target=ct.target.iOS16,
        compute_precision=ct.precision.FLOAT16,
    )

    # Add metadata
    mlmodel.author = "NoxVision"
    mlmodel.short_description = "YOLOv8 thermal object detection model (car, cat, dog, person)"
    mlmodel.version = "1.0"

    # Add class labels as metadata
    labels_str = "\n".join(class_labels)
    mlmodel.user_defined_metadata["classes"] = labels_str
    mlmodel.user_defined_metadata["num_classes"] = str(len(class_labels))

    # Save
    print(f"Saving CoreML model to: {output_path}")
    mlmodel.save(output_path)

    model_size = sum(
        os.path.getsize(os.path.join(dirpath, filename))
        for dirpath, dirnames, filenames in os.walk(output_path)
        for filename in filenames
    )
    print(f"CoreML model size: {model_size / 1024 / 1024:.1f} MB")
    print("\nDone! Add ThermalYOLO.mlpackage to your Xcode project:")
    print("  1. Drag ThermalYOLO.mlpackage into Xcode (NoxVision/Detection group)")
    print("  2. Make sure 'Copy items if needed' is checked")
    print("  3. Xcode will auto-compile it to .mlmodelc at build time")


if __name__ == "__main__":
    convert_tflite_to_coreml()
