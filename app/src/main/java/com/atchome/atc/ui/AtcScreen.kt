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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import kotlinx.coroutines.delay
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
    var registrationSearchDialogOpen by remember { mutableStateOf(false) }
    var registrationSearchQuery by remember { mutableStateOf("") }

    BoxWithConstraints(modifier = modifier.fillMaxSize().background(palette.radarBackground)) {
            val isLandscape = maxWidth > maxHeight
            if (isLandscape) {
                Row(Modifier.fillMaxSize().padding(10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Column(Modifier.weight(1.7f).fillMaxHeight()) {
                        Box(Modifier.weight(1f).fillMaxWidth()) {
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
                            speedUnit = uiState.settings.speedUnit,
                            highlightFilters = uiState.highlightFilters,
                            modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(18.dp)),
                            onSelectAt = { east, north, radius -> viewModel.selectNearestTo(east, north, radius) },
                            onSelectByHex = viewModel::selectByHex,
                            onZoomIn = { viewModel.zoomIn() },
                            onZoomOut = { viewModel.zoomOut() }
                        )
                            Surface(
                                modifier = Modifier.align(Alignment.BottomEnd).padding(end = 8.dp, bottom = 120.dp).size(48.dp),
                                color = palette.infoBackground.copy(alpha = 0.82f),
                                shape = CircleShape,
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (uiState.highlightFilters.isActive) palette.highlight else palette.tagBorder
                                )
                            ) {
                                IconButton(onClick = { viewModel.openHighlightFilters() }) {
                                    Icon(
                                        Icons.Default.FilterAlt,
                                        contentDescription = "Traffic filters",
                                        tint = if (uiState.highlightFilters.isActive) palette.highlight else palette.textPrimary
                                    )
                                }
                            }
                            Surface(
                                modifier = Modifier.align(Alignment.BottomEnd).padding(end = 8.dp, bottom = 11.dp).size(48.dp),
                                color = palette.infoBackground.copy(alpha = 0.96f),
                                shape = CircleShape,
                                border = androidx.compose.foundation.BorderStroke(1.5.dp, palette.highlight)
                            ) {
                                IconButton(onClick = { viewModel.openSettings() }) {
                                    Icon(Icons.Default.Settings, contentDescription = "Settings", tint = palette.highlight)
                                }
                            }
                            Surface(
                                modifier = Modifier.align(Alignment.BottomEnd).padding(end = 8.dp, bottom = 68.dp).size(48.dp),
                                color = palette.infoBackground.copy(alpha = 0.82f),
                                shape = CircleShape,
                                border = androidx.compose.foundation.BorderStroke(1.dp, palette.tagBorder)
                            ) {
                                IconButton(
                                    enabled = !uiState.isSearchingRegistration,
                                    onClick = {
                                        registrationSearchQuery = uiState.highlightFilters.registration
                                        registrationSearchDialogOpen = true
                                    }
                                ) {
                                    if (uiState.isSearchingRegistration) {
                                        CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp, color = palette.highlight)
                                    } else {
                                        Icon(Icons.Default.Search, contentDescription = "Find aircraft by registration", tint = palette.textPrimary)
                                    }
                                }
                            }
                            TrafficTicker(
                                aircraft = uiState.aircraft,
                                selectedHex = uiState.selectedAircraft?.hex,
                                onSelect = viewModel::selectByHex,
                                autoScroll = !uiState.settings.reduceMotion,
                                translucent = true,
                                modifier = Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(start = 8.dp, end = 64.dp, bottom = 8.dp)
                            )
                        }
                    }
                        if (uiState.isHighlightFilterSheetOpen) {
                            Surface(
                                modifier = Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(18.dp)),
                                color = palette.infoBackground,
                                contentColor = palette.textPrimary
                            ) {
                                AircraftHighlightFilterPanel(
                                    filters = uiState.highlightFilters,
                                    aircraft = uiState.aircraft,
                                    onFiltersChange = viewModel::updateHighlightFilters,
                                    onClear = viewModel::clearHighlightFilters,
                                    onDismiss = viewModel::closeHighlightFilters
                                )
                            }
                        } else {
                            InfoPanel(
                                selectedAircraft = uiState.selectedAircraft,
                                compact = true,
                                speedUnit = uiState.settings.speedUnit,
                                modifier = Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(18.dp))
                            )
                        }
                }
            } else {
                Column(Modifier.fillMaxSize()) {
                    Column(Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Box(Modifier.weight(1f).fillMaxWidth()) {
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
                        speedUnit = uiState.settings.speedUnit,
                        highlightFilters = uiState.highlightFilters,
                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(18.dp)),
                        onSelectAt = { east, north, radius ->
                            viewModel.selectNearestTo(east, north, radius)
                            showPhoneDetails = true
                        },
                        onSelectByHex = { hex ->
                            viewModel.selectByHex(hex)
                            showPhoneDetails = true
                        },
                        onZoomIn = { viewModel.zoomIn() },
                        onZoomOut = { viewModel.zoomOut() }
                    )
                        Surface(
                            modifier = Modifier.align(Alignment.BottomEnd).padding(end = 8.dp, bottom = 64.dp).size(48.dp),
                            color = palette.infoBackground.copy(alpha = 0.82f),
                            shape = CircleShape,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (uiState.highlightFilters.isActive) palette.highlight else palette.tagBorder
                            )
                        ) {
                            IconButton(onClick = {
                                viewModel.openHighlightFilters()
                                showPhoneDetails = true
                            }) {
                                Icon(
                                    Icons.Default.FilterAlt,
                                    contentDescription = "Traffic filters",
                                    tint = if (uiState.highlightFilters.isActive) palette.highlight else palette.textPrimary
                                )
                            }
                        }
                        Surface(
                            modifier = Modifier.align(Alignment.BottomEnd).padding(end = 60.dp, bottom = 8.dp).size(48.dp),
                            color = palette.infoBackground.copy(alpha = 0.96f),
                            shape = CircleShape,
                            border = androidx.compose.foundation.BorderStroke(1.5.dp, palette.highlight)
                        ) {
                            IconButton(onClick = { viewModel.openSettings() }) {
                                Icon(Icons.Default.Settings, contentDescription = "Settings", tint = palette.highlight)
                            }
                        }
                        Surface(
                            modifier = Modifier.align(Alignment.BottomEnd).padding(end = 8.dp, bottom = 8.dp).size(48.dp),
                            color = palette.infoBackground.copy(alpha = 0.82f),
                            shape = CircleShape,
                            border = androidx.compose.foundation.BorderStroke(1.dp, palette.tagBorder)
                        ) {
                            IconButton(
                                enabled = !uiState.isSearchingRegistration,
                                onClick = {
                                    registrationSearchQuery = uiState.highlightFilters.registration
                                    registrationSearchDialogOpen = true
                                }
                            ) {
                                if (uiState.isSearchingRegistration) {
                                    CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp, color = palette.highlight)
                                } else {
                                    Icon(Icons.Default.Search, contentDescription = "Find aircraft by registration", tint = palette.textPrimary)
                                }
                            }
                        }
                    }
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
    if (registrationSearchDialogOpen) {
        AlertDialog(
            onDismissRequest = { if (!uiState.isSearchingRegistration) registrationSearchDialogOpen = false },
            containerColor = palette.infoBackground,
            titleContentColor = palette.textPrimary,
            textContentColor = palette.textSecondary,
            title = { Text("FIND AIRCRAFT") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Search aircraft worldwide by registration. If it is no longer live, the most recent reported position may be shown.", fontSize = 13.sp)
                    androidx.compose.material3.OutlinedTextField(
                        value = registrationSearchQuery,
                        onValueChange = { registrationSearchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text("Registration") },
                        placeholder = { Text("e.g. G-XWBL") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = palette.textPrimary,
                            unfocusedTextColor = palette.textPrimary,
                            focusedBorderColor = palette.highlight,
                            unfocusedBorderColor = palette.textSecondary,
                            focusedLabelColor = palette.highlight,
                            unfocusedLabelColor = palette.textSecondary,
                            cursorColor = palette.highlight
                        )
                    )
                    if (uiState.isSearchingRegistration) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = palette.highlight)
                            Text("Searching live and last reported positions…", fontSize = 12.sp, color = palette.textSecondary)
                        }
                    } else if (uiState.registrationSearchMessage != null) {
                        Text(uiState.registrationSearchMessage.orEmpty(), fontSize = 12.sp, color = palette.highlight)
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = registrationSearchQuery.isNotBlank() && !uiState.isSearchingRegistration,
                    onClick = { viewModel.searchAircraftByRegistration(registrationSearchQuery) }
                ) { Text("Find on radar", color = palette.highlight) }
            },
            dismissButton = {
                TextButton(
                    enabled = !uiState.isSearchingRegistration,
                    onClick = { registrationSearchDialogOpen = false }
                ) { Text("Close", color = palette.textSecondary) }
            }
        )
    }
    if (showPhoneDetails && (uiState.selectedAircraft != null || uiState.isHighlightFilterSheetOpen)) {
        ModalBottomSheet(
            onDismissRequest = {
                showPhoneDetails = false
                viewModel.closeHighlightFilters()
            },
            containerColor = palette.infoBackground,
            contentColor = palette.textPrimary
        ) {
            if (uiState.isHighlightFilterSheetOpen) {
                AircraftHighlightFilterPanel(
                    filters = uiState.highlightFilters,
                    aircraft = uiState.aircraft,
                    onFiltersChange = viewModel::updateHighlightFilters,
                    onClear = viewModel::clearHighlightFilters,
                    onDismiss = viewModel::closeHighlightFilters
                )
            } else {
                InfoPanel(
                    selectedAircraft = uiState.selectedAircraft,
                    speedUnit = uiState.settings.speedUnit,
                    modifier = Modifier.fillMaxWidth().heightIn(max = 560.dp)
                )
            }
        }
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

@Composable
private fun TrafficTicker(
    aircraft: List<Aircraft>,
    selectedHex: String?,
    onSelect: (String) -> Unit,
    autoScroll: Boolean,
    translucent: Boolean = false,
    modifier: Modifier = Modifier
) {
    val colors = AtcColors
    val listState = rememberLazyListState()
    val tickerItems = remember(aircraft) { aircraft + aircraft }

    LaunchedEffect(aircraft.size, autoScroll) {
        if (!autoScroll || aircraft.size < 2) return@LaunchedEffect
        listState.scrollToItem(0)
        while (true) {
            delay(1800L)
            if (!listState.isScrollInProgress) {
                val nextIndex = listState.firstVisibleItemIndex + 1
                if (nextIndex >= tickerItems.size) {
                    listState.scrollToItem(0)
                } else {
                    listState.animateScrollToItem(nextIndex)
                }
            }
        }
    }

    Surface(
        modifier = modifier.height(54.dp),
        color = colors.infoBackground.copy(alpha = if (translucent) 0.78f else 1f),
        shape = RoundedCornerShape(15.dp),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = colors.highlight.copy(alpha = 0.45f)
        )
    ) {
        Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.width(126.dp).padding(start = 14.dp, end = 10.dp)) {
                Text(
                    "NEARBY TRAFFIC",
                    color = colors.highlight,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.6.sp,
                    maxLines = 1
                )
                Text(
                    "${aircraft.size} IN RANGE",
                    color = colors.highlight,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }
            Box(Modifier.width(1.dp).height(28.dp).background(colors.scopeBorder))
            if (aircraft.isEmpty()) {
                Text(
                    "No traffic in range",
                    modifier = Modifier.padding(horizontal = 14.dp),
                    color = colors.textSecondary,
                    fontSize = 12.sp
                )
            } else {
                LazyRow(
                    state = listState,
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    items(
                        count = tickerItems.size,
                        key = { index -> "${index / aircraft.size}-${tickerItems[index].hex}" }
                    ) { index ->
                        TrafficTickerItem(
                            aircraft = tickerItems[index],
                            selected = tickerItems[index].hex == selectedHex,
                            onSelect = onSelect
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TrafficTickerItem(
    aircraft: Aircraft,
    selected: Boolean,
    onSelect: (String) -> Unit
) {
    val colors = AtcColors
    Surface(
        modifier = Modifier.clickable { onSelect(aircraft.hex) },
        color = if (selected) colors.cardBackground else colors.infoBackground,
        contentColor = colors.textPrimary,
        shape = RoundedCornerShape(10.dp),
        border = androidx.compose.foundation.BorderStroke(
            width = if (selected) 1.dp else 0.dp,
            color = if (selected) colors.highlight else colors.infoBackground
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Box(
                Modifier.size(7.dp).background(
                    colors.aircraftAltitudeColor(aircraft.altitudeFt, aircraft.altitudeKnown),
                    CircleShape
                )
            )
            Text(
                aircraft.callsign.ifBlank { aircraft.hex.uppercase(Locale.US) },
                color = colors.textPrimary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                aircraft.formattedAltitude,
                color = colors.highlight,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }
    }
}
