package com.atchome.atc.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.atchome.atc.model.Aircraft
import com.atchome.atc.ui.theme.AtcColors
import com.atchome.atc.ui.theme.aircraftAltitudeColor
import com.atchome.atc.viewmodel.AtcViewModel
import java.util.Locale

@Composable
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
fun AtcScreen(viewModel: AtcViewModel, modifier: Modifier = Modifier) {
    val uiState by viewModel.uiState.collectAsState()
    val palette = AtcColors
    var showPhoneDetails by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.fillMaxSize().background(palette.radarBackground)
    ) {
        ConsoleTopBar(
            status = uiState.statusMessage,
            isScanning = uiState.isScanning,
            aircraftCount = uiState.aircraft.size,
            rangeNm = uiState.settings.radiusNm,
            onOpenSettings = { viewModel.openSettings() }
        )

        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val isLandscape = maxWidth > maxHeight
            val availableHeight = maxHeight
            val availableWidth = maxWidth
            if (isLandscape) {
                Row(Modifier.fillMaxSize().padding(horizontal = 10.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    RadarView(
                        radarAircraft = viewModel.radarAircraft,
                        selectedAircraft = uiState.selectedAircraft,
                        airports = uiState.airports,
                        rangeNm = uiState.settings.radiusNm.toFloat(),
                        showTrails = uiState.settings.showTrails,
                        showRangeRings = uiState.settings.showRangeRings,
                        showAirports = uiState.settings.showAirports,
                        showSweep = uiState.settings.showSweep,
                        reduceMotion = uiState.settings.reduceMotion,
                        dataTagMode = uiState.settings.dataTagMode,
                        highlightFilters = uiState.highlightFilters,
                        modifier = Modifier.weight(1.7f).fillMaxHeight().clip(RoundedCornerShape(18.dp)),
                        onSelectAt = { east, north, radius -> viewModel.selectNearestTo(east, north, radius) },
                        onZoomIn = { viewModel.zoomIn() },
                        onZoomOut = { viewModel.zoomOut() },
                        onCycleDataTagMode = { viewModel.cycleDataTagMode() },
                        onOpenHighlightFilters = { viewModel.openHighlightFilters() }
                    )
                    Column(
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TrafficList(
                            aircraft = uiState.aircraft,
                            selectedHex = uiState.selectedAircraft?.hex,
                            onSelect = viewModel::selectByHex,
                            modifier = Modifier.fillMaxWidth().height((availableHeight * 0.34f).coerceIn(132.dp, 220.dp))
                        )
                        InfoPanel(
                            selectedAircraft = uiState.selectedAircraft,
                            modifier = Modifier.weight(1f).fillMaxWidth()
                        )
                    }
                }
            } else {
                Column(Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    RadarView(
                        radarAircraft = viewModel.radarAircraft,
                        selectedAircraft = uiState.selectedAircraft,
                        airports = uiState.airports,
                        rangeNm = uiState.settings.radiusNm.toFloat(),
                        showTrails = uiState.settings.showTrails,
                        showRangeRings = uiState.settings.showRangeRings,
                        showAirports = uiState.settings.showAirports,
                        showSweep = uiState.settings.showSweep,
                        reduceMotion = uiState.settings.reduceMotion,
                        dataTagMode = uiState.settings.dataTagMode,
                        highlightFilters = uiState.highlightFilters,
                    modifier = Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(18.dp)),
                        onSelectAt = { east, north, radius ->
                            viewModel.selectNearestTo(east, north, radius)
                            showPhoneDetails = true
                        },
                        onZoomIn = { viewModel.zoomIn() },
                        onZoomOut = { viewModel.zoomOut() },
                        onCycleDataTagMode = { viewModel.cycleDataTagMode() },
                        onOpenHighlightFilters = { viewModel.openHighlightFilters() }
                    )
                    TrafficList(
                        aircraft = uiState.aircraft,
                        selectedHex = uiState.selectedAircraft?.hex,
                        onSelect = { hex ->
                            viewModel.selectByHex(hex)
                            showPhoneDetails = true
                        },
                        modifier = Modifier.fillMaxWidth().height(92.dp),
                        horizontal = true
                    )
                    SelectedFlightStrip(
                        selectedAircraft = uiState.selectedAircraft,
                    modifier = Modifier.height(48.dp).fillMaxWidth(),
                        onClick = { showPhoneDetails = true }
                    )
                }
            }
        }
    }

    if (uiState.isSettingsDialogOpen) {
        SettingsDialog(
            initialSettings = uiState.settings,
            onDismiss = { viewModel.closeSettings() },
            onSave = viewModel::saveSettings
        )
    }
    if (showPhoneDetails && uiState.selectedAircraft != null) {
        ModalBottomSheet(
            onDismissRequest = { showPhoneDetails = false },
            containerColor = palette.infoBackground,
            contentColor = palette.textPrimary
        ) {
            InfoPanel(
                selectedAircraft = uiState.selectedAircraft,
                modifier = Modifier.fillMaxWidth().heightIn(max = 560.dp)
            )
        }
    }
    if (uiState.isHighlightFilterSheetOpen) {
        AircraftHighlightFilterSheet(
            filters = uiState.highlightFilters,
            aircraft = uiState.aircraft,
            onFiltersChange = viewModel::updateHighlightFilters,
            onClear = { viewModel.clearHighlightFilters() },
            onDismiss = { viewModel.closeHighlightFilters() }
        )
    }
}

