package com.clockity.app.ui.timer

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clockity.app.data.models.TimerPreset
import com.clockity.app.ui.theme.*

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PresetChips(
    presets: List<TimerPreset>,
    onSelectPreset: (TimerPreset) -> Unit,
    onEditPreset: (TimerPreset) -> Unit,
    onAddPreset: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Presets",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = OneUITextSecondary
            )
            Text(
                text = "Hold to edit",
                fontSize = 11.sp,
                color = OneUITextTertiary
            )
        }

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items(presets, key = { it.id }) { preset ->
                Row(
                    modifier = Modifier
                        .height(52.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(OneUICardDark)
                        .combinedClickable(
                            onClick = { onSelectPreset(preset) },
                            onLongClick = { onEditPreset(preset) }
                        )
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.Center) {
                        Text(
                            text = preset.title,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = OneUITextPrimary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = preset.formatDurationSummary(),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = OneUIBlueLight
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(OneUICardElevated)
                            .clickable { onEditPreset(preset) }
                            .padding(4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Preset",
                            tint = OneUITextSecondary,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }

            // + Add preset button
            item {
                Row(
                    modifier = Modifier
                        .height(52.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(OneUICardElevated)
                        .clickable { onAddPreset() }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Preset",
                        tint = OneUIBlue,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "New",
                        color = OneUIBlue,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
