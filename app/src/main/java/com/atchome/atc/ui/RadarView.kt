package com.atchome.atc.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.atchome.atc.model.Aircraft
import com.atchome.atc.model.AircraftHighlightFilters
import com.atchome.atc.model.Airport
import com.atchome.atc.model.RadarAircraft
import com.atchome.atc.model.AircraftClass
import com.atchome.atc.model.aircraftClass
import com.atchome.atc.model.formatSpeedFromKnots
import com.atchome.atc.model.shortLabel
import com.atchome.atc.ui.theme.AtcColors
import com.atchome.atc.ui.theme.AtcPalette
import com.atchome.atc.ui.theme.aircraftAltitudeColor
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sqrt
import kotlin.math.sin

private val AIRPORT_SUFFIX = Regex("\\s+(?:international\\s+)?airport$", RegexOption.IGNORE_CASE)

@Composable
fun RadarView(
    radarAircraft: StateFlow<List<RadarAircraft>>,
    selectedAircraft: Aircraft?,
    airports: List<Airport> = emptyList(),
    rangeNm: Float,
    showTrails: Boolean,
    showRangeRings: Boolean,
    showAirports: Boolean = true,
    showSweep: Boolean = true,
    reduceMotion: Boolean = false,
    dataTagMode: String = "COMPACT",
    speedUnit: String = "KTS",
    highlightFilters: AircraftHighlightFilters = AircraftHighlightFilters(),
    modifier: Modifier = Modifier,
    onSelectAt: (eastNm: Float, northNm: Float, hitRadiusNm: Float) -> Unit,
    onSelectByHex: (String) -> Unit = {},
    onZoomIn: () -> Unit = {},
    onZoomOut: () -> Unit = {}
) {
    val aircraftList by radarAircraft.collectAsState()
    val selectedRadarAircraft = aircraftList.firstOrNull { it.hex == selectedAircraft?.hex }
    val palette = AtcColors
    val orderedAirports = remember(airports) {
        airports.sortedBy { when (it.type) { "large" -> 0; "medium" -> 1; else -> 2 } }
    }
    val textMeasurer = rememberTextMeasurer()

    val sweepAngle = if (showSweep && !reduceMotion) {
        val transition = rememberInfiniteTransition(label = "RadarSweepTransition")
        val angle by transition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(animation = tween(durationMillis = 6000, easing = LinearEasing), repeatMode = RepeatMode.Restart),
            label = "RadarSweepAngle"
        )
        angle
    } else 0f

    val reticlePulse = if (!reduceMotion && selectedAircraft != null) {
        val transition = rememberInfiniteTransition(label = "SelectionReticleTransition")
        val pulse by transition.animateFloat(
            initialValue = 0.97f,
            targetValue = 1.03f,
            animationSpec = infiniteRepeatable(animation = tween(durationMillis = 1600, easing = LinearEasing), repeatMode = RepeatMode.Reverse),
            label = "ReticlePulse"
        )
        pulse
    } else 1f

    // Real-time UTC Zulu clock
    var utcClockText by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        val sdf = SimpleDateFormat("HH:mm:ss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        while (isActive) {
            utcClockText = sdf.format(Date())
            delay(1000L)
        }
    }

    BoxWithConstraints(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(rangeNm, highlightFilters) {
                    detectTapGestures { tapOffset ->
                        val cx = size.width / 2f
                        val cy = size.height / 2f
                        val pxPerNm = (cx - 24f) / rangeNm.coerceAtLeast(1f)

                        val padding = 12.dp.toPx()
                        val zoomBtnSize = 44.dp.toPx()
                        val zoomGap = 8.dp.toPx()
                        // Center the zoom stack on the same right-side axis as the
                        // 48 dp filter and settings buttons overlaid by AtcScreen.
                        val zoomX = size.width - 8.dp.toPx() - 24.dp.toPx() - zoomBtnSize / 2f
                        val zoomInY = padding
                        val zoomOutY = zoomInY + zoomBtnSize + zoomGap
                        val zoomHitSlop = 4.dp.toPx()

                        // Vertically stacked zoom controls at the radar's upper-right.
                        val zoomInArea = (tapOffset.x in (zoomX - zoomHitSlop)..(zoomX + zoomBtnSize + zoomHitSlop)) &&
                                (tapOffset.y in (zoomInY - zoomHitSlop)..(zoomInY + zoomBtnSize + zoomHitSlop))
                        if (zoomInArea) {
                            onZoomIn()
                            return@detectTapGestures
                        }

                        val zoomOutArea = (tapOffset.x in (zoomX - zoomHitSlop)..(zoomX + zoomBtnSize + zoomHitSlop)) &&
                                (tapOffset.y in (zoomOutY - zoomHitSlop)..(zoomOutY + zoomBtnSize + zoomHitSlop))
                        if (zoomOutArea) {
                            onZoomOut()
                            return@detectTapGestures
                        }

                        // Otherwise: aircraft radar selection hit test
                        val eastNm = (tapOffset.x - cx) / pxPerNm
                        val northNm = (cy - tapOffset.y) / pxPerNm
                        val visualHitRadiusNm = 18.dp.toPx() / pxPerNm
                        val visualHitRadiusSq = visualHitRadiusNm * visualHitRadiusNm

                        // Matches are drawn above every non-match. Resolve taps in
                        // reverse draw order so tapping an overlapping match selects it.
                        val topmostMatch = if (highlightFilters.isActive) {
                            aircraftList.asReversed().firstOrNull { ac ->
                                val eastDelta = ac.eastNm - eastNm
                                val northDelta = ac.northNm - northNm
                                highlightFilters.matches(ac) &&
                                    eastDelta * eastDelta + northDelta * northDelta <= visualHitRadiusSq
                            }
                        } else null
                        if (topmostMatch != null) {
                            onSelectByHex(topmostMatch.hex)
                            return@detectTapGestures
                        }

                        val selectedHit = selectedRadarAircraft?.let { ac ->
                            val eastDelta = ac.eastNm - eastNm
                            val northDelta = ac.northNm - northNm
                            ac.takeIf { eastDelta * eastDelta + northDelta * northDelta <= visualHitRadiusSq }
                        }
                        if (selectedHit != null) {
                            onSelectByHex(selectedHit.hex)
                            return@detectTapGestures
                        }

                        val hitRadiusNm = (36f / pxPerNm).coerceAtLeast(2.5f)
                        onSelectAt(eastNm, northNm, hitRadiusNm)
                    }
                }
        ) {
            val sizePx = min(size.width, size.height)
            val cx = size.width / 2f
            val cy = size.height / 2f
            val radiusPx = cx - 24f
            val viewportRadiusPx = sqrt(
                maxOf(cx, size.width - cx) * maxOf(cx, size.width - cx) +
                    maxOf(cy, size.height - cy) * maxOf(cy, size.height - cy)
            )
            val pxPerNm = radiusPx / rangeNm.coerceAtLeast(1f)

            // 1. Radar Tube Background & Radial Vignette
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        palette.cardBackground,
                        palette.radarBackground,
                        palette.infoBackground
                    ),
                    center = Offset(cx, cy),
                    radius = viewportRadiusPx
                )
            )

            // 2. Tactical Compass Rose & Grid Lines
            drawRadarGrid(
                cx = cx,
                cy = cy,
                radiusPx = radiusPx,
                viewportRadiusPx = viewportRadiusPx,
                rangeNm = rangeNm,
                pxPerNm = pxPerNm,
                showRangeRings = showRangeRings,
                textMeasurer = textMeasurer,
                palette = palette
            )

            // 3. Rotating Radar Sweep Beam with Phosphor Decay
            if (showSweep) {
                drawRadarSweep(
                    cx = cx,
                    cy = cy,
                    radiusPx = viewportRadiusPx,
                    sweepAngle = sweepAngle,
                    palette = palette
                )
            }

            // 4. Historical Breadcrumb Trails
            if (showTrails) {
                aircraftList.forEach { ac ->
                    drawAircraftBreadcrumbs(ac, cx, cy, pxPerNm, ac.hex == selectedAircraft?.hex, palette)
                }
            }

            // 5. Airport ATC tower markers (under aircraft so traffic stays on top)
            val occupiedLabels = mutableListOf<Rect>()
            val symbolClearance = 10.dp.toPx()
            val trafficSymbols = aircraftList.map { aircraft ->
                val symbolX = cx + aircraft.eastNm * pxPerNm
                val symbolY = cy - aircraft.northNm * pxPerNm
                Rect(
                    symbolX - symbolClearance,
                    symbolY - symbolClearance,
                    symbolX + symbolClearance,
                    symbolY + symbolClearance
                )
            }
            if (showAirports) {
                orderedAirports.forEach { airport ->
                    drawAirportTower(
                        airport = airport,
                        cx = cx,
                        cy = cy,
                        pxPerNm = pxPerNm,
                        textMeasurer = textMeasurer,
                        palette = palette,
                        rangeNm = rangeNm,
                        occupiedLabels = occupiedLabels,
                        trafficSymbols = trafficSymbols
                    )
                }
            }
            // Keep airport identifiers visible, then use the traffic-symbol bounds
            // to keep aircraft data tags from covering other targets.
            occupiedLabels += trafficSymbols

            val matchingAircraft = if (highlightFilters.isActive) {
                aircraftList.filter(highlightFilters::matches)
            } else {
                emptyList()
            }
            val matchingHexes = matchingAircraft.mapTo(HashSet()) { it.hex }

            fun drawTarget(ac: RadarAircraft, isSelected: Boolean, isMatched: Boolean) {
                drawAircraftBlip(
                    ac = ac,
                    cx = cx,
                    cy = cy,
                    pxPerNm = pxPerNm,
                    isSelected = isSelected,
                    isHighlighted = isMatched,
                    isDimmed = false,
                    showSweep = showSweep,
                    sweepAngle = sweepAngle,
                    dataTagMode = dataTagMode,
                    speedUnit = speedUnit,
                    reticlePulse = if (isSelected) reticlePulse else 1f,
                    textMeasurer = textMeasurer,
                    palette = palette,
                    occupiedLabels = occupiedLabels,
                    scopeSize = sizePx
                )
            }

            // Base traffic is painted first, then a selected non-match, then all
            // matches (including a selected match) so filters always remain on top.
            aircraftList
                .filter { it.hex != selectedAircraft?.hex && it.hex !in matchingHexes }
                .forEach { drawTarget(it, isSelected = false, isMatched = false) }

            selectedRadarAircraft?.takeIf { it.hex !in matchingHexes }?.let {
                drawTarget(it, isSelected = true, isMatched = false)
            }

            matchingAircraft.forEach { ac ->
                drawTarget(
                    ac,
                    isSelected = ac.hex == selectedAircraft?.hex,
                    isMatched = true
                )
            }

            // 8. On-Canvas HUD Elements (Zoom Buttons, Counters, Zulu Clock)
            drawRadarHud(
                sizePx = sizePx,
                targetCount = aircraftList.size,
                matchedCount = if (highlightFilters.isActive) aircraftList.count(highlightFilters::matches) else aircraftList.size,
                highlightFilters = highlightFilters,
                utcClock = utcClockText,
                textMeasurer = textMeasurer,
                palette = palette
            )
        }
    }
}

