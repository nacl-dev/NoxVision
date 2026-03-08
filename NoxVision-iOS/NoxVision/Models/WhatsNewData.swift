import Foundation

struct ReleaseNote: Identifiable {
    let id = UUID()
    let title: String
    let description: String
}

struct WhatsNewRepository {
    static let features: [ReleaseNote] = [
        ReleaseNote(
            title: NSLocalizedString("whats_new_gallery_reload", comment: ""),
            description: NSLocalizedString("whats_new_gallery_reload_desc", comment: "")
        ),
        ReleaseNote(
            title: NSLocalizedString("whats_new_video_fix", comment: ""),
            description: NSLocalizedString("whats_new_video_fix_desc", comment: "")
        ),
        ReleaseNote(
            title: NSLocalizedString("whats_new_onboarding", comment: ""),
            description: NSLocalizedString("whats_new_onboarding_desc", comment: "")
        )
    ]
}
