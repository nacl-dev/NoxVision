import SwiftUI

struct AbschussListScreen: View {
    let onBack: () -> Void
    let onNew: () -> Void
    let onEdit: (HuntRecord) -> Void

    @EnvironmentObject var database: HuntingDatabaseManager
    @State private var showExportMenu = false
    @State private var showDeleteConfirm = false
    @State private var recordToDelete: HuntRecord?
    @State private var toastMessage: String?

    var body: some View {
        ZStack {
            VStack(spacing: 0) {
                NoxNavigationHeader(
                    title: NSLocalizedString("hunting_diary", comment: ""),
                    onBack: onBack,
                    trailing: AnyView(
                        HStack(spacing: 12) {
                            Menu {
                                Button(action: exportCSV) {
                                    Label(NSLocalizedString("export_csv", comment: ""), systemImage: "doc.text")
                                }
                                Button(action: exportPDF) {
                                    Label(NSLocalizedString("export_pdf", comment: ""), systemImage: "doc.richtext")
                                }
                            } label: {
                                Image(systemName: "square.and.arrow.up")
                                    .foregroundColor(NoxColors.primary)
                            }
                        }
                    )
                )

                if database.huntRecords.isEmpty {
                    emptyState
                } else {
                    recordsList
                }
            }

            // FAB
            VStack {
                Spacer()
                HStack {
                    Spacer()
                    Button(action: onNew) {
                        Image(systemName: "plus")
                            .font(.system(size: 24, weight: .medium))
                            .foregroundColor(NoxColors.onPrimary)
                            .frame(width: 56, height: 56)
                            .background(NoxColors.primary)
                            .cornerRadius(16)
                            .shadow(color: .black.opacity(0.3), radius: 8, y: 4)
                    }
                    .padding(.trailing, 16)
                    .padding(.bottom, 24)
                }
            }

            // Toast
            if let message = toastMessage {
                VStack {
                    Spacer()
                    Text(message)
                        .font(.system(size: 14))
                        .foregroundColor(NoxColors.onSurface)
                        .padding(.horizontal, 20)
                        .padding(.vertical, 10)
                        .background(NoxColors.surfaceVariant.opacity(0.95))
                        .cornerRadius(24)
                        .padding(.bottom, 100)
                }
            }
        }
        .alert(NSLocalizedString("delete_entry", comment: ""), isPresented: $showDeleteConfirm) {
            Button(NSLocalizedString("cancel", comment: ""), role: .cancel) {}
            Button(NSLocalizedString("delete", comment: ""), role: .destructive) {
                if let record = recordToDelete {
                    database.deleteHuntRecord(record)
                }
            }
        } message: {
            Text(NSLocalizedString("delete_entry_confirm", comment: ""))
        }
        .onAppear {
            database.fetchHuntRecords()
        }
    }

    private var emptyState: some View {
        VStack(spacing: 16) {
            Spacer()
            Image(systemName: "doc.text")
                .font(.system(size: 48))
                .foregroundColor(NoxColors.outline)
            Text(NSLocalizedString("no_entries", comment: ""))
                .font(.system(size: 16, weight: .medium))
                .foregroundColor(NoxColors.onSurface)
            Text(NSLocalizedString("tap_to_add", comment: ""))
                .font(.system(size: 14))
                .foregroundColor(NoxColors.onSurfaceVariant)
            Spacer()
        }
    }

    private var recordsList: some View {
        ScrollView {
            LazyVStack(spacing: 8) {
                ForEach(database.huntRecords, id: \.id) { record in
                    HuntRecordCard(record: record, onTap: { onEdit(record) })
                        .contextMenu {
                            Button(role: .destructive) {
                                recordToDelete = record
                                showDeleteConfirm = true
                            } label: {
                                Label(NSLocalizedString("delete", comment: ""), systemImage: "trash")
                            }
                        }
                }
            }
            .padding(.horizontal, 16)
            .padding(.bottom, 80)
        }
    }

    private func exportCSV() {
        let exporter = CsvExporter()
        if let url = exporter.export(records: database.huntRecords) {
            showToast(String(format: NSLocalizedString("csv_saved", comment: ""), url.lastPathComponent))
        }
    }

    private func exportPDF() {
        let exporter = PdfExporter()
        if let url = exporter.export(records: database.huntRecords) {
            showToast(String(format: NSLocalizedString("pdf_saved", comment: ""), url.lastPathComponent))
        }
    }

    private func showToast(_ message: String) {
        toastMessage = message
        DispatchQueue.main.asyncAfter(deadline: .now() + 3) {
            toastMessage = nil
        }
    }
}

struct HuntRecordCard: View {
    let record: HuntRecord
    let onTap: () -> Void

    var body: some View {
        Button(action: onTap) {
            HStack(spacing: 12) {
                // Wildlife type icon
                Image(systemName: "leaf.fill")
                    .font(.system(size: 20))
                    .foregroundColor(NoxColors.primary)
                    .frame(width: 40, height: 40)
                    .background(NoxColors.primary.opacity(0.12))
                    .cornerRadius(10)

                VStack(alignment: .leading, spacing: 4) {
                    HStack {
                        Text(record.wildlifeType)
                            .font(.system(size: 15, weight: .medium))
                            .foregroundColor(NoxColors.onSurface)
                        if let gender = record.gender {
                            Text("- \(gender)")
                                .font(.system(size: 14))
                                .foregroundColor(NoxColors.onSurfaceVariant)
                        }
                    }

                    HStack(spacing: 8) {
                        Text(record.timestamp, style: .date)
                            .font(.system(size: 12))
                            .foregroundColor(NoxColors.onSurfaceVariant)

                        if let weight = record.estimatedWeight {
                            Text("\(weight) kg")
                                .font(.system(size: 12))
                                .foregroundColor(NoxColors.onSurfaceVariant)
                        }

                        if let bl = record.bundesland {
                            Text(bl)
                                .font(.system(size: 11))
                                .foregroundColor(NoxColors.primary)
                                .padding(.horizontal, 6)
                                .padding(.vertical, 2)
                                .background(NoxColors.primary.opacity(0.12))
                                .cornerRadius(4)
                        }
                    }
                }

                Spacer()

                Image(systemName: "chevron.right")
                    .font(.system(size: 12))
                    .foregroundColor(NoxColors.outline)
            }
            .padding(12)
            .background(NoxColors.cardBackground)
            .cornerRadius(12)
        }
    }
}
