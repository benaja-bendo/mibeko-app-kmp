package com.mibeko.mibeko.data.repository

import com.mibeko.mibeko.data.local.dao.DossierArticleWithDetails
import com.mibeko.mibeko.data.local.dao.DossierWithCount
import com.mibeko.mibeko.data.local.dao.MibekoDao
import com.mibeko.mibeko.data.local.entities.DossierArticleEntity
import com.mibeko.mibeko.data.local.entities.DossierEntity
import com.mibeko.mibeko.data.local.entities.DossierTag
import com.mibeko.mibeko.data.local.entities.PendingDossierDeletionEntity
import com.mibeko.mibeko.data.preferences.UserPreferencesRepository
import com.mibeko.mibeko.data.remote.DossierApiService
import com.mibeko.mibeko.data.remote.DossierExportItem
import com.mibeko.mibeko.data.remote.DossierExportRequest
import com.mibeko.mibeko.data.remote.DossierSyncRequest
import com.mibeko.mibeko.data.remote.LegalApiService
import com.mibeko.mibeko.data.remote.RemoteDossier
import com.mibeko.mibeko.data.remote.RemoteDossierArticle
import com.mibeko.mibeko.getCurrentTimeMillis
import com.mibeko.mibeko.util.NetworkConnectivityChecker
import com.mibeko.mibeko.util.recordException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/** État de la synchronisation des dossiers, exposé à l'interface. */
sealed class DossierSyncState {
    data object Idle : DossierSyncState()
    data object Syncing : DossierSyncState()
    data class Synced(val at: Long) : DossierSyncState()
    data class Offline(val lastSyncAt: Long?) : DossierSyncState()
    data class Error(val message: String) : DossierSyncState()
}

/**
 * Repository pour la gestion des dossiers.
 *
 * Local-first : toutes les lectures/écritures passent par Room, donc tout
 * fonctionne hors-ligne. Lorsque l'utilisateur est connecté et que le réseau
 * est disponible, l'état complet est poussé vers `POST /v1/dossiers/sync` et
 * l'état fusionné renvoyé par le serveur est appliqué localement.
 */
