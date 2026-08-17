package com.mibeko.mibeko.data

import com.mibeko.mibeko.util.ArticleTable
data class LawCodeSpec(
    val id: String,
    val title: String,
    /**
     * Objet de l'acte DÉRIVÉ de son corps, pour les textes publiés en « actes
     * en abrégé » par le Journal officiel — leur titre se réduit alors au
     * type, au numéro et à la date (« Décret n° 2025-240 du 20 juin 2025. »).
     *
     * À AFFICHER À CÔTÉ DE `title`, JAMAIS À SA PLACE : c'est une paraphrase
     * tirée du texte, pas l'intitulé officiel. `null` la plupart du temps.
     */
    val descriptiveLabel: String? = null,
    val type: String, // Added for filtering
    val icon: String, // Simple identifier for icon selection
    val lastUpdated: Long,
    val isDownloaded: Boolean = false,
    val institutionName: String? = null,
    val dateSignature: String? = null,
    /** Slug d'URL publique (mibeko.fr/textes/{slug}) — `null` si inconnu. */
    val slug: String? = null,
    /**
     * « À jour au » du texte consolidé, tel que publié par la source
     * officielle. `null` pour un acte unitaire, qui n'est pas consolidé.
     */
    val consolidationAsOf: String? = null
)

data class ArticleSpec(
    val id: String,
    val codeId: String,
    val number: String,
    val title: String,
    val content: String?,
    val breadcrumb: String,
    val typeCode: String = "", // Added for filtering
    val isFavorite: Boolean = false,
    val isDownloaded: Boolean = false,
    /**
     * Tableaux de l'article. Vide pour la quasi-totalité du corpus, et pour tout
     * article synchronisé avant que l'API ne les transporte : le rendu retombe
     * alors sur le texte, qui reste lisible.
     */
    val tables: List<ArticleTable> = emptyList()
)

@kotlinx.serialization.Serializable
data class NotificationRemote(
    val id: String,
    val title: String,
    val message: String,
    val type: String,
    val data: Map<String, String>? = null,
    val read_at: String? = null,
    val created_at: String
)
