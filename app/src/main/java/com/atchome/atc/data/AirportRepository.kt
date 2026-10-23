package com.atchome.atc.data

import android.content.Context
import com.atchome.atc.geo.GeoCalculations
import com.atchome.atc.model.Airport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray

/**
 * Loads the bundled OurAirports subset and filters to airports within radar range.
 */
class AirportRepository(private val context: Context) {

    private data class AirportRecord(
        val ident: String,
        val name: String,
        val type: String,
        val lat: Double,
        val lon: Double
    )

    private val mutex = Mutex()
    private var catalog: List<AirportRecord>? = null

    suspend fun nearby(
        homeLat: Double,
        homeLon: Double,
        radiusNm: Int,
        airportTypes: Set<String>
    ): List<Airport> =
        withContext(Dispatchers.IO) {
            val all = ensureLoaded()
            val radius = radiusNm.toFloat()
            val radiusSq = radius * radius
            // Coarse bounding box in degrees before NM projection
            val latPad = radiusNm / 60.0 + 0.05
            val cosLat = kotlin.math.cos(Math.toRadians(homeLat)).coerceAtLeast(0.01)
            val lonPad = radiusNm / (60.0 * cosLat) + 0.05

            all.mapNotNull { rec ->
                if (rec.type !in airportTypes) return@mapNotNull null
                if (rec.lat < homeLat - latPad || rec.lat > homeLat + latPad) return@mapNotNull null
                if (rec.lon < homeLon - lonPad || rec.lon > homeLon + lonPad) return@mapNotNull null
                val (eastNm, northNm) = GeoCalculations.latLonToNm(
                    homeLat, homeLon, rec.lat, rec.lon
                )
                if (eastNm * eastNm + northNm * northNm > radiusSq) return@mapNotNull null
                Airport(
                    ident = rec.ident,
                    name = rec.name,
                    lat = rec.lat,
                    lon = rec.lon,
                    eastNm = eastNm,
                    northNm = northNm,
                    type = rec.type
                )
            }
        }

    private suspend fun ensureLoaded(): List<AirportRecord> = mutex.withLock {
        catalog?.let { return it }
        val loaded = loadFromAssets()
        catalog = loaded
        loaded
    }

    private fun loadFromAssets(): List<AirportRecord> {
        val json = context.assets.open(ASSET_NAME).bufferedReader().use { it.readText() }
        val array = JSONArray(json)
        val result = ArrayList<AirportRecord>(array.length())
        for (i in 0 until array.length()) {
            val obj = array.optJSONObject(i) ?: continue
            val ident = obj.optString("ident").trim()
            if (ident.isEmpty()) continue
            val type = obj.optString("type").trim()
            if (type !in AIRPORT_TYPES) continue
            val lat = obj.optDouble("lat", Double.NaN)
            val lon = obj.optDouble("lon", Double.NaN)
            if (!lat.isFinite() || !lon.isFinite()) continue
            result.add(
                AirportRecord(
                    ident = ident,
                    name = obj.optString("name").trim(),
                    type = type,
                    lat = lat,
                    lon = lon
                )
            )
        }
        return result
    }

    companion object {
        private const val ASSET_NAME = "airports.json"
        private val AIRPORT_TYPES = setOf("small", "medium", "large")
    }
}
