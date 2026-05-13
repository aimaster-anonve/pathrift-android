package com.pathrift.anonve.android.core.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// ---- Pathrift Brand Colors ----
val PathriftBackground = Color(0xFF0A0A0F)
val PathriftSurface = Color(0xFF12121A)
val PathriftSurfaceVariant = Color(0xFF1E1E2E)

val PathriftNeonBlue = Color(0xFF00C8FF)
val PathriftPurple = Color(0xFF8B4FFF)
val PathriftOrange = Color(0xFFFF6B00)
val PathriftGold = Color(0xFFFFD700)
val PathriftDanger = Color(0xFFFF2D55)
val PathriftSuccess = Color(0xFF30D158)

val PathriftTextPrimary = Color(0xFFFFFFFF)
val PathriftTextSecondary = Color(0xFF8E8E93)
val PathriftTextDisabled = Color(0xFF48484A)

// ---- Tower Colors ----
val BoltTowerColor = PathriftNeonBlue
val BlastTowerColor = PathriftOrange
val FrostTowerColor = PathriftPurple
val PierceTowerColor = Color(0xFFCCFF00)   // yellow-green
val CoreTowerColor = Color(0xFFFF4400)     // red-orange
val InfernoTowerColor = Color(0xFFFF2200)  // deep red
val TeslaTowerColor = Color(0xFF00AAFF)    // electric blue
val NovaTowerColor = Color(0xFFFFD700)     // gold

// ---- Enemy Colors ----
val RunnerEnemyColor = PathriftDanger
val TankEnemyColor = PathriftOrange

// ---- Semantic Colors ----
val PositiveColor = PathriftSuccess
val NegativeColor = PathriftDanger
val WarningColor = PathriftGold
val AccentColor = PathriftNeonBlue

// ---- Typography ----
private val PathriftTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 57.sp,
        lineHeight = 64.sp,
        color = PathriftTextPrimary
    ),
    displayMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 45.sp,
        lineHeight = 52.sp,
        color = PathriftTextPrimary
    ),
    headlineLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        color = PathriftTextPrimary
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        color = PathriftTextPrimary
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 28.sp,
        color = PathriftTextPrimary
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        color = PathriftTextPrimary
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        color = PathriftTextPrimary
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        color = PathriftTextPrimary
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        color = PathriftTextSecondary
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        color = PathriftTextSecondary
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        color = PathriftTextPrimary
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp,
        color = PathriftTextSecondary
    )
)

// ---- Color Scheme ----
private val PathriftDarkColorScheme = darkColorScheme(
    primary = PathriftNeonBlue,
    onPrimary = Color.Black,
    primaryContainer = PathriftSurfaceVariant,
    onPrimaryContainer = PathriftNeonBlue,

    secondary = PathriftPurple,
    onSecondary = Color.White,
    secondaryContainer = PathriftSurface,
    onSecondaryContainer = PathriftPurple,

    tertiary = PathriftOrange,
    onTertiary = Color.White,

    background = PathriftBackground,
    onBackground = PathriftTextPrimary,

    surface = PathriftSurface,
    onSurface = PathriftTextPrimary,
    surfaceVariant = PathriftSurfaceVariant,
    onSurfaceVariant = PathriftTextSecondary,

    error = PathriftDanger,
    onError = Color.White,

    outline = PathriftTextDisabled
)

@Composable
fun PathriftTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = PathriftDarkColorScheme,
        typography = PathriftTypography,
        content = content
    )
}
