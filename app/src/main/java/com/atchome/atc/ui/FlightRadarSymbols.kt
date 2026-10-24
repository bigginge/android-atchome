package com.atchome.atc.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.dp
import com.atchome.atc.model.AircraftClass

/**
 * Authentic top-down aircraft silhouette symbols modeled on Flightradar24 (FR24).
 *
 * Each aircraft category features distinct aerodynamic shapes, engine placement,
 * wingspan aspect ratios, and empennage profiles:
 * - Narrowbody Twinjet (A320/B737): Swept wings with 2 underslung engine nacelles.
 * - Widebody Twinjet (B777/A350/B787): Larger wingspan with larger turbofan nacelles.
 * - Jumbo 4-Engine (B747): Forward upper-deck hump profile, 4 engine nacelles.
 * - Super-Heavy 4-Engine (A380): Broad double-deck fuselage, massive wingspan, 4 engines.
 * - Regional / Bizjet (CRJ/Learjet/Citation): Sleek fuselage, rear-mounted twin engines, T-tail.
 * - Twin Turboprop (Dash 8/ATR 72): Straight wings with 2 turboprop nacelles and propeller discs, T-tail.
 * - Light General Aviation (Cessna 172/Piper): Straight rectangular wings, nose propeller, compact cabin.
 * - Helicopter: Rotor hub with blades, streamlined teardrop cabin, tail boom and tail rotor.
 * - Military Fighter (F-16/Typhoon): Needle radome, strakes, clipped delta wings with missile rails.
 * - Glider (Sailplane): Ultra-slender high-aspect ratio wings, needle fuselage, T-tail.
 * - Generic / Unknown: Classic Flightradar24 yellow airliner silhouette.
 */
object FlightRadarSymbols {

    private fun buildSymmetricalPath(points: List<Offset>): Path {
        val path = Path()
        if (points.isEmpty()) return path
        path.moveTo(points[0].x, points[0].y)
        for (i in 1 until points.size) {
            path.lineTo(points[i].x, points[i].y)
        }
        for (i in points.size - 2 downTo 0) {
            path.lineTo(-points[i].x, points[i].y)
        }
        path.close()
        return path
    }

    /**
     * Narrow-body commercial twinjet (A320, B737, E-Jets, A220).
     * The signature Flightradar24 commercial airliner icon.
     */
    val airlinerPath: Path by lazy {
        buildSymmetricalPath(
            listOf(
                Offset(0f, -14.0f),
                Offset(0.8f, -13.2f),
                Offset(1.5f, -11.5f),
                Offset(1.7f, -9.0f),
                Offset(1.7f, -4.5f),
                Offset(2.8f, -3.2f),
                Offset(4.2f, -1.8f),
                // Underslung engine nacelle
                Offset(4.0f, -3.4f),
                Offset(4.4f, -3.6f),
                Offset(5.3f, -3.6f),
                Offset(5.5f, -0.2f),
                Offset(4.7f, 0.6f),
                Offset(4.8f, -1.2f),
                // Wing sweep to tip
                Offset(8.5f, 1.8f),
                Offset(12.0f, 4.6f),
                Offset(12.0f, 5.4f),
                // Wing trailing edge with flap track fairings
                Offset(11.2f, 5.1f),
                Offset(8.8f, 3.8f),
                Offset(8.6f, 4.5f),
                Offset(8.3f, 3.6f),
                Offset(5.5f, 2.0f),
                Offset(5.3f, 2.7f),
                Offset(5.0f, 1.8f),
                Offset(1.7f, 0.5f),
                // Rear fuselage & horizontal stabilizer
                Offset(1.7f, 6.5f),
                Offset(1.4f, 7.8f),
                Offset(5.8f, 10.8f),
                Offset(5.6f, 11.6f),
                Offset(1.2f, 10.8f),
                Offset(0.8f, 12.0f),
                Offset(0f, 12.5f)
            )
        )
    }

