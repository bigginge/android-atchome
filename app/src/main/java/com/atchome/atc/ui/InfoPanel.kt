package com.atchome.atc.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.FlightLand
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.CircularProgressIndicator
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.atchome.atc.model.Aircraft
import com.atchome.atc.model.aircraftClass
import com.atchome.atc.model.shortLabel
import com.atchome.atc.ui.theme.AtcColors
import com.atchome.atc.ui.theme.aircraftAltitudeColor
import java.util.Locale

@Composable
fun InfoPanel(
    selectedAircraft: Aircraft?,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .background(AtcColors.infoBackground)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // A quiet section title keeps attention on the selected flight.
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("FLIGHT DETAILS", fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.1.sp, color = AtcColors.textSecondary)
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (selectedAircraft == null) {
            // Empty State
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .padding(vertical = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Flight,
                        contentDescription = null,
                        tint = AtcColors.textSecondary.copy(alpha = 0.4f),
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No Aircraft Selected",
                        fontSize = 15.sp,
                        color = AtcColors.textSecondary,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Tap any blip on the radar or wait for traffic in range.",
                        fontSize = 12.sp,
                        color = AtcColors.textSecondary.copy(alpha = 0.7f),
                        lineHeight = 16.sp
                    )
                }
            }
        } else {
            // Aircraft Detail Content
            val typeText = selectedAircraft.typeDescription.ifEmpty { selectedAircraft.typeCode }
            val aircraftTitle = buildString {
                if (selectedAircraft.manufacturer.isNotEmpty()) append(selectedAircraft.manufacturer).append(" ")
                if (typeText.isNotEmpty()) append(typeText)
                if (isEmpty()) append("Unknown Aircraft")
            }

            // Aircraft Image Card (when loaded or loading)
            if (!selectedAircraft.imageUrl.isNullOrEmpty() || !selectedAircraft.imageLoaded) {
                AircraftImageCard(
                    imageUrl = selectedAircraft.imageUrl,
                    photographer = selectedAircraft.imagePhotographer,
                    isLoading = !selectedAircraft.imageLoaded
                )
                Spacer(modifier = Modifier.height(10.dp))
            }

            // 1. Aircraft Type & Registration Card
            InfoCard(title = "AIRCRAFT") {
                Text(
                    text = aircraftTitle,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AtcColors.textPrimary
                )
                Text(
                    text = "${selectedAircraft.aircraftClass().shortLabel}  ·  ${selectedAircraft.typeCode.ifEmpty { "TYPE UNKNOWN" }}",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = AtcColors.highlight,
                    modifier = Modifier.padding(top = 3.dp)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (selectedAircraft.registration.isNotEmpty()) {
                        Text(
                            text = "Reg: ${selectedAircraft.registration}",
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace,
                            color = AtcColors.highlight
                        )
                    } else {
                        Spacer(modifier = Modifier.width(1.dp))
                    }
                    Text(
                        text = "Age: ${selectedAircraft.formattedAge}",
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        color = if (selectedAircraft.ageYears != null) AtcColors.textPrimary else AtcColors.textSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 2. Flight / Operator Card
            InfoCard(title = "FLIGHT") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = selectedAircraft.callsign.ifEmpty { selectedAircraft.hex.uppercase() },
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = AtcColors.textPrimary
                    )
                    Text(
                        text = "HEX ${selectedAircraft.hex.uppercase()}",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = AtcColors.textSecondary
                    )
                }
                if (selectedAircraft.registeredOwner.isNotEmpty()) {
                    Text(
                        text = selectedAircraft.registeredOwner,
                        fontSize = 13.sp,
                        color = AtcColors.textSecondary,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 3. Telemetry Matrix (Distance, Altitude, Speed, Track)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TelemetryMetric(
                    label = "DISTANCE",
                    value = String.format(Locale.US, "%.1f NM", selectedAircraft.distanceNm),
                    modifier = Modifier.weight(1f)
                )
                val altText = if (!selectedAircraft.altitudeKnown) {
                    "ALT ?"
                } else if (selectedAircraft.altitudeFt > 0) {
                    if (selectedAircraft.vertRateFpm != 0) {
                        String.format(Locale.US, "%,d FT %s", selectedAircraft.altitudeFt, selectedAircraft.trendSymbol)
                    } else {
                        String.format(Locale.US, "%,d FT", selectedAircraft.altitudeFt)
                    }
                } else "GND"

                TelemetryMetric(
                    label = "ALTITUDE",
                    value = altText,
                    color = AtcColors.aircraftAltitudeColor(selectedAircraft.altitudeFt, selectedAircraft.altitudeKnown),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TelemetryMetric(
                    label = "SPEED",
                    value = String.format(Locale.US, "%.0f KTS", selectedAircraft.groundSpeedKts),
                    icon = Icons.Default.Speed,
                    modifier = Modifier.weight(1f)
                )
                TelemetryMetric(
                    label = "HEADING",
                    value = String.format(Locale.US, "%.0f°", selectedAircraft.trackDeg),
                    icon = Icons.Default.NearMe,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 4. Route Card (Origin & Destination)
            InfoCard(title = "ROUTE") {
                val hasOrigin = selectedAircraft.origin.isNotEmpty() || selectedAircraft.originIcao.isNotEmpty()
                val hasDest = selectedAircraft.destination.isNotEmpty() || selectedAircraft.destinationIcao.isNotEmpty()

                if (!hasOrigin && !hasDest) {
                    Text(
                        text = if (selectedAircraft.callsign.isEmpty()) "Route unavailable" else "Fetching route info…",
                        fontSize = 13.sp,
                        color = AtcColors.textSecondary
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        RoutePointRow(
                            icon = Icons.Default.FlightTakeoff,
                            label = "ORIGIN",
                            name = selectedAircraft.origin,
                            icao = selectedAircraft.originIcao,
                            distanceLabel = selectedAircraft.distanceFromOriginNm?.let {
                                String.format(Locale.US, "%.1f NM from origin", it)
                            }
                        )
                        RoutePointRow(
                            icon = Icons.Default.FlightLand,
                            label = "DESTINATION",
                            name = selectedAircraft.destination,
                            icao = selectedAircraft.destinationIcao,
                            distanceLabel = selectedAircraft.distanceToDestinationNm?.let {
                                String.format(Locale.US, "%.1f NM to destination", it)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoCard(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = AtcColors.cardBackground)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = AtcColors.textSecondary,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            content()
        }
    }
}

@Composable
private fun TelemetryMetric(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    color: Color? = null,
    icon: ImageVector? = null
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = AtcColors.cardBackground)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = AtcColors.textSecondary,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                }
                Text(
                    text = label,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = AtcColors.textSecondary
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = color ?: AtcColors.textPrimary
            )
        }
    }
}

@Composable
private fun RoutePointRow(
    icon: ImageVector,
    label: String,
    name: String,
    icao: String,
    distanceLabel: String? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = AtcColors.highlight,
            modifier = Modifier
                .padding(top = 2.dp)
                .size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = AtcColors.textSecondary
                )
                if (icao.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "[$icao]",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = AtcColors.highlight
                    )
                }
            }
            Text(
                text = name.ifEmpty { icao.ifEmpty { "—" } },
                fontSize = 13.sp,
                color = AtcColors.textPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (distanceLabel != null) {
                Text(
                    text = distanceLabel,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = AtcColors.highlight,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun AircraftImageCard(
    imageUrl: String?,
    photographer: String?,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = AtcColors.cardBackground)
    ) {
        if (!imageUrl.isNullOrEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(imageUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Aircraft Image",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .clip(RoundedCornerShape(8.dp))
                )

                if (!photographer.isNullOrEmpty()) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(6.dp)
                            .background(
                                color = Color.Black.copy(alpha = 0.65f),
                                shape = RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "Photo © $photographer",
                            fontSize = 10.sp,
                            color = Color.White
                        )
                    }
                }
            }
        } else if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = AtcColors.textSecondary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Fetching aircraft photo…",
                        fontSize = 12.sp,
                        color = AtcColors.textSecondary
                    )
                }
            }
        }
    }
}

