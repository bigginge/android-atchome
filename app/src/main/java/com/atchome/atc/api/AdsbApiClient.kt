package com.atchome.atc.api

import com.atchome.atc.geo.GeoCalculations
import com.atchome.atc.model.Aircraft
import com.atchome.atc.model.MAX_AIRCRAFT
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.Locale
import java.util.concurrent.TimeUnit

class AdsbApiClient(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(4, TimeUnit.SECONDS)
        .readTimeout(4, TimeUnit.SECONDS)
        .build()
) {
    /**
     * Fetch nearby aircraft positions from adsb.fi.
     */
    suspend fun fetchNearbyAircraft(
        homeLat: Double,
        homeLon: Double,
        radiusNm: Int
    ): Result<List<Aircraft>> = withContext(Dispatchers.IO) {
        try {
            val url = String.format(
                Locale.US,
                "https://opendata.adsb.fi/api/v3/lat/%.4f/lon/%.4f/dist/%d",
                homeLat,
                homeLon,
                radiusNm
            )

            val request = Request.Builder()
                .url(url)
                .header("Accept", "application/json")
                .header("User-Agent", "AtcHome-Android/1.0")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(
                        Exception("HTTP error ${response.code}: ${response.message}")
                    )
                }

                val body = response.body?.string()
                    ?: return@withContext Result.success(emptyList())

                val json = JSONObject(body)
                val acArray = json.optJSONArray("ac")
                    ?: return@withContext Result.success(emptyList())

                val results = mutableListOf<Aircraft>()
                val count = acArray.length()

                for (i in 0 until count) {
                    val item = acArray.optJSONObject(i) ?: continue

                    val hex = item.optString("hex").trim()
                    if (hex.isEmpty()) continue

                    val lat = item.optDouble("lat", Double.NaN)
                    val lon = item.optDouble("lon", Double.NaN)
                    if (lat.isNaN() || lon.isNaN()) continue

                    val callsign = item.optString("flight").trim()
                    val registration = item.optString("r").trim()
                    val typeCode = item.optString("t").trim()
                    val desc = item.optString("desc").trim()
                    val ownOp = item.optString("ownOp").trim()

                    val yearRaw = item.opt("year")
                    val yearBuilt = when (yearRaw) {
                        is Number -> yearRaw.toInt()
                        is String -> yearRaw.toIntOrNull()
                        else -> null
                    }

                    val (eastNm, northNm) = GeoCalculations.latLonToNm(homeLat, homeLon, lat, lon)
                    val distanceNm = GeoCalculations.haversineNm(homeLat, homeLon, lat, lon)

                    val altBaroRaw = item.opt("alt_baro")
                    val altitudeFt = when (altBaroRaw) {
                        is Number -> altBaroRaw.toInt()
                        is String -> altBaroRaw.toIntOrNull() ?: 0
                        else -> 0
                    }
                    val altitudeKnown = when (altBaroRaw) {
                        is Number -> true
                        is String -> altBaroRaw.equals("ground", ignoreCase = true) || altBaroRaw.toIntOrNull() != null
                        else -> false
                    }

                    val gsRaw = item.opt("gs")
                    val groundSpeedKts = when (gsRaw) {
                        is Number -> gsRaw.toFloat()
                        is String -> gsRaw.toFloatOrNull() ?: 0f
                        else -> 0f
                    }

                    val trackRaw = item.opt("track")
                    val trackDeg = when (trackRaw) {
                        is Number -> trackRaw.toFloat()
                        is String -> trackRaw.toFloatOrNull() ?: 0f
                        else -> 0f
                    }

                    val baroRateRaw = if (item.has("baro_rate")) item.opt("baro_rate") else item.opt("geom_rate")
                    val vertRateFpm = when (baroRateRaw) {
                        is Number -> baroRateRaw.toInt()
                        is String -> baroRateRaw.toIntOrNull() ?: 0
                        else -> 0
                    }

                    val aircraft = Aircraft(
                        hex = hex,
                        callsign = callsign,
                        registration = registration,
                        typeCode = typeCode,
                        typeDescription = desc,
                        registeredOwner = ownOp,
                        yearBuilt = yearBuilt,
                        lat = lat,
                        lon = lon,
                        eastNm = eastNm,
                        northNm = northNm,
                        distanceNm = distanceNm,
                        trackDeg = trackDeg,
                        altitudeFt = altitudeFt,
                        altitudeKnown = altitudeKnown,
                        vertRateFpm = vertRateFpm,
                        groundSpeedKts = groundSpeedKts
                    )
                    results.add(aircraft)
                }

                Result.success(results)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
