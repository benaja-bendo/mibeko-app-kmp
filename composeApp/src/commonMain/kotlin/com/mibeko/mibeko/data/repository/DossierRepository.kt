package com.mibeko.mibeko.data.repository

import com.mibeko.mibeko.data.local.dao.DossierArticleWithDetails
import com.mibeko.mibeko.data.local.dao.MibekoDao
import com.mibeko.mibeko.data.local.entities.DossierArticleEntity
import com.mibeko.mibeko.data.local.entities.DossierEntity
import com.mibeko.mibeko.data.local.entities.DossierTag
import kotlinx.coroutines.flow.*
import com.mibeko.mibeko.getCurrentTimeMillis
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Repository pour la gestion des dossiers.
 * Fournit une abstraction au-dessus du DAO pour la logique métier.
 */
class DossierRepository(private val dao: MibekoDao) {

    // ========== DOSSIERS ==========

    fun getAllDossiers(): Flow<List<DossierWithCount>> {
        return dao.getAllDossiers().map { dossiers ->
            dossiers.map { dossier ->
                DossierWithCount(
                    dossier = dossier,
                    articleCount = 0 // Will be computed separately
                )
            }
        }
    }

    fun getDossiersByTag(tag: DossierTag): Flow<List<DossierEntity>> {
        return dao.getDossiersByTag(tag)
    }

    fun getDossierById(dossierId: String): Flow<DossierEntity?> {
        return dao.getDossierById(dossierId)
    }

    fun searchDossiers(query: String): Flow<List<DossierEntity>> {
        return dao.searchDossiers(query)
    }

    @OptIn(ExperimentalUuidApi::class)
    suspend fun createDossier(
        name: String,
        legalDomain: String,
        tag: DossierTag = DossierTag.EN_COURS,
        description: String? = null,
        color: String = "#1565C0"
    ): String {
        val dossierId = Uuid.random().toString()
        val now = getCurrentTimeMillis()
        val dossier = DossierEntity(
            id = dossierId,
            name = name,
            legal_domain = legalDomain,
            tag = tag,
            description = description,
            color = color,
            created_at = now,
            updated_at = now
        )
        dao.insertDossier(dossier)
        return dossierId
    }

    suspend fun updateDossier(
        dossierId: String,
        name: String,
        legalDomain: String,
        tag: DossierTag,
        description: String?,
        color: String
    ) {
        val dossier = dao.getDossierById(dossierId).first()
        dossier?.let {
            dao.updateDossier(
                it.copy(
                    name = name,
                    legal_domain = legalDomain,
                    tag = tag,
                    description = description,
                    color = color,
                    updated_at = getCurrentTimeMillis()
                )
            )
        }
    }

    suspend fun deleteDossier(dossierId: String) {
        dao.deleteDossierById(dossierId)
    }

    // ========== ARTICLES IN DOSSIERS ==========

    fun getDossierArticles(dossierId: String): Flow<List<DossierArticleWithDetails>> {
        return dao.getDossierArticles(dossierId)
    }

    fun getDossierArticleCount(dossierId: String): Flow<Int> {
        return dao.getDossierArticleCount(dossierId)
    }

    suspend fun addArticleToDossier(dossierId: String, articleId: String, note: String? = null) {
        val dossierArticle = DossierArticleEntity(
            dossierId = dossierId,
            articleId = articleId,
            personalNote = note,
            addedAt = getCurrentTimeMillis()
        )
        dao.addArticleToDossier(dossierArticle)
        
        // Update dossier's updated_at timestamp
        val dossier = dao.getDossierById(dossierId).first()
        dossier?.let {
            dao.updateDossier(it.copy(updated_at = getCurrentTimeMillis()))
        }
    }

    suspend fun removeArticleFromDossier(dossierId: String, articleId: String) {
        dao.removeArticleFromDossier(dossierId, articleId)
    }

    suspend fun updatePersonalNote(dossierId: String, articleId: String, note: String?) {
        dao.updatePersonalNote(dossierId, articleId, note)
    }

    fun getDossiersContainingArticle(articleId: String): Flow<List<String>> {
        return dao.getDossiersContainingArticle(articleId)
    }

    // ========== EXPORT ==========

    /**
     * Génère le contenu texte du dossier pour le partage.
     */
    suspend fun generateTextExport(dossierId: String): String {
        val builder = StringBuilder()
        
        val dossier = dao.getDossierById(dossierId).first()
        dossier?.let {
            builder.appendLine("=== ${it.name} ===")
            builder.appendLine("Domaine: ${it.legal_domain}")
            builder.appendLine("Statut: ${it.tag.name}")
            it.description?.let { desc -> builder.appendLine("Description: $desc") }
            builder.appendLine()
            builder.appendLine("--- Articles ---")
        }
        
        val articles = dao.getDossierArticles(dossierId).first()
        articles.forEachIndexed { index, article ->
            builder.appendLine()
            builder.appendLine("${index + 1}. Article ${article.article.number}")
            builder.appendLine("   ${article.node_title}")
            builder.appendLine("   ${article.article.content.take(200)}...")
            article.personal_note?.let { note ->
                builder.appendLine("   [Note personnelle]: $note")
            }
        }
        
        return builder.toString()
    }
}

/**
 * Dossier avec le comptage d'articles pour l'affichage dans la liste.
 */
data class DossierWithCount(
    val dossier: DossierEntity,
    val articleCount: Int
)
