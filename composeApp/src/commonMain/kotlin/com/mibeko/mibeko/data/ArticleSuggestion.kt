package com.mibeko.mibeko.data

/**
 * Represents an article suggestion from autocomplete.
 * Contains all information needed for display and navigation.
 */
data class ArticleSuggestion(
    /** Article ID for navigation to ReaderScreen */
    val id: String,
    /** Article number (e.g., "Article 45", "1er") */
    val number: String,
    /** Breadcrumb path (e.g., "Code du Travail > Chapitre 2") */
    val breadcrumb: String,
    /** Whether this article is downloaded and available offline */
    val isDownloaded: Boolean,
    /** Optional content snippet for preview */
    val contentSnippet: String? = null
)
