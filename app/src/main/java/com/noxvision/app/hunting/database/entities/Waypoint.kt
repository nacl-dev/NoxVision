package com.noxvision.app.hunting.database.entities

import androidx.annotation.StringRes
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.noxvision.app.R

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

enum class WaypointType(@StringRes val displayNameRes: Int) {
    LAST_SEEN(R.string.waypoint_last_seen),
    BLOOD_TRAIL(R.string.waypoint_blood_trail),
    RECOVERY(R.string.waypoint_recovery),
    ANSCHUSS(R.string.waypoint_anschuss),
    CUSTOM(R.string.waypoint_custom)
}
