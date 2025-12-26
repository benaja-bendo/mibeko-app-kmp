package com.mibeko.mibeko.data.repository

import com.mibeko.mibeko.data.ArticleSpec
import com.mibeko.mibeko.data.local.dao.MibekoDao
import com.mibeko.mibeko.data.toArticleSpec
import com.mibeko.mibeko.data.toLawCodeSpec
import com.mibeko.mibeko.data.local.entities.ArticleEntity
import com.mibeko.mibeko.data.local.entities.DocumentEntity
import com.mibeko.mibeko.data.local.entities.NodeEntity
import com.mibeko.mibeko.data.remote.LegalApiService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class LocalLegalRepository(
    private val mibekoDao: MibekoDao,
    private val apiService: LegalApiService
) {

    suspend fun sync() {
        val remoteData = apiService.fetchAllData()
        
        val documents = remoteData.documents.map { 
            DocumentEntity(it.id, it.title, it.type_code, it.last_updated) 
        }
        val nodes = remoteData.nodes.map { 
            NodeEntity(it.id, it.document_id, it.parent_id, it.title, it.sort_order) 
        }
        val articles = remoteData.articles.map { 
            ArticleEntity(it.id, it.node_id, it.number, it.content, it.is_favorite) 
        }

        mibekoDao.syncAll(documents, nodes, articles)
    }

    fun getLawCodes(): Flow<List<com.mibeko.mibeko.data.LawCodeSpec>> {
        // Need to implementation this in DAO too if not present
        // For now, let's assume we can get it from documents
        return mibekoDao.getAllDocuments().map { docs ->
            docs.map { it.toLawCodeSpec() }
        }
    }

    fun getFavoriteArticles(): Flow<List<ArticleSpec>> {
        return mibekoDao.getFavoriteArticles().map { results ->
            results.map { result ->
                result.article.toArticleSpec(
                    codeId = result.document_id,
                    title = result.node_title,
                    breadcrumb = result.node_title
                )
            }
        }
    }

    fun getArticleById(id: String): Flow<ArticleSpec?> {
        return mibekoDao.getArticleById(id).map { result ->
            result?.article?.toArticleSpec(
                codeId = result.document_id,
                title = result.node_title,
                breadcrumb = result.node_title
            )
        }
    }

    fun search(query: String): Flow<List<ArticleSpec>> {
        return mibekoDao.searchArticles(query).map { results ->
            results.map { result ->
                result.article.toArticleSpec(
                    codeId = result.document_id,
                    title = result.node_title,
                    breadcrumb = result.node_title // Simplification for now
                )
            }
        }
    }
}
