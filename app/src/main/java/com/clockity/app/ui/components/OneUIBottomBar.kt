package com.clockity.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clockity.app.ui.theme.OneUIBlack
import com.clockity.app.ui.theme.OneUIBlue
import com.clockity.app.ui.theme.OneUICardElevated
import com.clockity.app.ui.theme.OneUITextSecondary

enum class ClockTab(val title: String, val icon: ImageVector) {
    ALARM("Alarm", Icons.Default.Alarm),
    WORLD_CLOCK("World clock", Icons.Default.Public),
    STOPWATCH("Stopwatch", Icons.Default.Timer),
    TIMER("Timer", Icons.Default.HourglassBottom),
    SETTINGS("Settings", Icons.Default.Settings)
}

@Composable
fun OneUIBottomBar(
    currentTab: ClockTab,
    onTabSelected: (ClockTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(OneUIBlack)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {
        ClockTab.entries.forEach { tab ->
            val isSelected = currentTab == tab
            val tabTint by animateColorAsState(
                targetValue = if (isSelected) OneUIBlue else OneUITextSecondary,
                label = "tab_color"
            )

            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onTabSelected(tab) }
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = tab.icon,
                    contentDescription = tab.title,
                    tint = tabTint,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = tab.title,
                    fontSize = 12.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    color = tabTint
                )
            }
        }
    }
}
