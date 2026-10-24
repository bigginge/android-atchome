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
    val positionAgeSeconds: Int?
) {
    val formattedAltitude: String
        get() = when {
            !altitudeKnown -> "ALT ?"
            altitudeFt <= 0 -> "GND"
            altitudeFt >= 18000 -> "FL${(altitudeFt + 50) / 100}"
            else -> String.format(java.util.Locale.US, "%03d", (altitudeFt + 50) / 100)
        }

    val trendSymbol: String
        get() = when {
            vertRateFpm > 250 -> "▲"
            vertRateFpm < -250 -> "▼"
            else -> "─"
        }
}

fun Aircraft.toRadarAircraft(): RadarAircraft = RadarAircraft(
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
    trail = trail.toList(),
    seen = seen,
    positionAgeSeconds = positionAgeSeconds
)
