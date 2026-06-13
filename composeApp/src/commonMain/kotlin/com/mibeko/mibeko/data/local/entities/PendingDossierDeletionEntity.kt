package com.mibeko.mibeko.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Suppression de dossier effectuée localement et pas encore propagée au serveur.
 * Conservée jusqu'à la prochaine synchronisation réussie.
 */
@Entity(tableName = "pending_dossier_deletions")
data class PendingDossierDeletionEntity(
    @PrimaryKey val id: String,
    val deletedAt: Long
)
