package com.atchome.atc.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.atchome.atc.api.AdsbApiClient
import com.atchome.atc.api.AircraftImageApiClient
import com.atchome.atc.api.HexdbApiClient
import com.atchome.atc.data.AirportRepository
import com.atchome.atc.data.AppSettings
import com.atchome.atc.data.SettingsManager
import com.atchome.atc.data.Tracker
import com.atchome.atc.model.Aircraft
import com.atchome.atc.model.Airport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class AtcUiState(
    val aircraft: List<Aircraft> = emptyList(),
    val airports: List<Airport> = emptyList(),
    val selectedAircraft: Aircraft? = null,
    val statusMessage: String = "Connecting…",
    val isScanning: Boolean = false,
    val settings: AppSettings = AppSettings(),
    val isSettingsDialogOpen: Boolean = false
)

class AtcViewModel(application: Application) : AndroidViewModel(application) {
    private val settingsManager = SettingsManager(application)
    private val airportRepository = AirportRepository(application)
    private val adsbClient = AdsbApiClient()
    private val hexdbClient = HexdbApiClient()
    private val imageClient = AircraftImageApiClient()
    private val tracker = Tracker()

    private val _uiState = MutableStateFlow(AtcUiState(settings = settingsManager.loadSettings()))
    val uiState: StateFlow<AtcUiState> = _uiState.asStateFlow()

    private var pollJob: Job? = null
    private var tickerJob: Job? = null
    private var enrichmentJob: Job? = null
    private var airportJob: Job? = null
    private var lastEnrichedHex: String = ""

    init {
        refreshNearbyAirports()
        startPolling()
        startDeadReckoningTicker()
    }

    fun openSettings() {
        _uiState.value = _uiState.value.copy(isSettingsDialogOpen = true)
    }

    fun closeSettings() {
        _uiState.value = _uiState.value.copy(isSettingsDialogOpen = false)
    }

    fun saveSettings(newSettings: AppSettings) {
        val previous = _uiState.value.settings
        settingsManager.saveSettings(newSettings)
        _uiState.value = _uiState.value.copy(
            settings = newSettings,
            isSettingsDialogOpen = false
        )
        if (previous.homeLat != newSettings.homeLat ||
            previous.homeLon != newSettings.homeLon ||
            previous.radiusNm != newSettings.radiusNm
        ) {
            refreshNearbyAirports()
        }
        // Restart polling with new coordinates/interval
        startPolling()
    }

    private fun refreshNearbyAirports() {
        airportJob?.cancel()
        airportJob = viewModelScope.launch {
            val settings = _uiState.value.settings
            val nearby = airportRepository.nearby(
                homeLat = settings.homeLat,
                homeLon = settings.homeLon,
                radiusNm = settings.radiusNm
            )
            _uiState.value = _uiState.value.copy(airports = nearby)
        }
    }

    fun zoomIn() {
        val current = _uiState.value.settings
        val newRadius = when {
            current.radiusNm <= 10 -> (current.radiusNm - 2).coerceAtLeast(5)
            current.radiusNm <= 25 -> current.radiusNm - 5
            else -> current.radiusNm - 10
        }
        if (newRadius != current.radiusNm) {
            val updated = current.copy(radiusNm = newRadius)
            saveSettings(updated)
        }
    }

    fun zoomOut() {
        val current = _uiState.value.settings
        val newRadius = when {
            current.radiusNm < 10 -> current.radiusNm + 2
            current.radiusNm < 25 -> current.radiusNm + 5
            else -> (current.radiusNm + 10).coerceAtMost(100)
        }
        if (newRadius != current.radiusNm) {
            val updated = current.copy(radiusNm = newRadius)
            saveSettings(updated)
        }
    }

    fun cycleDataTagMode() {
        val current = _uiState.value.settings
        val nextMode = when (current.dataTagMode) {
            "SELECTED" -> "COMPACT"
            "COMPACT" -> "FULL"
            else -> "SELECTED"
        }
        val updated = current.copy(dataTagMode = nextMode)
        settingsManager.saveSettings(updated)
        _uiState.value = _uiState.value.copy(settings = updated)
    }

    fun selectByHex(hex: String) {
        if (tracker.selectByHex(hex)) {
            updateUiStateFromTracker()
            triggerEnrichment(tracker.selected())
        }
    }

    fun selectNearestTo(eastNm: Float, northNm: Float, hitRadiusNm: Float) {
        val selected = tracker.selectNearestTo(eastNm, northNm, hitRadiusNm)
        updateUiStateFromTracker()
        triggerEnrichment(selected)
    }

    private fun startPolling() {
        pollJob?.cancel()
        pollJob = viewModelScope.launch {
            while (isActive) {
                fetchSnapshot()
                val intervalMs = (_uiState.value.settings.refreshIntervalSeconds * 1000L).coerceAtLeast(3000L)
                delay(intervalMs)
            }
        }
    }

    private suspend fun fetchSnapshot() {
        val currentSettings = _uiState.value.settings
        _uiState.value = _uiState.value.copy(isScanning = true)

        val result = adsbClient.fetchNearbyAircraft(
            homeLat = currentSettings.homeLat,
            homeLon = currentSettings.homeLon,
            radiusNm = currentSettings.radiusNm
        )

        withContext(Dispatchers.Main) {
            result.onSuccess { list ->
                tracker.mergeSnapshot(list)
                val count = tracker.getAircraft().size
                val status = if (count == 0) {
                    "No aircraft nearby"
                } else {
                    "Live • $count aircraft tracked"
                }
                _uiState.value = _uiState.value.copy(
                    aircraft = tracker.getAircraft(),
                    selectedAircraft = tracker.selected()?.copy(),
                    statusMessage = status,
                    isScanning = false
                )
                triggerEnrichment(tracker.selected())
            }.onFailure {
                val status = if (tracker.getAircraft().isEmpty()) {
                    "Fetch failed"
                } else {
                    "Refresh error • ${tracker.getAircraft().size} cached"
                }
                _uiState.value = _uiState.value.copy(
                    statusMessage = status,
                    isScanning = false
                )
            }
        }
    }

