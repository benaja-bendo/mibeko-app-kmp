package com.mibeko.mibeko.ui.library

import com.mibeko.mibeko.data.ArticleSpec
import kotlin.test.Test
import kotlin.test.assertEquals

class LibraryFiltersTest {

    @Test
    fun `le filtre telecharges compte parmi les filtres actifs`() {
        assertEquals(0, LibraryUiState().activeFilterCount)
        assertEquals(1, LibraryUiState(downloadedOnly = true).activeFilterCount)
    }

    @Test
    fun `la reinitialisation logique conserve un compteur exact`() {
        val state = LibraryUiState(
            scope = LibraryScope.OHADA,
            selectedTypeCode = "code",
            selectedInstitutionId = "institution",
            downloadedOnly = true,
            sort = LibrarySort.DATE_DESC
        )

        assertEquals(5, state.activeFilterCount)
    }

    private fun article(id: String, type: String) =
        ArticleSpec(id = id, codeId = "d-$id", number = "1", title = "Chapitre I", content = "…", breadcrumb = "", typeCode = type)

    @Test
    fun `hors ligne le filtre de type s applique au repli local`() {
        val resultats = listOf(article("a", "LOI"), article("b", "DECRET"), article("c", "LOI"))

        val filtres = resultats.withLocalFilters(LibraryUiState(selectedTypeCode = "LOI"))

        assertEquals(listOf("a", "c"), filtres.map { it.id })
        assertEquals(3, resultats.withLocalFilters(LibraryUiState()).size)
    }

    @Test
    fun `hors ligne les filtres que la base locale ignore sont nommes`() {
        assertEquals(emptyList(), LibraryUiState(selectedTypeCode = "LOI").filtersIgnoredOffline())
        assertEquals(
            listOf("périmètre", "institution", "tri"),
            LibraryUiState(
                scope = LibraryScope.OHADA,
                selectedInstitutionId = "inst-1",
                sort = LibrarySort.DATE_DESC
            ).filtersIgnoredOffline()
        )
    }
}
