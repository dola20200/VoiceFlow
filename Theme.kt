package com.example.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = CartesiaPurplePrimary,
    onPrimary = TextPrimary,
    primaryContainer = CartesiaPurpleDark,
    onPrimaryContainer = TextPrimary,
    secondary = CartesiaPurpleLight,
    onSecondary = TextPrimary,
    tertiary = CartesiaAccentTeal,
    onTertiary = TextPrimary,
    background = CartesiaBackground,
    onBackground = TextPrimary,
    surface = CartesiaSurface,
    onSurface = TextPrimary,
    surfaceVariant = CartesiaSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = CartesiaBorder,
    error = DangerRed,
    onError = TextPrimary
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = DarkColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = CartesiaBackground.toArgb()
                window.navigationBarColor = CartesiaBackground.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
                WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
