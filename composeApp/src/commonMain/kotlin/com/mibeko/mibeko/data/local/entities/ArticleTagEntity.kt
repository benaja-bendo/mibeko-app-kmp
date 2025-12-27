package com.mibeko.mibeko.data.local.entities

import androidx.room.*

@Entity(
    tableName = "article_tags",
    primaryKeys = ["article_id", "tag_id"],
    foreignKeys = [
        ForeignKey(
            entity = ArticleEntity::class,
            parentColumns = ["id"],
            childColumns = ["article_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = TagEntity::class,
            parentColumns = ["id"],
            childColumns = ["tag_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["article_id"]),
        Index(value = ["tag_id"])
    ]
)
data class ArticleTagEntity(
    val article_id: String,
    val tag_id: String
)
