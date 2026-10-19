package com.atchome.atc.geo

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

object GeoCalculations {
    private const val DEG_TO_RAD = Math.PI / 180.0
    private const val EARTH_RADIUS_NM = 3440.065

    /**
     * Converts a lat/lon coordinate into East/North offsets in nautical miles from home.
     * Uses flat-earth approximation suitable for tactical radar display.
     */
    fun latLonToNm(
        homeLat: Double,
        homeLon: Double,
        lat: Double,
        lon: Double
    ): Pair<Float, Float> {
        val cosLat = cos(homeLat * DEG_TO_RAD)
        val eastNm = ((lon - homeLon) * 60.0 * cosLat).toFloat()
        val northNm = ((lat - homeLat) * 60.0).toFloat()
        return Pair(eastNm, northNm)
    }

    /**
     * Great-circle distance between two points in nautical miles.
     */
    fun haversineNm(
        lat1: Double,
        lon1: Double,
        lat2: Double,
        lon2: Double
    ): Float {
        val dLat = (lat2 - lat1) * DEG_TO_RAD
        val dLon = (lon2 - lon1) * DEG_TO_RAD
        val a = sin(dLat * 0.5) * sin(dLat * 0.5) +
                cos(lat1 * DEG_TO_RAD) * cos(lat2 * DEG_TO_RAD) *
                sin(dLon * 0.5) * sin(dLon * 0.5)
        val c = 2.0 * atan2(sqrt(a), sqrt(1.0 - a))
        return (EARTH_RADIUS_NM * c).toFloat()
    }
}
