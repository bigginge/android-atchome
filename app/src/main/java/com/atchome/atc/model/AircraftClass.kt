package com.atchome.atc.model

/**
 * Aircraft classification categories matching the symbols used on Flightradar24.
 */
enum class AircraftClass {
    AIRLINER,       // Narrow-body twinjet (A320, B737, E-Jets, A220, etc.)
    WIDEBODY,       // Large wide-body twinjet (B777, A350, B787, A330, etc.)
    JUMBO,          // Heavy 4-engine jumbo jet (B747, A340, etc.)
    SUPER,          // Super-heavy double-decker (A380, An-225)
    REGIONAL_JET,   // Regional/business jet with rear engines (CRJ, ERJ, Learjet, Citation, Gulfstream)
    TURBOPROP,      // Twin turboprop commuter (Dash 8, ATR 42/72, King Air)
    LIGHT,          // Light general aviation single-engine prop (Cessna 172, Piper, Cirrus)
    HELICOPTER,     // Rotorcraft (Helicopters)
    MILITARY,       // Military combat fighter jets (F-16, F-18, Typhoon, F-35, etc.)
    GLIDER,         // Sailplane / Glider (ASK 21, etc.)
    UNKNOWN         // Generic Flightradar24 commercial airliner
}

fun Aircraft.aircraftClass(): AircraftClass {
    return classifyAircraft(typeCode, typeDescription, manufacturer, registeredOwner, callsign)
}

fun RadarAircraft.aircraftClass(): AircraftClass =
    aircraftClass

internal fun classifyAircraft(
    typeCode: String,
    typeDescription: String,
    manufacturer: String,
    registeredOwner: String,
    callsign: String
): AircraftClass {
    val descriptor = listOf(typeDescription, manufacturer, registeredOwner).joinToString(" ").lowercase()
    val code = typeCode.uppercase()
    val call = callsign.uppercase()

    // 1. Military Combat & Transports
    if (listOf("military", "air force", "navy", "army aviation", "defence", "defense", "fighter", "interceptor", "combat").any { descriptor.contains(it) } ||
        listOf("F16", "F18", "FA18", "F22", "F35", "EUFI", "TYPH", "TORN", "RAFALE", "JAS39", "MIG", "SU2", "SU3", "SU5", "A400", "E3CF", "C130", "HAWK", "ALPHA", "AJET", "AV8B", "GR4").any { code.startsWith(it) } ||
        code in setOf("B1", "B2", "C17") ||
        listOf("RCH", "RRR", "ASCOT", "RAF", "CNV", "NAVY", "USAF", "JAKE", "VIPER", "REBEL", "SWORD").any { call.startsWith(it) }) {
        return AircraftClass.MILITARY
    }

    // 2. Rotorcraft / Helicopters
    if (listOf("helicopter", "rotorcraft", "gyroplane", "autogyro").any { descriptor.contains(it) } ||
        listOf("R22", "R44", "R66", "H47", "H60", "H64", "S76", "S92", "AW1", "AW2", "EC1", "EC2", "EC3", "EC4", "EC5", "AS3", "AS5", "B06", "B212", "B412", "B429", "A109", "A119", "A139", "A169", "BK117", "UH1", "CH47", "AH64", "EH10", "MD50", "MD52", "MD60", "B505", "GAZL").any { code.startsWith(it) }) {
        return AircraftClass.HELICOPTER
    }

    // 3. Gliders / Sailplanes
    if (listOf("glider", "sailplane", "motor glider").any { descriptor.contains(it) } ||
        listOf("GLID", "ASK", "ASW", "ASH", "DG", "DISC", "VENT", "LS", "DUO", "NIMB", "S10", "ARCUS").any { code.startsWith(it) }) {
        return AircraftClass.GLIDER
    }

    // 4. Super-Heavy Double-Decker 4-engine
    if (listOf("A38", "A225").any { code.startsWith(it) }) {
        return AircraftClass.SUPER
    }

    // 5. Heavy 4-engine Jumbo Jet
    if (listOf("B74", "A34", "IL96", "IL76", "A124").any { code.startsWith(it) }) {
        return AircraftClass.JUMBO
    }

    // 6. Large Wide-Body Twinjet
    if (listOf("B77", "B78", "A33", "A35", "B76").any { code.startsWith(it) } ||
        listOf("widebody", "wide-body", "heavy twin").any { descriptor.contains(it) }) {
        return AircraftClass.WIDEBODY
    }

    // 7. Regional & Business Jets (rear fuselage mounted engines)
    if (listOf("CRJ", "E135", "E145", "E45X", "GLF", "GLEX", "GL5T", "CL3", "CL6", "C500", "C510", "C525", "C550", "C560", "C680", "C700", "C750", "LJ", "FA7X", "FA8X", "F900", "F2TH", "HA4T", "PC24", "PRM1", "H25B", "E50P", "E55P", "HONDA").any { code.startsWith(it) } ||
        listOf("business jet", "bizjet", "executive jet", "learjet", "citation", "gulfstream", "falcon", "global express").any { descriptor.contains(it) }) {
        return AircraftClass.REGIONAL_JET
    }

    // 8. Twin Turboprops & Commuters
    if (listOf("DH8", "AT4", "AT7", "ATR", "B190", "B350", "BE20", "BE30", "BE99", "BE19", "C408", "C441", "DHC6", "DHC7", "JS31", "JS32", "JS41", "SF34", "SW4", "SB20", "L410", "F50", "AN24", "AN26", "PC12", "C208", "DO228", "DO328").any { code.startsWith(it) } ||
        listOf("turboprop", "twin turboprop").any { descriptor.contains(it) }) {
        return AircraftClass.TURBOPROP
    }

    // 9. Narrow-Body Commercial Airliner (A320, B737, etc.)
    if (listOf("A31", "A32", "B73", "B75", "B71", "B72", "BCS", "A220", "E17", "E19", "E29", "MD8", "MD9", "C919", "SU95", "B712").any { code.startsWith(it) } ||
        listOf("airliner", "commercial jet", "passenger aircraft", "airbus", "boeing", "regional jet").any { descriptor.contains(it) }) {
        return AircraftClass.AIRLINER
    }

    // 10. Light General Aviation (single-engine prop)
    if (listOf("C15", "C17", "C18", "C20", "C21", "PA", "DA", "SR2", "P28", "M20", "BE33", "BE35", "BE36", "BE55", "BE58", "AA5", "TB20", "RV", "A210", "G109").any { code.startsWith(it) } ||
        listOf("light aircraft", "single engine", "cessna", "piper", "diamond aircraft", "cirrus", "mooney", "beechcraft", "ultralight").any { descriptor.contains(it) }) {
        return AircraftClass.LIGHT
    }

    return AircraftClass.UNKNOWN
}

val AircraftClass.shortLabel: String
    get() = when (this) {
        AircraftClass.AIRLINER -> "AIRLINER"
        AircraftClass.WIDEBODY -> "WIDEBODY"
        AircraftClass.JUMBO -> "B747"
        AircraftClass.SUPER -> "A380"
        AircraftClass.REGIONAL_JET -> "RJ/BIZ"
        AircraftClass.TURBOPROP -> "PROP"
        AircraftClass.HELICOPTER -> "HELI"
        AircraftClass.MILITARY -> "MIL"
        AircraftClass.LIGHT -> "LIGHT"
        AircraftClass.GLIDER -> "GLIDER"
        AircraftClass.UNKNOWN -> "AIRCRAFT"
    }
