package com.mibeko.mibeko.data.local.dao

import androidx.room.*
import com.mibeko.mibeko.data.local.entities.ArticleEntity
import com.mibeko.mibeko.data.local.entities.ArticleTagEntity
import com.mibeko.mibeko.data.local.entities.DocumentEntity
import com.mibeko.mibeko.data.local.entities.DossierArticleEntity
import com.mibeko.mibeko.data.local.entities.DossierEntity
import com.mibeko.mibeko.data.local.entities.DossierTag
import com.mibeko.mibeko.data.local.entities.NodeEntity
import com.mibeko.mibeko.data.local.entities.TagEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MibekoDao {

    @Upsert
    suspend fun upsertDocuments(documents: List<DocumentEntity>)

    @Upsert
    suspend fun upsertNodes(nodes: List<NodeEntity>)

    @Upsert
    suspend fun upsertArticles(articles: List<ArticleEntity>)

    @Upsert
    suspend fun upsertTags(tags: List<TagEntity>)

    @Upsert
    suspend fun upsertArticleTags(articleTags: List<ArticleTagEntity>)

    @Query("DELETE FROM articles WHERE id IN (:ids)")
    suspend fun deleteArticlesByIds(ids: List<String>)

    @Query("SELECT * FROM documents")
    fun getAllDocuments(): Flow<List<DocumentEntity>>

    @Query("SELECT * FROM documents WHERE id = :id")
    fun getDocumentById(id: String): Flow<DocumentEntity?>

    @Query("SELECT * FROM documents WHERE is_downloaded = 1")
    fun getDownloadedDocuments(): Flow<List<DocumentEntity>>

    @Query("SELECT id FROM documents WHERE is_downloaded = 1")
    suspend fun getDownloadedDocumentIds(): List<String>

    @Query("SELECT DISTINCT type_code FROM documents")
    suspend fun getUniqueDocumentTypeCodes(): List<String>

    @Query("UPDATE documents SET is_downloaded = :isDownloaded WHERE id = :documentId")
    suspend fun updateDocumentDownloadStatus(documentId: String, isDownloaded: Boolean)

    @Query("UPDATE articles SET is_offline = :isOffline WHERE id = :articleId")
    suspend fun updateArticleOfflineStatus(articleId: String, isOffline: Boolean)

    @Query("UPDATE articles SET is_favorite = :isFavorite WHERE id = :articleId")
    suspend fun updateArticleFavoriteStatus(articleId: String, isFavorite: Boolean)

    @Query("DELETE FROM articles WHERE node_id IN (SELECT id FROM nodes WHERE document_id = :documentId) AND is_favorite = 0 AND is_offline = 0")
    suspend fun deleteNonFavoriteArticlesFromDocument(documentId: String)

    @Transaction
    @Query("""
        SELECT articles.*, nodes.document_id, nodes.title as node_title, documents.is_downloaded as doc_is_downloaded, documents.type_code
        FROM articles 
        JOIN nodes ON articles.node_id = nodes.id
        JOIN documents ON nodes.document_id = documents.id
        WHERE articles.is_favorite = 1
    """)
    fun getFavoriteArticles(): Flow<List<ArticleSearchResult>>

    @Transaction
    @Query("""
        SELECT articles.*, nodes.document_id, nodes.title as node_title, documents.is_downloaded as doc_is_downloaded, documents.type_code
        FROM articles 
        JOIN nodes ON articles.node_id = nodes.id
        JOIN documents ON nodes.document_id = documents.id
        WHERE articles.is_offline = 1
    """)
    fun getOfflineArticles(): Flow<List<ArticleSearchResult>>

    @Transaction
    @Query("""
        SELECT articles.*, nodes.document_id, nodes.title as node_title, documents.is_downloaded as doc_is_downloaded, documents.type_code
        FROM articles 
        JOIN nodes ON articles.node_id = nodes.id
        JOIN documents ON nodes.document_id = documents.id
        JOIN article_tags ON articles.id = article_tags.article_id
        JOIN tags ON article_tags.tag_id = tags.id
        WHERE tags.slug = :tagSlug
    """)
    fun searchArticlesByTag(tagSlug: String): Flow<List<ArticleSearchResult>>

    @Transaction
    suspend fun syncAll(
        documents: List<DocumentEntity>,
        nodes: List<NodeEntity>,
        articles: List<ArticleEntity>,
        tags: List<TagEntity> = emptyList(),
        articleTags: List<ArticleTagEntity> = emptyList(),
        deletedArticleIds: List<String> = emptyList()
    ) {
        if (deletedArticleIds.isNotEmpty()) {
            deleteArticlesByIds(deletedArticleIds)
        }
        upsertDocuments(documents)
        upsertNodes(nodes)
        upsertArticles(articles)
        upsertTags(tags)
        upsertArticleTags(articleTags)
    }

    @Transaction
    @Query("""
        SELECT * FROM nodes 
        JOIN articles ON nodes.id = articles.node_id 
        WHERE nodes.document_id = :documentId
    """)
    fun getStructure(documentId: String): Flow<Map<NodeEntity, List<ArticleEntity>>>

    @Transaction
    @Query("""
        SELECT articles.*, nodes.document_id, nodes.title as node_title, documents.is_downloaded as doc_is_downloaded, documents.type_code
        FROM articles 
        JOIN nodes ON articles.node_id = nodes.id
        JOIN documents ON nodes.document_id = documents.id
        WHERE articles.id = :id
    """)
    fun getArticleById(id: String): Flow<ArticleSearchResult?>

    @Transaction
    @Query("""
        SELECT articles.*, nodes.document_id, nodes.title as node_title, documents.is_downloaded as doc_is_downloaded, documents.type_code
        FROM articles 
        JOIN nodes ON articles.node_id = nodes.id
        JOIN documents ON nodes.document_id = documents.id
        JOIN articles_fts ON articles.id = articles_fts.rowid 
        WHERE articles_fts MATCH :query
    """)
    fun searchArticles(query: String): Flow<List<ArticleSearchResult>>

    // ========== DOSSIERS ==========


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDossier(dossier: DossierEntity)

    @Update
    suspend fun updateDossier(dossier: DossierEntity)

    @Delete
    suspend fun deleteDossier(dossier: DossierEntity)

    @Query("DELETE FROM dossiers WHERE id = :dossierId")
    suspend fun deleteDossierById(dossierId: String)

    @Query("SELECT * FROM dossiers ORDER BY updated_at DESC")
    fun getAllDossiers(): Flow<List<DossierEntity>>

    @Query("SELECT * FROM dossiers WHERE tag = :tag ORDER BY updated_at DESC")
    fun getDossiersByTag(tag: DossierTag): Flow<List<DossierEntity>>

    @Query("SELECT * FROM dossiers WHERE id = :dossierId")
    fun getDossierById(dossierId: String): Flow<DossierEntity?>

    @Query("SELECT * FROM dossiers WHERE name LIKE '%' || :query || '%' ORDER BY updated_at DESC")
    fun searchDossiers(query: String): Flow<List<DossierEntity>>

    @Query("SELECT COUNT(*) FROM dossier_articles WHERE dossier_id = :dossierId")
    fun getDossierArticleCount(dossierId: String): Flow<Int>

    // ========== DOSSIER ARTICLES ==========

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addArticleToDossier(dossierArticle: DossierArticleEntity)

    @Query("DELETE FROM dossier_articles WHERE dossier_id = :dossierId AND article_id = :articleId")
    suspend fun removeArticleFromDossier(dossierId: String, articleId: String)

    @Query("UPDATE dossier_articles SET personal_note = :note WHERE dossier_id = :dossierId AND article_id = :articleId")
    suspend fun updatePersonalNote(dossierId: String, articleId: String, note: String?)

    @Query("SELECT * FROM dossier_articles WHERE dossier_id = :dossierId AND article_id = :articleId")
    fun getDossierArticle(dossierId: String, articleId: String): Flow<DossierArticleEntity?>

    @Transaction
    @Query("""
        SELECT articles.*, nodes.document_id, nodes.title as node_title,
               documents.title as document_title,
               dossier_articles.personal_note, dossier_articles.added_at
        FROM dossier_articles
        JOIN articles ON dossier_articles.article_id = articles.id
        JOIN nodes ON articles.node_id = nodes.id
        JOIN documents ON nodes.document_id = documents.id
        WHERE dossier_articles.dossier_id = :dossierId
        ORDER BY dossier_articles.added_at DESC
    """)
    fun getDossierArticles(dossierId: String): Flow<List<DossierArticleWithDetails>>

    @Query("SELECT dossier_id FROM dossier_articles WHERE article_id = :articleId")
    fun getDossiersContainingArticle(articleId: String): Flow<List<String>>

    // ========== CLEAR DATA ==========

    @Query("DELETE FROM documents")
    suspend fun clearDocuments()

    @Query("DELETE FROM nodes")
    suspend fun clearNodes()

    @Query("DELETE FROM articles")
    suspend fun clearArticles()

    @Query("DELETE FROM tags")
    suspend fun clearTags()

    @Query("DELETE FROM article_tags")
    suspend fun clearArticleTags()

    @Query("DELETE FROM dossiers")
    suspend fun clearDossiers()

    @Query("DELETE FROM dossier_articles")
    suspend fun clearDossierArticles()

    @Transaction
    suspend fun clearAllData() {
        clearDocuments()
        clearNodes()
        clearArticles()
        clearTags()
        clearArticleTags()
        clearDossiers()
        clearDossierArticles()
    }
}

data class ArticleSearchResult(
    @Embedded val article: ArticleEntity,
    val document_id: String,
    val node_title: String,
    val doc_is_downloaded: Boolean,
    val type_code: String // Added for filtering
)

data class DossierArticleWithDetails(
    @Embedded val article: ArticleEntity,
    val document_id: String,
    val document_title: String,
    val node_title: String,
    val personal_note: String?,
    val added_at: Long
)
