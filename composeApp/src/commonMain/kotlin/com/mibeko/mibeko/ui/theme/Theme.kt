package com.mibeko.mibeko.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Refined Typography - Serif for Headings (Prestige), Sans for Body (Readability)
val AppTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Bold,
        fontSize = 57.sp,
        lineHeight = 64.sp,
        letterSpacing = (-0.25).sp
    ),
    headlineLarge = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = 0.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        lineHeight = 36.sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Serif, // Changed to Serif for section headers
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
)

private val LightColorScheme = lightColorScheme(
    primary = MibekoBluePrimary,
    onPrimary = OnPrimary,
    primaryContainer = MibekoBlueLight,
    onPrimaryContainer = OnPrimary,
    
    secondary = MibekoGold,
    onSecondary = OnSecondary,
    secondaryContainer = MibekoGoldLight,
    onSecondaryContainer = Color.Black,
    
    background = MibekoBackgroundLight,
    onBackground = TextPrimaryLight,
    
    surface = MibekoSurfaceLight,
    onSurface = TextPrimaryLight,
    
    surfaceVariant = MibekoBlueBackground,
    onSurfaceVariant = MibekoBlueDark,
    
    error = LegalRepealed,
    onError = Color.White
)

private val DarkColorScheme = darkColorScheme(
    primary = MibekoBlueLight,
    onPrimary = OnPrimary,
    primaryContainer = MibekoBlueDark,
    onPrimaryContainer = MibekoBlueLight,
    
    secondary = MibekoGold,
    onSecondary = Color.Black,
    secondaryContainer = MibekoGoldDark,
    onSecondaryContainer = MibekoGoldLight,
    
    background = MibekoBackgroundDark,
    onBackground = TextPrimaryDark,
    
    surface = MibekoSurfaceDark,
    onSurface = TextPrimaryDark,
    
    surfaceVariant = Color(0xFF2C2C2C),
    onSurfaceVariant = TextSecondaryDark,
    
    error = Color(0xFFCF6679),
    onError = Color.Black
)


@Composable
fun MibekoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}
