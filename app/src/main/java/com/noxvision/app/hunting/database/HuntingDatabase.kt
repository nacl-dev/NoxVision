package com.noxvision.app.hunting.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.noxvision.app.hunting.database.dao.HuntRecordDao
import com.noxvision.app.hunting.database.dao.HuntingStandDao
import com.noxvision.app.hunting.database.dao.WaypointDao
import com.noxvision.app.hunting.database.dao.WeatherDao
import com.noxvision.app.hunting.database.entities.CachedWeather
import com.noxvision.app.hunting.database.entities.HuntRecord
import com.noxvision.app.hunting.database.entities.HuntingStand
import com.noxvision.app.hunting.database.entities.HuntingStandType
import com.noxvision.app.hunting.database.entities.Waypoint
import com.noxvision.app.hunting.database.entities.WaypointType

class Converters {
    @TypeConverter
    fun fromWaypointType(value: WaypointType): String = value.name

    @TypeConverter
    fun toWaypointType(value: String): WaypointType = WaypointType.valueOf(value)

    @TypeConverter
    fun fromHuntingStandType(value: HuntingStandType): String = value.name

    @TypeConverter
    fun toHuntingStandType(value: String): HuntingStandType = HuntingStandType.valueOf(value)
}

@Database(
    entities = [
        HuntRecord::class,
        Waypoint::class,
        HuntingStand::class,
        CachedWeather::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class HuntingDatabase : RoomDatabase() {
    abstract fun huntRecordDao(): HuntRecordDao
    abstract fun waypointDao(): WaypointDao
    abstract fun huntingStandDao(): HuntingStandDao
    abstract fun weatherDao(): WeatherDao

    companion object {
        @Volatile
        private var INSTANCE: HuntingDatabase? = null

        fun getDatabase(context: Context): HuntingDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    HuntingDatabase::class.java,
                    "hunting_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