/**
 * Draw Compass Rose, Bearing Radials, Concentric Range Rings, and Airport Center Beacon.
 */
private fun DrawScope.drawRadarGrid(
    cx: Float,
    cy: Float,
    radiusPx: Float,
    viewportRadiusPx: Float,
    rangeNm: Float,
    pxPerNm: Float,
    showRangeRings: Boolean,
    textMeasurer: TextMeasurer,
    palette: AtcPalette
) {
    // 12 Bearing Radials every 30°
    for (deg in 0 until 360 step 30) {
        val rad = Math.toRadians((deg - 90).toDouble())
        val xEnd = (cx + cos(rad) * viewportRadiusPx).toFloat()
        val yEnd = (cy + sin(rad) * viewportRadiusPx).toFloat()
        drawLine(
            color = palette.spokes,
            start = Offset(cx, cy),
            end = Offset(xEnd, yEnd),
            strokeWidth = 1.dp.toPx()
        )
    }

    // Orthogonal Crosshairs
    drawLine(
        color = palette.crosshair,
        start = Offset(0f, cy),
        end = Offset(size.width, cy),
        strokeWidth = 1.2.dp.toPx()
    )
    drawLine(
        color = palette.crosshair,
        start = Offset(cx, 0f),
        end = Offset(cx, size.height),
        strokeWidth = 1.2.dp.toPx()
    )

    // Concentric Range Rings. Continue the same interval beyond the configured
    // outer ring so partial rings remain visible where the viewport extends
    // past that circle; Canvas clips them at the viewport edges.
    if (showRangeRings) {
        val ringStepNm = rangeNm / 4f
        val maxRing = ((viewportRadiusPx / pxPerNm) / ringStepNm).toInt()
        for (i in 1..maxRing) {
            val ringNm = ringStepNm * i
            val rPx = ringNm * pxPerNm

            drawCircle(
                color = palette.ring,
                radius = rPx,
                center = Offset(cx, cy),
                style = Stroke(width = 1.dp.toPx())
            )

            // Distance label badge
            val labelText = "${ringNm.toInt()} NM"
            val textLayout = textMeasurer.measure(
                text = labelText,
                style = TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.textSecondary.copy(alpha = 0.85f)
                )
            )

            val badgeW = textLayout.size.width + 6f
            val badgeH = textLayout.size.height + 4f
            val labelX = if (rPx <= cy) {
                cx + 6f
            } else {
                val xOffset = sqrt((rPx * rPx - cy * cy).coerceAtLeast(0f))
                val rightIntersection = cx + xOffset
                val leftIntersection = cx - xOffset
                if (rightIntersection + badgeW <= size.width) rightIntersection
                else (leftIntersection - badgeW).coerceAtLeast(0f)
            }
            val badgeX = labelX.coerceIn(3f, (size.width - badgeW + 3f).coerceAtLeast(3f))
            val labelY = if (rPx <= cy) cy - rPx - 6f else 2f
            val badgeY = labelY.coerceIn(2f, (size.height - textLayout.size.height - 2f).coerceAtLeast(2f))
            drawRoundRect(
                color = palette.tagBackground,
                topLeft = Offset(badgeX - 3f, badgeY - 2f),
                size = Size(badgeW, badgeH),
                cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
            )
            drawText(
                textLayoutResult = textLayout,
                topLeft = Offset(badgeX, badgeY)
            )
        }
    }

    // Outer Compass Rose Azimuth Ticks & Cardinal Labels
    for (deg in 0 until 360 step 10) {
        val isMajor = deg % 30 == 0
        val isCardinal = deg % 90 == 0
        val tickLen = if (isCardinal) 12.dp.toPx() else if (isMajor) 8.dp.toPx() else 4.dp.toPx()
        val rad = Math.toRadians((deg - 90).toDouble())

        val xOuter = (cx + cos(rad) * radiusPx).toFloat()
        val yOuter = (cy + sin(rad) * radiusPx).toFloat()
        val xInner = (cx + cos(rad) * (radiusPx - tickLen)).toFloat()
        val yInner = (cy + sin(rad) * (radiusPx - tickLen)).toFloat()

        drawLine(
            color = if (isCardinal) palette.highlight else if (isMajor) palette.scopeBorder else palette.minorRing,
            start = Offset(xInner, yInner),
            end = Offset(xOuter, yOuter),
            strokeWidth = if (isCardinal) 2.dp.toPx() else 1.dp.toPx()
        )

        // Degree numbers on major 30-degree ticks
        if (isMajor && !isCardinal) {
            val label = String.format(Locale.US, "%03d°", deg)
            val textLayout = textMeasurer.measure(
                text = label,
                style = TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Normal,
                    color = palette.textSecondary.copy(alpha = 0.65f)
                )
            )
            val labelR = radiusPx - tickLen - 10.dp.toPx()
            val lx = (cx + cos(rad) * labelR).toFloat() - textLayout.size.width / 2f
            val ly = (cy + sin(rad) * labelR).toFloat() - textLayout.size.height / 2f
            drawText(textLayoutResult = textLayout, topLeft = Offset(lx, ly))
        }
    }

    // Prominent Cardinal Letters (N, E, S, W)
    val cardinals = listOf(
        Triple("N", 0, palette.highlight),
        Triple("E", 90, palette.textSecondary),
        Triple("S", 180, palette.textSecondary),
        Triple("W", 270, palette.textSecondary)
    )
    for ((cardinal, deg, color) in cardinals) {
        val rad = Math.toRadians((deg - 90).toDouble())
        val cardR = radiusPx - 20.dp.toPx()
        val textLayout = textMeasurer.measure(
            text = cardinal,
            style = TextStyle(
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
        )
        val lx = (cx + cos(rad) * cardR).toFloat() - textLayout.size.width / 2f
        val ly = (cy + sin(rad) * cardR).toFloat() - textLayout.size.height / 2f
        drawText(textLayoutResult = textLayout, topLeft = Offset(lx, ly))
    }

    // Home Airport Runway Crosshair & Beacon Marker
    val rLen = 8.dp.toPx()
    drawLine(
        color = palette.homeMarker,
        start = Offset(cx - rLen, cy),
        end = Offset(cx + rLen, cy),
        strokeWidth = 2.dp.toPx(),
        cap = StrokeCap.Round
    )
    drawLine(
        color = palette.homeMarker,
        start = Offset(cx, cy - rLen),
        end = Offset(cx, cy + rLen),
        strokeWidth = 2.dp.toPx(),
        cap = StrokeCap.Round
    )
    drawCircle(
        color = palette.highlight,
        radius = 2.5.dp.toPx(),
        center = Offset(cx, cy)
    )
}

