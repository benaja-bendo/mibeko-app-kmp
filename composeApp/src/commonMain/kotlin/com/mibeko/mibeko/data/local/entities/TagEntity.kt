package com.mibeko.mibeko.data.local.entities

import androidx.room.*

@Entity(tableName = "tags")
data class TagEntity(
    @PrimaryKey val id: String,
    val name: String,
    val slug: String,
    val type: String
)
