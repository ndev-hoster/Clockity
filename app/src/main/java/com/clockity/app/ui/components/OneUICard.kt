package com.clockity.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.clockity.app.ui.theme.OneUICardDark

@Composable
fun OneUICard(
    modifier: Modifier = Modifier,
    backgroundColor: Color = OneUICardDark,
    cornerRadius: Dp = 24.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val cardModifier = modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(cornerRadius))
        .background(backgroundColor)
        .then(
            if (onClick != null) Modifier.clickable { onClick() } else Modifier
        )

    Column(
        modifier = cardModifier,
        content = content
    )
}
