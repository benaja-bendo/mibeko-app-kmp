package com.mibeko.mibeko.ui.home

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Represents a download in progress, displayed on the home dashboard.
 */
data class DownloadProgress(
    val documentId: String,
    val documentTitle: String,
    val progress: Float // 0.0 to 1.0
)

/**
 * Represents a fundamental legal text for the carousel.
 */
data class FundamentalText(
    val id: String,
    val title: String,
    val shortTitle: String,
    val isDownloaded: Boolean,
    val typeCode: String // "CONSTITUTION", "CODE", etc.
)

/**
 * Represents a life theme category for navigation.
 */
data class LifeTheme(
    val id: String,
    val title: String,
    val subtitle: String,
    val emoji: String,
    val filterTag: String // For filtering articles by theme
)

/**
 * Predefined life themes for the home screen.
 * These are static for MVP and represent common legal needs.
 */
object LifeThemes {
    val all = listOf(
        LifeTheme(
            id = "logement",
            title = "Logement & Foncier",
            subtitle = "Baux, terrains, propriété",
            emoji = "🏠",
            filterTag = "logement"
        ),
        LifeTheme(
            id = "travail",
            title = "Travail & Entreprise",
            subtitle = "Contrats, licenciement, droits",
            emoji = "💼",
            filterTag = "travail"
        ),
        LifeTheme(
            id = "famille",
            title = "Famille & Personnes",
            subtitle = "Mariage, divorce, succession",
            emoji = "💍",
            filterTag = "famille"
        ),
        LifeTheme(
            id = "justice",
            title = "Justice & Droits",
            subtitle = "Procédure, porter plainte",
            emoji = "⚖️",
            filterTag = "justice"
        )
    )
}