    private fun triggerEnrichment(target: Aircraft?) {
        if (target == null) {
            lastEnrichedHex = ""
            return
        }

        if (target.hex != lastEnrichedHex) {
            lastEnrichedHex = target.hex
        }

        if (target.detailsLoaded && (target.routeLoaded || target.callsign.isEmpty()) && target.imageLoaded) {
            return
        }

        enrichmentJob?.cancel()
        enrichmentJob = viewModelScope.launch(Dispatchers.IO) {
            val hex = target.hex
            val callsign = target.callsign

            // 0. Image (Planespotters / adsbdb / Wikipedia)
            if (!target.imageLoaded && hex.isNotEmpty()) {
                val photoResult = imageClient.fetchAircraftPhoto(
                    hex = hex,
                    registration = target.registration,
                    typeCode = target.typeCode,
                    typeDescription = target.typeDescription
                )
                photoResult.onSuccess { photo ->
                    withContext(Dispatchers.Main) {
                        val current = tracker.selected()
                        if (current != null && current.hex == hex) {
                            current.imageUrl = photo.imageUrl
                            current.imagePhotographer = photo.photographer
                            current.imageLoaded = true
                            updateUiStateFromTracker()
                        }
                    }
                }.onFailure {
                    withContext(Dispatchers.Main) {
                        val current = tracker.selected()
                        if (current != null && current.hex == hex) {
                            current.imageLoaded = true
                            updateUiStateFromTracker()
                        }
                    }
                }
            }

            // 1. Details (Manufacturer, Type, Registration, Owner)
            if (!target.detailsLoaded && hex.isNotEmpty()) {
                val detailsResult = hexdbClient.fetchAircraftDetails(hex)
                detailsResult.onSuccess { details ->
                    withContext(Dispatchers.Main) {
                        val current = tracker.selected()
                        if (current != null && current.hex == hex) {
                            current.manufacturer = details.manufacturer
                            current.typeDescription = details.typeDescription
                            current.registeredOwner = details.registeredOwner
                            if (current.registration.isEmpty() && details.registration.isNotEmpty()) {
                                current.registration = details.registration
                            }
                            current.detailsLoaded = true
                            updateUiStateFromTracker()
                        }
                    }
                }
                delay(600) // gentle delay between calls
            }

            // 2. Route (Origin & Destination ICAO)
            if (!target.routeLoaded && callsign.isNotEmpty()) {
                val routeResult = hexdbClient.fetchRoute(callsign)
                routeResult.onSuccess { route ->
                    withContext(Dispatchers.Main) {
                        val current = tracker.selected()
                        if (current != null && current.hex == hex) {
                            current.originIcao = route.originIcao
                            current.destinationIcao = route.destinationIcao
                            current.origin = route.originIcao
                            current.destination = route.destinationIcao
                            current.routeLookupStep = 1
                            updateUiStateFromTracker()
                        }
                    }

                    // 3. Airport friendly names + coordinates (for route distances)
                    if (route.originIcao.isNotEmpty()) {
                        delay(500)
                        val originResult = hexdbClient.fetchAirport(route.originIcao)
                        originResult.onSuccess { airport ->
                            withContext(Dispatchers.Main) {
                                val current = tracker.selected()
                                if (current != null && current.hex == hex) {
                                    current.origin = airport.name
                                    current.originLat = airport.lat
                                    current.originLon = airport.lon
                                    current.routeLookupStep = 2
                                    updateUiStateFromTracker()
                                }
                            }
                        }
                    }

                    if (route.destinationIcao.isNotEmpty()) {
                        delay(500)
                        val destResult = hexdbClient.fetchAirport(route.destinationIcao)
                        destResult.onSuccess { airport ->
                            withContext(Dispatchers.Main) {
                                val current = tracker.selected()
                                if (current != null && current.hex == hex) {
                                    current.destination = airport.name
                                    current.destinationLat = airport.lat
                                    current.destinationLon = airport.lon
                                    current.routeLookupStep = 3
                                    current.routeLoaded = true
                                    updateUiStateFromTracker()
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun startDeadReckoningTicker() {
        tickerJob?.cancel()
        tickerJob = viewModelScope.launch(Dispatchers.Default) {
            var lastTime = System.currentTimeMillis()
            while (isActive) {
                delay(100) // 10 FPS extrapolation
                val now = System.currentTimeMillis()
                val dt = (now - lastTime) / 1000f
                lastTime = now

                tracker.updatePositions(dt)
                withContext(Dispatchers.Main) {
                    updateUiStateFromTracker()
                }
            }
        }
    }

    private fun updateUiStateFromTracker() {
        _uiState.value = _uiState.value.copy(
            aircraft = tracker.getAircraft(),
            selectedAircraft = tracker.selected()?.copy()
        )
    }

    override fun onCleared() {
        super.onCleared()
        pollJob?.cancel()
        tickerJob?.cancel()
        enrichmentJob?.cancel()
        airportJob?.cancel()
    }
}
