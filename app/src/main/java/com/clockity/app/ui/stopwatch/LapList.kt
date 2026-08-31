package com.clockity.app.ui.stopwatch

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Divider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clockity.app.ui.theme.*
import com.clockity.app.utils.TimeUtils

@Composable
fun LapList(
    laps: List<LapData>,
    fastestLap: LapData?,
    slowestLap: LapData?,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(OneUICardDark)
            .padding(16.dp)
    ) {
        // Table Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp, start = 8.dp, end = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = "Lap", fontSize = 13.sp, color = OneUITextTertiary, fontWeight = FontWeight.SemiBold)
            Text(text = "Lap times", fontSize = 13.sp, color = OneUITextTertiary, fontWeight = FontWeight.SemiBold)
            Text(text = "Overall time", fontSize = 13.sp, color = OneUITextTertiary, fontWeight = FontWeight.SemiBold)
        }

        Divider(color = OneUIDivider, thickness = 1.dp)

        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            items(laps, key = { it.lapNumber }) { lap ->
                val isFastest = fastestLap?.lapNumber == lap.lapNumber
                val isSlowest = slowestLap?.lapNumber == lap.lapNumber

                val lapColor = when {
                    isFastest -> OneUIYellow
                    isSlowest -> OneUIRed
                    else -> OneUITextPrimary
                }

                val (_, _, lapStr) = TimeUtils.formatStopwatchTime(lap.lapTimeMillis)
                val (_, _, overallStr) = TimeUtils.formatStopwatchTime(lap.overallTimeMillis)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Lap #
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = String.format("%02d", lap.lapNumber),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = lapColor
                        )
                        if (isFastest) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "Fastest", fontSize = 10.sp, color = OneUIYellow, fontWeight = FontWeight.Bold)
                        } else if (isSlowest) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "Slowest", fontSize = 10.sp, color = OneUIRed, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Lap split
                    Text(
                        text = lapStr,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = lapColor
                    )

                    // Total elapsed
                    Text(
                        text = overallStr,
                        fontSize = 15.sp,
                        color = OneUITextSecondary
                    )
                }

                Divider(color = OneUIDivider.copy(alpha = 0.5f), thickness = 0.5.dp)
            }
        }
    }
}
