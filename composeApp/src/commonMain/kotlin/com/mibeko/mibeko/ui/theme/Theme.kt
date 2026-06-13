package com.mibeko.mibeko.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mibeko.composeapp.generated.resources.Res
import mibeko.composeapp.generated.resources.inter_bold
import mibeko.composeapp.generated.resources.inter_medium
import mibeko.composeapp.generated.resources.inter_regular
import mibeko.composeapp.generated.resources.inter_semibold
import mibeko.composeapp.generated.resources.source_serif4_italic
import mibeko.composeapp.generated.resources.source_serif4_regular
import mibeko.composeapp.generated.resources.source_serif4_semibold
import org.jetbrains.compose.resources.Font

/**
 * Inter : éléments d'interface, navigation, titres (« l'app est un outil »).
 */
@Composable
fun interFamily() = FontFamily(
    Font(Res.font.inter_regular, weight = FontWeight.Normal),
    Font(Res.font.inter_medium, weight = FontWeight.Medium),
    Font(Res.font.inter_semibold, weight = FontWeight.SemiBold),
    Font(Res.font.inter_bold, weight = FontWeight.Bold)
)

/**
 * Source Serif 4 : contenus juridiques longs (« le texte fait autorité »).
 */
@Composable
fun sourceSerifFamily() = FontFamily(
    Font(Res.font.source_serif4_regular, weight = FontWeight.Normal),
    Font(Res.font.source_serif4_semibold, weight = FontWeight.SemiBold),
    Font(Res.font.source_serif4_italic, weight = FontWeight.Normal, style = FontStyle.Italic)
)

/**
 * Typographie hybride du design system :
 * Inter pour l'interface, Source Serif 4 pour la lecture juridique,
 * interlignages généreux (1.6–1.75) pour la terminologie technique.
 */
@Composable
fun mibekoTypography(): Typography {
    val inter = interFamily()
    val serif = sourceSerifFamily()

    return Typography(
        displayLarge = TextStyle(
            fontFamily = inter,
            fontWeight = FontWeight.Bold,
            fontSize = 48.sp,
            lineHeight = 56.sp,
            letterSpacing = (-0.96).sp
        ),
        displayMedium = TextStyle(
            fontFamily = inter,
            fontWeight = FontWeight.Bold,
            fontSize = 40.sp,
            lineHeight = 48.sp,
            letterSpacing = (-0.6).sp
        ),
        headlineLarge = TextStyle(
            fontFamily = inter,
            fontWeight = FontWeight.SemiBold,
            fontSize = 28.sp,
            lineHeight = 36.sp,
            letterSpacing = (-0.28).sp
        ),
        headlineMedium = TextStyle(
            fontFamily = inter,
            fontWeight = FontWeight.SemiBold,
            fontSize = 24.sp,
            lineHeight = 32.sp
        ),
        headlineSmall = TextStyle(
            fontFamily = inter,
            fontWeight = FontWeight.SemiBold,
            fontSize = 20.sp,
            lineHeight = 28.sp
        ),
        titleLarge = TextStyle(
            fontFamily = inter,
            fontWeight = FontWeight.SemiBold,
            fontSize = 20.sp,
            lineHeight = 28.sp
        ),
        titleMedium = TextStyle(
            fontFamily = inter,
            fontWeight = FontWeight.Medium,
            fontSize = 16.sp,
            lineHeight = 24.sp,
            letterSpacing = 0.15.sp
        ),
        titleSmall = TextStyle(
            fontFamily = inter,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.1.sp
        ),
        // Corps de texte : Source Serif 4 (contenu juridique long)
        bodyLarge = TextStyle(
            fontFamily = serif,
            fontWeight = FontWeight.Normal,
            fontSize = 18.sp,
            lineHeight = 32.sp
        ),
        bodyMedium = TextStyle(
            fontFamily = serif,
            fontWeight = FontWeight.Normal,
            fontSize = 16.sp,
            lineHeight = 28.sp
        ),
        bodySmall = TextStyle(
            fontFamily = inter,
            fontWeight = FontWeight.Normal,
            fontSize = 12.sp,
            lineHeight = 16.sp
        ),
        labelLarge = TextStyle(
            fontFamily = inter,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.7.sp
        ),
        labelMedium = TextStyle(
            fontFamily = inter,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.6.sp
        ),
        labelSmall = TextStyle(
            fontFamily = inter,
            fontWeight = FontWeight.Normal,
            fontSize = 11.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.5.sp
        )
    )
}

/**
 * Formes « douces » : 4dp par défaut (boutons, champs), 8dp max pour les
 * grandes cartes. Pas de pilules ni de formes « bubbly ».
 */
val MibekoShapes = Shapes(
    extraSmall = RoundedCornerShape(2.dp),
    small = RoundedCornerShape(4.dp),
    medium = RoundedCornerShape(6.dp),
    large = RoundedCornerShape(8.dp),
    extraLarge = RoundedCornerShape(12.dp)
)

