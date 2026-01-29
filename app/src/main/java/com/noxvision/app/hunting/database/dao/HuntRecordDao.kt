package com.noxvision.app.hunting.database.dao

import androidx.room.*
import com.noxvision.app.hunting.database.entities.HuntRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface HuntRecordDao {
    @Query("SELECT * FROM hunt_records ORDER BY timestamp DESC")
    fun getAllRecords(): Flow<List<HuntRecord>>

    @Query("SELECT * FROM hunt_records WHERE id = :id")
    suspend fun getRecordById(id: Long): HuntRecord?

    @Query("SELECT * FROM hunt_records WHERE wildlifeType = :type ORDER BY timestamp DESC")
    fun getRecordsByType(type: String): Flow<List<HuntRecord>>

    @Query("SELECT * FROM hunt_records WHERE bundesland = :bundesland ORDER BY timestamp DESC")
    fun getRecordsByBundesland(bundesland: String): Flow<List<HuntRecord>>

    @Query("SELECT * FROM hunt_records WHERE timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp DESC")
    fun getRecordsByDateRange(startTime: Long, endTime: Long): Flow<List<HuntRecord>>

    @Query("SELECT wildlifeType, COUNT(*) as count FROM hunt_records GROUP BY wildlifeType")
    suspend fun getStatsByWildlifeType(): List<WildlifeTypeCount>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: HuntRecord): Long

    @Update
    suspend fun update(record: HuntRecord)

    @Delete
    suspend fun delete(record: HuntRecord)

    @Query("DELETE FROM hunt_records WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT COUNT(*) FROM hunt_records")
    suspend fun getCount(): Int
}

data class WildlifeTypeCount(
    val wildlifeType: String,
    val count: Int
)
