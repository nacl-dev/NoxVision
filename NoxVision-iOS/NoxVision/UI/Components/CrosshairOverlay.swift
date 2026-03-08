import SwiftUI

struct CrosshairOverlay: View {
    let style: CrosshairStyle
    let color: Color

    var body: some View {
        GeometryReader { geometry in
            let center = CGPoint(x: geometry.size.width / 2, y: geometry.size.height / 2)

            Canvas { context, size in
                switch style {
                case .simple:
                    drawSimple(context: context, center: center)
                case .gap:
                    drawGap(context: context, center: center)
                case .circleDot:
                    drawCircleDot(context: context, center: center)
                case .chevron:
                    drawChevron(context: context, center: center)
                }
            }
        }
        .allowsHitTesting(false)
    }

    private func drawSimple(context: GraphicsContext, center: CGPoint) {
        let length: CGFloat = 20
        let thickness: CGFloat = 1.5

        var path = Path()
        // Horizontal
        path.move(to: CGPoint(x: center.x - length, y: center.y))
        path.addLine(to: CGPoint(x: center.x + length, y: center.y))
        // Vertical
        path.move(to: CGPoint(x: center.x, y: center.y - length))
        path.addLine(to: CGPoint(x: center.x, y: center.y + length))

        context.stroke(path, with: .color(color), lineWidth: thickness)
    }

    private func drawGap(context: GraphicsContext, center: CGPoint) {
        let length: CGFloat = 20
        let gap: CGFloat = 6
        let thickness: CGFloat = 1.5

        var path = Path()
        // Left
        path.move(to: CGPoint(x: center.x - length, y: center.y))
        path.addLine(to: CGPoint(x: center.x - gap, y: center.y))
        // Right
        path.move(to: CGPoint(x: center.x + gap, y: center.y))
        path.addLine(to: CGPoint(x: center.x + length, y: center.y))
        // Top
        path.move(to: CGPoint(x: center.x, y: center.y - length))
        path.addLine(to: CGPoint(x: center.x, y: center.y - gap))
        // Bottom
        path.move(to: CGPoint(x: center.x, y: center.y + gap))
        path.addLine(to: CGPoint(x: center.x, y: center.y + length))

        context.stroke(path, with: .color(color), lineWidth: thickness)
    }

    private func drawCircleDot(context: GraphicsContext, center: CGPoint) {
        let radius: CGFloat = 12
        let dotRadius: CGFloat = 2
        let thickness: CGFloat = 1.5

        // Circle
        let circlePath = Path(ellipseIn: CGRect(
            x: center.x - radius, y: center.y - radius,
            width: radius * 2, height: radius * 2
        ))
        context.stroke(circlePath, with: .color(color), lineWidth: thickness)

        // Center dot
        let dotPath = Path(ellipseIn: CGRect(
            x: center.x - dotRadius, y: center.y - dotRadius,
            width: dotRadius * 2, height: dotRadius * 2
        ))
        context.fill(dotPath, with: .color(color))
    }

    private func drawChevron(context: GraphicsContext, center: CGPoint) {
        let size: CGFloat = 12
        let thickness: CGFloat = 1.5

        var path = Path()
        // Top chevron
        path.move(to: CGPoint(x: center.x - size, y: center.y - size * 0.6))
        path.addLine(to: CGPoint(x: center.x, y: center.y - size * 1.2))
        path.addLine(to: CGPoint(x: center.x + size, y: center.y - size * 0.6))
        // Bottom chevron
        path.move(to: CGPoint(x: center.x - size, y: center.y + size * 0.6))
        path.addLine(to: CGPoint(x: center.x, y: center.y + size * 1.2))
        path.addLine(to: CGPoint(x: center.x + size, y: center.y + size * 0.6))

        context.stroke(path, with: .color(color), lineWidth: thickness)
    }
}
