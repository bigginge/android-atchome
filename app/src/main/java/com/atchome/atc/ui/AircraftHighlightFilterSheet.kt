package com.atchome.atc.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.atchome.atc.model.AircraftHighlightFilters
import com.atchome.atc.model.Aircraft
import com.atchome.atc.ui.theme.AtcColors

@Composable
fun AircraftHighlightFilterPanel(
    filters: AircraftHighlightFilters,
    aircraft: List<Aircraft>,
    onFiltersChange: (AircraftHighlightFilters) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
        Column(
            modifier = modifier.fillMaxWidth().imePadding().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("HIGHLIGHT AIRCRAFT", fontWeight = FontWeight.Bold, color = AtcColors.highlight, letterSpacing = 1.sp)
            Text("Matching aircraft stand out; other traffic stays visible.", fontSize = 13.sp, color = AtcColors.textSecondary)
            FilterField("Type", "e.g. A320", filters.type, aircraft.typeSuggestions()) { onFiltersChange(filters.copy(type = it)) }
            FilterField("Airline", "e.g. British Airways", filters.airline, aircraft.airlineSuggestions()) {
                onFiltersChange(filters.copy(airline = it))
            }
            FilterField("Registration", "e.g. G-ABCD or N123AB", filters.registration, aircraft.registrationSuggestions()) {
                onFiltersChange(filters.copy(registration = it))
            }
            FilterField("Origin", "Airport name or ICAO", filters.origin, aircraft.routeSuggestions(isOrigin = true)) { onFiltersChange(filters.copy(origin = it)) }
            FilterField("Destination", "Airport name or ICAO", filters.destination, aircraft.routeSuggestions(isOrigin = false)) {
                onFiltersChange(filters.copy(destination = it))
            }
            TextButton(onClick = onClear) { Text("Clear filters", color = AtcColors.textSecondary) }
        }
}

private data class FilterSuggestion(val value: String, val label: String)

@Composable
@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
private fun FilterField(
    label: String,
    placeholder: String,
    value: String,
    suggestions: List<FilterSuggestion>,
    onValueChange: (String) -> Unit
) {
    val suggestionsRequester = remember { BringIntoViewRequester() }
    var isFocused by remember { androidx.compose.runtime.mutableStateOf(false) }
    val query = value.trim()
    val visibleSuggestions = suggestions
        .filter { query.isEmpty() || it.label.contains(query, ignoreCase = true) || it.value.contains(query, ignoreCase = true) }
        .take(6)

    LaunchedEffect(isFocused, visibleSuggestions) {
        if (isFocused && visibleSuggestions.isNotEmpty()) {
            suggestionsRequester.bringIntoView()
        }
    }

    Column {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth().onFocusChanged { isFocused = it.isFocused },
            label = { Text(label) },
            placeholder = { Text(placeholder, color = AtcColors.textSecondary) },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = AtcColors.textPrimary,
                unfocusedTextColor = AtcColors.textPrimary,
                focusedBorderColor = AtcColors.highlight,
                unfocusedBorderColor = AtcColors.textSecondary,
                focusedLabelColor = AtcColors.highlight,
                unfocusedLabelColor = AtcColors.textSecondary,
                cursorColor = AtcColors.highlight
            )
        )
        if (visibleSuggestions.isNotEmpty()) {
            Text("IN RANGE", modifier = Modifier.padding(start = 2.dp, top = 5.dp), fontSize = 9.sp, color = AtcColors.textSecondary, letterSpacing = 0.6.sp)
            FlowRow(
                modifier = Modifier.fillMaxWidth()
                    .bringIntoViewRequester(suggestionsRequester),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                visibleSuggestions.forEach { suggestion ->
                    SuggestionChip(
                        onClick = { onValueChange(suggestion.value) },
                        label = { Text(suggestion.label, maxLines = 1, fontSize = 11.sp) },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = AtcColors.cardBackground,
                            labelColor = AtcColors.textPrimary
                        )
                    )
                }
            }
        } else {
            Spacer(Modifier.height(2.dp))
        }
    }
}

private fun List<Aircraft>.typeSuggestions(): List<FilterSuggestion> = mapNotNull { aircraft ->
    val code = aircraft.typeCode.trim()
    val description = aircraft.typeDescription.trim()
    val value = code.ifEmpty { description }
    if (value.isEmpty()) null else FilterSuggestion(value, if (code.isNotEmpty() && description.isNotEmpty()) "$code · $description" else value)
}.distinctBy { it.value.lowercase() }.sortedBy { it.label.lowercase() }

private fun List<Aircraft>.airlineSuggestions(): List<FilterSuggestion> = mapNotNull { aircraft ->
    aircraft.registeredOwner.trim().takeIf(String::isNotEmpty)?.let { FilterSuggestion(it, it) }
}.distinctBy { it.value.lowercase() }.sortedBy { it.label.lowercase() }

private fun List<Aircraft>.registrationSuggestions(): List<FilterSuggestion> = mapNotNull { aircraft ->
    aircraft.registration.trim().takeIf(String::isNotEmpty)?.let { FilterSuggestion(it, it) }
}.distinctBy { it.value.lowercase() }.sortedBy { it.label.lowercase() }

private fun List<Aircraft>.routeSuggestions(isOrigin: Boolean): List<FilterSuggestion> = mapNotNull { aircraft ->
    val name = (if (isOrigin) aircraft.origin else aircraft.destination).trim()
    val icao = (if (isOrigin) aircraft.originIcao else aircraft.destinationIcao).trim()
    val value = icao.ifEmpty { name }
    if (value.isEmpty()) null else FilterSuggestion(value, if (name.isNotEmpty() && icao.isNotEmpty()) "$name · $icao" else value)
}.distinctBy { it.value.lowercase() }.sortedBy { it.label.lowercase() }
