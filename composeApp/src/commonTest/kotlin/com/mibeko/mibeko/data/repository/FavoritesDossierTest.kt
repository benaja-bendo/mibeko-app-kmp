package com.mibeko.mibeko.data.repository

import com.mibeko.mibeko.data.local.entities.DossierArticleEntity
import com.mibeko.mibeko.data.local.entities.DossierTag
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Verrouille le dossier Favoris unique par compte (kmp#11).
 *
 * Le bug réel : chaque appareil créait son « Mes Favoris » sous un identifiant
 * aléatoire, et la synchronisation, qui fusionne par identifiant, gardait
 * autant de dossiers Favoris que d'appareils.
 */
class FavoritesDossierTest {

    private val compte = "019a2b3c-4d5e-7f60-8a1b-2c3d4e5f6a7b"
    private val autreCompte = "019a2b3c-4d5e-7f60-8a1b-2c3d4e5f6a7c"

    @Test
    fun `deux appareils du meme compte obtiennent le meme identifiant`() {
        assertEquals(FavoritesDossier.idFor(compte), FavoritesDossier.idFor(compte))
    }

    @Test
    fun `deux comptes obtiennent deux identifiants differents`() {
        assertNotEquals(FavoritesDossier.idFor(compte), FavoritesDossier.idFor(autreCompte))
    }

    @Test
    fun `l identifiant est un UUID de version 8 distinct du compte`() {
        val id = assertNotNull(FavoritesDossier.idFor(compte))
        assertNotEquals(compte, id)
        // La route de synchronisation exige un UUID.
        assertTrue(Regex("^[0-9a-f]{8}-[0-9a-f]{4}-8[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$").matches(id))
    }

    @Test
    fun `un compte dont l identifiant n est pas un UUID n a pas d identifiant derive`() {
        assertNull(FavoritesDossier.idFor("42"))
    }

    @Test
    fun `rien a faire quand seul le dossier du compte existe`() {
        val canonique = FavoritesDossier.create(id = "c", now = 10)
        assertNull(FavoritesDossier.consolidate("c", listOf(canonique), emptyList(), now = 50))
    }

    @Test
    fun `les favoris de deux appareils sont fondus sans perte dans le dossier du compte`() {
        // Appareil A : X et Y. Appareil B : Y (ajouté plus tôt, avec une note) et Z.
        val appareilA = FavoritesDossier.create(id = "a", now = 100)
        val appareilB = FavoritesDossier.create(id = "b", now = 200)
        val liens = listOf(
            lien("a", "X", addedAt = 110),
            lien("a", "Y", addedAt = 120),
            lien("b", "Y", addedAt = 105, note = "à relire"),
            lien("b", "Z", addedAt = 210),
            // Un article d'un classeur ordinaire ne doit pas entrer dans les favoris.
            lien("classeur", "W", addedAt = 130)
        )

        val plan = assertNotNull(
            FavoritesDossier.consolidate("c", listOf(appareilA, appareilB), liens, now = 999)
        )

        assertEquals(listOf("a", "b"), plan.duplicateIds)
        assertEquals("c", plan.canonical.id)
        assertEquals(DossierTag.FAVORIS, plan.canonical.tag)
        assertEquals(100L, plan.canonical.created_at)
        assertEquals(999L, plan.canonical.updated_at)
        assertEquals(listOf("Y", "X", "Z"), plan.links.map { it.articleId })
        assertTrue(plan.links.all { it.dossierId == "c" })
        val y = plan.links.first { it.articleId == "Y" }
        assertEquals(105L, y.addedAt)
        assertEquals("à relire", y.personalNote)
    }

    @Test
    fun `un doublon venu d un autre appareil rejoint le dossier du compte deja present`() {
        val canonique = FavoritesDossier.create(id = "c", now = 100)
        val ancien = FavoritesDossier.create(id = "r", now = 50)
        val plan = assertNotNull(
            FavoritesDossier.consolidate(
                canonicalId = "c",
                favoris = listOf(canonique, ancien),
                links = listOf(lien("c", "X", addedAt = 150), lien("r", "Z", addedAt = 60)),
                now = 999
            )
        )

        assertEquals(listOf("r"), plan.duplicateIds)
        // Le dossier du compte garde sa date de création.
        assertEquals(100L, plan.canonical.created_at)
        // Plus récent que tout ce que connaît le serveur : l'union l'emporte.
        assertEquals(999L, plan.canonical.updated_at)
        assertEquals(listOf("Z", "X"), plan.links.map { it.articleId })
    }

    private fun lien(dossierId: String, articleId: String, addedAt: Long, note: String? = null) =
        DossierArticleEntity(dossierId = dossierId, articleId = articleId, personalNote = note, addedAt = addedAt)
}
