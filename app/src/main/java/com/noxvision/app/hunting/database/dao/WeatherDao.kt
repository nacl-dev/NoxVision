package com.noxvision.app.hunting.database.dao

import androidx.room.*
import com.noxvision.app.hunting.database.entities.CachedWeather
import kotlin.jvm.JvmSuppressWildcards

@JvmSuppressWildcards
@Dao
interface WeatherDao {
    @Query("SELECT * FROM cached_weather WHERE id = 0")
    suspend fun getCachedWeather(): CachedWeather?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(weather: CachedWeather): Long

    @Query("DELETE FROM cached_weather")
    suspend fun clear(): Int
}
