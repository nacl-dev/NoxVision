package com.noxvision.app

data class ReleaseNote(val title: String, val description: String)

object WhatsNewRepository {
    val features = listOf(
        ReleaseNote(
            title = "Galerie Reload",
            description = "Es gibt nun einen Button zum manuellen Aktualisieren der Galerie."
        ),
        ReleaseNote(
            title = "Fix: Video Vorschau",
            description = "Behoben: Video Vorschau blieb manchmal schwarz."
        ),
        ReleaseNote(
            title = "Onboarding",
            description = "Neuer Guide für den ersten Start."
        )
    )
}
