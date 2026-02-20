package com.noxvision.app.hunting.database.dao

import androidx.room.*
import com.noxvision.app.hunting.database.entities.Waypoint
import com.noxvision.app.hunting.database.entities.WaypointType
import kotlinx.coroutines.flow.Flow
import kotlin.jvm.JvmSuppressWildcards

@JvmSuppressWildcards
@Dao
interface WaypointDao {
    @Query("SELECT * FROM waypoints ORDER BY timestamp DESC")
    fun getAllWaypoints(): Flow<List<Waypoint>>

    @Query("SELECT * FROM waypoints WHERE huntRecordId = :huntRecordId ORDER BY timestamp ASC")
    fun getWaypointsForHunt(huntRecordId: Long): Flow<List<Waypoint>>

    @Query("SELECT * FROM waypoints WHERE huntRecordId IS NULL ORDER BY timestamp DESC")
    fun getStandaloneWaypoints(): Flow<List<Waypoint>>

    @Query("SELECT * FROM waypoints WHERE type = :type ORDER BY timestamp DESC")
    fun getWaypointsByType(type: WaypointType): Flow<List<Waypoint>>

    @Query("SELECT * FROM waypoints WHERE id = :id")
    suspend fun getWaypointById(id: Long): Waypoint?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(waypoint: Waypoint): Long

    @Update
    suspend fun update(waypoint: Waypoint): Int

    @Delete
    suspend fun delete(waypoint: Waypoint): Int

    @Query("DELETE FROM waypoints WHERE id = :id")
    suspend fun deleteById(id: Long): Int

    @Query("DELETE FROM waypoints WHERE huntRecordId = :huntRecordId")
    suspend fun deleteAllForHunt(huntRecordId: Long): Int
}
