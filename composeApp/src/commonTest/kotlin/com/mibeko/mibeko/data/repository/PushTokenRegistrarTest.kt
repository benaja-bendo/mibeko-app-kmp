package com.mibeko.mibeko.data.repository

import com.mibeko.mibeko.data.preferences.UserPreferencesRepository
import com.russhwolf.settings.MapSettings
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Verrouille l'enregistrement des appareils pour les push (kmp#34).
 *
 * Le bug réel : le réglage « Notifications » était désactivé par défaut et
 * l'enregistrement en dépendait. Se connecter n'inscrivait donc jamais
 * l'appareil : 3 appareils pour plus de 70 comptes mobiles.
 */
class PushTokenRegistrarTest {

    private data class Envoi(val deviceId: String, val token: String, val platform: String)

    private fun preferences(connecte: Boolean = true) = UserPreferencesRepository(MapSettings()).apply {
        if (connecte) setAuthToken("jeton-de-session")
    }

    private fun TestScope.registrar(
        prefs: UserPreferencesRepository,
        autorise: Boolean = true,
        envois: MutableList<Envoi>,
        accepte: Boolean = true
    ) = PushTokenRegistrar(
        userPreferencesRepository = prefs,
        registerDevice = { id, token, platform -> envois += Envoi(id, token, platform); accepte },
        isPermissionGranted = { autorise },
        scope = this,
        deviceId = { "appareil-1" },
        platformName = { "android 14" }
    )

    @Test
    fun `sans rien choisir un compte connecte avec l autorisation du systeme enregistre son appareil`() = runTest {
        val prefs = preferences()
        val envois = mutableListOf<Envoi>()

        registrar(prefs, envois = envois).onNewToken("fcm-123")
        advanceUntilIdle()

        assertEquals(listOf(Envoi("appareil-1", "fcm-123", "android")), envois)
        // Envoi accepté : plus rien en attente, le jeton reste connu.
        assertNull(prefs.getPendingPushToken())
        assertEquals("fcm-123", prefs.getLastPushToken())
    }

    @Test
    fun `sans autorisation du systeme l appareil n est pas enregistre`() = runTest {
        val prefs = preferences()
        val envois = mutableListOf<Envoi>()

        registrar(prefs, autorise = false, envois = envois).onNewToken("fcm-123")
        advanceUntilIdle()

        assertTrue(envois.isEmpty())
        // Le jeton attend : il partira quand l'autorisation sera accordée.
        assertEquals("fcm-123", prefs.getPendingPushToken())
    }

    @Test
    fun `des notifications coupees dans l application restent coupees`() = runTest {
        val prefs = preferences().apply { setNotificationsEnabled(false) }
        val envois = mutableListOf<Envoi>()

        registrar(prefs, envois = envois).onNewToken("fcm-123")
        advanceUntilIdle()

        assertTrue(envois.isEmpty())
    }

    @Test
    fun `un invite n enregistre pas d appareil`() = runTest {
        val envois = mutableListOf<Envoi>()

        registrar(preferences(connecte = false), envois = envois).onNewToken("fcm-123")
        advanceUntilIdle()

        assertTrue(envois.isEmpty())
    }

    @Test
    fun `apres un refus du serveur le jeton reste en attente`() = runTest {
        val prefs = preferences()
        val envois = mutableListOf<Envoi>()

        registrar(prefs, envois = envois, accepte = false).onNewToken("fcm-123")
        advanceUntilIdle()

        assertEquals(1, envois.size)
        assertEquals("fcm-123", prefs.getPendingPushToken())
    }

    @Test
    fun `au demarrage le dernier jeton connu rattache l appareil au compte`() = runTest {
        val prefs = preferences().apply { setLastPushToken("fcm-ancien") }
        val envois = mutableListOf<Envoi>()

        registrar(prefs, envois = envois).flushPendingToken()
        advanceUntilIdle()

        assertEquals(listOf(Envoi("appareil-1", "fcm-ancien", "android")), envois)
    }
}
