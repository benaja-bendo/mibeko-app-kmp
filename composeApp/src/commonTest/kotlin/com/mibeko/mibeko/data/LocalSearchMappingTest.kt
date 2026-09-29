package com.mibeko.mibeko.data

import com.mibeko.mibeko.data.local.dao.ArticleSearchResult
import com.mibeko.mibeko.data.local.entities.ArticleEntity
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Verrouille les titres des résultats de recherche hors-ligne (kmp#10).
 *
 * Le bug réel : en repli local, titre et fil d'Ariane valaient tous deux la
 * division (« Chapitre II »), si bien que la carte de résultat n'indiquait
 * jamais de quel texte venait l'article.
 */
class LocalSearchMappingTest {

    private fun resultat(documentTitle: String, label: String? = null, node: String = "Chapitre II") =
        ArticleSearchResult(
            article = ArticleEntity(
                id = "a1", node_id = "n1", number = "12", content = "Texte", is_favorite = false
            ),
            document_id = "d1",
            node_title = node,
            doc_is_downloaded = true,
            type_code = "CODE",
            document_title = documentTitle,
            document_descriptive_label = label
        )

    @Test
    fun `le resultat hors ligne porte le titre du document et non la division`() {
        val spec = resultat("Code du travail").toArticleSpec()

        assertEquals("Code du travail", spec.documentTitle)
        assertEquals("Code du travail > Chapitre II", spec.breadcrumb)
        // La dernière maille reste la division : le lecteur l'affiche seule.
        assertEquals("Chapitre II", spec.breadcrumb.split(">").last().trim())
    }

    @Test
    fun `le libelle descriptif s ajoute au titre officiel sans le remplacer`() {
        val spec = resultat("Décret n° 2025-240 du 20 juin 2025.", label = "Nomination du directeur", node = "Dispositions").toArticleSpec()

        assertEquals("Décret n° 2025-240 du 20 juin 2025.", spec.documentTitle)
        assertEquals("Nomination du directeur", spec.documentDescriptiveLabel)
        assertEquals(
            "Décret n° 2025-240 du 20 juin 2025. > Nomination du directeur > Dispositions",
            spec.breadcrumb
        )
    }

    @Test
    fun `les mailles vides ou repetees sont omises`() {
        assertEquals("Constitution", localBreadcrumb("Constitution", "  ", "Constitution"))
        assertEquals("Chapitre II", localBreadcrumb("", null, "Chapitre II"))
    }
}
