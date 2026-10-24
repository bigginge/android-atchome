package com.atchome.atc.data

import com.atchome.atc.model.Aircraft
import com.atchome.atc.model.MAX_AIRCRAFT
import com.atchome.atc.model.RadarAircraft
import com.atchome.atc.model.TRAIL_LENGTH
import com.atchome.atc.model.TrailPoint
import com.atchome.atc.model.toRadarAircraft
import kotlin.math.cos
import kotlin.math.sin

class Tracker {
    private val aircraftList = mutableListOf<Aircraft>()
    private companion object {
        private const val MAX_DEAD_RECKONING_DT_SECONDS = 1.0f
    }
    private var selectedHex: String = ""
    private var autoSelected: Boolean = true
    private var pinnedAircraft: Aircraft? = null
    private val trailSnapshots = mutableMapOf<String, List<TrailPoint>>()

    @Synchronized
    fun getAircraft(): List<Aircraft> = (aircraftList + listOfNotNull(
        pinnedAircraft?.takeIf { pinned -> aircraftList.none { it.hex.equals(pinned.hex, true) } }
    )).toList()

    @Synchronized
    fun getRadarAircraftSnapshot(positionTimestampNanos: Long): List<RadarAircraft> {
        val traffic = ArrayList<RadarAircraft>(aircraftList.size + 1)
        for (aircraft in aircraftList) {
            traffic += aircraft.toRadarAircraft(
                trailSnapshot = trailSnapshots[aircraft.hex.uppercase()] ?: emptyList(),
                positionTimestampNanos = positionTimestampNanos
            )
        }
        pinnedAircraft?.takeIf { pinned -> aircraftList.none { it.hex.equals(pinned.hex, true) } }?.let {
            traffic += it.toRadarAircraft(positionTimestampNanos = positionTimestampNanos)
        }
        return traffic
    }

    @Synchronized
    fun pinAircraft(aircraft: Aircraft?) { pinnedAircraft = aircraft }

    @Synchronized
    fun selected(): Aircraft? = findByHex(selectedHex)

    @Synchronized
    fun hasSelection(): Boolean = selectedHex.isNotEmpty()

    @Synchronized
    fun isAutoSelected(): Boolean = autoSelected

    @Synchronized
    fun clear() {
        aircraftList.clear()
        trailSnapshots.clear()
        pinnedAircraft = null
        selectedHex = ""
        autoSelected = true
    }

    @Synchronized
    fun mergeSnapshot(incoming: List<Aircraft>) {
        val previous = aircraftList.associateBy { it.hex.uppercase() }
        aircraftList.clear()

        val count = incoming.size
        for (i in 0 until count) {
            val dst = incoming[i].copy(trail = mutableListOf())
            val prev = previous[dst.hex.uppercase()]

            if (prev != null) {
                retainEnrichment(dst, prev)
                pushTrail(dst, dst.eastNm, dst.northNm)
            } else {
                pushTrail(dst, dst.eastNm, dst.northNm)
            }

            dst.seen = incoming[i].seen
            aircraftList.add(dst)
        }

        trailSnapshots.clear()
        aircraftList.forEach { aircraft ->
            trailSnapshots[aircraft.hex.uppercase()] = aircraft.trail.toList()
        }

        if (selectedHex.isNotEmpty() && findByHex(selectedHex) == null) {
            selectedHex = ""
            autoSelected = true
        }

        if (autoSelected) {
            selectNearest()
        }
    }

    @Synchronized
    fun selectNearest() {
        if (getAircraft().isEmpty()) {
            selectedHex = ""
            return
        }
        val nearest = getAircraft().minByOrNull { it.distanceNm }
        if (nearest != null) {
            selectedHex = nearest.hex
        }
    }

    @Synchronized
    fun selectByHex(hex: String): Boolean {
        val ac = findByHex(hex) ?: return false
        selectedHex = ac.hex
        autoSelected = false
        return true
    }

