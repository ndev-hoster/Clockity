package com.clockity.app.ui.worldclock

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.clockity.app.ui.theme.*

data class CityOption(val cityName: String, val countryName: String, val timeZoneId: String)

val POPULAR_CITIES = listOf(
    CityOption("Amsterdam", "Netherlands", "Europe/Amsterdam"),
    CityOption("Athens", "Greece", "Europe/Athens"),
    CityOption("Auckland", "New Zealand", "Pacific/Auckland"),
    CityOption("Bangkok", "Thailand", "Asia/Bangkok"),
    CityOption("Beijing", "China", "Asia/Shanghai"),
    CityOption("Berlin", "Germany", "Europe/Berlin"),
    CityOption("Cairo", "Egypt", "Africa/Cairo"),
    CityOption("Chicago", "United States", "America/Chicago"),
    CityOption("Dubai", "United Arab Emirates", "Asia/Dubai"),
    CityOption("Hong Kong", "Hong Kong", "Asia/Hong_Kong"),
    CityOption("London", "United Kingdom", "Europe/London"),
    CityOption("Los Angeles", "United States", "America/Los_Angeles"),
    CityOption("Madrid", "Spain", "Europe/Madrid"),
    CityOption("Melbourne", "Australia", "Australia/Melbourne"),
    CityOption("Mexico City", "Mexico", "America/Mexico_City"),
    CityOption("Mumbai", "India", "Asia/Kolkata"),
    CityOption("New Delhi", "India", "Asia/Kolkata"),
    CityOption("New York", "United States", "America/New_York"),
    CityOption("Paris", "France", "Europe/Paris"),
    CityOption("Rome", "Italy", "Europe/Rome"),
    CityOption("San Francisco", "United States", "America/Los_Angeles"),
    CityOption("Sao Paulo", "Brazil", "America/Sao_Paulo"),
    CityOption("Seoul", "South Korea", "Asia/Seoul"),
    CityOption("Singapore", "Singapore", "Asia/Singapore"),
    CityOption("Sydney", "Australia", "Australia/Sydney"),
    CityOption("Tokyo", "Japan", "Asia/Tokyo"),
    CityOption("Toronto", "Canada", "America/Toronto"),
    CityOption("Vancouver", "Canada", "America/Vancouver"),
    CityOption("Zurich", "Switzerland", "Europe/Zurich")
)

@Composable
fun AddCityDialog(
    onDismiss: () -> Unit,
    onCitySelected: (CityOption) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredCities = remember(searchQuery) {
        if (searchQuery.isBlank()) POPULAR_CITIES
        else POPULAR_CITIES.filter {
            it.cityName.contains(searchQuery, ignoreCase = true) ||
            it.countryName.contains(searchQuery, ignoreCase = true)
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.80f),
            shape = RoundedCornerShape(28.dp),
            color = OneUIBlack,
            border = BorderStroke(1.dp, OneUIDivider)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                Text(
                    text = "Add City",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = OneUITextPrimary
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search city or country...") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = OneUITextSecondary)
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = OneUITextPrimary,
                        unfocusedTextColor = OneUITextPrimary,
                        focusedBorderColor = OneUIBlue,
                        unfocusedBorderColor = OneUIDivider
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(filteredCities) { city ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    onCitySelected(city)
                                    onDismiss()
                                }
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = city.cityName,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = OneUITextPrimary
                                )
                                Text(
                                    text = city.countryName,
                                    fontSize = 13.sp,
                                    color = OneUITextSecondary
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Cancel", color = OneUIBlue, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
