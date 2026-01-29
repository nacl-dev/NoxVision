package com.noxvision.app.hunting.database.dao

import androidx.room.*
import com.noxvision.app.hunting.database.entities.HuntingStand
import com.noxvision.app.hunting.database.entities.HuntingStandType
import kotlinx.coroutines.flow.Flow

@Dao
interface HuntingStandDao {
    @Query("SELECT * FROM hunting_stands ORDER BY name ASC")
    fun getAllStands(): Flow<List<HuntingStand>>

    @Query("SELECT * FROM hunting_stands WHERE type = :type ORDER BY name ASC")
    fun getStandsByType(type: HuntingStandType): Flow<List<HuntingStand>>

    @Query("SELECT * FROM hunting_stands WHERE id = :id")
    suspend fun getStandById(id: Long): HuntingStand?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(stand: HuntingStand): Long

    @Update
    suspend fun update(stand: HuntingStand)

    @Delete
    suspend fun delete(stand: HuntingStand)

    @Query("DELETE FROM hunting_stands WHERE id = :id")
    suspend fun deleteById(id: Long)
}
