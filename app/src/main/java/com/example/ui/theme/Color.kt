package com.example.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// --- Clinical Daylight (Light Mode) Palette ---
val ClinicalTealPrimary = Color(0xFF0B6E75)
val ClinicalTealOnPrimary = Color(0xFFFFFFFF)
val ClinicalTealContainer = Color(0xFFD6F1F2)
val ClinicalTealOnContainer = Color(0xFF042F33)

val ClinicalSlateSecondary = Color(0xFF3E5666)
val ClinicalSlateOnSecondary = Color(0xFFFFFFFF)
val ClinicalSlateContainer = Color(0xFFE3ECF2)
val ClinicalSlateOnContainer = Color(0xFF152633)

val ClinicalIndigoTertiary = Color(0xFF2B5C8F)
val ClinicalIndigoOnTertiary = Color(0xFFFFFFFF)
val ClinicalIndigoContainer = Color(0xFFDCEAFB)
val ClinicalIndigoOnContainer = Color(0xFF0C2745)

val LightBackground = Color(0xFFF6F8FA)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceVariant = Color(0xFFEEF2F6)
val LightSurfaceContainerLow = Color(0xFFFAFBFD)
val LightSurfaceContainer = Color(0xFFF1F5F8)
val LightSurfaceContainerHigh = Color(0xFFE8EEF3)
val LightOnBackground = Color(0xFF0E1722)
val LightOnSurface = Color(0xFF0E1722)
val LightOnSurfaceVariant = Color(0xFF4B5C6B)
val LightOutline = Color(0xFFC5D0DA)
val LightOutlineVariant = Color(0xFFE1E8EE)

// --- Radiology Reading Room (Dark Mode) Palette ---
val DarkTealPrimary = Color(0xFF38D4C6)
val DarkTealOnPrimary = Color(0xFF002926)
val DarkTealContainer = Color(0xFF0B3F42)
val DarkTealOnContainer = Color(0xFFB5F4EE)

val DarkSlateSecondary = Color(0xFF9BB4C7)
val DarkSlateOnSecondary = Color(0xFF0F2230)
val DarkSlateContainer = Color(0xFF1F303E)
val DarkSlateOnContainer = Color(0xFFD2E2EE)

val DarkIndigoTertiary = Color(0xFF7CB7F4)
val DarkIndigoOnTertiary = Color(0xFF07223E)
val DarkIndigoContainer = Color(0xFF183454)
val DarkIndigoOnContainer = Color(0xFFD5E8FC)

val DarkBackground = Color(0xFF0A1016)
val DarkSurface = Color(0xFF101821)
val DarkSurfaceVariant = Color(0xFF1A2532)
val DarkSurfaceContainerLow = Color(0xFF121B24)
val DarkSurfaceContainer = Color(0xFF17222E)
val DarkSurfaceContainerHigh = Color(0xFF202E3D)
val DarkOnBackground = Color(0xFFECF2F7)
val DarkOnSurface = Color(0xFFECF2F7)
val DarkOnSurfaceVariant = Color(0xFF95A8BA)
val DarkOutline = Color(0xFF344557)
val DarkOutlineVariant = Color(0xFF1F2D3B)

// Error Colors
val LightError = Color(0xFFBE123C)
val LightOnError = Color(0xFFFFFFFF)
val LightErrorContainer = Color(0xFFFFE4EA)
val LightOnErrorContainer = Color(0xFF4C0519)

val DarkError = Color(0xFFFB7185)
val DarkOnError = Color(0xFF4C0519)
val DarkErrorContainer = Color(0xFF5A0D22)
val DarkOnErrorContainer = Color(0xFFFFD9E1)

@Immutable
data class ClinicalStatusColors(
    val authentic: Color,
    val authenticContainer: Color,
    val onAuthenticContainer: Color,
    val suspicious: Color,
    val suspiciousContainer: Color,
    val onSuspiciousContainer: Color,
    val synthetic: Color,
    val syntheticContainer: Color,
    val onSyntheticContainer: Color,
    val inconclusive: Color,
    val inconclusiveContainer: Color,
    val onInconclusiveContainer: Color,
    val hairlineBorder: Color,
    val gridAccent: Color
)

val LightClinicalStatusColors = ClinicalStatusColors(
    authentic = Color(0xFF0F766E),
    authenticContainer = Color(0xFFDDF6F2),
    onAuthenticContainer = Color(0xFF073B37),
    suspicious = Color(0xFFB45309),
    suspiciousContainer = Color(0xFFFEF3C7),
    onSuspiciousContainer = Color(0xFF451A03),
    synthetic = Color(0xFFBE123C),
    syntheticContainer = Color(0xFFFFE4E6),
    onSyntheticContainer = Color(0xFF4C0519),
    inconclusive = Color(0xFF475569),
    inconclusiveContainer = Color(0xFFE2E8F0),
    onInconclusiveContainer = Color(0xFF1E293B),
    hairlineBorder = Color(0xFFE2E8F0),
    gridAccent = Color(0xFF0B6E75)
)

val DarkClinicalStatusColors = ClinicalStatusColors(
    authentic = Color(0xFF2DD4BF),
    authenticContainer = Color(0xFF0D3835),
    onAuthenticContainer = Color(0xFFB3F5EC),
    suspicious = Color(0xFFFBBF24),
    suspiciousContainer = Color(0xFF3E2A06),
    onSuspiciousContainer = Color(0xFFFDE68A),
    synthetic = Color(0xFFFB7185),
    syntheticContainer = Color(0xFF4A1021),
    onSyntheticContainer = Color(0xFFFFD6DF),
    inconclusive = Color(0xFF94A3B8),
    inconclusiveContainer = Color(0xFF1E293B),
    onInconclusiveContainer = Color(0xFFE2E8F0),
    hairlineBorder = Color(0xFF233242),
    gridAccent = Color(0xFF38D4C6)
)

val LocalClinicalColors = staticCompositionLocalOf { LightClinicalStatusColors }
