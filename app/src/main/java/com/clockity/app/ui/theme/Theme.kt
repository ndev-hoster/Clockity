package com.clockity.app.ui.theme

import android.app.Activity
import android.graphics.Color.parseColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.clockity.app.utils.PreferencesManager

@Composable
fun ClockityTheme(
    content: @Composable () -> Unit
) {
    val accentHex by PreferencesManager.accentColorHex.collectAsState()
    val isAmoled by PreferencesManager.isAmoledBlack.collectAsState()

    val currentAccent = remember(accentHex) {
        try {
            Color(parseColor(accentHex))
        } catch (_: Exception) {
            OneUIBlue
        }
    }

    val cardDark = if (isAmoled) OneUIBlack else Color(0xFF1C1C1E)

    val colorScheme = darkColorScheme(
        primary = currentAccent,
        onPrimary = OneUIBlack,
        secondary = OneUIYellow,
        onSecondary = OneUIBlack,
        tertiary = OneUIYellow,
        background = OneUIBlack,
        onBackground = OneUITextPrimary,
        surface = OneUISurfaceDark,
        onSurface = OneUITextPrimary,
        surfaceVariant = cardDark,
        onSurfaceVariant = OneUITextSecondary,
        outline = OneUIDivider,
        error = OneUIRed,
        onError = OneUIBlack
    )

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = OneUIBlack.toArgb()
            window.navigationBarColor = OneUIBlack.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
