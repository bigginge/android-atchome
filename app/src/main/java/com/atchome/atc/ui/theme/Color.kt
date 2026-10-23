package com.atchome.atc.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf

data class AtcPalette(
    val radarBackground: Color,
    val infoBackground: Color,
    val ring: Color,
    val minorRing: Color,
    val crosshair: Color,
    val spokes: Color,
    val scopeBorder: Color,
    val sweepBeam: Color,
    val sweepTail: Color,
    val tagBackground: Color,
    val tagBorder: Color,
    val reticle: Color,
    val homeMarker: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val highlight: Color,
    val cardBackground: Color,
    val amber: Color,
    val green: Color,
    val groundTraffic: Color,
    val unknownTraffic: Color,
    val lowAltitude: Color,
    val midAltitude: Color,
    val highAltitude: Color,
    val cruiseAltitude: Color
)

val DarkAtcPalette = AtcPalette(
    radarBackground = Color(0xFF080E14), infoBackground = Color(0xFF0F1720),
    ring = Color(0xFF1E2E3E), minorRing = Color(0xFF14202C),
    crosshair = Color(0xFF243648), spokes = Color(0x1F4A6A8A),
    scopeBorder = Color(0xFF2A3D52), sweepBeam = Color(0x9900C8D8),
    sweepTail = Color(0x2400C8D8), tagBackground = Color(0xE60B1218),
    tagBorder = Color(0x6600C8D8), reticle = Color(0xFF00C8D8),
    homeMarker = Color(0xFFC8D0D8), textPrimary = Color(0xFFF0F4F8),
    textSecondary = Color(0xFF9AAAB8), highlight = Color(0xFF00C8D8),
    cardBackground = Color(0xFF141E28), amber = Color(0xFFFFB547),
    green = Color(0xFF58D68D), groundTraffic = Color(0xFFB1BBC4), unknownTraffic = Color(0xFF71818F),
    lowAltitude = Color(0xFF4DCC9A), midAltitude = Color(0xFF46B8CC),
    highAltitude = Color(0xFFE2B95C), cruiseAltitude = Color(0xFFB594F6)
)

val LightAtcPalette = AtcPalette(
    radarBackground = Color(0xFFF3F6F5), infoBackground = Color(0xFFE8EEEC),
    ring = Color(0xFFB8C9C4), minorRing = Color(0xFFDCE5E1),
    crosshair = Color(0xFF9EB5AE), spokes = Color(0x33758F87),
    scopeBorder = Color(0xFF75968B), sweepBeam = Color(0x99418C77),
    sweepTail = Color(0x24418C77), tagBackground = Color(0xF7FFFFFF),
    tagBorder = Color(0x88418C77), reticle = Color(0xFF167A68),
    homeMarker = Color(0xFF314842), textPrimary = Color(0xFF172622),
    textSecondary = Color(0xFF52665F), highlight = Color(0xFF167A68),
    cardBackground = Color(0xFFFAFCFB), amber = Color(0xFF946000),
    green = Color(0xFF267A4C), groundTraffic = Color(0xFF66766F), unknownTraffic = Color(0xFF899892),
    lowAltitude = Color(0xFF267A58), midAltitude = Color(0xFF18758A),
    highAltitude = Color(0xFF87620E), cruiseAltitude = Color(0xFF6745A0)
)

val LocalAtcPalette = staticCompositionLocalOf { DarkAtcPalette }

val AtcColors: AtcPalette
    @Composable get() = LocalAtcPalette.current

fun AtcPalette.aircraftAltitudeColor(altitudeFt: Int, altitudeKnown: Boolean = true): Color = when {
    !altitudeKnown -> unknownTraffic
    altitudeFt <= 0 -> groundTraffic
    altitudeFt < 10000 -> lowAltitude
    altitudeFt < 20000 -> midAltitude
    altitudeFt < 30000 -> highAltitude
    else -> cruiseAltitude
}

val RadarBackground = Color(0xFF080E14)
val InfoBackground = Color(0xFF0F1720)
val RadarRingColor = Color(0xFF1E2E3E)
val RadarRingMinorColor = Color(0xFF14202C)
val RadarCrossColor = Color(0xFF243648)
val RadarRadialSpokeColor = Color(0x1F4A6A8A)
val RadarScopeBorder = Color(0xFF2A3D52)
val RadarSweepBeam = Color(0x9900E5FF)
val RadarSweepTail = Color(0x2400E5FF)
val RadarTagBg = Color(0xDD0B1218)
val RadarTagBorder = Color(0x6600E5FF)
val RadarReticle = Color(0xFF00E5FF)
val HomeMarkerColor = Color(0xFFC8D0D8)

val TextPrimary = Color(0xFFF0F4F8)
val TextSecondary = Color(0xFF7A8A9A)
val TextHighlight = Color(0xFF00E5FF)
val CardBackground = Color(0xFF141E28)

val AccentAmber = Color(0xFFFFB300)
val AccentGreen = Color(0xFF00E676)
