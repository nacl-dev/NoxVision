import Foundation
import PDFKit

class CsvExporter {
    func export(records: [HuntRecord]) -> URL? {
        let dateFormatter = DateFormatter()
        dateFormatter.dateFormat = "yyyy-MM-dd HH:mm"

        var csv = "Datum,Wildart,Geschlecht,Gewicht_kg,Breitengrad,Laengengrad,Bundesland,Mondphase,Notizen\n"

        for record in records {
            let date = dateFormatter.string(from: record.timestamp)
            let lat = record.latitude.map { "\($0)" } ?? ""
            let lon = record.longitude.map { "\($0)" } ?? ""
            let weight = record.estimatedWeight.map { "\($0)" } ?? ""
            let notes = (record.notes ?? "").replacingOccurrences(of: ",", with: ";").replacingOccurrences(of: "\n", with: " ")

            csv += "\(date),\(record.wildlifeType),\(record.gender ?? ""),\(weight),\(lat),\(lon),\(record.bundesland ?? ""),\(record.moonPhase ?? ""),\(notes)\n"
        }

        let filename = "jagdtagebuch_\(Date().formatted(.dateTime.year().month().day())).csv"
        let documentsPath = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask).first!
        let fileURL = documentsPath.appendingPathComponent(filename)

        do {
            try csv.write(to: fileURL, atomically: true, encoding: .utf8)
            return fileURL
        } catch {
            AppLogger.shared.log("CSV export failed: \(error.localizedDescription)", type: .error)
            return nil
        }
    }
}

class PdfExporter {
    func export(records: [HuntRecord]) -> URL? {
        let pageWidth: CGFloat = 595.0  // A4
        let pageHeight: CGFloat = 842.0
        let margin: CGFloat = 40.0
        let dateFormatter = DateFormatter()
        dateFormatter.dateFormat = "dd.MM.yyyy HH:mm"

        let renderer = UIGraphicsPDFRenderer(bounds: CGRect(x: 0, y: 0, width: pageWidth, height: pageHeight))

        let data = renderer.pdfData { context in
            context.beginPage()
            var yPos: CGFloat = margin

            // Title
            let titleAttrs: [NSAttributedString.Key: Any] = [
                .font: UIFont.boldSystemFont(ofSize: 20),
                .foregroundColor: UIColor.white
            ]
            let title = "NoxVision - Jagdtagebuch"
            title.draw(at: CGPoint(x: margin, y: yPos), withAttributes: titleAttrs)
            yPos += 30

            // Date
            let dateAttrs: [NSAttributedString.Key: Any] = [
                .font: UIFont.systemFont(ofSize: 10),
                .foregroundColor: UIColor.gray
            ]
            let exportDate = "Export: \(Date().formatted(date: .abbreviated, time: .shortened))"
            exportDate.draw(at: CGPoint(x: margin, y: yPos), withAttributes: dateAttrs)
            yPos += 25

            // Records
            let recordTitleAttrs: [NSAttributedString.Key: Any] = [
                .font: UIFont.boldSystemFont(ofSize: 14),
                .foregroundColor: UIColor.white
            ]
            let recordBodyAttrs: [NSAttributedString.Key: Any] = [
                .font: UIFont.systemFont(ofSize: 11),
                .foregroundColor: UIColor.lightGray
            ]

            for record in records {
                if yPos > pageHeight - 100 {
                    context.beginPage()
                    yPos = margin
                }

                // Record title
                let recordTitle = "\(record.wildlifeType) \(record.gender.map { "- \($0)" } ?? "")"
                recordTitle.draw(at: CGPoint(x: margin, y: yPos), withAttributes: recordTitleAttrs)
                yPos += 18

                let date = dateFormatter.string(from: record.timestamp)
                "Datum: \(date)".draw(at: CGPoint(x: margin + 10, y: yPos), withAttributes: recordBodyAttrs)
                yPos += 14

                if let weight = record.estimatedWeight {
                    "Gewicht: \(weight) kg".draw(at: CGPoint(x: margin + 10, y: yPos), withAttributes: recordBodyAttrs)
                    yPos += 14
                }

                if let bl = record.bundesland {
                    "Bundesland: \(bl)".draw(at: CGPoint(x: margin + 10, y: yPos), withAttributes: recordBodyAttrs)
                    yPos += 14
                }

                if let lat = record.latitude, let lon = record.longitude {
                    "Position: \(String(format: "%.6f", lat)), \(String(format: "%.6f", lon))".draw(
                        at: CGPoint(x: margin + 10, y: yPos), withAttributes: recordBodyAttrs)
                    yPos += 14
                }

                if let notes = record.notes, !notes.isEmpty {
                    "Notizen: \(notes)".draw(at: CGPoint(x: margin + 10, y: yPos), withAttributes: recordBodyAttrs)
                    yPos += 14
                }

                yPos += 10
            }
        }

        let filename = "jagdtagebuch_\(Date().formatted(.dateTime.year().month().day())).pdf"
        let documentsPath = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask).first!
        let fileURL = documentsPath.appendingPathComponent(filename)

        do {
            try data.write(to: fileURL)
            return fileURL
        } catch {
            AppLogger.shared.log("PDF export failed: \(error.localizedDescription)", type: .error)
            return nil
        }
    }
}