    /**
     * Large wide-body twinjet (B777, A350, B787, A330).
     * Noticeably larger wingspan, wider fuselage, and larger engine nacelles.
     */
    val widebodyPath: Path by lazy {
        buildSymmetricalPath(
            listOf(
                Offset(0f, -16.5f),
                Offset(1.1f, -15.5f),
                Offset(2.1f, -13.5f),
                Offset(2.3f, -10.0f),
                Offset(2.3f, -5.5f),
                Offset(3.6f, -4.0f),
                Offset(5.2f, -2.4f),
                // Massive turbofan engine nacelle
                Offset(5.0f, -4.3f),
                Offset(5.5f, -4.6f),
                Offset(6.6f, -4.6f),
                Offset(6.8f, 0.4f),
                Offset(5.7f, 1.4f),
                Offset(5.9f, -1.6f),
                // Wing sweep to raked tip
                Offset(10.5f, 2.0f),
                Offset(15.5f, 5.8f),
                Offset(15.5f, 6.8f),
                Offset(14.5f, 6.5f),
                // Trailing edge with flap fairings
                Offset(11.5f, 4.8f),
                Offset(11.2f, 5.8f),
                Offset(10.8f, 4.5f),
                Offset(6.8f, 2.8f),
                Offset(6.5f, 3.8f),
                Offset(6.2f, 2.5f),
                Offset(2.3f, 0.8f),
                // Aft fuselage & tail
                Offset(2.2f, 8.5f),
                Offset(1.8f, 10.0f),
                Offset(7.5f, 13.8f),
                Offset(7.2f, 14.8f),
                Offset(1.5f, 13.8f),
                Offset(1.0f, 15.2f),
                Offset(0f, 15.8f)
            )
        )
    }

    /**
     * Heavy 4-engine jumbo jet (Boeing 747).
     * Features the iconic upper-deck forward hump and two engines on each wing.
     */
    val jumboPath: Path by lazy {
        buildSymmetricalPath(
            listOf(
                Offset(0f, -17.5f),
                // Upper-deck hump profile
                Offset(1.1f, -16.5f),
                Offset(2.6f, -14.0f),
                Offset(2.6f, -10.0f),
                Offset(2.4f, -6.5f),
                Offset(2.4f, -5.0f),
                // Wing leading edge to inner engine
                Offset(4.2f, -3.2f),
                // Inner engine nacelle
                Offset(5.1f, -4.0f),
                Offset(6.3f, -4.0f),
                Offset(6.4f, 0.0f),
                Offset(5.5f, 0.8f),
                Offset(6.0f, -1.8f),
                // Wing leading edge to outer engine
                Offset(8.5f, 0.2f),
                // Outer engine nacelle
                Offset(9.7f, -0.6f),
                Offset(10.9f, -0.6f),
                Offset(11.0f, 3.2f),
                Offset(10.2f, 4.0f),
                Offset(10.6f, 1.8f),
                // Wingtip & winglet
                Offset(16.5f, 6.5f),
                Offset(16.5f, 7.5f),
                Offset(15.5f, 7.2f),
                // Trailing edge with flap track fairings
                Offset(12.5f, 5.5f),
                Offset(12.2f, 6.4f),
                Offset(11.8f, 5.2f),
                Offset(7.5f, 3.2f),
                Offset(7.2f, 4.2f),
                Offset(6.8f, 3.0f),
                Offset(2.4f, 1.5f),
                // Aft fuselage & tail
                Offset(2.2f, 9.0f),
                Offset(1.8f, 10.5f),
                Offset(8.0f, 14.5f),
                Offset(7.6f, 15.5f),
                Offset(1.5f, 14.5f),
                Offset(1.0f, 16.0f),
                Offset(0f, 16.8f)
            )
        )
    }

    /**
     * Super-heavy 4-engine double-decker (Airbus A380).
     * Broad full-length fuselage, massive wingspan, and 4 engine pods.
     */
    val superPath: Path by lazy {
        buildSymmetricalPath(
            listOf(
                Offset(0f, -18.0f),
                Offset(1.4f, -17.0f),
                Offset(3.0f, -14.5f),
                Offset(3.2f, -6.5f),
                Offset(4.5f, -4.5f),
                Offset(6.0f, -3.0f),
                // Inner engine nacelle
                Offset(6.7f, -4.0f),
                Offset(8.1f, -4.0f),
                Offset(8.2f, 0.4f),
                Offset(7.2f, 1.2f),
                Offset(7.8f, -1.6f),
                // Outer engine nacelle
                Offset(11.0f, 0.8f),
                Offset(11.9f, -0.2f),
                Offset(13.3f, -0.2f),
                Offset(13.4f, 3.8f),
                Offset(12.4f, 4.6f),
                Offset(13.0f, 2.2f),
                // Wide wingspan & wingtip fence
                Offset(18.5f, 6.5f),
                Offset(18.5f, 7.8f),
                Offset(17.4f, 7.5f),
                // Deep chord trailing edge with flap track fairings
                Offset(14.0f, 6.0f),
                Offset(13.7f, 7.0f),
                Offset(13.2f, 5.8f),
                Offset(9.0f, 4.0f),
                Offset(8.7f, 5.0f),
                Offset(8.2f, 3.8f),
                Offset(3.2f, 2.0f),
                // Wide fuselage & tail
                Offset(3.0f, 9.5f),
                Offset(2.2f, 11.0f),
                Offset(9.0f, 15.0f),
                Offset(8.5f, 16.2f),
                Offset(1.8f, 15.0f),
                Offset(1.2f, 16.5f),
                Offset(0f, 17.2f)
            )
        )
    }

