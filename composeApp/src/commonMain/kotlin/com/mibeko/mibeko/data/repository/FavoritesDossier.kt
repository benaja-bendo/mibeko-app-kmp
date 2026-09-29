package com.mibeko.mibeko.data.repository

import com.mibeko.mibeko.data.local.entities.DossierArticleEntity
import com.mibeko.mibeko.data.local.entities.DossierEntity
import com.mibeko.mibeko.data.local.entities.DossierTag
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Un seul dossier « Mes Favoris » par compte, quel que soit le nombre
 * d'appareils (kmp#11, D-052).
 *
 * Jusqu'à la 1.4, chaque appareil créait son dossier Favoris sous un
 * identifiant aléatoire. La synchronisation fusionnant par identifiant, chaque
 * nouvel appareil ajoutait un doublon définitif. L'identifiant est désormais
 * dérivé du compte : tous les appareils écrivent dans le même dossier, et les
 * doublons déjà créés y sont fondus.
 */
@OptIn(ExperimentalUuidApi::class)
internal object FavoritesDossier {

    // Masque fixe : l'identifiant du dossier ne vaut jamais celui du compte.
    private val MASK = Uuid.parse("7c1e5a0f-3b94-4d2e-9a61-f08b2c4d7e35")

    /**
     * Identifiant du dossier Favoris d'un compte, le même sur tous ses
     * appareils. `null` si l'identifiant du compte n'est pas un UUID.
     *
     * C'est un UUID de version 8 (dérivation propre à l'application,
     * RFC 9562), valide pour la route de synchronisation qui exige un UUID.
     * Les comptes étant des UUID v4 ou v7, leurs bits de version et de
     * variante sont identiques d'un compte à l'autre : les écraser ne crée
     * aucune collision entre deux comptes.
     */
    fun idFor(userId: String): String? {
        val account = runCatching { Uuid.parse(userId) }.getOrNull() ?: return null
        val bytes = account.toByteArray()
        val mask = MASK.toByteArray()
        for (i in bytes.indices) {
            bytes[i] = (bytes[i].toInt() xor mask[i].toInt()).toByte()
        }
        bytes[6] = ((bytes[6].toInt() and 0x0F) or 0x80).toByte()
        bytes[8] = ((bytes[8].toInt() and 0x3F) or 0x80).toByte()
        return Uuid.fromByteArray(bytes).toString()
    }

    fun create(id: String, now: Long) = DossierEntity(
        id = id,
        name = "Mes Favoris",
        legal_domain = "Général",
        tag = DossierTag.FAVORIS,
        description = "Collection automatique de vos articles favoris",
        color = "#8F4C31",
        created_at = now,
        updated_at = now
    )

    /** Ce qu'il faut écrire pour ne garder que le dossier Favoris du compte. */
    data class Consolidation(
        val canonical: DossierEntity,
        val links: List<DossierArticleEntity>,
        val duplicateIds: List<String>
    )

    /**
     * Fond tous les dossiers Favoris dans celui du compte. Renvoie `null` s'il
     * n'y a rien à faire, c'est-à-dire aucun dossier Favoris sous un autre
     * identifiant.
     *
     * Aucun favori ne se perd : le dossier canonique reçoit l'union des
     * articles, avec la date d'ajout la plus ancienne et la première note
     * trouvée. Son `updated_at` passe à `now`, pour que la fusion serveur
     * (le plus récent l'emporte) retienne cette union.
     */
    fun consolidate(
        canonicalId: String,
        favoris: List<DossierEntity>,
        links: List<DossierArticleEntity>,
        now: Long
    ): Consolidation? {
        val duplicates = favoris.filter { it.id != canonicalId }
        if (duplicates.isEmpty()) return null

        val favorisIds = favoris.map { it.id }.toSet()
        val merged = links
            .filter { it.dossierId in favorisIds }
            .groupBy { it.articleId }
            .map { (articleId, sameArticle) ->
                DossierArticleEntity(
                    dossierId = canonicalId,
                    articleId = articleId,
                    personalNote = sameArticle.firstNotNullOfOrNull { it.personalNote },
                    addedAt = sameArticle.minOf { it.addedAt }
                )
            }
            .sortedBy { it.addedAt }

        val canonical = favoris.firstOrNull { it.id == canonicalId }
            ?: create(canonicalId, now).copy(created_at = favoris.minOf { it.created_at })

        return Consolidation(
            canonical = canonical.copy(updated_at = now),
            links = merged,
            duplicateIds = duplicates.map { it.id }
        )
    }
}
