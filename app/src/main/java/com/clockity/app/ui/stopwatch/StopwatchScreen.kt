package com.clockity.app.ui.stopwatch

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clockity.app.ui.components.OneUIHeader
import com.clockity.app.ui.theme.*
import com.clockity.app.utils.TimeUtils

@Composable
fun StopwatchScreen(
    viewModel: StopwatchViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val (mainTime, hundredths, _) = TimeUtils.formatStopwatchTime(uiState.elapsedMillis)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(OneUIBlack)
    ) {
        // One UI Header
        OneUIHeader(
            title = "Stopwatch",
            subtitle = if (uiState.isRunning) "Running" else if (uiState.elapsedMillis > 0) "Paused" else "Ready"
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Large Digital Display (e.g. 00:15.34)
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp)
            ) {
                Text(
                    text = mainTime,
                    fontSize = 58.sp,
                    fontWeight = FontWeight.Light,
                    color = OneUITextPrimary
                )
                Text(
                    text = hundredths,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Normal,
                    color = OneUIYellow,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
            }

            // Control Buttons (One UI style circular thumb controls)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Button: Reset or Lap
                if (uiState.isRunning) {
                    Button(
                        onClick = { viewModel.recordLap() },
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape),
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = OneUICardElevated,
                            contentColor = OneUITextPrimary
                        ),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("Lap", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    }
                } else {
                    Button(
                        onClick = { viewModel.reset() },
                        enabled = uiState.elapsedMillis > 0,
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape),
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = OneUICardElevated,
                            contentColor = OneUITextPrimary,
                            disabledContainerColor = OneUICardDark,
                            disabledContentColor = OneUITextDisabled
                        ),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("Reset", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    }
                }

                // Right Button: Start or Stop
                if (uiState.isRunning) {
                    Button(
                        onClick = { viewModel.pause() },
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape),
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = OneUIRed,
                            contentColor = Color.White
                        ),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("Stop", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Button(
                        onClick = { viewModel.start() },
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape),
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = OneUIBlue,
                            contentColor = OneUIBlack
                        ),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("Start", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Lap Times List
            if (uiState.laps.isNotEmpty()) {
                LapList(
                    laps = uiState.laps,
                    fastestLap = uiState.fastestLap,
                    slowestLap = uiState.slowestLap,
                    modifier = Modifier.weight(1f)
                )
            } else {
                Spacer(modifier = Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}
