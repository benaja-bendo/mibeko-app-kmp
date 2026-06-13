package com.mibeko.mibeko.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/**
 * Entité de liaison entre un dossier et ses articles.
 * Permet d'ajouter des notes personnelles à chaque article dans le contexte d'un dossier.
 *
 * Volontairement sans clé étrangère vers [ArticleEntity] : l'appartenance d'un
 * article à un dossier est une donnée utilisateur synchronisée avec le serveur,
 * alors que la table des articles n'est qu'un cache local. Un lien peut donc
 * référencer un article non téléchargé (ajouté depuis un autre appareil) et
 * doit survivre au nettoyage du cache.
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
