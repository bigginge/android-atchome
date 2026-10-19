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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
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
import com.atchome.atc.ui.theme.CardBackground
import com.atchome.atc.ui.theme.HomeMarkerColor
import com.atchome.atc.ui.theme.RadarBackground
import com.atchome.atc.ui.theme.RadarCrossColor
import com.atchome.atc.ui.theme.RadarRadialSpokeColor
import com.atchome.atc.ui.theme.RadarReticle
import com.atchome.atc.ui.theme.RadarRingColor
import com.atchome.atc.ui.theme.RadarRingMinorColor
import com.atchome.atc.ui.theme.RadarScopeBorder
import com.atchome.atc.ui.theme.RadarSweepBeam
import com.atchome.atc.ui.theme.RadarSweepTail
import com.atchome.atc.ui.theme.RadarTagBg
import com.atchome.atc.ui.theme.RadarTagBorder
import com.atchome.atc.ui.theme.TextHighlight
import com.atchome.atc.ui.theme.TextPrimary
import com.atchome.atc.ui.theme.TextSecondary
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
import kotlin.math.sin

@Composable
fun RadarView(
    aircraftList: List<Aircraft>,
    selectedAircraft: Aircraft?,
    rangeNm: Float,
    showTrails: Boolean,
    showRangeRings: Boolean,
    showSweep: Boolean = true,
    showVelocityVectors: Boolean = true,
    dataTagMode: String = "COMPACT",
    modifier: Modifier = Modifier,
    onSelectAt: (eastNm: Float, northNm: Float, hitRadiusNm: Float) -> Unit,
    onZoomIn: () -> Unit = {},
    onZoomOut: () -> Unit = {},
    onCycleDataTagMode: () -> Unit = {}
) {
    val textMeasurer = rememberTextMeasurer()

    // Continuous 360-degree radar sweep rotation (4-second period)
    val infiniteTransition = rememberInfiniteTransition(label = "RadarSweepTransition")
    val sweepAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "RadarSweepAngle"
    )

    // Pulsing selection reticle
    val reticlePulse by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ReticlePulse"
    )

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
                .pointerInput(rangeNm, aircraftList, dataTagMode) {
                    detectTapGestures { tapOffset ->
                        val sizePx = min(size.width, size.height).toFloat()
                        val cx = sizePx / 2f
                        val cy = sizePx / 2f
                        val pxPerNm = (cx - 24f) / rangeNm.coerceAtLeast(1f)

                        val zoomBtnSize = 40.dp.toPx()
                        val padding = 12.dp.toPx()

                        // Check Top-Right Zoom In button
                        val zoomInArea = (tapOffset.x in (sizePx - padding - zoomBtnSize)..(sizePx - padding)) &&
                                (tapOffset.y in padding..(padding + zoomBtnSize))
                        if (zoomInArea) {
                            onZoomIn()
                            return@detectTapGestures
                        }

                        // Check Top-Right Zoom Out button
                        val zoomOutY = padding + zoomBtnSize + 8.dp.toPx()
                        val zoomOutArea = (tapOffset.x in (sizePx - padding - zoomBtnSize)..(sizePx - padding)) &&
                                (tapOffset.y in zoomOutY..(zoomOutY + zoomBtnSize))
                        if (zoomOutArea) {
                            onZoomOut()
                            return@detectTapGestures
                        }

                        // Check Top-Left Tag Mode Pill
                        val tagPillArea = (tapOffset.x in padding..(padding + 100.dp.toPx())) &&
                                (tapOffset.y in (padding + 42.dp.toPx())..(padding + 72.dp.toPx()))
                        if (tagPillArea) {
                            onCycleDataTagMode()
                            return@detectTapGestures
                        }

                        // Otherwise: aircraft radar selection hit test
                        val eastNm = (tapOffset.x - cx) / pxPerNm
                        val northNm = (cy - tapOffset.y) / pxPerNm
                        val hitRadiusNm = (28f / pxPerNm).coerceAtLeast(2.5f)
                        onSelectAt(eastNm, northNm, hitRadiusNm)
                    }
                }
        ) {
            val sizePx = min(size.width, size.height)
            val cx = sizePx / 2f
            val cy = sizePx / 2f
            val radiusPx = cx - 24f
            val pxPerNm = radiusPx / rangeNm.coerceAtLeast(1f)

            // 1. Radar Tube Background & Radial Vignette
            drawRect(color = RadarBackground)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF0F1A24),
                        Color(0xFF091017),
                        Color(0xFF060B0F)
                    ),
                    center = Offset(cx, cy),
                    radius = radiusPx
                ),
                radius = radiusPx,
                center = Offset(cx, cy)
            )

            // 2. Tactical Compass Rose & Grid Lines
            drawRadarGrid(
                cx = cx,
                cy = cy,
                radiusPx = radiusPx,
                rangeNm = rangeNm,
                pxPerNm = pxPerNm,
                showRangeRings = showRangeRings,
                textMeasurer = textMeasurer
            )

            // 3. Rotating Radar Sweep Beam with Phosphor Decay
            if (showSweep) {
                drawRadarSweep(
                    cx = cx,
                    cy = cy,
                    radiusPx = radiusPx,
                    sweepAngle = sweepAngle
                )
            }

            // 4. Historical Breadcrumb Trails
            if (showTrails) {
                aircraftList.forEach { ac ->
                    drawAircraftBreadcrumbs(ac, cx, cy, pxPerNm, ac.hex == selectedAircraft?.hex)
                }
            }

            // 5. Velocity Predictor Vectors (1-minute projected ground path)
            if (showVelocityVectors) {
                aircraftList.forEach { ac ->
                    drawVelocityVector(ac, cx, cy, pxPerNm, isSelected = ac.hex == selectedAircraft?.hex)
                }
            }

            // 6. Aircraft Blips (Unselected first, then selected)
            aircraftList.forEach { ac ->
                if (ac.hex != selectedAircraft?.hex) {
                    drawAircraftBlip(
                        ac = ac,
                        cx = cx,
                        cy = cy,
                        pxPerNm = pxPerNm,
                        isSelected = false,
                        showSweep = showSweep,
                        sweepAngle = sweepAngle,
                        dataTagMode = dataTagMode,
                        textMeasurer = textMeasurer
                    )
                }
            }

            // Selected aircraft rendered on top with tactical reticle & full tag
            selectedAircraft?.let { ac ->
                drawAircraftBlip(
                    ac = ac,
                    cx = cx,
                    cy = cy,
                    pxPerNm = pxPerNm,
                    isSelected = true,
                    showSweep = showSweep,
                    sweepAngle = sweepAngle,
                    dataTagMode = dataTagMode,
                    reticlePulse = reticlePulse,
                    textMeasurer = textMeasurer
                )
            }

            // 7. On-Canvas HUD Elements (Zoom Buttons, Counters, Zulu Clock)
            drawRadarHud(
                sizePx = sizePx,
                rangeNm = rangeNm,
                targetCount = aircraftList.size,
                dataTagMode = dataTagMode,
                utcClock = utcClockText,
                textMeasurer = textMeasurer
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
    rangeNm: Float,
    pxPerNm: Float,
    showRangeRings: Boolean,
    textMeasurer: TextMeasurer
) {
    // 12 Bearing Radials every 30°
    for (deg in 0 until 360 step 30) {
        val rad = Math.toRadians((deg - 90).toDouble())
        val xEnd = (cx + cos(rad) * radiusPx).toFloat()
        val yEnd = (cy + sin(rad) * radiusPx).toFloat()
        drawLine(
            color = RadarRadialSpokeColor,
            start = Offset(cx, cy),
            end = Offset(xEnd, yEnd),
            strokeWidth = 1.dp.toPx()
        )
    }

    // Orthogonal Crosshairs
    drawLine(
        color = RadarCrossColor,
        start = Offset(cx - radiusPx, cy),
        end = Offset(cx + radiusPx, cy),
        strokeWidth = 1.2.dp.toPx()
    )
    drawLine(
        color = RadarCrossColor,
        start = Offset(cx, cy - radiusPx),
        end = Offset(cx, cy + radiusPx),
        strokeWidth = 1.2.dp.toPx()
    )

    // Concentric Range Rings (4 rings)
    if (showRangeRings) {
        val rings = 4
        for (i in 1..rings) {
            val ringNm = rangeNm * (i.toFloat() / rings.toFloat())
            val rPx = ringNm * pxPerNm

            drawCircle(
                color = if (i == rings) RadarScopeBorder else RadarRingColor,
                radius = rPx,
                center = Offset(cx, cy),
                style = Stroke(width = if (i == rings) 2.dp.toPx() else 1.dp.toPx())
            )

            // Distance label badge
            val labelText = "${ringNm.toInt()} NM"
            val textLayout = textMeasurer.measure(
                text = labelText,
                style = TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary.copy(alpha = 0.85f)
                )
            )

            val badgeX = cx + 6f
            val badgeY = cy - rPx - 6f
            drawRoundRect(
                color = RadarTagBg,
                topLeft = Offset(badgeX - 3f, badgeY - 2f),
                size = Size(textLayout.size.width + 6f, textLayout.size.height + 4f),
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
            color = if (isCardinal) TextHighlight else if (isMajor) RadarScopeBorder else RadarRingMinorColor,
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
                    color = TextSecondary.copy(alpha = 0.65f)
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
        Triple("N", 0, TextHighlight),
        Triple("E", 90, TextSecondary),
        Triple("S", 180, TextSecondary),
        Triple("W", 270, TextSecondary)
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
        color = HomeMarkerColor,
        start = Offset(cx - rLen, cy),
        end = Offset(cx + rLen, cy),
        strokeWidth = 2.dp.toPx(),
        cap = StrokeCap.Round
    )
    drawLine(
        color = HomeMarkerColor,
        start = Offset(cx, cy - rLen),
        end = Offset(cx, cy + rLen),
        strokeWidth = 2.dp.toPx(),
        cap = StrokeCap.Round
    )
    drawCircle(
        color = TextHighlight,
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
    sweepAngle: Float
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
                colors = listOf(RadarSweepTail, Color.Transparent),
                center = Offset(cx, cy),
                radius = radiusPx
            )
        )

        // Sharp leading sweep line
        drawLine(
            color = RadarSweepBeam,
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
    ac: Aircraft,
    cx: Float,
    cy: Float,
    pxPerNm: Float,
    isSelected: Boolean
) {
    val count = ac.trail.size
    if (count < 2) return

    val baseColor = ac.displayColor

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

/**
 * 1-minute velocity predictor line extending ahead of aircraft along its track.
 */
private fun DrawScope.drawVelocityVector(
    ac: Aircraft,
    cx: Float,
    cy: Float,
    pxPerNm: Float,
    isSelected: Boolean
) {
    if (ac.groundSpeedKts < 20f) return

    val x0 = cx + ac.eastNm * pxPerNm
    val y0 = cy - ac.northNm * pxPerNm

    // 1 minute ahead in NM
    val predNm = (ac.groundSpeedKts / 60f).coerceIn(0.5f, 15f)
    val rad = Math.toRadians((ac.trackDeg - 90).toDouble())

    val x1 = (x0 + cos(rad) * predNm * pxPerNm).toFloat()
    val y1 = (y0 + sin(rad) * predNm * pxPerNm).toFloat()

    val vectorColor = if (isSelected) TextHighlight else ac.displayColor.copy(alpha = 0.75f)
    val strokeWidth = if (isSelected) 1.8.dp.toPx() else 1.1.dp.toPx()

    drawLine(
        color = vectorColor,
        start = Offset(x0, y0),
        end = Offset(x1, y1),
        strokeWidth = strokeWidth,
        cap = StrokeCap.Round
    )

    // Vector tip marker
    drawCircle(
        color = vectorColor,
        radius = if (isSelected) 2.dp.toPx() else 1.4.dp.toPx(),
        center = Offset(x1, y1)
    )
}

/**
 * Aircraft blip chevron, tactical selection brackets, and ATC data tags.
 */
private fun DrawScope.drawAircraftBlip(
    ac: Aircraft,
    cx: Float,
    cy: Float,
    pxPerNm: Float,
    isSelected: Boolean,
    showSweep: Boolean,
    sweepAngle: Float,
    dataTagMode: String,
    reticlePulse: Float = 1.0f,
    textMeasurer: TextMeasurer
) {
    val x = cx + ac.eastNm * pxPerNm
    val y = cy - ac.northNm * pxPerNm
    val center = Offset(x, y)

    // Phosphor Excitation Calculation: brighten when swept
    var blipColor = ac.displayColor
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
        val reticleColor = RadarReticle

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

    // 2. Directional Chevron Rotated by Track
    val len = if (isSelected) 12.dp.toPx() else 8.5.dp.toPx()
    val half = if (isSelected) 6.5.dp.toPx() else 4.8.dp.toPx()

    rotate(degrees = ac.trackDeg, pivot = center) {
        val path = Path().apply {
            moveTo(x, y - len) // Nose pointing North
            lineTo(x - half, y + len * 0.5f) // Left wing
            lineTo(x, y + len * 0.18f) // Tail notch
            lineTo(x + half, y + len * 0.5f) // Right wing
            close()
        }
        drawPath(path = path, color = blipColor)
    }

    // 3. ATC Data Blocks & Leader Lines (3-Way Mode)
    val shouldDrawFullTag = isSelected || dataTagMode == "FULL"
    val shouldDrawCompactTag = !isSelected && dataTagMode == "COMPACT"

    if (shouldDrawFullTag) {
        val callsign = ac.callsign.ifEmpty { ac.registration.ifEmpty { ac.hex.uppercase() } }
        val speedStr = String.format(Locale.US, "%.0fK", ac.groundSpeedKts)
        val line1 = callsign
        val line2 = "${ac.formattedAltitude} ${ac.trendSymbol} $speedStr"

        val textLayout1 = textMeasurer.measure(
            text = line1,
            style = TextStyle(
                fontFamily = FontFamily.Monospace,
                fontSize = if (isSelected) 11.sp else 9.5.sp,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) Color.White else TextPrimary
            )
        )
        val textLayout2 = textMeasurer.measure(
            text = line2,
            style = TextStyle(
                fontFamily = FontFamily.Monospace,
                fontSize = if (isSelected) 10.sp else 8.5.sp,
                fontWeight = FontWeight.Normal,
                color = if (isSelected) TextHighlight else TextSecondary
            )
        )

        val tagWidth = maxOf(textLayout1.size.width, textLayout2.size.width) + 12f
        val tagHeight = textLayout1.size.height + textLayout2.size.height + 8f

        // Angled Leader Line (45 degrees up-right from chevron)
        val elbowX = x + 18.dp.toPx()
        val elbowY = y - 18.dp.toPx()
        val tagRightX = elbowX + tagWidth + 4f

        drawLine(
            color = if (isSelected) TextHighlight else RadarTagBorder,
            start = Offset(x + 4f, y - 4f),
            end = Offset(elbowX, elbowY),
            strokeWidth = 1.2.dp.toPx()
        )
        drawLine(
            color = if (isSelected) TextHighlight else RadarTagBorder,
            start = Offset(elbowX, elbowY),
            end = Offset(tagRightX, elbowY),
            strokeWidth = 1.2.dp.toPx()
        )

        // Data Block Background Pill
        drawRoundRect(
            color = RadarTagBg,
            topLeft = Offset(elbowX + 4f, elbowY - tagHeight + 4f),
            size = Size(tagWidth, tagHeight),
            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
        )
        drawRoundRect(
            color = if (isSelected) TextHighlight.copy(alpha = 0.8f) else RadarTagBorder,
            topLeft = Offset(elbowX + 4f, elbowY - tagHeight + 4f),
            size = Size(tagWidth, tagHeight),
            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()),
            style = Stroke(width = 1.dp.toPx())
        )

        // Text lines
        drawText(textLayoutResult = textLayout1, topLeft = Offset(elbowX + 8f, elbowY - tagHeight + 6f))
        drawText(textLayoutResult = textLayout2, topLeft = Offset(elbowX + 8f, elbowY - tagHeight + textLayout1.size.height + 6f))

    } else if (shouldDrawCompactTag) {
        val callsign = ac.callsign.ifEmpty { ac.registration.ifEmpty { ac.hex.uppercase() } }
        val label = "$callsign ${ac.formattedAltitude}"
        val textLayout = textMeasurer.measure(
            text = label,
            style = TextStyle(
                fontFamily = FontFamily.Monospace,
                fontSize = 9.sp,
                fontWeight = FontWeight.Medium,
                color = TextSecondary.copy(alpha = 0.9f)
            )
        )
        val tagW = textLayout.size.width + 8f
        val tagH = textLayout.size.height + 4f
        val lx = x - tagW / 2f
        val ly = y + 12.dp.toPx()

        drawRoundRect(
            color = RadarTagBg,
            topLeft = Offset(lx, ly),
            size = Size(tagW, tagH),
            cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
        )
        drawText(textLayoutResult = textLayout, topLeft = Offset(lx + 4f, ly + 2f))
    }
}

