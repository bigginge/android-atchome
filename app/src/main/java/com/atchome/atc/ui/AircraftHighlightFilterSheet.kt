package com.atchome.atc.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.atchome.atc.model.AircraftHighlightFilters
import com.atchome.atc.ui.theme.CardBackground
import com.atchome.atc.ui.theme.RadarBackground
import com.atchome.atc.ui.theme.TextHighlight
import com.atchome.atc.ui.theme.TextPrimary
import com.atchome.atc.ui.theme.TextSecondary

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun AircraftHighlightFilterSheet(
    filters: AircraftHighlightFilters,
    onFiltersChange: (AircraftHighlightFilters) -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = RadarBackground,
        contentColor = TextPrimary,
        scrimColor = Color.Black.copy(alpha = 0.65f)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("HIGHLIGHT AIRCRAFT", fontWeight = FontWeight.Bold, color = TextHighlight, letterSpacing = 1.sp)
            Text("Matching aircraft stand out; other traffic stays visible.", fontSize = 13.sp, color = TextSecondary)
            FilterField("Type", "e.g. A320", filters.type) { onFiltersChange(filters.copy(type = it)) }
            FilterField("Airline / callsign", "Callsign starts with… e.g. BAW", filters.airlineCallsign) {
                onFiltersChange(filters.copy(airlineCallsign = it))
            }
            FilterField("Origin", "Airport name or ICAO", filters.origin) { onFiltersChange(filters.copy(origin = it)) }
            FilterField("Destination", "Airport name or ICAO", filters.destination) {
                onFiltersChange(filters.copy(destination = it))
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(onClick = onClear) { Text("Clear filters", color = TextSecondary) }
                Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = TextHighlight)) {
                    Text("Done", color = CardBackground, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun FilterField(label: String, placeholder: String, value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        placeholder = { Text(placeholder, color = TextSecondary) },
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = TextPrimary,
            unfocusedTextColor = TextPrimary,
            focusedBorderColor = TextHighlight,
            unfocusedBorderColor = TextSecondary,
            focusedLabelColor = TextHighlight,
            unfocusedLabelColor = TextSecondary,
            cursorColor = TextHighlight
        )
    )
}
