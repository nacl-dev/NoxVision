package com.noxvision.app.hunting.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "hunt_records")
data class HuntRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val latitude: Double?,
    val longitude: Double?,
    val wildlifeType: String,
    val gender: String?,
    val estimatedWeight: Int?,
    val notes: String?,
    val thermalImagePath: String?,
    val moonPhase: String?,
    val weatherSnapshot: String?,
    val bundesland: String?
)

object WildlifeTypes {
    val REHWILD = listOf("Bock", "Ricke", "Kitz")
    val SCHWARZWILD = listOf("Keiler", "Bache", "Frischling", "Ueberlaeufer")
    val ROTWILD = listOf("Hirsch", "Tier", "Kalb")
    val DAMWILD = listOf("Hirsch", "Tier", "Kalb")
    val MUFFELWILD = listOf("Widder", "Schaf", "Lamm")
    val RAUBWILD = listOf("Fuchs", "Dachs", "Waschbaer", "Marderhund", "Marder")
    val NIEDERWILD = listOf("Hase", "Wildkaninchen")
    val FEDERWILD = listOf("Fasan", "Wildente", "Wildgans", "Taube", "Kraehe")

    val ALL_TYPES = mapOf(
        "Rehwild" to REHWILD,
        "Schwarzwild" to SCHWARZWILD,
        "Rotwild" to ROTWILD,
        "Damwild" to DAMWILD,
        "Muffelwild" to MUFFELWILD,
        "Raubwild" to RAUBWILD,
        "Niederwild" to NIEDERWILD,
        "Federwild" to FEDERWILD
    )

    fun getGendersForType(type: String): List<String> {
        return ALL_TYPES[type] ?: emptyList()
    }
}
