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
    val is_favorite: Boolean
)