/**
 * Continuous rotating radar sweep beam with trailing phosphor gradient fan.
 */
private fun DrawScope.drawRadarSweep(
    cx: Float,
    cy: Float,
    radiusPx: Float,
    sweepAngle: Float,
    palette: AtcPalette
) {
    rotate(degrees = sweepAngle, pivot = Offset(cx, cy)) {
        // Trailing phosphor decay fan (fades across ~55 degrees)
        val fanPath = Path().apply {
            moveTo(cx, cy)
            val fanAngleRad = Math.toRadians(55.0)
            val segments = 12
            for (i in segments downTo 0) {
                val a = -fanAngleRad * (i.toDouble() / segments.toDouble())
                val px = (cx + cos(a - PI / 2.0) * radiusPx).toFloat()
                val py = (cy + sin(a - PI / 2.0) * radiusPx).toFloat()
                lineTo(px, py)
            }
            close()
        }
        drawPath(
            path = fanPath,
            brush = Brush.radialGradient(
                colors = listOf(palette.sweepTail, Color.Transparent),
                center = Offset(cx, cy),
                radius = radiusPx
            )
        )

        // Sharp leading sweep line
        drawLine(
            color = palette.sweepBeam,
            start = Offset(cx, cy),
            end = Offset(cx, cy - radiusPx),
            strokeWidth = 1.8.dp.toPx(),
            cap = StrokeCap.Round
        )
    }
}

