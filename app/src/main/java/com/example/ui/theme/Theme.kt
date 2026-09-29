package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

enum class AppThemeMode {
    DARK,
    LIGHT,
    SYSTEM
}

val LocalIsDarkTheme = staticCompositionLocalOf { true }

private val DarkColorScheme = darkColorScheme(
    primary = TealPrimary,
    onPrimary = Color.White,
    primaryContainer = TealDarkSurfaceVariant,
    onPrimaryContainer = TealMint,
    secondary = TealAccent,
    onSecondary = Color.Black,
    tertiary = TealMint,
    background = TealDarkBackground,
    onBackground = TealDarkTextPrimary,
    surface = TealDarkSurface,
    onSurface = TealDarkTextPrimary,
    surfaceVariant = TealDarkSurfaceVariant,
    onSurfaceVariant = TealDarkTextSecondary,
    outline = TealDarkDivider,
    error = ErrorRed,
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = TealPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFCCFBF1),
    onPrimaryContainer = TealPrimaryDark,
    secondary = TealPrimaryDark,
    onSecondary = Color.White,
    tertiary = TealAccent,
    background = TealLightBackground,
    onBackground = TealLightTextPrimary,
    surface = TealLightSurface,
    onSurface = TealLightTextPrimary,
    surfaceVariant = TealLightSurfaceVariant,
    onSurfaceVariant = TealLightTextSecondary,
    outline = TealLightDivider,
    error = ErrorRed,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    themeMode: AppThemeMode = AppThemeMode.DARK,
    content: @Composable () -> Unit
) {
    val isDark = when (themeMode) {
        AppThemeMode.DARK -> true
        AppThemeMode.LIGHT -> false
        AppThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    val colorScheme = if (isDark) DarkColorScheme else LightColorScheme

    CompositionLocalProvider(
        LocalIsDarkTheme provides isDark
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