@Composable
private fun SelectedFlightStrip(
    selectedAircraft: Aircraft?,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val colors = AtcColors
    val flightLabel = selectedAircraft?.let { it.callsign.ifBlank { it.hex.uppercase(Locale.US) } } ?: "Tap a target to inspect"
    Surface(modifier = modifier.clickable(onClick = onClick), color = colors.infoBackground, shape = RoundedCornerShape(14.dp)) {
        Row(Modifier.fillMaxSize().padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("SELECTED FLIGHT", color = colors.textSecondary, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.9.sp)
                Text(
                    flightLabel,
                    color = colors.textPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (selectedAircraft != null) {
                Text(selectedAircraft.formattedAltitude, color = colors.highlight, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Spacer(Modifier.width(8.dp))
                Text("DETAILS  ›", color = colors.textSecondary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun ConsoleTopBar(
    status: String,
    isScanning: Boolean,
    aircraftCount: Int,
    rangeNm: Int,
    onOpenSettings: () -> Unit
) {
    val colors = AtcColors
    Surface(color = colors.infoBackground) {
        Row(
            modifier = Modifier.fillMaxWidth().height(42.dp).padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(Modifier.weight(1f)) {
                Text("ATC HOME", color = colors.textPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(7.dp).background(if (isScanning) colors.amber else colors.green, CircleShape))
                    Spacer(Modifier.width(6.dp))
                    val statusLabel = when {
                        isScanning -> "UPDATING"
                        status.startsWith("Fetch failed", ignoreCase = true) -> "FETCH FAILED"
                        status.startsWith("Refresh error", ignoreCase = true) -> "STALE DATA"
                        status.startsWith("No aircraft", ignoreCase = true) -> "NO TRAFFIC"
                        status.startsWith("Connecting", ignoreCase = true) -> "CONNECTING"
                        else -> "LIVE DATA"
                    }
                    Text(statusLabel, color = colors.textSecondary, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            Text("$aircraftCount AIRCRAFT", color = colors.highlight, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, maxLines = 1)
            Text("${rangeNm} NM", Modifier.clip(RoundedCornerShape(10.dp)).background(colors.cardBackground).padding(horizontal = 10.dp, vertical = 7.dp), color = colors.textPrimary, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 10.sp)
            IconButton(onClick = onOpenSettings, modifier = Modifier.size(40.dp)) {
                Icon(Icons.Default.Settings, contentDescription = "Settings", tint = colors.textSecondary)
            }
        }
    }
}

@Composable
private fun TrafficList(
    aircraft: List<Aircraft>,
    selectedHex: String?,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    horizontal: Boolean = false
) {
    val colors = AtcColors
    Surface(modifier = modifier, color = colors.infoBackground, shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("NEARBY TRAFFIC", color = colors.textSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Spacer(Modifier.weight(1f))
                Text("${aircraft.size}", color = colors.highlight, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            }
            Spacer(Modifier.height(5.dp))
            if (horizontal) {
                androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(aircraft, key = { it.hex }) { ac -> TrafficItem(ac, selectedHex == ac.hex, onSelect) }
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(aircraft, key = { it.hex }) { ac -> TrafficItem(ac, selectedHex == ac.hex, onSelect) }
                }
            }
        }
    }
}

@Composable
private fun TrafficItem(aircraft: Aircraft, selected: Boolean, onSelect: (String) -> Unit) {
    val colors = AtcColors
    val bg = if (selected) colors.cardBackground else colors.infoBackground
    val title = aircraft.callsign.ifBlank { aircraft.hex.uppercase(Locale.US) }
    Row(
        Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .border(if (selected) 1.dp else 0.dp, if (selected) colors.highlight else bg, RoundedCornerShape(10.dp))
            .clickable { onSelect(aircraft.hex) }
            .padding(horizontal = 9.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(Modifier.size(if (selected) 9.dp else 7.dp).background(colors.aircraftAltitudeColor(aircraft.altitudeFt, aircraft.altitudeKnown), CircleShape))
        Column(Modifier.weight(1f, fill = false)) {
            Text(title, color = colors.textPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(aircraft.typeCode.ifBlank { "TYPE UNKNOWN" }, color = colors.textSecondary, fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Text(aircraft.formattedAltitude, color = colors.textPrimary, fontSize = 10.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold)
        if (selected) Text("SELECTED", color = colors.highlight, fontSize = 8.sp, fontWeight = FontWeight.Bold)
    }
}