/**
 * Fading phosphor breadcrumbs representing historical flight trajectory.
 */
private fun DrawScope.drawAircraftBreadcrumbs(
    ac: RadarAircraft,
    cx: Float,
    cy: Float,
    pxPerNm: Float,
    isSelected: Boolean,
    palette: AtcPalette
) {
    val count = ac.trail.size
    if (count < 2) return

    val baseColor = palette.aircraftAltitudeColor(ac.altitudeFt, ac.altitudeKnown)

    for (i in 0 until count) {
        val p = ac.trail[i]
        val px = cx + p.eastNm * pxPerNm
        val py = cy - p.northNm * pxPerNm

        val ageFactor = (i + 1).toFloat() / count.toFloat()
        val alpha = (ageFactor * ageFactor).coerceIn(0.12f, 0.9f)
        val dotRadius = if (isSelected) (1.2f + ageFactor * 2.2f).dp.toPx() else (0.8f + ageFactor * 1.8f).dp.toPx()

        drawCircle(
            color = baseColor.copy(alpha = alpha),
            radius = dotRadius,
            center = Offset(px, py)
        )
    }
}

/** Draw a compact tower beacon and airport identifier at its radar position. */
private fun DrawScope.drawAirportTower(
    airport: Airport,
    cx: Float,
    cy: Float,
    pxPerNm: Float,
    textMeasurer: TextMeasurer,
    palette: AtcPalette,
    rangeNm: Float,
    occupiedLabels: MutableList<Rect>,
    trafficSymbols: List<Rect>
) {
    val x = cx + airport.eastNm * pxPerNm
    val y = cy - airport.northNm * pxPerNm
    val isLarge = airport.type == "large"
    val isMedium = airport.type == "medium"
    val markerRadius = when (airport.type) { "large" -> 5.dp.toPx(); "medium" -> 3.5.dp.toPx(); else -> 2.5.dp.toPx() }
    val color = if (isLarge) palette.amber.copy(alpha = 0.82f) else palette.textSecondary.copy(alpha = 0.74f)

    if (isLarge) {
        drawCircle(color, markerRadius + 2.dp.toPx(), Offset(x, y), style = Stroke(width = 1.2.dp.toPx()))
        drawLine(color, Offset(x - 7.dp.toPx(), y), Offset(x + 7.dp.toPx(), y), 1.4.dp.toPx(), cap = StrokeCap.Round)
        drawLine(color, Offset(x, y - 6.dp.toPx()), Offset(x, y + 6.dp.toPx()), 1.dp.toPx(), cap = StrokeCap.Round)
    } else if (isMedium) {
        drawCircle(color, markerRadius, Offset(x, y), style = Stroke(width = 1.2.dp.toPx()))
        drawLine(color, Offset(x - 4.dp.toPx(), y), Offset(x + 4.dp.toPx(), y), 1.dp.toPx(), cap = StrokeCap.Round)
    } else {
        val diamond = Path().apply {
            moveTo(x, y - markerRadius)
            lineTo(x + markerRadius, y)
            lineTo(x, y + markerRadius)
            lineTo(x - markerRadius, y)
            close()
        }
        drawPath(diamond, color)
    }

    val showLabel = isLarge || (isMedium && rangeNm <= 25f) || rangeNm <= 10f
    if (!showLabel) return
    // Names are easier to recognize at overview ranges than ICAO codes. Strip the
    // generic suffix to keep labels compact (e.g. “London Heathrow”).
    val sourceLabel = if (isLarge || isMedium) {
        airport.name.replace(AIRPORT_SUFFIX, "").ifBlank { airport.ident }
    } else {
        airport.ident
    }
    val label = if (sourceLabel.length > 18) sourceLabel.take(17) + "…" else sourceLabel
    val layout = textMeasurer.measure(
        text = label,
        style = TextStyle(
            fontFamily = FontFamily.SansSerif,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
    )
    val labelGap = markerRadius + 5.dp.toPx()
    val horizontalPadding = 3.dp.toPx()
    val verticalPadding = 2.dp.toPx()
    val positions = listOf(
        Offset(x + labelGap, y - layout.size.height / 2f),
        Offset(x - labelGap - layout.size.width, y - layout.size.height / 2f),
        Offset(x - layout.size.width / 2f, y - labelGap - layout.size.height),
        Offset(x - layout.size.width / 2f, y + labelGap)
    )
    val candidates = positions.map { textTopLeft ->
        val rect = Rect(
            textTopLeft.x - horizontalPadding,
            textTopLeft.y - verticalPadding,
            textTopLeft.x + layout.size.width + horizontalPadding,
            textTopLeft.y + layout.size.height + verticalPadding
        )
        textTopLeft to rect
    }.filter { (_, rect) ->
        rect.left >= 0 && rect.right <= size.width && rect.top >= 0 && rect.bottom <= size.height
    }
    if (candidates.isEmpty()) return

    // Prefer a free spot; if every side is busy, place the label where it covers
    // the fewest aircraft symbols instead of hiding the airport altogether.
    val placement = candidates.minByOrNull { (_, rect) ->
        occupiedLabels.count { it.overlaps(rect) } * 1000 + trafficSymbols.count { it.overlaps(rect) }
    } ?: return
    val (textTopLeft, rect) = placement
    occupiedLabels += rect
    drawRoundRect(palette.tagBackground.copy(alpha = palette.tagBackground.alpha * 0.82f), rect.topLeft, Size(rect.width, rect.height), CornerRadius(3.dp.toPx(), 3.dp.toPx()))
    drawText(layout, topLeft = textTopLeft)
}

/**
 * Aircraft blip chevron, tactical selection brackets, and ATC data tags.
 */
private fun DrawScope.drawAircraftBlip(
    ac: RadarAircraft,
    cx: Float,
    cy: Float,
    pxPerNm: Float,
    isSelected: Boolean,
    isHighlighted: Boolean = false,
    isDimmed: Boolean = false,
    showSweep: Boolean,
    sweepAngle: Float,
    dataTagMode: String,
    speedUnit: String,
    reticlePulse: Float = 1.0f,
    textMeasurer: TextMeasurer,
    palette: AtcPalette,
    occupiedLabels: MutableList<Rect>,
    scopeSize: Float
) {
    val x = cx + ac.eastNm * pxPerNm
    val y = cy - ac.northNm * pxPerNm
    val center = Offset(x, y)
    val dimAlpha = if (isDimmed) 0.3f else 1f

    if (isHighlighted) {
        drawCircle(
            color = palette.highlight.copy(alpha = 0.95f),
            radius = 13.dp.toPx(),
            center = center,
            style = Stroke(width = 1.8.dp.toPx())
        )
    }

    // Phosphor Excitation Calculation: brighten when swept
    var blipColor = palette.aircraftAltitudeColor(ac.altitudeFt, ac.altitudeKnown).copy(alpha = if (isDimmed) 0.24f else 1f)
    if (showSweep) {
        val dx = x - cx
        val dy = y - cy
        var bearingDeg = Math.toDegrees(atan2(dx.toDouble(), -dy.toDouble())).toFloat()
        if (bearingDeg < 0) bearingDeg += 360f

        var diff = sweepAngle - bearingDeg
        if (diff < 0) diff += 360f
        if (diff < 35f) {
            // Sweep beam recently excited this blip
            val excitation = 1.0f - (diff / 35f)
            blipColor = blipColor.copy(
                red = (blipColor.red + (1f - blipColor.red) * excitation * 0.7f).coerceIn(0f, 1f),
                green = (blipColor.green + (1f - blipColor.green) * excitation * 0.7f).coerceIn(0f, 1f),
                blue = (blipColor.blue + (1f - blipColor.blue) * excitation * 0.7f).coerceIn(0f, 1f)
            )
        }
    }

    // 1. Tactical Selection Reticle: 4 Corner Brackets
    if (isSelected) {
        val bracketSize = 14.dp.toPx() * reticlePulse
        val armLen = 5.dp.toPx()
        val reticleColor = palette.reticle

        // Top-Left [
        drawLine(reticleColor, Offset(x - bracketSize, y - bracketSize), Offset(x - bracketSize + armLen, y - bracketSize), 1.8.dp.toPx())
        drawLine(reticleColor, Offset(x - bracketSize, y - bracketSize), Offset(x - bracketSize, y - bracketSize + armLen), 1.8.dp.toPx())

        // Top-Right ]
        drawLine(reticleColor, Offset(x + bracketSize, y - bracketSize), Offset(x + bracketSize - armLen, y - bracketSize), 1.8.dp.toPx())
        drawLine(reticleColor, Offset(x + bracketSize, y - bracketSize), Offset(x + bracketSize, y - bracketSize + armLen), 1.8.dp.toPx())

        // Bottom-Left [
        drawLine(reticleColor, Offset(x - bracketSize, y + bracketSize), Offset(x - bracketSize + armLen, y + bracketSize), 1.8.dp.toPx())
        drawLine(reticleColor, Offset(x - bracketSize, y + bracketSize), Offset(x - bracketSize, y + bracketSize - armLen), 1.8.dp.toPx())

        // Bottom-Right ]
        drawLine(reticleColor, Offset(x + bracketSize, y + bracketSize), Offset(x + bracketSize - armLen, y + bracketSize), 1.8.dp.toPx())
        drawLine(reticleColor, Offset(x + bracketSize, y + bracketSize), Offset(x + bracketSize, y + bracketSize - armLen), 1.8.dp.toPx())
    }

    // 2. Heading-aware class symbols use shape as well as altitude color.
    val aircraftClass = ac.aircraftClass()
    val len = if (isSelected) 18.dp.toPx() else 14.dp.toPx()
    val half = if (isSelected) 13.dp.toPx() else 10.dp.toPx()
    val symbolColor = if (!ac.seen) palette.amber else if (aircraftClass == AircraftClass.UNKNOWN) palette.textSecondary.copy(alpha = dimAlpha) else blipColor
    rotate(degrees = ac.trackDeg, pivot = center) {
        when (aircraftClass) {
            AircraftClass.HELICOPTER -> {
                // Top-down rotor, cabin, tail boom, and tail rotor.
                val rotor = if (isSelected) 15.dp.toPx() else 12.dp.toPx()
                val rotorY = y - len * 0.22f
                drawLine(symbolColor, Offset(x - rotor, rotorY), Offset(x + rotor, rotorY), 1.8.dp.toPx(), cap = StrokeCap.Round)
                drawLine(symbolColor, Offset(x, rotorY - 2.dp.toPx()), Offset(x, rotorY + 2.dp.toPx()), 1.2.dp.toPx(), cap = StrokeCap.Round)
                drawOval(
                    color = symbolColor,
                    topLeft = Offset(x - 3.dp.toPx(), rotorY - 4.dp.toPx()),
                    size = Size(6.dp.toPx(), 9.dp.toPx())
                )
                drawLine(symbolColor, Offset(x, rotorY + 4.dp.toPx()), Offset(x, y + len * 0.72f), 1.5.dp.toPx(), cap = StrokeCap.Round)
                drawLine(symbolColor, Offset(x - 2.5.dp.toPx(), y + len * 0.72f), Offset(x + 2.5.dp.toPx(), y + len * 0.72f), 1.4.dp.toPx(), cap = StrokeCap.Round)
            }
            AircraftClass.LIGHT -> {
                // High-wing single-engine aircraft with a propeller and tailplane.
                val plane = Path().apply {
                    moveTo(x, y - len)
                    lineTo(x + 1.5.dp.toPx(), y - len * 0.58f)
                    lineTo(x + half, y - len * 0.08f)
                    lineTo(x + half, y + len * 0.08f)
                    lineTo(x + 1.5.dp.toPx(), y - len * 0.02f)
                    lineTo(x + 3.dp.toPx(), y + len * 0.63f)
                    lineTo(x + 4.dp.toPx(), y + len * 0.78f)
                    lineTo(x - 4.dp.toPx(), y + len * 0.78f)
                    lineTo(x - 3.dp.toPx(), y + len * 0.63f)
                    lineTo(x - 1.5.dp.toPx(), y - len * 0.02f)
                    lineTo(x - half, y + len * 0.08f)
                    lineTo(x - half, y - len * 0.08f)
                    lineTo(x - 1.5.dp.toPx(), y - len * 0.58f)
                    close()
                }
                drawPath(plane, symbolColor)
                drawLine(palette.radarBackground, Offset(x - half * 0.83f, y - len * 0.05f), Offset(x + half * 0.83f, y - len * 0.05f), 1.dp.toPx())
                drawLine(symbolColor, Offset(x - 4.dp.toPx(), y - len - 1.dp.toPx()), Offset(x + 4.dp.toPx(), y - len - 1.dp.toPx()), 1.2.dp.toPx(), cap = StrokeCap.Round)
            }
            AircraftClass.MILITARY -> {
                // Narrow fuselage and sharply swept delta wings distinguish a jet.
                val jet = Path().apply {
                    moveTo(x, y - len)
                    lineTo(x + 1.8.dp.toPx(), y - len * 0.38f)
                    lineTo(x + half, y + len * 0.27f)
                    lineTo(x + half * 0.83f, y + len * 0.48f)
                    lineTo(x + 2.dp.toPx(), y + len * 0.2f)
                    lineTo(x + 3.dp.toPx(), y + len)
                    lineTo(x - 3.dp.toPx(), y + len)
                    lineTo(x - 2.dp.toPx(), y + len * 0.2f)
                    lineTo(x - half * 0.83f, y + len * 0.48f)
                    lineTo(x - half, y + len * 0.27f)
                    lineTo(x - 1.8.dp.toPx(), y - len * 0.38f)
                    close()
                }
                drawPath(jet, symbolColor)
                drawLine(palette.radarBackground, Offset(x - 1.dp.toPx(), y - len * 0.48f), Offset(x - 1.dp.toPx(), y + len * 0.78f), 1.dp.toPx())
            }
            AircraftClass.AIRLINER -> {
                // Long fuselage, swept wings, and a separate horizontal tailplane.
                val plane = Path().apply {
                    moveTo(x, y - len)
                    lineTo(x + 1.8.dp.toPx(), y - len * 0.72f)
                    lineTo(x + 2.dp.toPx(), y + len * 0.65f)
                    lineTo(x + 4.dp.toPx(), y + len * 0.83f)
                    lineTo(x - 4.dp.toPx(), y + len * 0.83f)
                    lineTo(x - 2.dp.toPx(), y + len * 0.65f)
                    lineTo(x - 1.8.dp.toPx(), y - len * 0.72f)
                    close()
                }
                val wings = Path().apply {
                    moveTo(x - 1.4.dp.toPx(), y - len * 0.13f)
                    lineTo(x - half, y + len * 0.34f)
                    lineTo(x - half * 0.92f, y + len * 0.45f)
                    lineTo(x - 1.4.dp.toPx(), y + len * 0.17f)
                    lineTo(x + 1.4.dp.toPx(), y + len * 0.17f)
                    lineTo(x + half * 0.92f, y + len * 0.45f)
                    lineTo(x + half, y + len * 0.34f)
                    lineTo(x + 1.4.dp.toPx(), y - len * 0.13f)
                    close()
                }
                val tailplane = Path().apply {
                    moveTo(x - 1.5.dp.toPx(), y + len * 0.57f)
                    lineTo(x - half * 0.43f, y + len * 0.78f)
                    lineTo(x - half * 0.37f, y + len * 0.87f)
                    lineTo(x - 1.dp.toPx(), y + len * 0.78f)
                    lineTo(x + 1.dp.toPx(), y + len * 0.78f)
                    lineTo(x + half * 0.37f, y + len * 0.87f)
                    lineTo(x + half * 0.43f, y + len * 0.78f)
                    lineTo(x + 1.5.dp.toPx(), y + len * 0.57f)
                    close()
                }
                drawPath(plane, symbolColor)
                drawPath(wings, symbolColor)
                drawPath(tailplane, symbolColor)
            }
            AircraftClass.UNKNOWN -> {
                drawCircle(symbolColor, if (isSelected) 4.dp.toPx() else 3.dp.toPx(), center, style = Stroke(width = 1.7.dp.toPx()))
                drawLine(symbolColor, Offset(x, y), Offset(x, y - len), 1.3.dp.toPx(), cap = StrokeCap.Round)
            }
        }
    }
    if (!ac.altitudeKnown) {
        drawCircle(palette.unknownTraffic.copy(alpha = dimAlpha), 10.dp.toPx(), center, style = Stroke(width = 0.9.dp.toPx()))
    }

    // 3. ATC Data Blocks & Leader Lines (3-Way Mode)
    val shouldDrawFullTag = isSelected || dataTagMode == "FULL"
    val shouldDrawCompactTag = !isSelected && dataTagMode == "COMPACT"

    if (shouldDrawFullTag) {
        val callsign = ac.callsign.ifEmpty { ac.registration.ifEmpty { ac.hex.uppercase() } }
        val speedStr = formatSpeedFromKnots(ac.groundSpeedKts, speedUnit).replace(" ", "")
        val line1 = callsign.take(14)
        val line2 = "${aircraftClass.shortLabel} • ${ac.formattedAltitude} ${ac.trendSymbol} $speedStr"
        val line3 = if (!ac.seen) "LAST SEEN · ${com.atchome.atc.model.formatPositionAge(ac.positionAgeSeconds)}" else null

        val textLayout1 = textMeasurer.measure(
            text = line1,
            style = TextStyle(
                fontFamily = FontFamily.Monospace,
                fontSize = if (isSelected) 11.sp else 9.5.sp,
                fontWeight = FontWeight.Bold,
                color = palette.textPrimary.copy(alpha = dimAlpha)
            )
        )
        val textLayout2 = textMeasurer.measure(
            text = line2,
            style = TextStyle(
                fontFamily = FontFamily.Monospace,
                fontSize = if (isSelected) 10.sp else 8.5.sp,
                fontWeight = FontWeight.Normal,
                color = (if (isSelected) palette.highlight else palette.textSecondary).copy(alpha = dimAlpha)
            )
        )
        val textLayout3 = line3?.let { label ->
            textMeasurer.measure(
                text = label,
                style = TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.amber
                )
            )
        }
        val tagLayouts = listOfNotNull(textLayout1, textLayout2, textLayout3)

        drawAircraftTag(
            center = center,
            width = (tagLayouts.maxOfOrNull { it.size.width } ?: 0).toFloat() + 12f,
            height = tagLayouts.sumOf { it.size.height.toDouble() }.toFloat() + 8f,
            layouts = tagLayouts,
            isSelected = isSelected,
            dimAlpha = dimAlpha,
            palette = palette,
            occupiedLabels = occupiedLabels,
            scopeSize = scopeSize
        )

    } else if (shouldDrawCompactTag) {
        val callsign = ac.callsign.ifEmpty { ac.registration.ifEmpty { ac.hex.uppercase() } }
        val label = "$callsign · ${aircraftClass.shortLabel} · ${ac.formattedAltitude}"
        val textLayout = textMeasurer.measure(
            text = label,
            style = TextStyle(
                fontFamily = FontFamily.Monospace,
                fontSize = 9.sp,
                fontWeight = FontWeight.Medium,
                color = palette.textSecondary.copy(alpha = 0.9f * dimAlpha)
            )
        )
        val tagW = textLayout.size.width + 8f
        val tagH = textLayout.size.height + 4f
        drawAircraftTag(
            center = center,
            width = tagW,
            height = tagH,
            layouts = listOf(textLayout),
            isSelected = false,
            dimAlpha = dimAlpha,
            palette = palette,
            occupiedLabels = occupiedLabels,
            scopeSize = scopeSize
        )
    }
}

