package com.noxvision.app.hunting.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "waypoints",
    foreignKeys = [
        ForeignKey(
            entity = HuntRecord::class,
            parentColumns = ["id"],
            childColumns = ["huntRecordId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("huntRecordId")]
)
data class Waypoint(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val huntRecordId: Long?,
    val latitude: Double,
    val longitude: Double,
    val type: WaypointType,
    val timestamp: Long,
    val compassBearing: Float?,
    val notes: String?
)

enum class WaypointType {
    LAST_SEEN,
    BLOOD_TRAIL,
    RECOVERY,
    ANSCHUSS,
    CUSTOM
}