    /**
     * Regional / Business Jet (CRJ, ERJ, Learjet, Citation, Gulfstream).
     * Sleek slim fuselage, clean wings, rear-mounted twin engines, and T-tail.
     */
    val regionalJetPath: Path by lazy {
        buildSymmetricalPath(
            listOf(
                Offset(0f, -15.0f),
                Offset(0.7f, -14.2f),
                Offset(1.3f, -12.5f),
                Offset(1.3f, -3.0f),
                // Clean swept wing (no wing engines)
                Offset(1.3f, -2.5f),
                Offset(6.0f, 0.5f),
                Offset(11.5f, 3.5f),
                Offset(11.5f, 4.3f),
                Offset(10.8f, 4.1f),
                Offset(5.5f, 2.5f),
                Offset(1.3f, 1.2f),
                // Rear-mounted twin engine nacelles
                Offset(1.3f, 3.5f),
                Offset(2.3f, 3.8f),
                Offset(3.4f, 3.8f),
                Offset(3.4f, 7.2f),
                Offset(2.5f, 7.8f),
                Offset(1.2f, 8.0f),
                // T-tail horizontal stabilizer
                Offset(1.0f, 11.0f),
                Offset(1.0f, 11.2f),
                Offset(5.5f, 12.5f),
                Offset(5.3f, 13.5f),
                Offset(0.6f, 13.2f),
                Offset(0f, 13.8f)
            )
        )
    }

    /**
     * Twin Turboprop Commuter (Dash 8, ATR 42/72, King Air).
     * Straight wings with two distinct turboprop nacelles & propeller discs, T-tail.
     */
    val turbopropPath: Path by lazy {
        buildSymmetricalPath(
            listOf(
                Offset(0f, -13.5f),
                Offset(0.8f, -12.5f),
                Offset(1.6f, -10.5f),
                Offset(1.6f, -2.5f),
                Offset(3.2f, -2.5f),
                // Turboprop nacelle with propeller disc protrusion
                Offset(3.2f, -4.8f),
                Offset(4.5f, -5.2f),
                Offset(5.8f, -4.8f),
                Offset(5.6f, 1.0f),
                Offset(4.8f, 1.8f),
                Offset(4.5f, 0.5f),
                // Straight wing to tip
                Offset(12.5f, -1.0f),
                Offset(12.5f, 0.2f),
                Offset(11.8f, 0.2f),
                Offset(6.0f, 0.5f),
                Offset(1.6f, 0.5f),
                // Fuselage & T-tail
                Offset(1.5f, 7.5f),
                Offset(1.0f, 8.8f),
                Offset(5.5f, 9.2f),
                Offset(5.3f, 10.4f),
                Offset(0.6f, 10.4f),
                Offset(0f, 11.0f)
            )
        )
    }

    /**
     * Light General Aviation / Single Propeller (Cessna 172, Piper Cherokee).
     * Straight rectangular wings, nose propeller spinner, compact cabin.
     */
    val lightPath: Path by lazy {
        buildSymmetricalPath(
            listOf(
                // Propeller spinner & blade at nose
                Offset(0f, -10.6f),
                Offset(0.4f, -10.2f),
                Offset(3.5f, -10.0f),
                Offset(3.5f, -9.4f),
                Offset(0.8f, -9.4f),
                // Cowling & cabin
                Offset(1.2f, -8.8f),
                Offset(1.4f, -7.0f),
                Offset(1.4f, -4.5f),
                // Straight rectangular wing
                Offset(9.5f, -4.2f),
                Offset(9.5f, -1.2f),
                Offset(1.4f, -1.2f),
                // Rear fuselage & horizontal stabilizer
                Offset(1.0f, 4.5f),
                Offset(1.0f, 5.0f),
                Offset(4.0f, 5.2f),
                Offset(3.8f, 6.5f),
                Offset(0.6f, 6.5f),
                Offset(0f, 7.5f)
            )
        )
    }

