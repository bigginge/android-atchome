package com.atchome.atc

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.atchome.atc.model.Aircraft
import com.atchome.atc.model.RadarAircraft
import com.atchome.atc.model.TrailPoint
import com.atchome.atc.ui.RadarView
import com.atchome.atc.ui.theme.AtcColors
import com.atchome.atc.ui.theme.AtcHomeTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Debug-only deterministic radar scene, launched with --ei count <0..500>. */
class RadarBenchmarkActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val count = intent.getIntExtra(EXTRA_COUNT, 50).coerceIn(0, MAX_COUNT)
        val dataTagMode = intent.getStringExtra(EXTRA_TAG_MODE) ?: "COMPACT"
        val showTrails = intent.getBooleanExtra(EXTRA_SHOW_TRAILS, true)
        val showSweep = intent.getBooleanExtra(EXTRA_SHOW_SWEEP, true)
        val traffic = rememberTraffic(count)
        val selected = traffic.firstOrNull()?.toAircraft()

        setContent {
            AtcHomeTheme(themeMode = "DARK") {
                Surface(modifier = Modifier.fillMaxSize(), color = AtcColors.radarBackground) {
                    RadarView(
                        radarAircraft = remember { MutableStateFlow(traffic) },
                        selectedAircraft = selected,
                        rangeNm = 50f,
                        showTrails = showTrails,
                        showRangeRings = true,
                        showAirports = false,
                        showSweep = showSweep,
                        reduceMotion = false,
                        dataTagMode = dataTagMode,
                        modifier = Modifier.fillMaxSize(),
                        onSelectAt = { _, _, _ -> },
                        onSelectByHex = {},
                        onZoomIn = {},
                        onZoomOut = {}
                    )
                }
            }
        }
    }

    private fun rememberTraffic(count: Int): List<RadarAircraft> = List(count) { index ->
        val angle = index * GOLDEN_ANGLE
        // Keep every synthetic target within the default benchmark viewport.
        val radius = 2f + (index % 47) * 0.45f
        val east = cos(angle) * radius
        val north = sin(angle) * radius
        val hex = (0x400000 + index).toString(16).padStart(6, '0')
        val type = if (index % 4 == 0) "C172" else "A320"
        val trail = List(12) { point ->
            val distance = point * 0.08f
            TrailPoint(
                eastNm = east - sin(angle) * distance,
                northNm = north - cos(angle) * distance
            )
        }
        RadarAircraft(
            hex = hex,
            callsign = "TEST${index.toString().padStart(3, '0')}",
            registration = "N${index.toString().padStart(4, '0')}",
            typeCode = type,
            typeDescription = "Benchmark aircraft",
            manufacturer = "Test",
            registeredOwner = "Benchmark",
            eastNm = east,
            northNm = north,
            trackDeg = ((index * 37) % 360).toFloat(),
            altitudeFt = 3000 + (index % 30) * 1000,
            altitudeKnown = true,
            vertRateFpm = if (index % 2 == 0) 500 else -300,
            groundSpeedKts = 120f + (index % 180),
            origin = "",
            destination = "",
            originIcao = "",
            destinationIcao = "",
            trail = trail,
            seen = true,
            positionAgeSeconds = null
        )
    }

    private fun RadarAircraft.toAircraft() = Aircraft(
        hex = hex,
        callsign = callsign,
        registration = registration,
        typeCode = typeCode,
        eastNm = eastNm,
        northNm = northNm,
        trackDeg = trackDeg,
        altitudeFt = altitudeFt,
        altitudeKnown = altitudeKnown,
        vertRateFpm = vertRateFpm,
        groundSpeedKts = groundSpeedKts
    )

    companion object {
        const val EXTRA_COUNT = "count"
        const val EXTRA_TAG_MODE = "tag_mode"
        const val EXTRA_SHOW_TRAILS = "show_trails"
        const val EXTRA_SHOW_SWEEP = "show_sweep"
        private const val MAX_COUNT = 500
        private val GOLDEN_ANGLE = (PI * (3.0 - kotlin.math.sqrt(5.0))).toFloat()
    }
}
