package com.atchome.atc.api

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.util.concurrent.TimeUnit

data class GeocodeResult(
    val displayName: String,
    val lat: Double,
    val lon: Double
)

class NominatimGeocoder(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build(),
    private val userAgent: String = "AtcHome/1.0 (com.atchome.atc)"
) {
    suspend fun search(query: String, limit: Int = 5): Result<List<GeocodeResult>> =
        withContext(Dispatchers.IO) {
            try {
                val trimmed = query.trim()
                if (trimmed.isEmpty()) {
                    return@withContext Result.success(emptyList())
                }

                val url = "https://nominatim.openstreetmap.org/search".toHttpUrl()
                    .newBuilder()
                    .addQueryParameter("q", trimmed)
                    .addQueryParameter("format", "json")
                    .addQueryParameter("limit", limit.toString())
                    .addQueryParameter("addressdetails", "0")
                    .build()

                val request = Request.Builder()
                    .url(url)
                    .header("Accept", "application/json")
                    .header("User-Agent", userAgent)
                    .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        return@withContext Result.failure(
                            IllegalStateException("Geocode failed: HTTP ${response.code}")
                        )
                    }
                    val body = response.body?.string().orEmpty()
                    val results = parseResults(body)
                    Result.success(results)
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    private fun parseResults(body: String): List<GeocodeResult> {
        val array = JSONArray(body)
        val out = ArrayList<GeocodeResult>(array.length())
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            val lat = obj.optString("lat").toDoubleOrNull() ?: continue
            val lon = obj.optString("lon").toDoubleOrNull() ?: continue
            val name = obj.optString("display_name").ifBlank { "Unknown place" }
            out.add(GeocodeResult(displayName = name, lat = lat, lon = lon))
        }
        return out
    }
}
