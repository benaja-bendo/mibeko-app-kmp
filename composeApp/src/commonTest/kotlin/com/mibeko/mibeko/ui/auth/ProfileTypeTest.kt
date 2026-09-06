package com.mibeko.mibeko.ui.auth

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * mibeko-dashboard#98 : ces quatre libellés sont l'unique source de vérité
 * côté mobile, réutilisée par le sélecteur des Réglages — un désaccord avec
 * la liste fermée côté serveur (contrainte CHECK sur `mobile_profiles`)
 * ferait échouer silencieusement l'enregistrement du profil.
 */
class ProfileTypeTest {

    @Test
    fun `les quatre libelles correspondent exactement a la liste fermee du serveur`() {
        assertEquals(
            listOf("Citoyen", "Étudiant", "Professionnel du droit", "Autre"),
            ProfileType.entries.map { it.label },
        )
    }

    @Test
    fun `fromLabel retrouve le type a partir du libelle exact`() {
        assertEquals(ProfileType.STUDENT, ProfileType.fromLabel("Étudiant"))
        assertEquals(ProfileType.OTHER, ProfileType.fromLabel("Autre"))
    }

    @Test
    fun `fromLabel renvoie null pour un libelle hors liste`() {
        assertNull(ProfileType.fromLabel("Avocat"))
        assertNull(ProfileType.fromLabel(""))
    }
}
