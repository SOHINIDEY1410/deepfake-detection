package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.example.data.ThemeMode

private val ClinicalLightColorScheme = lightColorScheme(
    primary = ClinicalTealPrimary,
    onPrimary = ClinicalTealOnPrimary,
    primaryContainer = ClinicalTealContainer,
    onPrimaryContainer = ClinicalTealOnContainer,
    secondary = ClinicalSlateSecondary,
    onSecondary = ClinicalSlateOnSecondary,
    secondaryContainer = ClinicalSlateContainer,
    onSecondaryContainer = ClinicalSlateOnContainer,
    tertiary = ClinicalIndigoTertiary,
    onTertiary = ClinicalIndigoOnTertiary,
    tertiaryContainer = ClinicalIndigoContainer,
    onTertiaryContainer = ClinicalIndigoOnContainer,
    background = LightBackground,
    onBackground = LightOnBackground,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    surfaceContainerLow = LightSurfaceContainerLow,
    surfaceContainer = LightSurfaceContainer,
    surfaceContainerHigh = LightSurfaceContainerHigh,
    outline = LightOutline,
    outlineVariant = LightOutlineVariant,
    error = LightError,
    onError = LightOnError,
    errorContainer = LightErrorContainer,
    onErrorContainer = LightOnErrorContainer
)

private val ClinicalDarkColorScheme = darkColorScheme(
    primary = DarkTealPrimary,
    onPrimary = DarkTealOnPrimary,
    primaryContainer = DarkTealContainer,
    onPrimaryContainer = DarkTealOnContainer,
    secondary = DarkSlateSecondary,
    onSecondary = DarkSlateOnSecondary,
    secondaryContainer = DarkSlateContainer,
    onSecondaryContainer = DarkSlateOnContainer,
    tertiary = DarkIndigoTertiary,
    onTertiary = DarkIndigoOnTertiary,
    tertiaryContainer = DarkIndigoContainer,
    onTertiaryContainer = DarkIndigoOnContainer,
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    surfaceContainerLow = DarkSurfaceContainerLow,
    surfaceContainer = DarkSurfaceContainer,
    surfaceContainerHigh = DarkSurfaceContainerHigh,
    outline = DarkOutline,
    outlineVariant = DarkOutlineVariant,
    error = DarkError,
    onError = DarkOnError,
    errorContainer = DarkErrorContainer,
    onErrorContainer = DarkOnErrorContainer
)

private val VeriMedShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(26.dp)
)

@Composable
fun VeriMedTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val isDark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val colorScheme = if (isDark) ClinicalDarkColorScheme else ClinicalLightColorScheme
    val clinicalColors = if (isDark) DarkClinicalStatusColors else LightClinicalStatusColors

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                val controller = WindowCompat.getInsetsController(window, view)
                controller.isAppearanceLightStatusBars = !isDark
                controller.isAppearanceLightNavigationBars = !isDark
            }
        }
    }

    CompositionLocalProvider(LocalClinicalColors provides clinicalColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = VeriMedShapes,
            content = content
        )
    }
}
