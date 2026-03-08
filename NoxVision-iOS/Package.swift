// swift-tools-version: 5.9
import PackageDescription

let package = Package(
    name: "NoxVision",
    platforms: [
        .iOS(.v16)
    ],
    products: [
        .library(name: "NoxVision", targets: ["NoxVision"])
    ],
    dependencies: [
        .package(url: "https://github.com/nicklama/coreml-yolo", from: "1.0.0"),
        .package(url: "https://github.com/tylerjonesio/vlckit-spm", from: "3.5.0"),
    ],
    targets: [
        .target(
            name: "NoxVision",
            path: "NoxVision",
            dependencies: [
                .product(name: "VLCKitSPM", package: "vlckit-spm"),
            ]
        )
    ]
)
