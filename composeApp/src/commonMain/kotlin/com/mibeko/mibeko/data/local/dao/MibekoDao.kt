package com.mibeko.mibeko.data.local.dao

import androidx.room.*
import com.mibeko.mibeko.data.local.entities.ArticleEntity
import com.mibeko.mibeko.data.local.entities.DocumentEntity
import com.mibeko.mibeko.data.local.entities.NodeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MibekoDao {

    @Upsert
    suspend fun upsertDocuments(documents: List<DocumentEntity>)

    @Upsert
    suspend fun upsertNodes(nodes: List<NodeEntity>)

    @Upsert
    suspend fun upsertArticles(articles: List<ArticleEntity>)

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
