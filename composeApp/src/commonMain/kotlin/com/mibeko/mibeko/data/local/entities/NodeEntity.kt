package com.mibeko.mibeko.data.local.entities

import androidx.room.*

@Entity(
    tableName = "nodes",
    foreignKeys = [
        ForeignKey(
            entity = DocumentEntity::class,
            parentColumns = ["id"],
            childColumns = ["document_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["document_id"]),
        Index(value = ["parent_id"])
    ]
)
data class NodeEntity(
    @PrimaryKey val id: String,
    val document_id: String,
    val parent_id: String?,
    val title: String,
    val sort_order: Int
)
