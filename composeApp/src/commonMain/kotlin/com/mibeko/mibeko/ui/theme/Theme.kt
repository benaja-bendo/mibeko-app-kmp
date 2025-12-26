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

// Define a simple typography
val AppTypography = Typography(
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
    titleMedium = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 18.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.5.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.25.sp
    )
)

private val LightColorScheme = lightColorScheme(
    primary = MibekoGreenPrimary,
    onPrimary = MibekoTextOnPrimary,
    primaryContainer = MibekoGreenLight,
    onPrimaryContainer = MibekoTextOnPrimary,
    
    secondary = MibekoStatusGreen,
    onSecondary = MibekoTextOnPrimary,
    
    background = MibekoBackground,
    onBackground = MibekoTextPrimary,
    
    surface = MibekoSurface,
    onSurface = MibekoTextPrimary,
    
    error = Color(0xFFB00020)
)

private val DarkColorScheme = darkColorScheme(
    primary = MibekoGreenLight,
    onPrimary = MibekoTextOnPrimary,
    background = Color(0xFF121212),
    onBackground = MibekoBackground,
    surface = Color(0xFF1E1E1E),
    onSurface = MibekoBackground
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
