package com.clockity.app.ui.worldclock

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brightness2
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clockity.app.data.models.WorldCity
import com.clockity.app.ui.theme.*

@Composable
fun CityCard(
    city: WorldCity,
    offsetHours: Float,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (timeStr, amPm) = city.formatTime(offsetHours)
    val isDay = city.isDaytime(offsetHours)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(OneUICardDark)
            .padding(horizontal = 20.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Day/Night indicator icon
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(if (isDay) OneUIYellow.copy(alpha = 0.15f) else OneUIBlue.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isDay) Icons.Default.WbSunny else Icons.Default.Brightness2,
                contentDescription = if (isDay) "Day" else "Night",
                tint = if (isDay) OneUIYellow else OneUIBlueLight,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        // City & Country & Time difference
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = city.cityName,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = OneUITextPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = city.countryName,
                    fontSize = 13.sp,
                    color = OneUITextSecondary
                )
                Text(
                    text = " • ",
                    fontSize = 13.sp,
                    color = OneUITextTertiary
                )
                Text(
                    text = city.formatTimeDifference(),
                    fontSize = 13.sp,
                    color = OneUIBlueLight,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Digital Time Display
        Row(
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                text = timeStr,
                fontSize = 28.sp,
                fontWeight = FontWeight.Light,
                color = OneUITextPrimary
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = amPm,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = OneUITextSecondary,
                modifier = Modifier.padding(bottom = 3.dp)
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Delete button
        IconButton(
            onClick = onDelete,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Delete City",
                tint = OneUITextTertiary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
