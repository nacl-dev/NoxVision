package com.noxvision.app.hunting.maps

import android.content.Context
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import java.io.File

class OfflineMapManager(private val context: Context) {

    init {
        // Initialize OSMDroid configuration
        Configuration.getInstance().load(context, context.getSharedPreferences("osmdroid", Context.MODE_PRIVATE))
        Configuration.getInstance().userAgentValue = "NoxVision/${context.packageManager.getPackageInfo(context.packageName, 0).versionName}"

        // Set tile cache location
        val tileCache = File(context.cacheDir, "osmdroid/tiles")
        if (!tileCache.exists()) {
            tileCache.mkdirs()
        }
        Configuration.getInstance().osmdroidTileCache = tileCache
    }

    fun getTileSource() = TileSourceFactory.MAPNIK

    fun getOfflineTileSource() = TileSourceFactory.MAPNIK

    fun calculateBoundingBox(centerLat: Double, centerLon: Double, radiusKm: Double): BoundingBox {
        // Approximate degrees per km at given latitude
        val latDegPerKm = 1.0 / 111.0
        val lonDegPerKm = 1.0 / (111.0 * kotlin.math.cos(Math.toRadians(centerLat)))

        val latDelta = radiusKm * latDegPerKm
        val lonDelta = radiusKm * lonDegPerKm

        return BoundingBox(
            centerLat + latDelta, // north
            centerLon + lonDelta, // east
            centerLat - latDelta, // south
            centerLon - lonDelta  // west
        )
    }

    fun getCacheSize(): Long {
        val cacheDir = Configuration.getInstance().osmdroidTileCache
        return cacheDir?.let { calculateDirSize(it) } ?: 0L
    }

    private fun calculateDirSize(dir: File): Long {
        var size = 0L
        dir.listFiles()?.forEach { file ->
            size += if (file.isDirectory) {
                calculateDirSize(file)
            } else {
                file.length()
            }
        }
        return size
    }

    fun clearCache() {
        val cacheDir = Configuration.getInstance().osmdroidTileCache
        cacheDir?.deleteRecursively()
        cacheDir?.mkdirs()
    }

    fun formatCacheSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            else -> "${bytes / (1024 * 1024)} MB"
        }
    }

    companion object {
        val DEFAULT_CENTER = GeoPoint(51.1657, 10.4515) // Center of Germany
        const val DEFAULT_ZOOM = 6.0
        const val MIN_ZOOM = 4.0
        const val MAX_ZOOM = 19.0
    }
}
