package com.clockity.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
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
import com.clockity.app.ui.theme.OneUIBlack
import com.clockity.app.ui.theme.OneUICardElevated
import com.clockity.app.ui.theme.OneUITextPrimary
import com.clockity.app.ui.theme.OneUITextSecondary

@Composable
fun OneUIHeader(
    title: String,
    subtitle: String? = null,
    onAddClick: (() -> Unit)? = null,
    onMenuClick: (() -> Unit)? = null,
    extraContent: (@Composable () -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(OneUIBlack)
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        // Top Action Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.End),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onAddClick != null) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(OneUICardElevated)
                        .clickable { onAddClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add",
                        tint = OneUITextPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            if (onMenuClick != null) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(OneUICardElevated)
                        .clickable { onMenuClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "More",
                        tint = OneUITextPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Large One UI Viewing Title
        Text(
            text = title,
            fontSize = 34.sp,
            fontWeight = FontWeight.Bold,
            color = OneUITextPrimary
        )

        // Subtitle (e.g. Next alarm in X hrs or summary)
        if (!subtitle.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                fontSize = 15.sp,
                color = OneUITextSecondary,
                fontWeight = FontWeight.Normal
            )
        }

        if (extraContent != null) {
            Spacer(modifier = Modifier.height(12.dp))
            extraContent()
        }
    }
}
