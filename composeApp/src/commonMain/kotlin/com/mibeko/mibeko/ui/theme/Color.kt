package com.mibeko.mibeko.ui.theme

import androidx.compose.ui.graphics.Color

// =============================================================================
// Palette Mibeko — DESIGN.md (« Stability, Authority, Accessibility »)
// Vert forêt profond (institution), terracotta sobre (accent), fond crème.
// =============================================================================

// Primary — Deep Forest Green
// MibekoGreen est le vert d'ACTION (boutons, liens, accents) : aligné sur le
// web (mibeko-classic.ts : « #1e6b47 au lieu de #03271a qui paraissait noir »).
// MibekoForest (#03271A) reste une couleur de MARQUE pour les surfaces héros
// (en-têtes pleins avec texte blanc) — jamais comme couleur de texte/action.
val MibekoGreen = Color(0xFF1E6B47)             // primary (actions) — parité web
val MibekoForest = Color(0xFF03271A)            // surface héros / marque
val MibekoForestContainer = Color(0xFF1B3D2F)   // primary-container
val MibekoOnForestContainer = Color(0xFF84A895) // on-primary-container
val MibekoForestInverse = Color(0xFFAACFBB)     // inverse-primary (primary en sombre)
val MibekoForestLight = Color(0xFF436556)       // surface-tint / variante claire
val MibekoForestDark = Color(0xFF002115)        // on-primary-fixed (le plus profond)

// Fixed tones (M3) — utilisés pour dériver le thème sombre
val MibekoForestFixed = Color(0xFFC5EBD7)           // primary-fixed
val MibekoForestFixedDim = Color(0xFFAACFBB)        // primary-fixed-dim
val MibekoOnForestFixedVariant = Color(0xFF2C4D3F)  // on-primary-fixed-variant

// Secondary — Sober Terracotta
val MibekoTerracotta = Color(0xFF8F4C31)            // secondary
val MibekoTerracottaContainer = Color(0xFFFDA685)   // secondary-container
val MibekoOnTerracottaContainer = Color(0xFF773A20) // on-secondary-container
val MibekoTerracottaFixed = Color(0xFFFFDBCE)       // secondary-fixed
val MibekoTerracottaFixedDim = Color(0xFFFFB59A)    // secondary-fixed-dim
val MibekoTerracottaDark = Color(0xFF72351D)        // on-secondary-fixed-variant
val MibekoTerracottaLight = Color(0xFFFFDBCE)       // alias lisible de secondary-fixed
val MibekoOnTerracottaFixed = Color(0xFF370E00)     // on-secondary-fixed

// Tertiary — vert très sombre de soutien
val MibekoTertiary = Color(0xFF142519)
val MibekoTertiaryContainer = Color(0xFF293B2E)
val MibekoOnTertiaryContainer = Color(0xFF91A594)
val MibekoTertiaryFixed = Color(0xFFD3E8D5)
val MibekoTertiaryFixedDim = Color(0xFFB7CCB9)
val MibekoOnTertiaryFixedVariant = Color(0xFF394B3D)

// Surfaces — crème chaud, profondeur tonale sans ombres
val MibekoCream = Color(0xFFFCF9F8)                 // surface / background
val MibekoCreamDim = Color(0xFFDCD9D9)              // surface-dim
val MibekoSurfaceContainerLowest = Color(0xFFFFFFFF)
val MibekoSurfaceContainerLow = Color(0xFFF6F3F2)
val MibekoSurfaceContainer = Color(0xFFF0EDED)
val MibekoSurfaceContainerHigh = Color(0xFFEAE7E7)
val MibekoSurfaceContainerHighest = Color(0xFFE4E2E1)

// Contenu & contours
val MibekoOnSurface = Color(0xFF1B1C1C)
val MibekoOnSurfaceVariant = Color(0xFF414844)
val MibekoOutline = Color(0xFF727974)
val MibekoOutlineVariant = Color(0xFFC1C8C2)
val MibekoInverseSurface = Color(0xFF303030)
val MibekoInverseOnSurface = Color(0xFFF3F0EF)

// Erreur (M3 standard)
val MibekoError = Color(0xFFBA1A1A)
val MibekoOnError = Color(0xFFFFFFFF)
val MibekoErrorContainer = Color(0xFFFFDAD6)
val MibekoOnErrorContainer = Color(0xFF93000A)
val MibekoErrorDark = Color(0xFFFFB4AB)
val MibekoOnErrorDark = Color(0xFF690005)
val MibekoErrorContainerDark = Color(0xFF93000A)
val MibekoOnErrorContainerDark = Color(0xFFFFDAD6)

// Surfaces sombres (dérivées de la même teinte neutre chaude)
val MibekoDarkSurface = Color(0xFF131314)
val MibekoDarkSurfaceContainerLowest = Color(0xFF0E0E0F)
val MibekoDarkSurfaceContainerLow = Color(0xFF1B1C1C)
val MibekoDarkSurfaceContainer = Color(0xFF1F2020)
val MibekoDarkSurfaceContainerHigh = Color(0xFF2A2B2A)
val MibekoDarkSurfaceContainerHighest = Color(0xFF353635)
val MibekoDarkOnSurface = Color(0xFFE4E2E1)
val MibekoDarkOnSurfaceVariant = Color(0xFFC1C8C2)
val MibekoDarkOutline = Color(0xFF8C938E)
val MibekoDarkOutlineVariant = Color(0xFF414844)

// =============================================================================
// Sémantique métier — statut des textes juridiques
// Toujours passer par MibekoTheme.status (Theme.kt) dans les écrans : les
// valeurs « Dark » garantissent le contraste sur les surfaces sombres.
// =============================================================================
val LegalValid = Color(0xFF2E6B4A)    // En vigueur (vert harmonisé)
val LegalRepealed = Color(0xFFBA1A1A) // Abrogé
val LegalPending = Color(0xFF8F4C31)  // En attente / projet (terracotta)
val LegalValidDark = Color(0xFF8FD0AC)
val LegalRepealedDark = Color(0xFFFFB4AB)
val LegalPendingDark = Color(0xFFFFB59A)

// =============================================================================
// Neutres conservés pour compatibilité
// =============================================================================
val MibekoWhite = Color(0xFFFFFFFF)
val MibekoBlack = Color(0xFF1B1C1C) // charbon profond plutôt que noir pur

val MibekoSurfaceLight = MibekoSurfaceContainerLow
val MibekoSurfaceDark = MibekoDarkSurfaceContainerLow

val MibekoBackgroundLight = MibekoCream
val MibekoBackgroundDark = MibekoDarkSurface

val TextPrimaryLight = MibekoOnSurface
val TextSecondaryLight = MibekoOnSurfaceVariant

val TextPrimaryDark = MibekoDarkOnSurface
val TextSecondaryDark = Color(0xFFAFB4B0)

val OnPrimary = Color(0xFFFFFFFF)
val OnSecondary = Color(0xFFFFFFFF)

val MibekoDividerLight = MibekoOutlineVariant
val MibekoDividerDark = MibekoDarkOutlineVariant
