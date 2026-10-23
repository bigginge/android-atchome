package com.atchome.atc.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import com.atchome.atc.ui.theme.RadarBackground
import com.atchome.atc.viewmodel.AtcViewModel

@Composable
fun AtcScreen(
    viewModel: AtcViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(RadarBackground)
    ) {
        if (isLandscape) {
            // Landscape Layout: Square Tactical Radar (Left) + Detail Telemetry Panel (Right)
            Row(modifier = Modifier.fillMaxSize()) {
                RadarView(
                    aircraftList = uiState.aircraft,
                    selectedAircraft = uiState.selectedAircraft,
                    airports = uiState.airports,
                    rangeNm = uiState.settings.radiusNm.toFloat(),
                    showTrails = uiState.settings.showTrails,
                    showRangeRings = uiState.settings.showRangeRings,
                    showAirports = uiState.settings.showAirports,
                    showSweep = uiState.settings.showSweep,
                    showVelocityVectors = uiState.settings.showVelocityVectors,
                    dataTagMode = uiState.settings.dataTagMode,
                    highlightFilters = uiState.highlightFilters,
                    modifier = Modifier
                        .fillMaxHeight()
                        .aspectRatio(1f),
                    onSelectAt = { eastNm, northNm, hitRadiusNm ->
                        viewModel.selectNearestTo(eastNm, northNm, hitRadiusNm)
                    },
                    onZoomIn = { viewModel.zoomIn() },
                    onZoomOut = { viewModel.zoomOut() },
                    onCycleDataTagMode = { viewModel.cycleDataTagMode() },
                    onOpenHighlightFilters = { viewModel.openHighlightFilters() }
                )

                InfoPanel(
                    selectedAircraft = uiState.selectedAircraft,
                    statusMessage = uiState.statusMessage,
                    isScanning = uiState.isScanning,
                    onOpenSettings = { viewModel.openSettings() },
                    modifier = Modifier.weight(1f)
                )
            }
        } else {
            // Portrait Layout: Square Tactical Radar (Top) + Detail Telemetry Panel (Bottom)
            Column(modifier = Modifier.fillMaxSize()) {
                RadarView(
                    aircraftList = uiState.aircraft,
                    selectedAircraft = uiState.selectedAircraft,
                    airports = uiState.airports,
                    rangeNm = uiState.settings.radiusNm.toFloat(),
                    showTrails = uiState.settings.showTrails,
                    showRangeRings = uiState.settings.showRangeRings,
                    showAirports = uiState.settings.showAirports,
                    showSweep = uiState.settings.showSweep,
                    showVelocityVectors = uiState.settings.showVelocityVectors,
                    dataTagMode = uiState.settings.dataTagMode,
                    highlightFilters = uiState.highlightFilters,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f),
                    onSelectAt = { eastNm, northNm, hitRadiusNm ->
                        viewModel.selectNearestTo(eastNm, northNm, hitRadiusNm)
                    },
                    onZoomIn = { viewModel.zoomIn() },
                    onZoomOut = { viewModel.zoomOut() },
                    onCycleDataTagMode = { viewModel.cycleDataTagMode() },
                    onOpenHighlightFilters = { viewModel.openHighlightFilters() }
                )

                InfoPanel(
                    selectedAircraft = uiState.selectedAircraft,
                    statusMessage = uiState.statusMessage,
                    isScanning = uiState.isScanning,
                    onOpenSettings = { viewModel.openSettings() },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        if (uiState.isSettingsDialogOpen) {
            SettingsDialog(
                initialSettings = uiState.settings,
                onDismiss = { viewModel.closeSettings() },
                onSave = { newSettings ->
                    viewModel.saveSettings(newSettings)
                }
            )
        }

        if (uiState.isHighlightFilterSheetOpen) {
            AircraftHighlightFilterSheet(
                filters = uiState.highlightFilters,
                aircraft = uiState.aircraft,
                onFiltersChange = { viewModel.updateHighlightFilters(it) },
                onClear = { viewModel.clearHighlightFilters() },
                onDismiss = { viewModel.closeHighlightFilters() }
            )
        }
    }
}
