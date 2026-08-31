package com.clockity.app.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = OneUIBlue,
    onPrimary = OneUIBlack,
    secondary = OneUIYellow,
    onSecondary = OneUIBlack,
    tertiary = OneUIYellow,
    background = OneUIBlack,
    onBackground = OneUITextPrimary,
    surface = OneUISurfaceDark,
    onSurface = OneUITextPrimary,
    surfaceVariant = OneUICardDark,
    onSurfaceVariant = OneUITextSecondary,
    outline = OneUIDivider,
    error = OneUIRed,
    onError = OneUIBlack
)

@Composable
fun ClockityTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = DarkColorScheme
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
