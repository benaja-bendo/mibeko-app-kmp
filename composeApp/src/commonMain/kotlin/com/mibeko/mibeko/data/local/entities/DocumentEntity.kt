package com.mibeko.mibeko.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "documents")
data class DocumentEntity(
    @PrimaryKey val id: String,
    val title: String,
    /**
     * Objet de l'acte DÉRIVÉ de son corps, pour les textes que le Journal
     * officiel publie en « actes en abrégé » : le titre s'y réduit au type, au
     * numéro et à la date (« Décret n° 2025-240 du 20 juin 2025. »), et ne dit
     * donc rien de ce que l'acte fait.
     *
     * À AFFICHER À CÔTÉ DE `title`, JAMAIS À SA PLACE : ce libellé est une
     * paraphrase, pas l'intitulé officiel du texte. `null` pour l'immense
     * majorité des documents, et pour tous ceux synchronisés avant la v12.
     */
    val descriptive_label: String? = null,
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
    val slug: String? = null,
    /**
     * Date de consolidation du texte (« à jour au JJ/MM/AAAA ») telle que
     * publiée par l'API. Renseignée pour les textes consolidés (STOCK),
     * `null` pour les actes unitaires (FLUX) qui n'en ont pas.
     */
    val consolidation_as_of: String? = null,
    /**
     * Empreinte de version renvoyée par le catalogue lors de la dernière
     * récupération de ce document. C'est elle qu'on compare au catalogue pour
     * savoir si le texte local est périmé — `null` pour un document qui n'a
     * jamais été synchronisé depuis le catalogue (coquille locale).
     */
    val version_hash: String? = null
)
