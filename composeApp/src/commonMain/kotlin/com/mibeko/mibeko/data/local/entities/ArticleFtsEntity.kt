package com.mibeko.mibeko.data.local.entities

import androidx.room.Entity
import androidx.room.Fts4

@Entity(tableName = "articles_fts")
@Fts4(contentEntity = ArticleEntity::class)
data class ArticleFtsEntity(
    val number: String,
    val content: String?
)
