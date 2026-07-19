package com.mibeko.mibeko.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "documents")
data class DocumentEntity(
    @PrimaryKey val id: String,
    val title: String,
    val type_code: String,
    val last_updated: Long,
    val is_downloaded: Boolean = false,
    val institution_name: String? = null,
    val date_signature: String? = null,
    /**
     * Slug d'URL publique (site vitrine `/textes/{slug}`) — sert à générer les
     * liens de partage vers mibeko.fr. `null` tant que le document n'a pas de
     * slug (jamais publié, ou coquille créée localement depuis un résultat de
     * recherche / mode hors-ligne).
     */
    val slug: String? = null
)
