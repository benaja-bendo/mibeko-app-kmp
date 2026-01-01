package com.mibeko.mibeko.data.local.entities

import androidx.room.*

/**
 * Entité représentant un dossier juridique créé par l'utilisateur.
 * Les dossiers permettent d'organiser les articles par affaire/projet.
 */
@Entity(
    tableName = "dossiers",
    indices = [
        Index(value = ["name"]),
        Index(value = ["updated_at"])
    ]
)
data class DossierEntity(
    @PrimaryKey val id: String,
    val name: String,
    val legal_domain: String,
    val tag: DossierTag = DossierTag.EN_COURS,
    val description: String? = null,
    val color: String = "#1565C0", // Bleu par défaut
    val created_at: Long = 0L,
    val updated_at: Long = 0L
)

/**
 * Étiquettes disponibles pour les dossiers
 */
enum class DossierTag {
    EN_COURS,
    URGENT,
    ARCHIVE
}
