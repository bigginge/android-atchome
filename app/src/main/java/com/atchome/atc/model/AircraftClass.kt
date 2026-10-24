package com.atchome.atc.model

enum class AircraftClass { AIRLINER, HELICOPTER, MILITARY, LIGHT, UNKNOWN }

fun Aircraft.aircraftClass(): AircraftClass {
    return classifyAircraft(typeCode, typeDescription, manufacturer, registeredOwner, callsign)
}

fun RadarAircraft.aircraftClass(): AircraftClass =
    aircraftClass

internal fun classifyAircraft(typeCode: String, typeDescription: String, manufacturer: String, registeredOwner: String, callsign: String): AircraftClass {
    val descriptor = listOf(typeDescription, manufacturer, registeredOwner).joinToString(" ").lowercase()
    val code = typeCode.uppercase()
    val call = callsign.uppercase()

    if (listOf("military", "air force", "navy", "army aviation", "defence", "defense").any { descriptor.contains(it) } ||
        listOf("F16", "F18", "F22", "F35", "A400", "EUFI", "E3CF", "B1", "B2").any { code.startsWith(it) } ||
        listOf("RCH", "RRR", "ASCOT", "RAF", "CNV", "NAVY").any { call.startsWith(it) }) return AircraftClass.MILITARY

    if (listOf("helicopter", "rotorcraft", "gyroplane").any { descriptor.contains(it) } ||
        listOf("R22", "R44", "R66", "H47", "H60", "H64", "S76", "S92", "AW1", "AW2", "EC1", "EC2", "AS3", "AS5").any { code.startsWith(it) }) {
        return AircraftClass.HELICOPTER
    }

    if (listOf("airliner", "commercial jet", "passenger aircraft", "airbus", "boeing", "regional jet").any { descriptor.contains(it) } ||
        listOf("A31", "A32", "A33", "A35", "A38", "B73", "B74", "B75", "B76", "B77", "B78", "E17", "E19", "CRJ").any { code.startsWith(it) }) {
        return AircraftClass.AIRLINER
    }

    if (listOf("light aircraft", "single engine", "cessna", "piper", "diamond aircraft").any { descriptor.contains(it) } ||
        listOf("C15", "C17", "C18", "PA", "DA", "SR2", "P28", "M20").any { code.startsWith(it) }) return AircraftClass.LIGHT

    return AircraftClass.UNKNOWN
}

val AircraftClass.shortLabel: String
    get() = when (this) {
        AircraftClass.AIRLINER -> "AIRLINER"
        AircraftClass.HELICOPTER -> "HELI"
        AircraftClass.MILITARY -> "MIL"
        AircraftClass.LIGHT -> "LIGHT"
        AircraftClass.UNKNOWN -> "AIRCRAFT"
    }
