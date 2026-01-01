package com.mibeko.mibeko.data.local.entities

import androidx.room.*

/**
 * Entité de liaison entre un dossier et ses articles.
 * Permet d'ajouter des notes personnelles à chaque article dans le contexte d'un dossier.
 */
@Entity(
    tableName = "dossier_articles",
    primaryKeys = ["dossier_id", "article_id"],
    foreignKeys = [
        ForeignKey(
            entity = DossierEntity::class,
            parentColumns = ["id"],
            childColumns = ["dossier_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ArticleEntity::class,
            parentColumns = ["id"],
            childColumns = ["article_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["dossier_id"]),
        Index(value = ["article_id"])
    ]
)
data class DossierArticleEntity(
    @ColumnInfo(name = "dossier_id") val dossierId: String,
    @ColumnInfo(name = "article_id") val articleId: String,
    @ColumnInfo(name = "personal_note") val personalNote: String? = null,
    @ColumnInfo(name = "added_at") val addedAt: Long = 0L
)
