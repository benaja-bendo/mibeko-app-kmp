package com.mibeko.mibeko.data.repository

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * La recherche et l'autocomplétion hors-ligne passent la saisie à
 * `articles_fts MATCH`. Une expression MATCH invalide lève une SQLiteException
 * jusque dans le viewModelScope — donc un crash. Ces cas sont exactement les
 * saisies intermédiaires qu'un utilisateur produit en tapant.
 */
class FtsQuerySanitizerTest {

    private fun sanitize(raw: String) = LocalLegalRepository.sanitizeFtsQuery(raw)

    @Test
    fun `chaque terme est encadre de guillemets`() {
        assertEquals("\"code\" \"travail\"", sanitize("code travail"))
    }

    @Test
    fun `les guillemets de la saisie sont echappes`() {
        // `"c` en cours de frappe : sans échappement, expression MATCH invalide.
        assertEquals("\"\"\"c\"", sanitize("\"c"))
        assertEquals("\"\"\"code\"\"\"", sanitize("\"code\""))
    }

    @Test
    fun `les caracteres de syntaxe FTS deviennent litteraux`() {
        for (raw in listOf("(a", "co\"", "-mot", "a*b", "code AND", "OR", "NOT")) {
            val sanitized = sanitize(raw)
            // Tout jeton produit est encadré : aucun opérateur ne fuit.
            assertTrue(
                sanitized.split(" ").all { it.startsWith("\"") && it.endsWith("\"") },
                "saisie « $raw » → $sanitized"
            )
        }
    }

    @Test
    fun `les references juridiques usuelles sont preservees`() {
        assertEquals("\"art.\" \"12\"", sanitize("art. 12"))
        assertEquals("\"L.121-3\"", sanitize("L.121-3"))
    }

    @Test
    fun `espaces multiples et saisie vide`() {
        assertEquals("\"code\" \"civil\"", sanitize("  code   civil  "))
        assertEquals("", sanitize("   "))
        assertEquals("", sanitize(""))
    }
}