    /**
     * Military Combat Fighter (F-16, F-18, Eurofighter Typhoon).
     * Needle radome nose, LERX strakes, clipped delta wings with missile launch rails.
     */
    val militaryPath: Path by lazy {
        buildSymmetricalPath(
            listOf(
                Offset(0f, -15.0f),
                Offset(0.6f, -13.5f),
                Offset(1.0f, -10.5f),
                Offset(1.8f, -6.5f),
                Offset(2.2f, -3.5f),
                // Clipped delta wing leading edge
                Offset(2.2f, -3.0f),
                Offset(8.5f, 1.0f),
                // Wingtip missile launch rail
                Offset(8.8f, 0.5f),
                Offset(9.0f, 2.5f),
                Offset(8.5f, 2.5f),
                // Trailing edge
                Offset(6.5f, 2.2f),
                Offset(2.2f, 3.0f),
                Offset(2.0f, 5.5f),
                // Swept all-moving horizontal stabilizer
                Offset(2.0f, 6.5f),
                Offset(6.0f, 9.5f),
                Offset(5.5f, 10.8f),
                Offset(1.6f, 9.5f),
                // Engine exhaust nozzle
                Offset(1.2f, 11.5f),
                Offset(0f, 11.5f)
            )
        )
    }

    /**
     * Glider / Sailplane (ASK 21).
     * Ultra-slender high-aspect ratio wings, needle fuselage, T-tail.
     */
    val gliderPath: Path by lazy {
        buildSymmetricalPath(
            listOf(
                Offset(0f, -7.0f),
                Offset(0.7f, -6.0f),
                Offset(0.9f, -3.5f),
                Offset(0.8f, -1.0f),
                // Ultra-slender long wings
                Offset(8.0f, -0.8f),
                Offset(17.0f, -0.5f),
                Offset(17.0f, 0.5f),
                Offset(8.0f, 0.5f),
                Offset(0.8f, 0.5f),
                // Needle tail boom & T-tail
                Offset(0.5f, 3.5f),
                Offset(0.35f, 8.5f),
                Offset(0.35f, 9.0f),
                Offset(3.5f, 9.2f),
                Offset(3.3f, 10.2f),
                Offset(0f, 10.2f)
            )
        )
    }

    /**
     * Helicopter fuselage and tail boom (Bell / Eurocopter / Sikorsky).
     * Streamlined teardrop cabin, slender tail boom, horizontal fin, and tail rotor.
     */
    val helicopterFuselagePath: Path by lazy {
        Path().apply {
            // Teardrop cabin nose
            moveTo(0f, -7.5f)
            lineTo(1.8f, -6.5f)
            lineTo(2.5f, -4.0f)
            lineTo(2.5f, -1.0f)
            lineTo(1.4f, 1.5f)
            lineTo(0.8f, 3.5f)
            lineTo(0.6f, 9.0f)
            // Starboard horizontal fin
            lineTo(2.2f, 9.2f)
            lineTo(2.2f, 10.0f)
            lineTo(0.5f, 10.0f)
            // Vertical tail fin
            lineTo(0.4f, 12.0f)
            // Starboard tail rotor
            lineTo(1.8f, 11.2f)
            lineTo(1.8f, 12.8f)
            lineTo(0f, 12.8f)
            // Port side (mirror)
            lineTo(-0.4f, 12.0f)
            lineTo(-0.5f, 10.0f)
            lineTo(-2.2f, 10.0f)
            lineTo(-2.2f, 9.2f)
            lineTo(-0.6f, 9.0f)
            lineTo(-0.8f, 3.5f)
            lineTo(-1.4f, 1.5f)
            lineTo(-2.5f, -1.0f)
            lineTo(-2.5f, -4.0f)
            lineTo(-1.8f, -6.5f)
            close()
        }
    }

    /**
     * Resolves the corresponding Flightradar24 silhouette Path for an [AircraftClass].
     */
    fun getSymbolPath(aircraftClass: AircraftClass): Path = when (aircraftClass) {
        AircraftClass.AIRLINER -> airlinerPath
        AircraftClass.WIDEBODY -> widebodyPath
        AircraftClass.JUMBO -> jumboPath
        AircraftClass.SUPER -> superPath
        AircraftClass.REGIONAL_JET -> regionalJetPath
        AircraftClass.TURBOPROP -> turbopropPath
        AircraftClass.LIGHT -> lightPath
        AircraftClass.MILITARY -> militaryPath
        AircraftClass.GLIDER -> gliderPath
        AircraftClass.HELICOPTER -> helicopterFuselagePath
        AircraftClass.UNKNOWN -> airlinerPath
    }