private fun DrawScope.drawAircraftTag(
    center: Offset,
    width: Float,
    height: Float,
    layouts: List<androidx.compose.ui.text.TextLayoutResult>,
    isSelected: Boolean,
    dimAlpha: Float,
    palette: AtcPalette,
    occupiedLabels: MutableList<Rect>,
    scopeSize: Float
) {
    val gap = 12.dp.toPx()
    val margin = 8.dp.toPx()
    val candidates = listOf(
        Rect(center.x + gap, center.y - height - gap, center.x + gap + width, center.y - gap),
        Rect(center.x - gap - width, center.y - height - gap, center.x - gap, center.y - gap),
        Rect(center.x + gap, center.y + gap, center.x + gap + width, center.y + gap + height),
        Rect(center.x - gap - width, center.y + gap, center.x - gap, center.y + gap + height),
        Rect(center.x - width / 2f, center.y - height - gap, center.x + width / 2f, center.y - gap)
    )
    fun inBounds(rect: Rect) = rect.left >= margin && rect.top >= margin && rect.right <= scopeSize - margin && rect.bottom <= scopeSize - margin
    val preferred = candidates.firstOrNull { inBounds(it) && occupiedLabels.none(it::overlaps) }
    val fallback = if (isSelected) candidates.firstOrNull(::inBounds) else null
    val bounds = preferred ?: fallback ?: return
    occupiedLabels += bounds

    val border = if (isSelected) palette.highlight else palette.tagBorder
    drawLine(border.copy(alpha = dimAlpha * 0.8f), center, Offset(bounds.center.x, bounds.center.y), if (isSelected) 1.2.dp.toPx() else 0.8.dp.toPx())
    drawRoundRect(
        color = palette.tagBackground.copy(alpha = palette.tagBackground.alpha * dimAlpha),
        topLeft = bounds.topLeft,
        size = Size(bounds.width, bounds.height),
        cornerRadius = CornerRadius(if (isSelected) 6.dp.toPx() else 4.dp.toPx(), if (isSelected) 6.dp.toPx() else 4.dp.toPx())
    )
    drawRoundRect(
        color = border.copy(alpha = dimAlpha),
        topLeft = bounds.topLeft,
        size = Size(bounds.width, bounds.height),
        cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()),
        style = Stroke(width = if (isSelected) 1.1.dp.toPx() else 0.7.dp.toPx())
    )
    var textY = bounds.top + 4.dp.toPx()
    layouts.forEachIndexed { index, layout ->
        drawText(layout, topLeft = Offset(bounds.left + 6.dp.toPx(), textY))
        textY += layout.size.height + if (index == 0 && layouts.size > 1) 1.dp.toPx() else 0f
    }
}