class DossierRepository(
    private val dao: MibekoDao,
    private val legalApiService: LegalApiService,
    private val dossierApiService: DossierApiService,
    private val preferences: UserPreferencesRepository,
    private val connectivity: NetworkConnectivityChecker
) {

    private val syncMutex = Mutex()

    private val _syncState = MutableStateFlow<DossierSyncState>(DossierSyncState.Idle)
    val syncState: StateFlow<DossierSyncState> = _syncState.asStateFlow()

    // ========== DOSSIERS ==========

    fun getAllDossiers(): Flow<List<DossierEntity>> = dao.getAllDossiers()

    fun getDossiersWithCount(): Flow<List<DossierWithCount>> = dao.getDossiersWithCount()

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
    suspend fun getOrCreateFavoritesDossier(): DossierEntity {
        val favorisList = dao.getDossiersByTag(DossierTag.FAVORIS).first()
        if (favorisList.isNotEmpty()) {
            return favorisList.first()
        }

        val dossierId = Uuid.random().toString()
        val now = getCurrentTimeMillis()
        val dossier = DossierEntity(
            id = dossierId,
            name = "Mes Favoris",
            legal_domain = "Général",
            tag = DossierTag.FAVORIS,
            description = "Collection automatique de vos articles favoris",
            color = "#8F4C31",
            created_at = now,
            updated_at = now
        )
        dao.insertDossier(dossier)
        return dossier
    }

    @OptIn(ExperimentalUuidApi::class)
    suspend fun createDossier(
        name: String,
        legalDomain: String,
        tag: DossierTag = DossierTag.EN_COURS,
        description: String? = null,
        color: String = "#1B3D2F"
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
        val dossier = dao.getDossierById(dossierId).first() ?: return
        dao.updateDossier(
            dossier.copy(
                name = name,
                legal_domain = legalDomain,
                tag = tag,
                description = description,
                color = color,
                updated_at = getCurrentTimeMillis()
            )
        )
    }

    suspend fun deleteDossier(dossierId: String) {
        dao.deleteDossierById(dossierId)
        dao.insertPendingDossierDeletion(
            PendingDossierDeletionEntity(id = dossierId, deletedAt = getCurrentTimeMillis())
        )
    }

    // ========== ARTICLES IN DOSSIERS ==========

    fun getDossierArticles(dossierId: String): Flow<List<DossierArticleWithDetails>> {
        return dao.getDossierArticles(dossierId)
    }

    fun getDossierArticleCount(dossierId: String): Flow<Int> {
        return dao.getDossierArticleCount(dossierId)
    }

    suspend fun addArticleToDossier(dossierId: String, articleId: String, note: String? = null) {
        dao.addArticleToDossier(
            DossierArticleEntity(
                dossierId = dossierId,
                articleId = articleId,
                personalNote = note,
                addedAt = getCurrentTimeMillis()
            )
        )
        touchDossier(dossierId)
    }

    suspend fun removeArticleFromDossier(dossierId: String, articleId: String) {
        dao.removeArticleFromDossier(dossierId, articleId)
        touchDossier(dossierId)
    }

    suspend fun updatePersonalNote(dossierId: String, articleId: String, note: String?) {
        dao.updatePersonalNote(dossierId, articleId, note)
        touchDossier(dossierId)
    }

    fun getDossiersContainingArticle(articleId: String): Flow<List<String>> {
        return dao.getDossiersContainingArticle(articleId)
    }

    /**
     * Rafraîchit `updated_at` : la fusion serveur (last-write-wins) considère
     * la version la plus récente comme autoritaire, liste d'articles incluse.
     */
    private suspend fun touchDossier(dossierId: String) {
        val dossier = dao.getDossierById(dossierId).first() ?: return
        dao.updateDossier(dossier.copy(updated_at = getCurrentTimeMillis()))
    }

    // ========== SYNC ==========

    /**
     * Synchronise l'état local avec le serveur si l'utilisateur est connecté
     * et que le réseau est disponible. Sans connexion, l'app reste utilisable :
     * l'état local fait foi jusqu'à la prochaine synchronisation.
     */
    suspend fun syncNow() {
        if (!preferences.isLoggedIn()) {
            return
        }
        if (!connectivity.isNetworkAvailable() || preferences.isOfflineModeEnabled()) {
            _syncState.value = DossierSyncState.Offline(
                preferences.getDossierLastSyncAt().takeIf { it > 0 }
            )
            return
        }

        syncMutex.withLock {
            _syncState.value = DossierSyncState.Syncing
            try {
                guardAccountSwitch()

                val pendingDeletions = dao.getPendingDossierDeletions()
                val request = DossierSyncRequest(
                    dossiers = buildLocalSnapshot(),
                    deleted_ids = pendingDeletions.map { it.id }
                )

                val response = dossierApiService.sync(request)
                val data = response.data
                if (!response.success || data == null) {
                    _syncState.value = DossierSyncState.Error(
                        response.message.ifBlank { "Synchronisation impossible" }
                    )
                    return
                }

                dao.applyDossierSyncState(
                    dossiers = data.dossiers.map { it.toEntity() },
                    linksByDossier = data.dossiers.associate { dossier ->
                        dossier.id to dossier.articles.map { it.toEntity(dossier.id) }
                    },
                    deletedIds = data.deleted_ids
                )
                dao.clearPendingDossierDeletions(pendingDeletions.map { it.id })

                val syncedAt = data.synced_at.takeIf { it > 0 } ?: getCurrentTimeMillis()
                preferences.setDossierLastSyncAt(syncedAt)
                _syncState.value = DossierSyncState.Synced(syncedAt)
            } catch (e: Exception) {
                recordException(e, context = "DossierRepository.sync")
                _syncState.value = DossierSyncState.Error(e.message ?: "Erreur réseau")
            }
        }
    }

    /**
     * Si l'utilisateur connecté n'est pas celui dont les dossiers sont en base
     * locale, on repart de zéro pour ne pas mélanger deux comptes.
     */
    private suspend fun guardAccountSwitch() {
        val currentAccount = preferences.getUserEmail() ?: return
        val syncedAccount = preferences.getDossierSyncAccount()
        if (syncedAccount != null && syncedAccount != currentAccount) {
            dao.clearDossiers()
            dao.clearDossierArticles()
            dao.clearAllPendingDossierDeletions()
            preferences.setDossierLastSyncAt(0L)
        }
        preferences.setDossierSyncAccount(currentAccount)
    }

    private suspend fun buildLocalSnapshot(): List<RemoteDossier> {
        val linksByDossier = dao.getDossierArticlesOnce().groupBy { it.dossierId }
        return dao.getDossiersOnce().map { dossier ->
            RemoteDossier(
                id = dossier.id,
                name = dossier.name,
                legal_domain = dossier.legal_domain,
                tag = dossier.tag.name,
                description = dossier.description,
                color = dossier.color,
                created_at = dossier.created_at,
                updated_at = dossier.updated_at,
                articles = linksByDossier[dossier.id].orEmpty().map { link ->
                    RemoteDossierArticle(
                        article_id = link.articleId,
                        personal_note = link.personalNote,
                        added_at = link.addedAt
                    )
                }
            )
        }
    }

    private fun RemoteDossier.toEntity() = DossierEntity(
        id = id,
        name = name,
        legal_domain = legal_domain,
        tag = runCatching { DossierTag.valueOf(tag) }.getOrDefault(DossierTag.EN_COURS),
        description = description,
        color = color,
        created_at = created_at,
        updated_at = updated_at
    )

    private fun RemoteDossierArticle.toEntity(dossierId: String) = DossierArticleEntity(
        dossierId = dossierId,
        articleId = article_id,
        personalNote = personal_note,
        addedAt = added_at
    )

    // ========== EXPORT ==========

    /**
     * Call backend to generate PDF for the dossier.
     */
    suspend fun exportDossierPdf(dossierId: String): ByteArray {
        val dossier = dao.getDossierById(dossierId).first() ?: throw Exception("Dossier non trouvé")
        val articles = dao.getDossierArticles(dossierId).first()

        val request = DossierExportRequest(
            title = dossier.name,
            description = dossier.description,
            items = articles.map {
                DossierExportItem(
                    type = "article",
                    id = it.article.id,
                    note = it.personal_note
                )
            }
        )

        return legalApiService.exportDossierPdf(request)
    }
}