    /**
     * Hit-test nearest aircraft within hitRadiusNm.
     * If no aircraft within radius, reverts to auto-selecting the nearest aircraft.
     */
    @Synchronized
    fun selectNearestTo(eastNm: Float, northNm: Float, hitRadiusNm: Float): Aircraft? {
        var best: Aircraft? = null
        var bestDist2 = hitRadiusNm * hitRadiusNm

        for (ac in getAircraft()) {
            val de = ac.eastNm - eastNm
            val dn = ac.northNm - northNm
            val d2 = de * de + dn * dn
            if (d2 <= bestDist2) {
                bestDist2 = d2
                best = ac
            }
        }

        if (best != null) {
            selectedHex = best.hex
            autoSelected = false
        } else {
            selectedHex = ""
            autoSelected = true
            selectNearest()
        }
        return best
    }

    /**
     * Dead-reckoning extrapolation for smooth aircraft animation between polling snapshots.
     */
    @Synchronized
    fun updatePositions(dtSeconds: Float) {
        val dt = dtSeconds.coerceIn(0f, MAX_DEAD_RECKONING_DT_SECONDS)
        if (dt <= 0f) return

        for (ac in aircraftList) {
            if (ac.groundSpeedKts > 0f) {
                val speedNmps = ac.groundSpeedKts / 3600f
                val distNm = speedNmps * dt
                val rad = Math.toRadians(ac.trackDeg.toDouble())
                ac.eastNm += (sin(rad) * distNm).toFloat()
                ac.northNm += (cos(rad) * distNm).toFloat()
            }
        }
    }

    private fun findByHex(hex: String): Aircraft? {
        if (hex.isEmpty()) return null
        return aircraftList.firstOrNull { it.hex.equals(hex, ignoreCase = true) }
            ?: pinnedAircraft?.takeIf { it.hex.equals(hex, ignoreCase = true) }
    }

    private fun pushTrail(ac: Aircraft, eastNm: Float, northNm: Float) {
        if (ac.trail.isNotEmpty()) {
            val last = ac.trail.last()
            val de = eastNm - last.eastNm
            val dn = northNm - last.northNm
            if ((de * de + dn * dn) < (0.05f * 0.05f)) {
                return
            }
        }

        if (ac.trail.size >= TRAIL_LENGTH) {
            ac.trail.removeAt(0)
        }
        ac.trail.add(TrailPoint(eastNm, northNm))
    }

    private fun retainEnrichment(dst: Aircraft, prev: Aircraft) {
        if (dst.yearBuilt == null && prev.yearBuilt != null) {
            dst.yearBuilt = prev.yearBuilt
        }

        if (prev.imageLoaded) {
            dst.imageUrl = prev.imageUrl
            dst.imagePhotographer = prev.imagePhotographer
            dst.imageLoaded = true
        }

        if (prev.detailsLoaded) {
            dst.manufacturer = prev.manufacturer
            if (prev.typeDescription.isNotEmpty()) {
                dst.typeDescription = prev.typeDescription
            }
            dst.registeredOwner = prev.registeredOwner
            if (dst.registration.isEmpty()) {
                dst.registration = prev.registration
            }
            dst.detailsLoaded = true
        } else if (dst.typeDescription.isEmpty() && prev.typeDescription.isNotEmpty()) {
            dst.typeDescription = prev.typeDescription
        }

        if (prev.routeLookupStep != 0 || prev.routeLoaded) {
            dst.origin = prev.origin
            dst.destination = prev.destination
            dst.originIcao = prev.originIcao
            dst.destinationIcao = prev.destinationIcao
            dst.originLat = prev.originLat
            dst.originLon = prev.originLon
            dst.destinationLat = prev.destinationLat
            dst.destinationLon = prev.destinationLon
            dst.routeLoaded = prev.routeLoaded
            dst.routeLookupStep = prev.routeLookupStep
        }

        dst.trail.clear()
        dst.trail.addAll(prev.trail)
    }
}
