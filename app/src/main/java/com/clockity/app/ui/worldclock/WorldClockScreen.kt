package com.clockity.app.ui.worldclock

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clockity.app.ui.components.OneUIHeader
import com.clockity.app.ui.theme.*

@Composable
fun WorldClockScreen(
    viewModel: WorldClockViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(OneUIBlack)
    ) {
        // One UI Header
        OneUIHeader(
            title = "World clock",
            subtitle = "${uiState.cities.size} cities saved",
            onAddClick = { showAddDialog = true }
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 96.dp, top = 4.dp)
        ) {
            // Interactive Time Zone Converter / Scrubber
            item {
                TimeScrubberBar(
                    offsetHours = uiState.scrubberOffsetHours,
                    onOffsetChange = { viewModel.setScrubberOffset(it) },
                    onReset = { viewModel.resetScrubber() }
                )
            }

            // Saved Cities List
            items(uiState.cities, key = { it.id }) { city ->
                CityCard(
                    city = city,
                    offsetHours = uiState.scrubberOffsetHours,
                    onDelete = { viewModel.deleteCity(city) }
                )
            }

            if (uiState.cities.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 60.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No world cities added yet. Tap + to add one.",
                            fontSize = 15.sp,
                            color = OneUITextTertiary
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddCityDialog(
            onDismiss = { showAddDialog = false },
            onCitySelected = { option ->
                viewModel.addCity(option.cityName, option.countryName, option.timeZoneId)
            }
        )
    }
}
