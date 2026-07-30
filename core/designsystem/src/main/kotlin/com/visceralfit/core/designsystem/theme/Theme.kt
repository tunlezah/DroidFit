package com.visceralfit.core.designsystem.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val LightScheme = lightColorScheme(
    primary = Palette.Teal40,
    onPrimary = Palette.White,
    primaryContainer = Palette.Teal90,
    onPrimaryContainer = Palette.Teal10,
    secondary = Palette.Slate40,
    onSecondary = Palette.White,
    secondaryContainer = Palette.Slate90,
    onSecondaryContainer = Palette.Slate20,
    tertiary = Palette.Amber40,
    onTertiary = Palette.White,
    tertiaryContainer = Palette.Amber90,
    error = Palette.Red40,
    onError = Palette.White,
    errorContainer = Palette.Red90,
    surface = Palette.NeutralLightSurface,
    onSurface = Palette.NeutralLightOnSurface,
)

private val DarkScheme = darkColorScheme(
    primary = Palette.Teal80,
    onPrimary = Palette.Teal20,
    primaryContainer = Palette.Teal30,
    onPrimaryContainer = Palette.Teal90,
    secondary = Palette.Slate80,
    onSecondary = Palette.Slate20,
    secondaryContainer = Palette.Slate30,
    onSecondaryContainer = Palette.Slate90,
    tertiary = Palette.Amber80,
    onTertiary = Palette.Slate20,
    error = Palette.Red80,
    surface = Palette.NeutralDarkSurface,
    onSurface = Palette.NeutralDarkOnSurface,
)

/**
 * True-black variant for the Motorola Edge 60's pOLED panel: unlit pixels draw no
 * power, so a workout screen that sits open for 45 minutes costs measurably less
 * battery in AMOLED mode. See /framework/15_device_targets_motorola_edge_60.md.
 */
private val AmoledScheme = DarkScheme.copy(
    background = Color.Black,
    surface = Color.Black,
    surfaceContainerLowest = Color.Black,
    surfaceContainerLow = Color(0xFF0A0F0F),
    surfaceContainer = Color(0xFF121818),
)

val LocalZoneColours: ProvidableCompositionLocal<ZoneColours> =
    staticCompositionLocalOf { ZoneColours.Light }

/**
 * @param darkTheme whether to use a dark scheme; callers pass the resolved user
 *   preference rather than letting the theme read settings itself, so previews and
 *   screenshot tests can force either mode.
 * @param amoled collapse dark surfaces to true black.
 * @param dynamicColour honour the Material You wallpaper palette. Ignored below
 *   API 31 and overridden to false when [amoled] is set, because a wallpaper-derived
 *   surface is never pure black.
 */
@Composable
fun VisceralFitTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    amoled: Boolean = false,
    dynamicColour: Boolean = true,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val useDynamic = dynamicColour && !amoled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    val colourScheme = when {
        useDynamic && darkTheme -> dynamicDarkColorScheme(context)
        useDynamic -> dynamicLightColorScheme(context)
        darkTheme && amoled -> AmoledScheme
        darkTheme -> DarkScheme
        else -> LightScheme
    }

    val zoneColours = if (darkTheme) ZoneColours.Dark else ZoneColours.Light

    CompositionLocalProvider(LocalZoneColours provides zoneColours) {
        MaterialTheme(
            colorScheme = colourScheme,
            typography = VisceralFitTypography,
            shapes = VisceralFitShapes,
            content = content,
        )
    }
}

/** Exposed so screens can reference the timer type scale without importing internals. */
val MaterialTheme.zoneColours: ZoneColours
    @Composable get() = LocalZoneColours.current

internal val VisceralFitTypography: Typography = buildVisceralFitTypography()
