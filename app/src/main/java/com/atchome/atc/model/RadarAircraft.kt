package com.atchome.atc.model

/** Compact, immutable snapshot used by the frequently redrawn radar scope. */
data class RadarAircraft(
    val hex: String,
    val callsign: String,
    val registration: String,
    val typeCode: String,
    val typeDescription: String,
    val manufacturer: String,
    val registeredOwner: String,
    val eastNm: Float,
    val northNm: Float,
    val trackDeg: Float,
    val altitudeFt: Int,
    val altitudeKnown: Boolean,
    val vertRateFpm: Int,
    val groundSpeedKts: Float,
    val origin: String,
    val destination: String,
    val originIcao: String,
    val destinationIcao: String,
    val trail: List<TrailPoint>,
    val seen: Boolean,
    val positionAgeSeconds: Int?,
    val positionTimestampNanos: Long = System.nanoTime(),
    val eastNmPerSecond: Float = (
        kotlin.math.sin(Math.toRadians(trackDeg.toDouble())) * groundSpeedKts / 3600.0
    ).toFloat(),
    val northNmPerSecond: Float = (
        kotlin.math.cos(Math.toRadians(trackDeg.toDouble())) * groundSpeedKts / 3600.0
    ).toFloat(),
    val headingEast: Float = kotlin.math.sin(Math.toRadians(trackDeg.toDouble())).toFloat(),
    val headingNorth: Float = kotlin.math.cos(Math.toRadians(trackDeg.toDouble())).toFloat()
) {
    val aircraftClass: AircraftClass = classifyAircraft(
        typeCode, typeDescription, manufacturer, registeredOwner, callsign
    )
    private val cachedFormattedAltitude: String = when {
            !altitudeKnown -> "ALT ?"
            altitudeFt <= 0 -> "GND"
            altitudeFt >= 18000 -> "FL${(altitudeFt + 50) / 100}"
            else -> String.format(java.util.Locale.US, "%03d", (altitudeFt + 50) / 100)
        }

    private val cachedTrendSymbol: String = when {
            vertRateFpm > 250 -> "▲"
            vertRateFpm < -250 -> "▼"
            else -> "─"
        }
    private val cachedSpeedKts = String.format(java.util.Locale.US, "%.0fKTS", groundSpeedKts)
    private val cachedSpeedMph = String.format(java.util.Locale.US, "%.0fMPH", groundSpeedKts * 1.150779f)
    private val cachedSpeedKph = String.format(java.util.Locale.US, "%.0fKPH", groundSpeedKts * 1.852f)

    val formattedAltitude: String get() = cachedFormattedAltitude
    val trendSymbol: String get() = cachedTrendSymbol
    fun formattedSpeedCompact(unit: String): String = when (unit) {
        "MPH" -> cachedSpeedMph
        "KPH" -> cachedSpeedKph
        else -> cachedSpeedKts
    }
}

fun Aircraft.toRadarAircraft(
    trailSnapshot: List<TrailPoint> = trail.toList(),
    positionTimestampNanos: Long = System.nanoTime()
): RadarAircraft = RadarAircraft(
    hex = hex,
    callsign = callsign,
    registration = registration,
    typeCode = typeCode,
    typeDescription = typeDescription,
    manufacturer = manufacturer,
    registeredOwner = registeredOwner,
    eastNm = eastNm,
    northNm = northNm,
    trackDeg = trackDeg,
    altitudeFt = altitudeFt,
    altitudeKnown = altitudeKnown,
    vertRateFpm = vertRateFpm,
    groundSpeedKts = groundSpeedKts,
    origin = origin,
    destination = destination,
    originIcao = originIcao,
    destinationIcao = destinationIcao,
    trail = trailSnapshot,
    seen = seen,
    positionAgeSeconds = positionAgeSeconds,
    positionTimestampNanos = positionTimestampNanos
)