/**
 * On-Canvas HUD Controls:
 * - Top-Left: Range, Target Counter, Tag Mode Pill
 * - Top-Right: Quick Zoom In [+] and Zoom Out [-] buttons
 * - Bottom-Right: Live UTC Zulu Clock
 */
private fun DrawScope.drawRadarHud(
    sizePx: Float,
    rangeNm: Float,
    targetCount: Int,
    dataTagMode: String,
    utcClock: String,
    textMeasurer: TextMeasurer
) {
    val padding = 12.dp.toPx()

    // 1. Top-Left Scope Telemetry & Mode Pill
    val infoText = "RNG ${rangeNm.toInt()} NM • TGT $targetCount"
    val infoLayout = textMeasurer.measure(
        text = infoText,
        style = TextStyle(
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = TextHighlight,
            letterSpacing = 0.5.sp
        )
    )
    val infoBadgeW = infoLayout.size.width + 16f
    val infoBadgeH = infoLayout.size.height + 8f

    drawRoundRect(
        color = RadarTagBg,
        topLeft = Offset(padding, padding),
        size = Size(infoBadgeW, infoBadgeH),
        cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
    )
    drawRoundRect(
        color = RadarTagBorder,
        topLeft = Offset(padding, padding),
        size = Size(infoBadgeW, infoBadgeH),
        cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()),
        style = Stroke(width = 1.dp.toPx())
    )
    drawText(textLayoutResult = infoLayout, topLeft = Offset(padding + 8f, padding + 4f))

    // Tag Mode Pill Button
    val tagPillY = padding + infoBadgeH + 6.dp.toPx()
    val tagPillText = "TAGS: $dataTagMode"
    val tagPillLayout = textMeasurer.measure(
        text = tagPillText,
        style = TextStyle(
            fontFamily = FontFamily.Monospace,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
    )
    val tagPillW = tagPillLayout.size.width + 14f
    val tagPillH = tagPillLayout.size.height + 6f

    drawRoundRect(
        color = CardBackground,
        topLeft = Offset(padding, tagPillY),
        size = Size(tagPillW, tagPillH),
        cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
    )
    drawRoundRect(
        color = RadarTagBorder,
        topLeft = Offset(padding, tagPillY),
        size = Size(tagPillW, tagPillH),
        cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx()),
        style = Stroke(width = 1.dp.toPx())
    )
    drawText(textLayoutResult = tagPillLayout, topLeft = Offset(padding + 7f, tagPillY + 3f))

    // 2. Top-Right Quick Zoom Buttons [+] and [-]
    val btnSize = 34.dp.toPx()
    val zoomInX = sizePx - padding - btnSize
    val zoomInY = padding

    // Zoom [+] Button
    drawRoundRect(
        color = RadarTagBg,
        topLeft = Offset(zoomInX, zoomInY),
        size = Size(btnSize, btnSize),
        cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
    )
    drawRoundRect(
        color = RadarTagBorder,
        topLeft = Offset(zoomInX, zoomInY),
        size = Size(btnSize, btnSize),
        cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx()),
        style = Stroke(width = 1.2.dp.toPx())
    )
    val plusLayout = textMeasurer.measure(
        text = "+",
        style = TextStyle(
            fontFamily = FontFamily.Monospace,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = TextHighlight
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
    val zoomOutY = zoomInY + btnSize + 8.dp.toPx()
    drawRoundRect(
        color = RadarTagBg,
        topLeft = Offset(zoomInX, zoomOutY),
        size = Size(btnSize, btnSize),
        cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
    )
    drawRoundRect(
        color = RadarTagBorder,
        topLeft = Offset(zoomInX, zoomOutY),
        size = Size(btnSize, btnSize),
        cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx()),
        style = Stroke(width = 1.2.dp.toPx())
    )
    val minusLayout = textMeasurer.measure(
        text = "−",
        style = TextStyle(
            fontFamily = FontFamily.Monospace,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = TextHighlight
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
                color = TextPrimary.copy(alpha = 0.85f),
                letterSpacing = 1.sp
            )
        )
        val clockW = clockLayout.size.width + 14f
        val clockH = clockLayout.size.height + 6f
        val clockX = sizePx - padding - clockW
        val clockY = sizePx - padding - clockH

        drawRoundRect(
            color = RadarTagBg,
            topLeft = Offset(clockX, clockY),
            size = Size(clockW, clockH),
            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
        )
        drawText(textLayoutResult = clockLayout, topLeft = Offset(clockX + 7f, clockY + 3f))
    }
}