    /**
     * Relative scale factor for each aircraft category, mirroring Flightradar24's
     * visual size hierarchy (Super/Jumbo > Widebody > Airliner > Regional > Light/Heli).
     */
    fun getClassScale(aircraftClass: AircraftClass): Float = when (aircraftClass) {
        AircraftClass.SUPER -> 1.25f
        AircraftClass.JUMBO -> 1.18f
        AircraftClass.WIDEBODY -> 1.12f
        AircraftClass.AIRLINER -> 1.00f
        AircraftClass.REGIONAL_JET -> 0.90f
        AircraftClass.TURBOPROP -> 0.92f
        AircraftClass.MILITARY -> 0.92f
        AircraftClass.LIGHT -> 0.85f
        AircraftClass.GLIDER -> 0.95f
        AircraftClass.HELICOPTER -> 0.90f
        AircraftClass.UNKNOWN -> 1.00f
    }
}

/**
 * Draws the Flightradar24 aircraft symbol at [center] rotated towards [trackDeg].
 */
fun DrawScope.drawFlightRadarAircraftSymbol(
    aircraftClass: AircraftClass,
    center: Offset,
    trackDeg: Float,
    symbolColor: Color,
    isSelected: Boolean,
    simplifiedSymbol: Boolean,
    dimAlpha: Float = 1f
) {
    val densityScale = 1.05f
    val baseScale = if (simplifiedSymbol && !isSelected) 0.78f else 1.0f
    val selectionScale = if (isSelected) 1.18f else 1.0f
    val classScale = FlightRadarSymbols.getClassScale(aircraftClass)
    val totalScale = baseScale * selectionScale * classScale * densityScale

    val outlineColor = Color.Black.copy(alpha = 0.55f * dimAlpha)
    val outlineWidth = 1.1.dp.toPx()

    translate(left = center.x, top = center.y) {
        rotate(degrees = trackDeg, pivot = Offset.Zero) {
            scale(scale = totalScale, pivot = Offset.Zero) {
                if (aircraftClass == AircraftClass.HELICOPTER) {
                    // Draw helicopter fuselage
                    val fuselage = FlightRadarSymbols.helicopterFuselagePath
                    drawPath(fuselage, color = outlineColor, style = Stroke(width = outlineWidth))
                    drawPath(fuselage, color = symbolColor, style = Fill)

                    // Draw 4-blade rotor centered over rotor mast (y = -2.5)
                    val mastY = -2.5f
                    val bladeRadius = 9.5f
                    val bladeWidth = 1.4f

                    // Rotor blades cross
                    drawLine(
                        color = outlineColor,
                        start = Offset(-bladeRadius, mastY),
                        end = Offset(bladeRadius, mastY),
                        strokeWidth = bladeWidth + 0.8f,
                        cap = StrokeCap.Round
                    )
                    drawLine(
                        color = symbolColor,
                        start = Offset(-bladeRadius, mastY),
                        end = Offset(bladeRadius, mastY),
                        strokeWidth = bladeWidth,
                        cap = StrokeCap.Round
                    )

                    drawLine(
                        color = outlineColor,
                        start = Offset(0f, mastY - bladeRadius),
                        end = Offset(0f, mastY + bladeRadius),
                        strokeWidth = bladeWidth + 0.8f,
                        cap = StrokeCap.Round
                    )
                    drawLine(
                        color = symbolColor,
                        start = Offset(0f, mastY - bladeRadius),
                        end = Offset(0f, mastY + bladeRadius),
                        strokeWidth = bladeWidth,
                        cap = StrokeCap.Round
                    )

                    // Central rotor hub
                    drawCircle(
                        color = outlineColor,
                        radius = 2.0f,
                        center = Offset(0f, mastY),
                        style = Stroke(width = 0.8f)
                    )
                    drawCircle(
                        color = symbolColor,
                        radius = 1.8f,
                        center = Offset(0f, mastY),
                        style = Fill
                    )
                } else {
                    val path = FlightRadarSymbols.getSymbolPath(aircraftClass)
                    // Crisp dark stroke outline for contrast
                    drawPath(path, color = outlineColor, style = Stroke(width = outlineWidth))
                    // Filled silhouette in aircraft status/altitude color
                    drawPath(path, color = symbolColor, style = Fill)
                }
            }
        }
    }
}
