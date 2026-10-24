package com.atchome.atc.model

import org.junit.Assert.assertEquals
import org.junit.Test

class AircraftClassTest {

    @Test
    fun testFlightRadarClassification() {
        // Narrowbody commercial jets -> AIRLINER
        assertEquals(AircraftClass.AIRLINER, classifyAircraft("A320", "Airbus A320-214", "Airbus", "British Airways", "BAW123"))
        assertEquals(AircraftClass.AIRLINER, classifyAircraft("B738", "Boeing 737-800", "Boeing", "Ryanair", "RYR456"))
        assertEquals(AircraftClass.AIRLINER, classifyAircraft("BCS3", "Airbus A220-300", "Airbus", "Air Baltic", "BTI789"))
        assertEquals(AircraftClass.AIRLINER, classifyAircraft("E190", "Embraer E190", "Embraer", "KLM Cityhopper", "KLM101"))

        // Large widebody commercial twinjets -> WIDEBODY
        assertEquals(AircraftClass.WIDEBODY, classifyAircraft("B77W", "Boeing 777-300ER", "Boeing", "Emirates", "UAE1"))
        assertEquals(AircraftClass.WIDEBODY, classifyAircraft("A359", "Airbus A350-941", "Airbus", "Qatar Airways", "QTR2"))
        assertEquals(AircraftClass.WIDEBODY, classifyAircraft("B789", "Boeing 787-9", "Boeing", "Virgin Atlantic", "VIR3"))
        assertEquals(AircraftClass.WIDEBODY, classifyAircraft("A333", "Airbus A330-300", "Airbus", "Lufthansa", "DLH4"))

        // Heavy 4-engine Jumbo jets -> JUMBO
        assertEquals(AircraftClass.JUMBO, classifyAircraft("B744", "Boeing 747-400", "Boeing", "Lufthansa", "DLH5"))
        assertEquals(AircraftClass.JUMBO, classifyAircraft("B748", "Boeing 747-8F", "Boeing", "Cargolux", "CLX6"))
        assertEquals(AircraftClass.JUMBO, classifyAircraft("A346", "Airbus A340-600", "Airbus", "Lufthansa", "DLH7"))

        // Super-heavy double deckers -> SUPER
        assertEquals(AircraftClass.SUPER, classifyAircraft("A388", "Airbus A380-842", "Airbus", "Emirates", "UAE8"))

        // Regional & business jets with rear engines -> REGIONAL_JET
        assertEquals(AircraftClass.REGIONAL_JET, classifyAircraft("CRJ9", "Canadair Regional Jet CRJ-900", "Bombardier", "Lufthansa CityLine", "LH9"))
        assertEquals(AircraftClass.REGIONAL_JET, classifyAircraft("E145", "Embraer ERJ-145", "Embraer", "Loganair", "LOG10"))
        assertEquals(AircraftClass.REGIONAL_JET, classifyAircraft("GLF6", "Gulfstream G650", "Gulfstream", "Private", "N123GA"))
        assertEquals(AircraftClass.REGIONAL_JET, classifyAircraft("C560", "Cessna Citation V", "Cessna", "ExecJet", "EJA11"))

        // Twin turboprops & commuters -> TURBOPROP
        assertEquals(AircraftClass.TURBOPROP, classifyAircraft("DH8D", "De Havilland Dash 8-400", "Bombardier", "Flybe", "BEE12"))
        assertEquals(AircraftClass.TURBOPROP, classifyAircraft("AT76", "ATR 72-600", "ATR", "Aurigny", "AUR13"))
        assertEquals(AircraftClass.TURBOPROP, classifyAircraft("B350", "Beechcraft Super King Air 350", "Beechcraft", "Private", "GKING"))
        assertEquals(AircraftClass.TURBOPROP, classifyAircraft("B190", "Beechcraft 1900D", "Beechcraft", "Airline", "BEE190"))

        // Light general aviation single-engine prop -> LIGHT
        assertEquals(AircraftClass.LIGHT, classifyAircraft("C172", "Cessna 172S Skyhawk", "Cessna", "Flying Club", "GBOAC"))
        assertEquals(AircraftClass.LIGHT, classifyAircraft("P28A", "Piper PA-28-181 Archer", "Piper", "Private", "GPIPR"))
        assertEquals(AircraftClass.LIGHT, classifyAircraft("SR22", "Cirrus SR22", "Cirrus", "Private", "GSR22"))

        // Rotorcraft / Helicopters -> HELICOPTER
        assertEquals(AircraftClass.HELICOPTER, classifyAircraft("R44", "Robinson R44 Raven II", "Robinson", "Helicopters Ltd", "GHELI"))
        assertEquals(AircraftClass.HELICOPTER, classifyAircraft("EC35", "Eurocopter EC135", "Airbus Helicopters", "Police Air Support", "UKP1"))
        assertEquals(AircraftClass.HELICOPTER, classifyAircraft("AW139", "AgustaWestland AW139", "Leonardo", "Coastguard", "RESCUE"))

        // Military combat & fighters -> MILITARY
        assertEquals(AircraftClass.MILITARY, classifyAircraft("F16", "General Dynamics F-16 Fighting Falcon", "Lockheed", "USAF", "VIPER1"))
        assertEquals(AircraftClass.MILITARY, classifyAircraft("EUFI", "Eurofighter Typhoon", "BAE Systems", "Royal Air Force", "TYPH1"))
        assertEquals(AircraftClass.MILITARY, classifyAircraft("F35", "Lockheed Martin F-35 Lightning II", "Lockheed", "RAF", "LIGHT1"))
        assertEquals(AircraftClass.MILITARY, classifyAircraft("C17", "Boeing C-17 Globemaster III", "Boeing", "USAF", "RCH456"))

        // Gliders -> GLIDER
        assertEquals(AircraftClass.GLIDER, classifyAircraft("ASK21", "Schleicher ASK 21", "Schleicher", "Gliding Club", "GGLID"))
        assertEquals(AircraftClass.GLIDER, classifyAircraft("GLID", "Sailplane", "Schempp-Hirth", "Private", "GDISC"))

        // Unknown / unclassified -> UNKNOWN
        assertEquals(AircraftClass.UNKNOWN, classifyAircraft("", "", "", "", ""))
    }

    @Test
    fun testShortLabels() {
        assertEquals("AIRLINER", AircraftClass.AIRLINER.shortLabel)
        assertEquals("WIDEBODY", AircraftClass.WIDEBODY.shortLabel)
        assertEquals("B747", AircraftClass.JUMBO.shortLabel)
        assertEquals("A380", AircraftClass.SUPER.shortLabel)
        assertEquals("RJ/BIZ", AircraftClass.REGIONAL_JET.shortLabel)
        assertEquals("PROP", AircraftClass.TURBOPROP.shortLabel)
        assertEquals("LIGHT", AircraftClass.LIGHT.shortLabel)
        assertEquals("HELI", AircraftClass.HELICOPTER.shortLabel)
        assertEquals("MIL", AircraftClass.MILITARY.shortLabel)
        assertEquals("GLIDER", AircraftClass.GLIDER.shortLabel)
        assertEquals("AIRCRAFT", AircraftClass.UNKNOWN.shortLabel)
    }
}
