package com.mibeko.mibeko.ui.library

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
}
