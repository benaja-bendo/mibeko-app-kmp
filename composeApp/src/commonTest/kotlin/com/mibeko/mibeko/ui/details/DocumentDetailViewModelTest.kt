package com.mibeko.mibeko.ui.details

import com.mibeko.mibeko.data.local.entities.ArticleEntity
import com.mibeko.mibeko.data.local.entities.NodeEntity
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * buildDocumentTree : reconstruction de la hiérarchie Livre/Titre/Chapitre
 * (mibeko-app-kmp#8 — les nœuds intermédiaires étaient stockés via
 * `parent_id` mais jamais rendus, un simple JOIN les excluait).
 */
class DocumentDetailViewModelTest {

    private fun node(id: String, parentId: String?, title: String, sortOrder: Int) =
        NodeEntity(id = id, document_id = "doc", parent_id = parentId, title = title, sort_order = sortOrder)

    private fun article(id: String, nodeId: String, number: String) =
        ArticleEntity(id = id, node_id = nodeId, number = number, content = "contenu", is_favorite = false)

    @Test
    fun `livre titre chapitre s'imbriquent en ordre de lecture avec la bonne profondeur`() {
        val livre = node("livre", null, "Livre I", 0)
        val titre1 = node("titre1", "livre", "Titre 1", 0)
        val chapitre1 = node("chap1", "titre1", "Chapitre 1", 0)
        val titre2 = node("titre2", "livre", "Titre 2", 1)
        val chapitre2 = node("chap2", "titre2", "Chapitre 2", 0)

        val structure = mapOf(
            livre to emptyList(),
            titre1 to emptyList(),
            chapitre1 to listOf(article("a1", "chap1", "1"), article("a2", "chap1", "2")),
            titre2 to emptyList(),
            chapitre2 to listOf(article("a3", "chap2", "3"))
        )

        val items = buildDocumentTree(structure)

        assertEquals(
            listOf(
                "Section(Livre I, 0)",
                "Section(Titre 1, 1)",
                "Section(Chapitre 1, 2)",
                "Article(1, 2)",
                "Article(2, 2)",
                "Section(Titre 2, 1)",
                "Section(Chapitre 2, 2)",
                "Article(3, 2)"
            ),
            items.map { it.describe() }
        )
    }

    @Test
    fun `le tri respecte le parent plutot qu'un sort_order global qui se recoupe entre branches`() {
        // chap1 et chap2 partagent le même sort_order (0) sous des titres
        // différents : un tri global sur sort_order ne garantirait pas
        // l'ordre Titre1->Chapitre1 avant Titre2->Chapitre2.
        val livre = node("livre", null, "Livre I", 0)
        val titre1 = node("titre1", "livre", "Titre 1", 0)
        val chapitre1 = node("chap1", "titre1", "Chapitre 1", 0)
        val titre2 = node("titre2", "livre", "Titre 2", 1)
        val chapitre2 = node("chap2", "titre2", "Chapitre 2", 0)

        val structure = mapOf(
            livre to emptyList(),
            titre2 to emptyList(),
            chapitre2 to listOf(article("a2", "chap2", "2")),
            titre1 to emptyList(),
            chapitre1 to listOf(article("a1", "chap1", "1"))
        )

        val items = buildDocumentTree(structure)
        val articleOrder = items.filterIsInstance<DocumentDetailItem.ArticleRow>().map { it.article.number }

        assertEquals(listOf("1", "2"), articleOrder)
    }

    @Test
    fun `le noeud racine synthetique sans titre n'affiche pas d'en-tete`() {
        val root = node("root_doc", null, "", 0)
        val structure = mapOf(
            root to listOf(article("a1", "root_doc", "1"), article("a2", "root_doc", "2"))
        )

        val items = buildDocumentTree(structure)

        assertEquals(0, items.filterIsInstance<DocumentDetailItem.Section>().size)
        assertEquals(listOf("1", "2"), items.filterIsInstance<DocumentDetailItem.ArticleRow>().map { it.article.number })
    }

    @Test
    fun `un noeud dont le parent est absent du jeu de donnees est traite comme racine`() {
        val orphanTitre = node("titre-orphelin", "livre-jamais-recu", "Titre orphelin", 0)
        val structure = mapOf(
            orphanTitre to listOf(article("a1", "titre-orphelin", "1"))
        )

        val items = buildDocumentTree(structure)

        assertEquals(DocumentDetailItem.Section(orphanTitre, 0), items.first())
    }

    @Test
    fun `un noeud avec ses propres articles et des enfants rend d'abord ses articles`() {
        val chapitre = node("chap", null, "Chapitre unique", 0)
        val section = node("section", "chap", "Section 1", 0)

        val structure = mapOf(
            chapitre to listOf(article("a1", "chap", "1")),
            section to listOf(article("a2", "section", "2"))
        )

        val items = buildDocumentTree(structure)

        assertEquals(
            listOf(
                "Section(Chapitre unique, 0)",
                "Article(1, 0)",
                "Section(Section 1, 1)",
                "Article(2, 1)"
            ),
            items.map { it.describe() }
        )
    }

    private fun DocumentDetailItem.describe(): String = when (this) {
        is DocumentDetailItem.Section -> "Section(${node.title}, $depth)"
        is DocumentDetailItem.ArticleRow -> "Article(${article.number}, $depth)"
    }
}
