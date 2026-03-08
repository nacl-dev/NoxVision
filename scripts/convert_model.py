#!/usr/bin/env python3
"""
Convert the TFLite YOLOv8 model (detect.tflite) to CoreML format for iOS.

Pipeline: TFLite -> ONNX -> PyTorch -> CoreML

Requirements:
    pip install coremltools tensorflow tf2onnx onnx onnx2torch torch torchvision numpy

Usage:
    python3 scripts/convert_model.py

Output:
    NoxVision-iOS/NoxVision/Detection/ThermalYOLO.mlpackage/
"""

import sys
import os
import shutil
import subprocess
import tempfile


def main():
    script_dir = os.path.dirname(os.path.abspath(__file__))
    project_root = os.path.dirname(script_dir)
    tflite_path = os.path.join(project_root, "app", "src", "main", "assets", "detect.tflite")
    output_path = os.path.join(project_root, "NoxVision-iOS", "NoxVision", "Detection", "ThermalYOLO.mlpackage")
    onnx_path = os.path.join(tempfile.gettempdir(), "noxvision_detect.onnx")

    if not os.path.exists(tflite_path):
        print(f"Error: TFLite model not found at {tflite_path}")
        sys.exit(1)

    print(f"Model: {tflite_path}")
    print(f"Size:  {os.path.getsize(tflite_path) / 1024 / 1024:.1f} MB")

    # Step 1: TFLite -> ONNX (via tf2onnx)
    print("\nStep 1: Converting TFLite -> ONNX...")
    result = subprocess.run(
        [sys.executable, "-m", "tf2onnx.convert",
         "--tflite", tflite_path,
         "--output", onnx_path,
         "--opset", "13"],
        capture_output=True, text=True,
    )
    if not os.path.exists(onnx_path):
        print(f"tf2onnx failed:\n{result.stderr[-500:]}")
        sys.exit(1)
    print(f"ONNX model: {os.path.getsize(onnx_path) / 1024 / 1024:.1f} MB")

    # Step 2: ONNX -> PyTorch -> CoreML
    # Runs in subprocess with PROTOCOL_BUFFERS_PYTHON_IMPLEMENTATION=python
    # to work around protobuf version conflict between TF and coremltools
    print("\nStep 2: Converting ONNX -> PyTorch -> CoreML...")

    convert_script = f'''
import os, sys, shutil
os.environ['TF_CPP_MIN_LOG_LEVEL'] = '3'

import torch
from onnx2torch import convert as onnx_to_torch
import coremltools as ct

# Load ONNX as PyTorch model
print("Loading ONNX model as PyTorch...")
torch_model = onnx_to_torch("{onnx_path}")
torch_model.eval()

# Verify inference
test_input = torch.randn(1, 640, 640, 3)
with torch.no_grad():
    test_output = torch_model(test_input)
print(f"PyTorch output shape: {{test_output.shape}}")

# Trace the model for CoreML conversion
print("Tracing model...")
with torch.no_grad():
    traced = torch.jit.trace(torch_model, test_input)

# Convert to CoreML
# Input is NHWC (1, 640, 640, 3) with TensorType since the model expects
# pixel values normalized to [0, 1] - preprocessing done in Swift
print("Converting to CoreML...")
mlmodel = ct.convert(
    traced,
    inputs=[ct.TensorType(name="image", shape=(1, 640, 640, 3))],
    minimum_deployment_target=ct.target.iOS16,
    compute_precision=ct.precision.FLOAT16,
)

# Add metadata
mlmodel.author = "NoxVision"
mlmodel.short_description = "YOLOv8 thermal object detection model (car, cat, dog, person)"
mlmodel.version = "1.0"
mlmodel.user_defined_metadata["classes"] = "car\\ncat\\ndog\\nperson"
mlmodel.user_defined_metadata["num_classes"] = "4"
mlmodel.user_defined_metadata["input_format"] = "NHWC"
mlmodel.user_defined_metadata["input_size"] = "640"
mlmodel.user_defined_metadata["output_format"] = "YOLOv8 [1, 8, 8400] = [batch, 4+num_classes, anchors]"

# Save
output = "{output_path}"
if os.path.exists(output):
    shutil.rmtree(output)
mlmodel.save(output)

size = sum(os.path.getsize(os.path.join(d, f)) for d, _, fs in os.walk(output) for f in fs)
print(f"CoreML model: {{size / 1024 / 1024:.1f}} MB")
print("SUCCESS")
'''

    env = os.environ.copy()
    env["PROTOCOL_BUFFERS_PYTHON_IMPLEMENTATION"] = "python"
    env["TF_CPP_MIN_LOG_LEVEL"] = "3"

    result = subprocess.run(
        [sys.executable, "-c", convert_script],
        capture_output=True, text=True, env=env,
    )

    for line in result.stdout.strip().split('\n'):
        if line.strip():
            print(f"  {line}")

    if result.returncode != 0 or "SUCCESS" not in result.stdout:
        print(f"\nConversion failed:")
        stderr_lines = [l for l in result.stderr.split('\n')
                       if l.strip() and not l.startswith(('WARNING', 'I0000', 'Failed to load'))]
        for line in stderr_lines[-10:]:
            print(f"  {line}")
        sys.exit(1)

    # Clean up intermediate ONNX file
    if os.path.exists(onnx_path):
        os.unlink(onnx_path)

    print("\nDone! Add ThermalYOLO.mlpackage to your Xcode project:")
    print("  1. Drag ThermalYOLO.mlpackage into Xcode (NoxVision/Detection group)")
    print("  2. Make sure 'Copy items if needed' is checked")
    print("  3. Xcode will auto-compile it to .mlmodelc at build time")


if __name__ == "__main__":
    main()
