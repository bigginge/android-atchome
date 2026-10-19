package com.atchome.atc.ui

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.location.LocationManager
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.DialogProperties
import com.atchome.atc.api.GeocodeResult
import com.atchome.atc.api.NominatimGeocoder
import com.atchome.atc.ui.theme.CardBackground
import com.atchome.atc.ui.theme.InfoBackground
import com.atchome.atc.ui.theme.TextHighlight
import com.atchome.atc.ui.theme.TextPrimary
import com.atchome.atc.ui.theme.TextSecondary
import kotlinx.coroutines.launch
import java.util.Locale

class MapLocationBridge(
    private val onPositionChanged: (Double, Double) -> Unit
) {
    @JavascriptInterface
    fun onMarkerMoved(lat: Double, lng: Double) {
        onPositionChanged(lat, lng)
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun MapLocationPickerDialog(
    initialLat: Double,
    initialLon: Double,
    onDismiss: () -> Unit,
    onLocationConfirmed: (lat: Double, lon: Double) -> Unit
) {
    var selectedLat by remember { mutableDoubleStateOf(initialLat) }
    var selectedLon by remember { mutableDoubleStateOf(initialLon) }
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<GeocodeResult>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    var searchMessage by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val scope = rememberCoroutineScope()
    val geocoder = remember { NominatimGeocoder() }
    val mapWebViewRef = remember { arrayOfNulls<WebView>(1) }

    fun moveMapTo(lat: Double, lon: Double) {
        selectedLat = lat
        selectedLon = lon
        mapWebViewRef[0]?.evaluateJavascript(
            "window.setPickerLocation($lat, $lon);",
            null
        )
    }

    fun runSearch() {
        val query = searchQuery.trim()
        if (query.isEmpty() || isSearching) return
        keyboardController?.hide()
        isSearching = true
        searchMessage = null
        searchResults = emptyList()
        scope.launch {
            val result = geocoder.search(query)
            isSearching = false
            result.fold(
                onSuccess = { places ->
                    searchResults = places
                    searchMessage = if (places.isEmpty()) "No places found" else null
                    if (places.size == 1) {
                        moveMapTo(places[0].lat, places[0].lon)
                        searchResults = emptyList()
                    }
                },
                onFailure = {
                    searchMessage = "Search failed — check network"
                }
            )
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .fillMaxWidth(0.92f)
            .height(580.dp),
        containerColor = InfoBackground,
        shape = RoundedCornerShape(14.dp),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "SELECT BASE LOCATION",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = TextHighlight,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Search, tap, or drag the pin to set your radar center",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }

                IconButton(
                    onClick = {
                        try {
                            val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
                            val lastGps = lm?.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                                ?: lm?.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                            if (lastGps != null) {
                                searchResults = emptyList()
                                searchMessage = null
                                moveMapTo(lastGps.latitude, lastGps.longitude)
                            }
                        } catch (_: SecurityException) {
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.MyLocation,
                        contentDescription = "My GPS Location",
                        tint = TextHighlight
                    )
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = {
                            searchQuery = it
                            if (searchResults.isNotEmpty()) searchResults = emptyList()
                            if (searchMessage != null) searchMessage = null
                        },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        placeholder = {
                            Text("Search town, airport, postcode…", fontSize = 12.sp)
                        },
                        textStyle = androidx.compose.ui.text.TextStyle(
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace,
                            color = TextPrimary
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TextHighlight,
                            unfocusedBorderColor = TextSecondary.copy(alpha = 0.45f),
                            cursorColor = TextHighlight,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedPlaceholderColor = TextSecondary,
                            unfocusedPlaceholderColor = TextSecondary,
                            focusedContainerColor = CardBackground,
                            unfocusedContainerColor = CardBackground
                        ),
                        shape = RoundedCornerShape(8.dp),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { runSearch() }),
                        trailingIcon = {
                            if (isSearching) {
                                CircularProgressIndicator(
                                    modifier = Modifier
                                        .padding(end = 10.dp)
                                        .size(18.dp),
                                    color = TextHighlight,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                IconButton(onClick = { runSearch() }) {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = "Search",
                                        tint = TextHighlight
                                    )
                                }
                            }
                        }
                    )
                }

                if (searchMessage != null) {
                    Text(
                        text = searchMessage!!,
                        fontSize = 11.sp,
                        color = TextSecondary,
                        modifier = Modifier.padding(top = 6.dp, start = 2.dp)
                    )
                }

                if (searchResults.isNotEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp)
                            .heightIn(max = 120.dp)
                            .background(CardBackground, RoundedCornerShape(8.dp))
                            .verticalScroll(rememberScrollState())
                    ) {
                        searchResults.forEachIndexed { index, place ->
                            Text(
                                text = place.displayName,
                                fontSize = 12.sp,
                                color = TextPrimary,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        moveMapTo(place.lat, place.lon)
                                        searchResults = emptyList()
                                        searchQuery = place.displayName
                                            .substringBefore(',')
                                            .trim()
                                    }
                                    .padding(horizontal = 12.dp, vertical = 10.dp)
                            )
                            if (index < searchResults.lastIndex) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(1.dp)
                                        .background(TextSecondary.copy(alpha = 0.2f))
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Interactive Leaflet OpenStreetMap in WebView.
                // Do not use Modifier.clip() here — clipping a WebView in Compose
                // commonly produces an opaque black surface on Android / Fire OS.
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(CardBackground, RoundedCornerShape(8.dp))
                ) {
                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { ctx ->
                            WebView(ctx).apply {
                                layoutParams = ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                                setBackgroundColor(Color.TRANSPARENT)

                                settings.javaScriptEnabled = true
                                settings.domStorageEnabled = true
                                settings.cacheMode = WebSettings.LOAD_DEFAULT
                                settings.allowFileAccess = true
                                settings.allowContentAccess = true
                                @Suppress("DEPRECATION")
                                settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW

                                // OSM rejects generic WebView User-Agents with HTTP 403,
                                // which leaves only the dark map CSS background visible.
                                val versionName = runCatching {
                                    ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName
                                }.getOrNull() ?: "1.0"
                                settings.userAgentString =
                                    "AtcHome/$versionName (${ctx.packageName})"

                                webViewClient = object : WebViewClient() {
                                    override fun onPageFinished(view: WebView?, url: String?) {
                                        view?.evaluateJavascript(
                                            "if (window.map && window.map.invalidateSize) { window.map.invalidateSize(true); }",
                                            null
                                        )
                                    }
                                }

                                val bridge = MapLocationBridge { lat, lng ->
                                    post {
                                        selectedLat = lat
                                        selectedLon = lng
                                    }
                                }
                                addJavascriptInterface(bridge, "AndroidBridge")

                                val html = buildMapHtml(initialLat, initialLon)
                                loadDataWithBaseURL(
                                    "file:///android_asset/",
                                    html,
                                    "text/html",
                                    "UTF-8",
                                    null
                                )

                                mapWebViewRef[0] = this
                            }
                        },
                        onRelease = { webView ->
                            if (mapWebViewRef[0] === webView) {
                                mapWebViewRef[0] = null
                            }
                            webView.removeJavascriptInterface("AndroidBridge")
                            webView.destroy()
                        }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CardBackground, RoundedCornerShape(6.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = String.format(Locale.US, "LAT: %.6f", selectedLat),
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = String.format(Locale.US, "LON: %.6f", selectedLon),
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onLocationConfirmed(selectedLat, selectedLon) },
                colors = ButtonDefaults.buttonColors(containerColor = TextHighlight)
            ) {
                Text(
                    text = "Apply Base Location",
                    color = CardBackground,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Cancel", color = TextSecondary)
            }
        }
    )
}

private fun buildMapHtml(lat: Double, lon: Double): String {
    return """
        <!DOCTYPE html>
        <html>
        <head>
          <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
          <link rel="stylesheet" href="leaflet.css" />
          <script src="leaflet.js"></script>
          <style>
            html, body, #map { margin: 0; padding: 0; width: 100%; height: 100%; background: #E8EEF4; }
            .leaflet-container { font-family: monospace; background: #E8EEF4; }
          </style>
        </head>
        <body>
          <div id="map"></div>
          <script>
            delete L.Icon.Default.prototype._getIconUrl;
            L.Icon.Default.mergeOptions({
              iconUrl: 'marker-icon.png',
              iconRetinaUrl: 'marker-icon-2x.png',
              shadowUrl: 'marker-shadow.png',
            });

            var startLat = $lat;
            var startLon = $lon;
            window.map = L.map('map', { zoomControl: true }).setView([startLat, startLon], 10);

            L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', {
                maxZoom: 19,
                attribution: '&copy; OpenStreetMap'
            }).addTo(window.map);

            var marker = L.marker([startLat, startLon], { draggable: true }).addTo(window.map);

            function notifyNative(lat, lng) {
              if (window.AndroidBridge && window.AndroidBridge.onMarkerMoved) {
                window.AndroidBridge.onMarkerMoved(lat, lng);
              }
            }

            window.setPickerLocation = function(lat, lng) {
              marker.setLatLng([lat, lng]);
              window.map.setView([lat, lng], Math.max(window.map.getZoom(), 12));
              notifyNative(lat, lng);
            };

            marker.on('dragend', function(e) {
              var pos = marker.getLatLng();
              notifyNative(pos.lat, pos.lng);
            });

            window.map.on('click', function(e) {
              marker.setLatLng(e.latlng);
              notifyNative(e.latlng.lat, e.latlng.lng);
            });

            setTimeout(function() { window.map.invalidateSize(true); }, 100);
            setTimeout(function() { window.map.invalidateSize(true); }, 400);
          </script>
        </body>
        </html>
    """.trimIndent()
}
