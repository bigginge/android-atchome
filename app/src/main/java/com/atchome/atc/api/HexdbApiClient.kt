package com.atchome.atc.api

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class AircraftEnrichment(
    val manufacturer: String = "",
    val typeDescription: String = "",
    val registeredOwner: String = "",
    val registration: String = ""
)

data class RouteEnrichment(
    val originIcao: String = "",
    val destinationIcao: String = ""
)

data class AirportEnrichment(
    val name: String = "",
    val lat: Double? = null,
    val lon: Double? = null
)

class HexdbApiClient(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(4, TimeUnit.SECONDS)
        .readTimeout(4, TimeUnit.SECONDS)
        .build()
) {
    /**
     * Enrich aircraft details (Manufacturer, Type, Registered Owners) by ICAO hex code.
     */
    suspend fun fetchAircraftDetails(hex: String): Result<AircraftEnrichment> = withContext(Dispatchers.IO) {
        if (hex.isBlank()) return@withContext Result.failure(IllegalArgumentException("Empty hex"))
        try {
            val url = "https://hexdb.io/api/v1/aircraft/$hex"
            val request = Request.Builder()
                .url(url)
                .header("Accept", "application/json")
                .header("User-Agent", "AtcHome-Android/1.0")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("HTTP ${response.code}"))
                }
                val body = response.body?.string() ?: return@withContext Result.failure(Exception("Empty body"))
                val json = JSONObject(body)

                val manufacturer = json.optString("Manufacturer").trim()
                val typeDescription = json.optString("Type").trim()
                val registeredOwner = json.optString("RegisteredOwners").trim()
                val registration = json.optString("Registration").trim()

                Result.success(
                    AircraftEnrichment(
                        manufacturer = sanitizeAscii(manufacturer),
                        typeDescription = sanitizeAscii(typeDescription),
                        registeredOwner = sanitizeAscii(registeredOwner),
                        registration = sanitizeAscii(registration)
                    )
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Enrich route (Origin and Destination ICAO codes) by callsign.
     */
    suspend fun fetchRoute(callsign: String): Result<RouteEnrichment> = withContext(Dispatchers.IO) {
        if (callsign.isBlank()) return@withContext Result.failure(IllegalArgumentException("Empty callsign"))
        try {
            val url = "https://hexdb.io/api/v1/route/icao/$callsign"
            val request = Request.Builder()
                .url(url)
                .header("Accept", "application/json")
                .header("User-Agent", "AtcHome-Android/1.0")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("HTTP ${response.code}"))
                }
                val body = response.body?.string() ?: return@withContext Result.failure(Exception("Empty body"))
                val json = JSONObject(body)
                val route = json.optString("route").trim()

                val hyphenIndex = route.indexOf('-')
                if (hyphenIndex <= 0 || hyphenIndex >= route.length - 1) {
                    return@withContext Result.failure(Exception("Invalid route format: $route"))
                }

                val originIcao = route.substring(0, hyphenIndex).trim()
                val destinationIcao = route.substring(hyphenIndex + 1).trim()

                Result.success(
                    RouteEnrichment(
                        originIcao = originIcao,
                        destinationIcao = destinationIcao
                    )
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Enrich airport name and coordinates by ICAO airport code.
     */
    suspend fun fetchAirport(icao: String): Result<AirportEnrichment> = withContext(Dispatchers.IO) {
        if (icao.isBlank()) return@withContext Result.failure(IllegalArgumentException("Empty icao"))
        try {
            val url = "https://hexdb.io/api/v1/airport/icao/$icao"
            val request = Request.Builder()
                .url(url)
                .header("Accept", "application/json")
                .header("User-Agent", "AtcHome-Android/1.0")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("HTTP ${response.code}"))
                }
                val body = response.body?.string() ?: return@withContext Result.failure(Exception("Empty body"))
                val json = JSONObject(body)

                val airport = json.optString("airport").trim()
                val countryCode = json.optString("country_code").trim()
                val lat = json.optDouble("latitude", Double.NaN).takeUnless { it.isNaN() }
                val lon = json.optDouble("longitude", Double.NaN).takeUnless { it.isNaN() }

                val name = when {
                    airport.isNotEmpty() && countryCode.isNotEmpty() ->
                        sanitizeAscii("$airport, $countryCode")
                    airport.isNotEmpty() -> sanitizeAscii(airport)
                    else -> icao
                }

                Result.success(AirportEnrichment(name = name, lat = lat, lon = lon))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun sanitizeAscii(input: String): String {
        return input.filter { it.code < 128 }
    }
}
