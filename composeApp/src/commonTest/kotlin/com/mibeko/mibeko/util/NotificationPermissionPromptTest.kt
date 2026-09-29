package com.mibeko.mibeko.util

import com.mibeko.mibeko.data.preferences.UserPreferencesRepository
import com.mibeko.mibeko.data.repository.PushTokenRegistrar
import com.russhwolf.settings.MapSettings
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Verrouille le « bon moment » de la demande d'autorisation (APP-004, kmp#34) :
 * une seule fois, après un téléchargement, jamais sur une plateforme sans push.
 */
class NotificationPermissionPromptTest {

    private class FauxSysteme(var autorise: Boolean, var fenetrePossible: Boolean = true) : NotificationManager {
        var demandes = 0
        override fun requestPermission(onPermissionResult: (Boolean) -> Unit) {
            demandes++
            // Comme Android 13 et plus : le rappel arrive avant la réponse
            // réelle, et vaut `false` si aucune fenêtre n'a pu s'ouvrir.
            onPermissionResult(fenetrePossible)
        }
        override fun isPermissionGranted() = autorise
        override fun getPushToken(onTokenResult: (String?) -> Unit) =
            onTokenResult(if (autorise) "fcm-123" else null)
    }

    private class FauxAnalytics : AnalyticsManager {
        val evenements = mutableListOf<Pair<String, Map<String, Any>?>>()
        override fun logEvent(name: String, params: Map<String, Any>?) {
            evenements += name to params
        }
        override fun setCollectionEnabled(enabled: Boolean) = Unit
    }

    private class Montage(
        val prefs: UserPreferencesRepository,
        val systeme: FauxSysteme,
        val analytics: FauxAnalytics,
        val envois: MutableList<String>,
        val prompt: NotificationPermissionPrompt
    )

    private fun TestScope.monter(autorise: Boolean = false, pushSupported: Boolean = true): Montage {
        val prefs = UserPreferencesRepository(MapSettings()).apply { setAuthToken("jeton-de-session") }
        val systeme = FauxSysteme(autorise)
        val analytics = FauxAnalytics()
        val envois = mutableListOf<String>()
        val registrar = PushTokenRegistrar(
            userPreferencesRepository = prefs,
            registerDevice = { _, token, _ -> envois += token; true },
            isPermissionGranted = { systeme.autorise },
            scope = this,
            deviceId = { "appareil-1" },
            platformName = { "android" }
        )
        val prompt = NotificationPermissionPrompt(
            prefs, systeme, registrar, MibekoAnalytics(analytics, prefs), pushSupported
        )
        return Montage(prefs, systeme, analytics, envois, prompt)
    }

    @Test
    fun `le premier telechargement ouvre la fenetre du systeme et la suite non`() = runTest {
        val m = monter()

        m.prompt.afterOfflineDownload()
        m.prompt.afterOfflineDownload()

        assertEquals(1, m.systeme.demandes)
        assertTrue(m.prefs.wasNotificationPermissionAsked())
    }

    @Test
    fun `l appareil s enregistre quand l utilisateur accepte dans la fenetre`() = runTest {
        val m = monter()

        m.prompt.afterOfflineDownload()
        advanceUntilIdle()
        // Rien tant que la réponse n'est pas arrivée.
        assertTrue(m.envois.isEmpty())

        m.systeme.autorise = true
        m.prompt.onSystemPermissionResult(granted = true)
        advanceUntilIdle()

        assertEquals(listOf("fcm-123"), m.envois)
        assertEquals(
            mapOf("granted" to true, "context" to "after_download"),
            m.analytics.evenements.single { it.first == AnalyticsEvents.NOTIFICATION_OPT_IN }.second
        )
    }

    @Test
    fun `sans fenetre possible la demande n est pas consommee`() = runTest {
        val m = monter()
        m.systeme.fenetrePossible = false

        m.prompt.afterOfflineDownload()
        assertTrue(!m.prefs.wasNotificationPermissionAsked())

        // Au téléchargement suivant, l'activité est revenue : la fenêtre s'ouvre.
        m.systeme.fenetrePossible = true
        m.prompt.afterOfflineDownload()
        assertEquals(2, m.systeme.demandes)
        assertTrue(m.prefs.wasNotificationPermissionAsked())
    }

    @Test
    fun `un refus n enregistre rien`() = runTest {
        val m = monter()

        m.prompt.afterOfflineDownload()
        m.prompt.onSystemPermissionResult(granted = false)
        advanceUntilIdle()

        assertTrue(m.envois.isEmpty())
    }

    @Test
    fun `autorisation deja donnee l appareil s enregistre sans fenetre`() = runTest {
        val m = monter(autorise = true)

        m.prompt.afterOfflineDownload()
        advanceUntilIdle()

        assertEquals(0, m.systeme.demandes)
        assertEquals(listOf("fcm-123"), m.envois)
    }

    @Test
    fun `sans push sur la plateforme la question n est jamais posee`() = runTest {
        val m = monter(pushSupported = false)

        m.prompt.afterOfflineDownload()

        assertEquals(0, m.systeme.demandes)
        // L'unique demande d'iOS reste disponible pour le jour où les push y arriveront.
        assertTrue(!m.prefs.wasNotificationPermissionAsked())
    }

    @Test
    fun `des notifications coupees dans l application empechent la demande`() = runTest {
        val m = monter()
        m.prefs.setNotificationsEnabled(false)

        m.prompt.afterOfflineDownload()

        assertEquals(0, m.systeme.demandes)
    }
}
