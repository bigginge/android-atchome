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
        fun contains(value: String, query: String) =
            query.isBlank() || value.contains(query.trim(), ignoreCase = true)

        return contains("${aircraft.typeCode} ${aircraft.typeDescription}", type) &&
            (airlineCallsign.isBlank() || aircraft.callsign.startsWith(airlineCallsign.trim(), ignoreCase = true)) &&
            contains("${aircraft.origin} ${aircraft.originIcao}", origin) &&
            contains("${aircraft.destination} ${aircraft.destinationIcao}", destination)
    }

    fun summary(): String = listOfNotNull(
        type.takeIf { it.isNotBlank() }?.let { "TYPE $it" },
        airlineCallsign.takeIf { it.isNotBlank() }?.let { "CALL $it" },
        origin.takeIf { it.isNotBlank() }?.let { "FROM $it" },
        destination.takeIf { it.isNotBlank() }?.let { "TO $it" }
    ).joinToString(" • ")
}
