package com.noxvision.app.hunting.database.entities

import androidx.annotation.StringRes
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.noxvision.app.R

@Entity(tableName = "hunting_stands")
data class HuntingStand(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val type: HuntingStandType,
    val notes: String?
)

enum class HuntingStandType(@param:StringRes val displayNameRes: Int) {
    HOCHSITZ(R.string.stand_hochsitz),
    KANZEL(R.string.stand_kanzel),
    DRUCKJAGD(R.string.stand_druckjagd),
    ANSITZ(R.string.stand_ansitz),
    CUSTOM(R.string.stand_custom)
}