private val LightColorScheme = lightColorScheme(
    // Vert d'action aligné sur le web (#1E6B47) : lisible sur crème, AA sur blanc.
    primary = MibekoGreen,
    onPrimary = OnPrimary,
    primaryContainer = MibekoForestFixed,
    onPrimaryContainer = MibekoForestDark,
    inversePrimary = MibekoForestInverse,

    secondary = MibekoTerracotta,
    onSecondary = OnSecondary,
    secondaryContainer = MibekoTerracottaFixed,
    onSecondaryContainer = MibekoOnTerracottaContainer,

    tertiary = MibekoTertiary,
    onTertiary = MibekoWhite,
    tertiaryContainer = MibekoTertiaryFixed,
    onTertiaryContainer = MibekoOnTertiaryFixedVariant,

    background = MibekoCream,
    onBackground = MibekoOnSurface,

    surface = MibekoCream,
    onSurface = MibekoOnSurface,
    surfaceDim = MibekoCreamDim,
    surfaceBright = MibekoCream,
    surfaceContainerLowest = MibekoSurfaceContainerLowest,
    surfaceContainerLow = MibekoSurfaceContainerLow,
    surfaceContainer = MibekoSurfaceContainer,
    surfaceContainerHigh = MibekoSurfaceContainerHigh,
    surfaceContainerHighest = MibekoSurfaceContainerHighest,
    surfaceVariant = MibekoSurfaceContainerHighest,
    onSurfaceVariant = MibekoOnSurfaceVariant,
    surfaceTint = MibekoGreen,

    inverseSurface = MibekoInverseSurface,
    inverseOnSurface = MibekoInverseOnSurface,

    outline = MibekoOutline,
    outlineVariant = MibekoOutlineVariant,

    error = MibekoError,
    onError = MibekoOnError,
    errorContainer = MibekoErrorContainer,
    onErrorContainer = MibekoOnErrorContainer
)

private val DarkColorScheme = darkColorScheme(
    primary = MibekoForestInverse,
    onPrimary = MibekoForestDark,
    primaryContainer = MibekoOnForestFixedVariant,
    onPrimaryContainer = MibekoForestFixed,
    inversePrimary = MibekoForestContainer,

    secondary = MibekoTerracottaFixedDim,
    onSecondary = MibekoOnTerracottaFixed,
    secondaryContainer = MibekoTerracottaDark,
    onSecondaryContainer = MibekoTerracottaFixed,

    tertiary = MibekoTertiaryFixedDim,
    onTertiary = MibekoTertiary,
    tertiaryContainer = MibekoOnTertiaryFixedVariant,
    onTertiaryContainer = MibekoTertiaryFixed,

    background = MibekoDarkSurface,
    onBackground = MibekoDarkOnSurface,

    surface = MibekoDarkSurface,
    onSurface = MibekoDarkOnSurface,
    surfaceDim = MibekoDarkSurface,
    surfaceBright = MibekoDarkSurfaceContainerHighest,
    surfaceContainerLowest = MibekoDarkSurfaceContainerLowest,
    surfaceContainerLow = MibekoDarkSurfaceContainerLow,
    surfaceContainer = MibekoDarkSurfaceContainer,
    surfaceContainerHigh = MibekoDarkSurfaceContainerHigh,
    surfaceContainerHighest = MibekoDarkSurfaceContainerHighest,
    surfaceVariant = MibekoDarkSurfaceContainerHigh,
    onSurfaceVariant = MibekoDarkOnSurfaceVariant,
    surfaceTint = MibekoForestInverse,

    inverseSurface = MibekoDarkOnSurface,
    inverseOnSurface = MibekoInverseSurface,

    outline = MibekoDarkOutline,
    outlineVariant = MibekoDarkOutlineVariant,

    error = MibekoErrorDark,
    onError = MibekoOnErrorDark,
    errorContainer = MibekoErrorContainerDark,
    onErrorContainer = MibekoOnErrorContainerDark
)

/**
 * Couleurs de statut juridique, résolues selon le thème : les variantes
 * sombres garantissent le contraste sur fond sombre (le vert/rouge clairs
 * y deviendraient illisibles).
 */
@Immutable
data class MibekoStatusColors(
    val valid: Color,
    val repealed: Color,
    val pending: Color
)

private val LightStatusColors = MibekoStatusColors(
    valid = LegalValid,
    repealed = LegalRepealed,
    pending = LegalPending
)

private val DarkStatusColors = MibekoStatusColors(
    valid = LegalValidDark,
    repealed = LegalRepealedDark,
    pending = LegalPendingDark
)

val LocalMibekoStatusColors = staticCompositionLocalOf { LightStatusColors }

/** Point d'accès aux extensions du thème : `MibekoTheme.status.valid`. */
object MibekoTheme {
    val status: MibekoStatusColors
        @Composable get() = LocalMibekoStatusColors.current
}

@Composable
fun MibekoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val statusColors = if (darkTheme) DarkStatusColors else LightStatusColors

    CompositionLocalProvider(LocalMibekoStatusColors provides statusColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = mibekoTypography(),
            shapes = MibekoShapes,
            content = content
        )
    }
}
