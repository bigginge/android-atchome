package com.atchome.atc.model

data class AircraftHighlightFilters(
    val type: String = "",
    val airline: String = "",
    val registration: String = "",
    val origin: String = "",
    val destination: String = ""
) {
    val isActive: Boolean
        get() = type.isNotBlank() || airline.isNotBlank() || registration.isNotBlank() ||
            origin.isNotBlank() || destination.isNotBlank()

    fun matches(aircraft: Aircraft): Boolean {
        return matches(aircraft.typeCode, aircraft.typeDescription, aircraft.registeredOwner, aircraft.registration, aircraft.origin, aircraft.originIcao, aircraft.destination, aircraft.destinationIcao)
    }

    fun matches(aircraft: RadarAircraft): Boolean {
        return matches(aircraft.typeCode, aircraft.typeDescription, aircraft.registeredOwner, aircraft.registration, aircraft.origin, aircraft.originIcao, aircraft.destination, aircraft.destinationIcao)
    }

    private fun matches(typeCode: String, typeDescription: String, airlineName: String, registration: String, origin: String, originIcao: String, destination: String, destinationIcao: String): Boolean {
        fun contains(value: String, query: String) =
            query.isBlank() || value.contains(query.trim(), ignoreCase = true)

        return contains("$typeCode $typeDescription", type) &&
            contains(airlineName, airline) &&
            contains(registration, this.registration) &&
            contains("$origin $originIcao", this.origin) &&
            contains("$destination $destinationIcao", this.destination)
    }

    fun summary(): String = listOfNotNull(
        type.takeIf { it.isNotBlank() }?.let { "TYPE $it" },
        airline.takeIf { it.isNotBlank() }?.let { "AIRLINE $it" },
        registration.takeIf { it.isNotBlank() }?.let { "REG $it" },
        origin.takeIf { it.isNotBlank() }?.let { "FROM $it" },
        destination.takeIf { it.isNotBlank() }?.let { "TO $it" }
    ).joinToString(" • ")
}
