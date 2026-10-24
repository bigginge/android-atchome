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
    radarBackground = Color(0xFF0B1118), infoBackground = Color(0xFF111A24),
    ring = Color(0xFF263748), minorRing = Color(0xFF192633),
    crosshair = Color(0xFF304457), spokes = Color(0x224F6D87),
    scopeBorder = Color(0xFF344B60), sweepBeam = Color(0x9945C4BE),
    sweepTail = Color(0x2445C4BE), tagBackground = Color(0xF20E1720),
    tagBorder = Color(0x7745C4BE), reticle = Color(0xFF45C4BE),
    homeMarker = Color(0xFFD0D9E2), textPrimary = Color(0xFFF1F5F8),
    textSecondary = Color(0xFF9BAAB8), highlight = Color(0xFF55C7C0),
    cardBackground = Color(0xFF192431), amber = Color(0xFFE9B45E),
    green = Color(0xFF68C995), groundTraffic = Color(0xFFB9C4CE), unknownTraffic = Color(0xFF8090A0),
    lowAltitude = Color(0xFF58C89A), midAltitude = Color(0xFF50B8C7),
    highAltitude = Color(0xFFE4BA6C), cruiseAltitude = Color(0xFFB69BEA)
)

val LightAtcPalette = AtcPalette(
    radarBackground = Color(0xFFF2F5F8), infoBackground = Color(0xFFE7EDF2),
    ring = Color(0xFFB7C5D1), minorRing = Color(0xFFDCE4EA),
    crosshair = Color(0xFF91A5B5), spokes = Color(0x334F687D),
    scopeBorder = Color(0xFF71899C), sweepBeam = Color(0x99318A87),
    sweepTail = Color(0x24318A87), tagBackground = Color(0xF8FBFCFD),
    tagBorder = Color(0x99577386), reticle = Color(0xFF117D78),
    homeMarker = Color(0xFF354A59), textPrimary = Color(0xFF172633),
    textSecondary = Color(0xFF526575), highlight = Color(0xFF087F83),
    cardBackground = Color(0xFFFBFCFD), amber = Color(0xFF865900),
    green = Color(0xFF21744B), groundTraffic = Color(0xFF657582), unknownTraffic = Color(0xFF7B8B98),
    lowAltitude = Color(0xFF287C5C), midAltitude = Color(0xFF1F7183),
    highAltitude = Color(0xFF805D11), cruiseAltitude = Color(0xFF674A9D)
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
