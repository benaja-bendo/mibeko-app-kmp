package com.mibeko.mibeko.data.repository

import com.mibeko.mibeko.data.ArticleSpec
import com.mibeko.mibeko.data.local.dao.MibekoDao
import com.mibeko.mibeko.data.toArticleSpec
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class LocalLegalRepository(
    private val mibekoDao: MibekoDao
) {

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
