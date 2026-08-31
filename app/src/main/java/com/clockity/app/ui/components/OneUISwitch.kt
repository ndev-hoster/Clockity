package com.clockity.app.ui.components

import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.clockity.app.ui.theme.OneUIBlack
import com.clockity.app.ui.theme.OneUIBlue
import com.clockity.app.ui.theme.OneUICardElevated
import com.clockity.app.ui.theme.OneUITextSecondary

@Composable
fun OneUISwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    checkedTrackColor: Color = OneUIBlue
) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        colors = SwitchDefaults.colors(
            checkedThumbColor = Color.White,
            checkedTrackColor = checkedTrackColor,
            uncheckedThumbColor = OneUITextSecondary,
            uncheckedTrackColor = OneUICardElevated,
            uncheckedBorderColor = Color.Transparent
        )
    )
}
