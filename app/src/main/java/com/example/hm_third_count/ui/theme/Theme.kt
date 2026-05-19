package com.example.hm_third_count.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = TravelDarkPrimary,
    onPrimary = TravelDarkOnPrimary,
    primaryContainer = TravelDarkPrimaryContainer,
    onPrimaryContainer = TravelDarkOnPrimaryContainer,
    secondary = TravelDarkSecondary,
    onSecondary = TravelDarkOnSecondary,
    secondaryContainer = TravelDarkSecondaryContainer,
    onSecondaryContainer = TravelDarkOnSecondaryContainer,
    tertiary = TravelDarkTertiary,
    onTertiary = TravelDarkOnTertiary,
    tertiaryContainer = TravelDarkTertiaryContainer,
    onTertiaryContainer = TravelDarkOnTertiaryContainer,
    background = TravelDarkBackground,
    onBackground = TravelDarkOnBackground,
    surface = TravelDarkSurface,
    onSurface = TravelDarkOnSurface,
    surfaceVariant = TravelDarkSurfaceVariant,
    onSurfaceVariant = TravelDarkOnSurfaceVariant,
    outline = TravelDarkOutline,
    error = TravelDarkError,
    errorContainer = TravelDarkErrorContainer,
    onErrorContainer = TravelDarkOnErrorContainer
)

private val LightColorScheme = lightColorScheme(
    primary = TravelLightPrimary,
    onPrimary = TravelLightOnPrimary,
    primaryContainer = TravelLightPrimaryContainer,
    onPrimaryContainer = TravelLightOnPrimaryContainer,
    secondary = TravelLightSecondary,
    onSecondary = TravelLightOnSecondary,
    secondaryContainer = TravelLightSecondaryContainer,
    onSecondaryContainer = TravelLightOnSecondaryContainer,
    tertiary = TravelLightTertiary,
    onTertiary = TravelLightOnTertiary,
    tertiaryContainer = TravelLightTertiaryContainer,
    onTertiaryContainer = TravelLightOnTertiaryContainer,
    background = TravelLightBackground,
    onBackground = TravelLightOnBackground,
    surface = TravelLightSurface,
    onSurface = TravelLightOnSurface,
    surfaceVariant = TravelLightSurfaceVariant,
    onSurfaceVariant = TravelLightOnSurfaceVariant,
    outline = TravelLightOutline,
    error = TravelLightError,
    errorContainer = TravelLightErrorContainer,
    onErrorContainer = TravelLightOnErrorContainer
)

@Composable
fun HM_third_countTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
