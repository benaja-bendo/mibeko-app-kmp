package com.mibeko.mibeko.data.local.dao

import androidx.room.*
import com.mibeko.mibeko.data.local.entities.ArticleEntity
import com.mibeko.mibeko.data.local.entities.ArticleTagEntity
import com.mibeko.mibeko.data.local.entities.DocumentEntity
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

    @Transaction
    @Query("""
        SELECT articles.*, nodes.document_id, nodes.title as node_title 
        FROM articles 
        JOIN nodes ON articles.node_id = nodes.id
        WHERE articles.is_favorite = 1
    """)
    fun getFavoriteArticles(): Flow<List<ArticleSearchResult>>

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
        SELECT articles.*, nodes.document_id, nodes.title as node_title 
        FROM articles 
        JOIN nodes ON articles.node_id = nodes.id
        WHERE articles.id = :id
    """)
    fun getArticleById(id: String): Flow<ArticleSearchResult?>

    @Transaction
    @Query("""
        SELECT articles.*, nodes.document_id, nodes.title as node_title 
        FROM articles 
        JOIN nodes ON articles.node_id = nodes.id
        JOIN articles_fts ON articles.id = articles_fts.rowid 
        WHERE articles_fts MATCH :query
    """)
    fun searchArticles(query: String): Flow<List<ArticleSearchResult>>
}

data class ArticleSearchResult(
    @Embedded val article: ArticleEntity,
    val document_id: String,
    val node_title: String
)
