package com.atchome.atc.api

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

data class AircraftPhoto(
    val imageUrl: String,
    val photographer: String = "",
    val sourceTitle: String = ""
)

class AircraftImageApiClient(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(4, TimeUnit.SECONDS)
        .readTimeout(4, TimeUnit.SECONDS)
        .build()
) {
    /**
     * Fetch aircraft photo with cascading fallbacks:
     * 1. Planespotters.net (by 24-bit ICAO hex)
     * 2. Planespotters.net (by registration)
     * 3. adsbdb.com (by hex)
     * 4. Wikipedia aircraft type summary
     */
    suspend fun fetchAircraftPhoto(
        hex: String,
        registration: String = "",
        typeCode: String = "",
        typeDescription: String = ""
    ): Result<AircraftPhoto> = withContext(Dispatchers.IO) {
        // 1. Planespotters by Hex
        if (hex.isNotBlank()) {
            val hexPhoto = fetchPlanespotters("https://api.planespotters.net/pub/photos/hex/$hex")
            if (hexPhoto != null) return@withContext Result.success(hexPhoto)
        }

        // 2. Planespotters by Registration
        if (registration.isNotBlank()) {
            val regClean = registration.trim().replace("-", "")
            val regPhoto = fetchPlanespotters("https://api.planespotters.net/pub/photos/reg/$registration")
                ?: fetchPlanespotters("https://api.planespotters.net/pub/photos/reg/$regClean")
            if (regPhoto != null) return@withContext Result.success(regPhoto)
        }

        // 3. adsbdb.com by Hex
        if (hex.isNotBlank()) {
            val adsbdbPhoto = fetchAdsbDb(hex)
            if (adsbdbPhoto != null) return@withContext Result.success(adsbdbPhoto)
        }

        // 4. Wikipedia Type Summary Image
        val wikiPhoto = fetchWikipediaTypeImage(typeDescription, typeCode)
        if (wikiPhoto != null) return@withContext Result.success(wikiPhoto)

        Result.failure(Exception("No aircraft photo found"))
    }

    private fun fetchPlanespotters(url: String): AircraftPhoto? {
        return try {
            val request = Request.Builder()
                .url(url)
                .header("Accept", "application/json")
                .header("User-Agent", "AtcHome-Android/1.0 (+https://github.com/atchome/atc)")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val body = response.body?.string() ?: return null
                val json = JSONObject(body)
                val photos = json.optJSONArray("photos") ?: return null
                if (photos.length() == 0) return null

                val photo = photos.getJSONObject(0)
                val thumbLarge = photo.optJSONObject("thumbnail_large")?.optString("src")
                val thumb = photo.optJSONObject("thumbnail")?.optString("src")
                val imgUrl = if (!thumbLarge.isNullOrEmpty()) thumbLarge else thumb

                if (imgUrl.isNullOrEmpty()) return null
                val photographer = photo.optString("photographer").trim()

                AircraftPhoto(
                    imageUrl = imgUrl,
                    photographer = photographer,
                    sourceTitle = "Planespotters.net"
                )
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun fetchAdsbDb(hex: String): AircraftPhoto? {
        return try {
            val url = "https://api.adsbdb.com/v0/aircraft/$hex"
            val request = Request.Builder()
                .url(url)
                .header("Accept", "application/json")
                .header("User-Agent", "AtcHome-Android/1.0")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val body = response.body?.string() ?: return null
                val json = JSONObject(body)
                val responseObj = json.optJSONObject("response") ?: return null
                val aircraftObj = responseObj.optJSONObject("aircraft") ?: return null

                val photoThumb = aircraftObj.optString("url_photo_thumbnail")
                val photo = aircraftObj.optString("url_photo")
                val imgUrl = if (photo.isNotEmpty()) photo else photoThumb

                if (imgUrl.isNotEmpty() && imgUrl != "null") {
                    AircraftPhoto(
                        imageUrl = imgUrl,
                        photographer = "",
                        sourceTitle = "Airport-Data"
                    )
                } else null
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun fetchWikipediaTypeImage(typeDescription: String, typeCode: String): AircraftPhoto? {
        val query = deriveWikipediaPageTitle(typeDescription, typeCode) ?: return null
        return try {
            val encoded = URLEncoder.encode(query, "UTF-8")
            val url = "https://en.wikipedia.org/api/rest_v1/page/summary/$encoded"
            val request = Request.Builder()
                .url(url)
                .header("Accept", "application/json")
                .header("User-Agent", "AtcHome-Android/1.0 (+https://github.com/atchome/atc)")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val body = response.body?.string() ?: return null
                val json = JSONObject(body)

                val thumb = json.optJSONObject("thumbnail")?.optString("source")
                val orig = json.optJSONObject("originalimage")?.optString("source")
                val imgUrl = if (!thumb.isNullOrEmpty()) thumb else orig

                if (!imgUrl.isNullOrEmpty()) {
                    AircraftPhoto(
                        imageUrl = imgUrl,
                        photographer = "",
                        sourceTitle = "Wikipedia"
                    )
                } else null
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun deriveWikipediaPageTitle(typeDescription: String, typeCode: String): String? {
        val descUpper = typeDescription.uppercase()
        return when {
            descUpper.contains("A-320") || descUpper.contains("A320") || typeCode.equals("A320", ignoreCase = true) -> "Airbus A320"
            descUpper.contains("A-321") || descUpper.contains("A321") || typeCode.equals("A321", ignoreCase = true) -> "Airbus A321"
            descUpper.contains("A-319") || descUpper.contains("A319") || typeCode.equals("A319", ignoreCase = true) -> "Airbus A319"
            descUpper.contains("A-330") || descUpper.contains("A330") || typeCode.startsWith("A33", ignoreCase = true) -> "Airbus A330"
            descUpper.contains("A-350") || descUpper.contains("A350") || typeCode.startsWith("A35", ignoreCase = true) -> "Airbus A350"
            descUpper.contains("A-380") || descUpper.contains("A380") || typeCode.equals("A388", ignoreCase = true) -> "Airbus A380"
            descUpper.contains("737") || typeCode.startsWith("B73", ignoreCase = true) || typeCode.equals("B38M", ignoreCase = true) -> "Boeing 737"
            descUpper.contains("777") || typeCode.startsWith("B77", ignoreCase = true) -> "Boeing 777"
            descUpper.contains("787") || typeCode.startsWith("B78", ignoreCase = true) -> "Boeing 787 Dreamliner"
            descUpper.contains("747") || typeCode.startsWith("B74", ignoreCase = true) -> "Boeing 747"
            descUpper.contains("767") || typeCode.startsWith("B76", ignoreCase = true) -> "Boeing 767"
            descUpper.contains("E190") || descUpper.contains("E195") || typeCode.startsWith("E19", ignoreCase = true) -> "Embraer E-Jet family"
            descUpper.contains("CRJ") || typeCode.startsWith("CRJ", ignoreCase = true) -> "Bombardier CRJ"
            descUpper.contains("ATR 72") || descUpper.contains("AT7") || typeCode.startsWith("AT7", ignoreCase = true) -> "ATR 72"
            descUpper.contains("ATR 42") || descUpper.contains("AT4") || typeCode.startsWith("AT4", ignoreCase = true) -> "ATR 42"
            descUpper.contains("CESSNA 172") || typeCode.equals("C172", ignoreCase = true) -> "Cessna 172 Skyhawk"
            descUpper.contains("DA40") || typeCode.equals("DA40", ignoreCase = true) -> "Diamond DA40 Diamond Star"
            descUpper.contains("DA42") || typeCode.equals("DA42", ignoreCase = true) -> "Diamond DA42 Twin Star"
            typeDescription.isNotBlank() -> typeDescription
            else -> null
        }
    }
}
