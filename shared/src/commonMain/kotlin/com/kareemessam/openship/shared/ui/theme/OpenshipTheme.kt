package com.kareemessam.openship.shared.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.*

enum class ThemeMode {
    SYSTEM,
    DARK,
    LIGHT
}

val LocalThemeMode = compositionLocalOf { mutableStateOf(ThemeMode.DARK) }

private val OpenshipDarkColorScheme = darkColorScheme(
    background = OpenshipColors.Dark.BgPage,
    surface = OpenshipColors.Dark.BgPage,
    surfaceVariant = OpenshipColors.Dark.BgCard,
    surfaceContainer = OpenshipColors.Dark.BgCard,
    surfaceContainerHigh = OpenshipColors.Dark.BgCardElevated,
    onBackground = OpenshipColors.Dark.TextTitle,
    onSurface = OpenshipColors.Dark.TextTitle,
    onSurfaceVariant = OpenshipColors.Dark.TextBody,
    outline = OpenshipColors.Dark.BorderDefault,
    outlineVariant = OpenshipColors.Dark.BorderSubtle,
    primary = OpenshipColors.Dark.ButtonPrimaryBg,
    onPrimary = OpenshipColors.Dark.ButtonPrimaryText,
    primaryContainer = OpenshipColors.Dark.BgCardElevated,
    onPrimaryContainer = OpenshipColors.Dark.TextHeading,
    secondary = OpenshipColors.Dark.StatusInfoSolid,
    onSecondary = OpenshipColors.Dark.ButtonPrimaryBg,
    secondaryContainer = OpenshipColors.Dark.BgCardElevated,
    onSecondaryContainer = OpenshipColors.Dark.TextTitle,
    error = OpenshipColors.Dark.StatusDangerFg,
    onError = OpenshipColors.Dark.ButtonPrimaryText,
    errorContainer = OpenshipColors.Dark.StatusDangerBg,
    onErrorContainer = OpenshipColors.Dark.StatusDangerFg
)

private val OpenshipLightColorScheme = lightColorScheme(
    background = OpenshipColors.Light.BgPage,
    surface = OpenshipColors.Light.BgPage,
    surfaceVariant = OpenshipColors.Light.BgCard,
    surfaceContainer = OpenshipColors.Light.BgCard,
    surfaceContainerHigh = OpenshipColors.Light.BgCardElevated,
    onBackground = OpenshipColors.Light.TextTitle,
    onSurface = OpenshipColors.Light.TextTitle,
    onSurfaceVariant = OpenshipColors.Light.TextBody,
    outline = OpenshipColors.Light.BorderDefault,
    outlineVariant = OpenshipColors.Light.BorderSubtle,
    primary = OpenshipColors.Light.ButtonPrimaryBg,
    onPrimary = OpenshipColors.Light.ButtonPrimaryText,
    primaryContainer = OpenshipColors.Light.BgCardElevated,
    onPrimaryContainer = OpenshipColors.Light.TextHeading,
    secondary = OpenshipColors.Light.StatusInfoSolid,
    onSecondary = OpenshipColors.Light.ButtonPrimaryBg,
    secondaryContainer = OpenshipColors.Light.BgCardElevated,
    onSecondaryContainer = OpenshipColors.Light.TextTitle,
    error = OpenshipColors.Light.StatusDangerFg,
    onError = OpenshipColors.Light.ButtonPrimaryText,
    errorContainer = OpenshipColors.Light.StatusDangerBg,
    onErrorContainer = OpenshipColors.Light.StatusDangerFg
)

@Composable
fun OpenshipTheme(
    themeModeState: MutableState<ThemeMode> = remember { mutableStateOf(ThemeMode.DARK) },
    content: @Composable () -> Unit
) {
    val isSystemDark = isSystemInDarkTheme()
    val isDark = when (themeModeState.value) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> isSystemDark
    }

    val customColors = if (isDark) darkOpenshipColors() else lightOpenshipColors()
    val colorScheme = if (isDark) OpenshipDarkColorScheme else OpenshipLightColorScheme

    CompositionLocalProvider(
        LocalOpenshipColors provides customColors,
        LocalThemeMode provides themeModeState
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            content = content
        )
    }
}

object OpenshipAppTheme {
    val colors: OpenshipCustomColors
        @Composable
        @ReadOnlyComposable
        get() = LocalOpenshipColors.current
}
