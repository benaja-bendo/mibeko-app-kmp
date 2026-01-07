package com.mibeko.mibeko.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "documents")
data class DocumentEntity(
    @PrimaryKey val id: String,
    val title: String,
    val type_code: String,
    val last_updated: Long,
    val is_downloaded: Boolean = false
)
