package com.atchome.atc.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.atchome.atc.data.AppSettings
import com.atchome.atc.ui.theme.CardBackground
import com.atchome.atc.ui.theme.InfoBackground
import com.atchome.atc.ui.theme.TextHighlight
import com.atchome.atc.ui.theme.TextPrimary
import com.atchome.atc.ui.theme.TextSecondary
import java.util.Locale

@Composable
fun SettingsDialog(
    initialSettings: AppSettings,
    onDismiss: () -> Unit,
    onSave: (AppSettings) -> Unit
) {
    var latText by remember { mutableStateOf(String.format(Locale.US, "%.6f", initialSettings.homeLat)) }
    var lonText by remember { mutableStateOf(String.format(Locale.US, "%.6f", initialSettings.homeLon)) }
    var radiusNm by remember { mutableFloatStateOf(initialSettings.radiusNm.toFloat()) }
    var refreshIntervalSec by remember { mutableFloatStateOf(initialSettings.refreshIntervalSeconds.toFloat()) }
    var keepScreenOn by remember { mutableStateOf(initialSettings.keepScreenOn) }
    var showTrails by remember { mutableStateOf(initialSettings.showTrails) }
    var showRangeRings by remember { mutableStateOf(initialSettings.showRangeRings) }
    var showAirports by remember { mutableStateOf(initialSettings.showAirports) }
    var showSmallAirports by remember { mutableStateOf(initialSettings.showSmallAirports) }
    var showMediumAirports by remember { mutableStateOf(initialSettings.showMediumAirports) }
    var showLargeAirports by remember { mutableStateOf(initialSettings.showLargeAirports) }
    var showSweep by remember { mutableStateOf(initialSettings.showSweep) }
    var showVelocityVectors by remember { mutableStateOf(initialSettings.showVelocityVectors) }
    var dataTagMode by remember { mutableStateOf(initialSettings.dataTagMode) }
    var isMapPickerOpen by remember { mutableStateOf(false) }

    if (isMapPickerOpen) {
        MapLocationPickerDialog(
            initialLat = latText.toDoubleOrNull() ?: initialSettings.homeLat,
            initialLon = lonText.toDoubleOrNull() ?: initialSettings.homeLon,
            onDismiss = { isMapPickerOpen = false },
            onLocationConfirmed = { newLat, newLon ->
                latText = String.format(Locale.US, "%.6f", newLat)
                lonText = String.format(Locale.US, "%.6f", newLon)
                isMapPickerOpen = false
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = InfoBackground,
        shape = RoundedCornerShape(12.dp),
        title = {
            Text(
                text = "ATC Radar Settings",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Section: Home Location
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "HOME LOCATION",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = TextHighlight,
                        letterSpacing = 0.5.sp
                    )

                    OutlinedButton(
                        onClick = { isMapPickerOpen = true },
                        shape = RoundedCornerShape(6.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextHighlight),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Map,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Pick on Map", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = latText,
                        onValueChange = { latText = it },
                        label = { Text("Latitude", fontSize = 12.sp) },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                    OutlinedTextField(
                        value = lonText,
                        onValueChange = { lonText = it },
                        label = { Text("Longitude", fontSize = 12.sp) },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Search Radius Slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "SEARCH RADIUS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = TextHighlight
                    )
                    Text(
                        text = "${radiusNm.toInt()} NM",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = TextPrimary
                    )
                }
                Slider(
                    value = radiusNm,
                    onValueChange = { radiusNm = it },
                    valueRange = 5f..100f,
                    steps = 18,
                    colors = SliderDefaults.colors(
                        thumbColor = TextHighlight,
                        activeTrackColor = TextHighlight
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Refresh Interval Slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "POLL INTERVAL",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = TextHighlight
                    )
                    Text(
                        text = "${refreshIntervalSec.toInt()}s",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = TextPrimary
                    )
                }
                Slider(
                    value = refreshIntervalSec,
                    onValueChange = { refreshIntervalSec = it },
                    valueRange = 5f..60f,
                    steps = 10,
                    colors = SliderDefaults.colors(
                        thumbColor = TextHighlight,
                        activeTrackColor = TextHighlight
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Data Tag Mode Selector
                Text(
                    text = "AIRCRAFT DATA TAGS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = TextHighlight,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CardBackground, RoundedCornerShape(6.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val modes = listOf(
                        "SELECTED" to "Selected",
                        "COMPACT" to "Compact",
                        "FULL" to "Full Tags"
                    )
                    modes.forEach { (modeKey, modeLabel) ->
                        val isSelected = dataTagMode == modeKey
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (isSelected) TextHighlight else CardBackground)
                                .clickable { dataTagMode = modeKey }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = modeLabel,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) CardBackground else TextSecondary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Feature Switches
                SettingSwitchRow(
                    title = "Radar Sweep Beam",
                    subtitle = "Continuous rotating radar scan with phosphor decay",
                    checked = showSweep,
                    onCheckedChange = { showSweep = it }
                )

                SettingSwitchRow(
                    title = "Velocity Predictors",
                    subtitle = "1-minute heading & speed projection lines",
                    checked = showVelocityVectors,
                    onCheckedChange = { showVelocityVectors = it }
                )

                SettingSwitchRow(
                    title = "Keep Screen On",
                    subtitle = "Recommended for Fire tablet stand/kiosk",
                    checked = keepScreenOn,
                    onCheckedChange = { keepScreenOn = it }
                )

                SettingSwitchRow(
                    title = "Aircraft Trails",
                    subtitle = "Show altitude-faded historical breadcrumbs",
                    checked = showTrails,
                    onCheckedChange = { showTrails = it }
                )

                SettingSwitchRow(
                    title = "Range Rings",
                    subtitle = "Draw concentric radar distance rings & rose",
                    checked = showRangeRings,
                    onCheckedChange = { showRangeRings = it }
                )

                SettingSwitchRow(
                    title = "Airports",
                    subtitle = "Show commercial airports in range",
                    checked = showAirports,
                    onCheckedChange = { showAirports = it }
                )

                SettingSwitchRow(
                    title = "Large Airports",
                    subtitle = "International hubs and major airports",
                    checked = showLargeAirports,
                    onCheckedChange = { showLargeAirports = it }
                )

                SettingSwitchRow(
                    title = "Medium Airports",
                    subtitle = "Regional and city airports",
                    checked = showMediumAirports,
                    onCheckedChange = { showMediumAirports = it }
                )

                SettingSwitchRow(
                    title = "Small Airports",
                    subtitle = "Smaller airports with scheduled airline service",
                    checked = showSmallAirports,
                    onCheckedChange = { showSmallAirports = it }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val lat = latText.toDoubleOrNull() ?: initialSettings.homeLat
                    val lon = lonText.toDoubleOrNull() ?: initialSettings.homeLon
                    onSave(
                        initialSettings.copy(
                            homeLat = lat,
                            homeLon = lon,
                            radiusNm = radiusNm.toInt(),
                            refreshIntervalSeconds = refreshIntervalSec.toInt(),
                            keepScreenOn = keepScreenOn,
                            showTrails = showTrails,
                            showRangeRings = showRangeRings,
                            showAirports = showAirports,
                            showSmallAirports = showSmallAirports,
                            showMediumAirports = showMediumAirports,
                            showLargeAirports = showLargeAirports,
                            showSweep = showSweep,
                            showVelocityVectors = showVelocityVectors,
                            dataTagMode = dataTagMode
                        )
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = TextHighlight)
            ) {
                Text(text = "Save & Apply", color = CardBackground, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Cancel", color = TextSecondary)
            }
        }
    )
}

@Composable
private fun SettingSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontSize = 13.sp, color = TextPrimary, fontWeight = FontWeight.Medium)
            Text(text = subtitle, fontSize = 11.sp, color = TextSecondary)
        }
        Spacer(modifier = Modifier.width(8.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = TextHighlight,
                checkedTrackColor = CardBackground
            )
        )
    }
}
