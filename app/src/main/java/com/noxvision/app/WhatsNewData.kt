package com.noxvision.app

data class ReleaseNote(val title: String, val description: String)

object WhatsNewRepository {
    val features = listOf(
        ReleaseNote(
            title = "Willkommen im Play Store!",
            description = "NoxVision ist jetzt offiziell im Google Play Store verfügbar."
        ),
        ReleaseNote(
            title = "Onboarding Guide",
            description = "Ein neuer Einrichtungsassistent führt dich Schritt für Schritt durch den ersten Start."
        ),
    )
}