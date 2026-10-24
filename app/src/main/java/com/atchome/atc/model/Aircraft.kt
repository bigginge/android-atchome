package com.atchome.atc.model

import androidx.compose.ui.graphics.Color
import com.atchome.atc.geo.GeoCalculations
import kotlin.math.roundToInt

const val MAX_AIRCRAFT = 500
const val TRAIL_LENGTH = 12
const val MAX_ALTITUDE_FT = 40000

fun formatPositionAge(seconds: Int?): String {
    if (seconds == null) return "age unknown"
    return when {
        seconds < 60 -> "${seconds}s ago"
        seconds < 3600 -> "${seconds / 60}m ago"
        seconds < 86400 -> "${seconds / 3600}h ago"
        else -> "${seconds / 86400}d ago"
    }
}

fun formatSpeedFromKnots(speedKnots: Float, unit: String): String {
    val (speed, label) = when (unit.uppercase()) {
        "MPH" -> speedKnots * 1.150779f to "MPH"
        "KPH" -> speedKnots * 1.852f to "KPH"
        else -> speedKnots to "KTS"
    }
    return String.format(java.util.Locale.US, "%.0f %s", speed, label)
}

/**
 * Aircraft representation tracking live telemetry, enrichment, and trail history.
 */
data class Aircraft(
    val hex: String = "",
    val callsign: String = "",
    var registration: String = "",
    val typeCode: String = "",

    var lat: Double = 0.0,
    var lon: Double = 0.0,
    var eastNm: Float = 0f,
    var northNm: Float = 0f,
    var distanceNm: Float = 0f,
    var trackDeg: Float = 0f,
    var altitudeFt: Int = 0,
    var altitudeKnown: Boolean = false,
    var vertRateFpm: Int = 0,
    var groundSpeedKts: Float = 0f,

    var manufacturer: String = "",
    var typeDescription: String = "",
    var registeredOwner: String = "",
    var origin: String = "",
    var destination: String = "",
    var originIcao: String = "",
    var destinationIcao: String = "",
    var originLat: Double? = null,
    var originLon: Double? = null,
    var destinationLat: Double? = null,
    var destinationLon: Double? = null,

    var yearBuilt: Int? = null,
    var imageUrl: String? = null,
    var imagePhotographer: String? = null,
    var imageLoaded: Boolean = false,

    var detailsLoaded: Boolean = false,
    var routeLoaded: Boolean = false,
    var routeLookupStep: Int = 0, // 0: route, 1: origin, 2: destination, 3: done

    val hue: Float = hueFromHex(hex),
    val trail: MutableList<TrailPoint> = mutableListOf(),
    var seen: Boolean = true,
    var positionAgeSeconds: Int? = null
) {
    /** Great-circle distance from origin airport to current position, if known. */
    val distanceFromOriginNm: Float?
        get() {
            val oLat = originLat ?: return null
            val oLon = originLon ?: return null
            if (lat == 0.0 && lon == 0.0) return null
            return GeoCalculations.haversineNm(oLat, oLon, lat, lon)
        }

    /** Great-circle distance from current position to destination airport, if known. */
    val distanceToDestinationNm: Float?
        get() {
            val dLat = destinationLat ?: return null
            val dLon = destinationLon ?: return null
            if (lat == 0.0 && lon == 0.0) return null
            return GeoCalculations.haversineNm(lat, lon, dLat, dLon)
        }
    val ageYears: Int?
        get() {
            val y = yearBuilt ?: return null
            val currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
            return if (y in 1900..currentYear) currentYear - y else null
        }

    val formattedAge: String
        get() = ageYears?.let { age ->
            val y = yearBuilt
            if (y != null) "$age yrs ($y)" else "$age yrs"
        } ?: "Unknown"

    val trendSymbol: String
        get() = when {
            vertRateFpm > 250 -> "▲"
            vertRateFpm < -250 -> "▼"
            else -> "─"
        }

    val formattedAltitude: String
        get() = when {
            !altitudeKnown -> "ALT ?"
            altitudeFt <= 0 -> "GND"
            altitudeFt >= 18000 -> "FL${(altitudeFt + 50) / 100}"
            else -> String.format(java.util.Locale.US, "%03d", (altitudeFt + 50) / 100)
        }
    /**
     * Map altitude 0..40,000 ft to 0..1 (clamped).
     */
    val normalizedAltitude: Float
        get() = altitudeNorm(altitudeFt)

    /**
     * Compute display color matching the ESP32 ATC HSV algorithm:
     * - Low altitude -> darker, more saturated
     * - High altitude -> lighter, less saturated
     */
    val displayColor: Color
        get() {
            val n = normalizedAltitude
            val v = (0.28f + n * 0.72f).coerceIn(0f, 1f)
            val s = (0.92f - n * 0.28f).coerceIn(0f, 1f)
            return Color.hsv(hue, s, v)
        }

    /**
     * Calculate opacity for trail segments. Lower altitude = more distinct trail.
     */
    fun trailOpacity(ageIndex: Int, count: Int): Float {
        val altFactor = 1.0f - normalizedAltitude
        val base = 40.0f + altFactor * 180.0f // 0..255 scale
        val ageFade = if (count <= 1) 1.0f else (ageIndex + 1).toFloat() / count.toFloat()
        return (base * ageFade / 255.0f).coerceIn(0.1f, 1.0f)
    }

    companion object {
        fun altitudeNorm(altitudeFt: Int): Float {
            if (altitudeFt <= 0) return 0.0f
            if (altitudeFt >= MAX_ALTITUDE_FT) return 1.0f
            return altitudeFt.toFloat() / MAX_ALTITUDE_FT.toFloat()
        }

        /**
         * Stable hue (0..360°) computed from FNV-1a hash of ICAO hex string.
         */
        fun hueFromHex(hex: String): Float {
            if (hex.isEmpty()) return 180f
            var hash = 2166136261L
            for (char in hex.uppercase()) {
                hash = hash xor char.code.toLong()
                hash = (hash * 16777619L) and 0xFFFFFFFFL
            }
            val h8 = (hash and 0xFFL).toFloat()
            return h8 * 360f / 255f
        }
    }
}
