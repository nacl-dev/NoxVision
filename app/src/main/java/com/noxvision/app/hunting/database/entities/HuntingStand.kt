package com.noxvision.app.hunting.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "hunting_stands")
data class HuntingStand(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val type: HuntingStandType,
    val notes: String?
)

enum class HuntingStandType {
    HOCHSITZ,
    KANZEL,
    DRUCKJAGD,
    ANSITZ,
    CUSTOM
}