/**
 * On-Canvas HUD Controls:
 * - Top-Left: Active filter summary
 * - Top-Right: Quick Zoom In [+] and Zoom Out [-] buttons
 * - Bottom-Right: Live UTC Zulu Clock
 */
private fun DrawScope.drawRadarHud(
    sizePx: Float,
    targetCount: Int,
    matchedCount: Int,
    highlightFilters: AircraftHighlightFilters,
    utcClock: String,
    textMeasurer: TextMeasurer,
    palette: AtcPalette
) {
    val padding = 12.dp.toPx()

    if (highlightFilters.isActive) {
        val summary = "MATCH $matchedCount/$targetCount • ${highlightFilters.summary().take(28)}${if (highlightFilters.summary().length > 28) "…" else ""}"
        val summaryLayout = textMeasurer.measure(
            summary,
            style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 8.sp, fontWeight = FontWeight.Bold, color = palette.highlight)
        )
        val summaryY = padding
        drawRoundRect(
            color = palette.tagBackground,
            topLeft = Offset(padding, summaryY),
            size = Size(summaryLayout.size.width + 12f, summaryLayout.size.height + 6f),
            cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
        )
        drawText(summaryLayout, topLeft = Offset(padding + 6f, summaryY + 3f))
    }

    // Top-Right Quick Zoom Buttons [+] and [-], stacked vertically and centered
    // on the filter/settings control axis.
    val btnSize = 44.dp.toPx()
    val zoomGap = 8.dp.toPx()
    val controlCenterX = size.width - 8.dp.toPx() - 24.dp.toPx()
    val zoomInX = controlCenterX - btnSize / 2f
    val zoomInY = padding

    // Zoom [+] Button, styled as a circular radar control like filter/settings.
    drawCircle(
        color = palette.infoBackground.copy(alpha = 0.82f),
        radius = btnSize / 2f,
        center = Offset(controlCenterX, zoomInY + btnSize / 2f)
    )
    drawCircle(
        color = palette.tagBorder,
        radius = btnSize / 2f,
        center = Offset(controlCenterX, zoomInY + btnSize / 2f),
        style = Stroke(width = 1.dp.toPx())
    )
    val plusLayout = textMeasurer.measure(
        text = "+",
        style = TextStyle(
            fontFamily = FontFamily.Monospace,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = palette.highlight
        )
    )
    drawText(
        textLayoutResult = plusLayout,
        topLeft = Offset(
            zoomInX + (btnSize - plusLayout.size.width) / 2f,
            zoomInY + (btnSize - plusLayout.size.height) / 2f
        )
    )

    // Zoom [-] Button
    val zoomOutY = zoomInY + btnSize + zoomGap
    drawCircle(
        color = palette.infoBackground.copy(alpha = 0.82f),
        radius = btnSize / 2f,
        center = Offset(controlCenterX, zoomOutY + btnSize / 2f)
    )
    drawCircle(
        color = palette.tagBorder,
        radius = btnSize / 2f,
        center = Offset(controlCenterX, zoomOutY + btnSize / 2f),
        style = Stroke(width = 1.dp.toPx())
    )
    val minusLayout = textMeasurer.measure(
        text = "−",
        style = TextStyle(
            fontFamily = FontFamily.Monospace,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = palette.highlight
        )
    )
    drawText(
        textLayoutResult = minusLayout,
        topLeft = Offset(
            zoomInX + (btnSize - minusLayout.size.width) / 2f,
            zoomOutY + (btnSize - minusLayout.size.height) / 2f
        )
    )

    // 3. Bottom-Right UTC Zulu Clock
    if (utcClock.isNotEmpty()) {
        val clockLayout = textMeasurer.measure(
            text = utcClock,
            style = TextStyle(
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = palette.textPrimary.copy(alpha = 0.85f),
                letterSpacing = 1.sp
            )
        )
        val clockW = clockLayout.size.width + 14f
        val clockH = clockLayout.size.height + 6f
        val clockX = sizePx - padding - clockW
        val clockY = sizePx - padding - clockH

        drawRoundRect(
            color = palette.tagBackground,
            topLeft = Offset(clockX, clockY),
            size = Size(clockW, clockH),
            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
        )
        drawText(textLayoutResult = clockLayout, topLeft = Offset(clockX + 7f, clockY + 3f))
    }
}
