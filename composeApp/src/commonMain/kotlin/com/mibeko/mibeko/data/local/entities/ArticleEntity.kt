package com.mibeko.mibeko.data.local.entities

import androidx.room.*

@Entity(
    tableName = "articles",
    foreignKeys = [
        ForeignKey(
            entity = NodeEntity::class,
            parentColumns = ["id"],
            childColumns = ["node_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["node_id"])
    ]
)
data class ArticleEntity(
    @PrimaryKey val id: String,
    val node_id: String,
    val number: String,
    val content: String?,
    val is_favorite: Boolean,
    val is_offline: Boolean = false,
    /**
     * Tableaux de l'article, sérialisés en JSON (`List<RemoteLegalTable>`).
     *
     * `content` ne contient jamais de balisage : un tableau y est linéarisé
     * (« A | B | C » par rangée) et sa structure est stockée ici. Sans elle, le
     * lecteur afficherait ces lignes telles quelles — lisible, mais la colonne
     * se perd. Null pour la quasi-totalité du corpus, et pour tout article
     * synchronisé avant cette colonne : le rendu retombe alors sur le texte.
     */
    val tables_json: String? = null
)
