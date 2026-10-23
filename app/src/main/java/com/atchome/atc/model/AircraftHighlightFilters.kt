package com.atchome.atc.model

data class AircraftHighlightFilters(
    val type: String = "",
    val airlineCallsign: String = "",
    val origin: String = "",
    val destination: String = ""
) {
    val isActive: Boolean
        get() = type.isNotBlank() || airlineCallsign.isNotBlank() ||
            origin.isNotBlank() || destination.isNotBlank()

    fun matches(aircraft: Aircraft): Boolean {
        return matches(aircraft.typeCode, aircraft.typeDescription, aircraft.callsign, aircraft.origin, aircraft.originIcao, aircraft.destination, aircraft.destinationIcao)
    }

    fun matches(aircraft: RadarAircraft): Boolean {
        return matches(aircraft.typeCode, aircraft.typeDescription, aircraft.callsign, aircraft.origin, aircraft.originIcao, aircraft.destination, aircraft.destinationIcao)
    }

    private fun matches(typeCode: String, typeDescription: String, callsign: String, origin: String, originIcao: String, destination: String, destinationIcao: String): Boolean {
        fun contains(value: String, query: String) =
            query.isBlank() || value.contains(query.trim(), ignoreCase = true)

        return contains("$typeCode $typeDescription", type) &&
            (airlineCallsign.isBlank() || callsign.startsWith(airlineCallsign.trim(), ignoreCase = true)) &&
            contains("$origin $originIcao", this.origin) &&
            contains("$destination $destinationIcao", this.destination)
    }

    fun summary(): String = listOfNotNull(
        type.takeIf { it.isNotBlank() }?.let { "TYPE $it" },
        airlineCallsign.takeIf { it.isNotBlank() }?.let { "CALL $it" },
        origin.takeIf { it.isNotBlank() }?.let { "FROM $it" },
        destination.takeIf { it.isNotBlank() }?.let { "TO $it" }
    ).joinToString(" • ")
}
