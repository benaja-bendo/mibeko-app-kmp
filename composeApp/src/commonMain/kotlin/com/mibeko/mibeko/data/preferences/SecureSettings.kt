package com.mibeko.mibeko.data.preferences

import com.russhwolf.settings.Settings

/**
 * Fournit le stockage sécurisé des préférences utilisateur (dont le jeton
 * Sanctum) : EncryptedSharedPreferences sur Android, Keychain sur iOS.
 *
 * Chaque implémentation migre au premier lancement les valeurs présentes
 * dans l'ancien stockage en clair (SharedPreferences par défaut /
 * NSUserDefaults) puis les efface : les utilisateurs déjà connectés ne
 * sont pas déconnectés par le changement de stockage.
 */
expect fun createSecureSettings(): Settings

/**
 * Migration ponctuelle de l'ancien stockage en clair vers le stockage
 * sécurisé. Seules les clés connues de [UserPreferencesRepository] sont
 * migrées : les autres gestionnaires (historique de recherche, consultations
 * récentes) restent volontairement dans le stockage par défaut, leurs
 * données n'étant pas sensibles.
 */
internal object LegacySettingsMigration {

    private val STRING_KEYS = listOf(
        UserPreferencesRepository.KEY_AUTH_TOKEN,
        UserPreferencesRepository.KEY_USER_EMAIL,
        UserPreferencesRepository.KEY_USER_NAME,
        UserPreferencesRepository.KEY_APP_THEME,
        UserPreferencesRepository.KEY_TEXT_SIZE,
        UserPreferencesRepository.KEY_READER_THEME,
        UserPreferencesRepository.KEY_DOSSIER_SYNC_ACCOUNT
    )

    private val BOOLEAN_KEYS = listOf(
        UserPreferencesRepository.KEY_ONBOARDING_COMPLETED,
        UserPreferencesRepository.KEY_OFFLINE_MODE,
        UserPreferencesRepository.KEY_NOTIFICATIONS_ENABLED,
        UserPreferencesRepository.KEY_DISCLAIMER_ACCEPTED,
        UserPreferencesRepository.KEY_DYSLEXIA_FONT,
        UserPreferencesRepository.KEY_WIFI_ONLY_DOWNLOAD,
        UserPreferencesRepository.KEY_LEGAL_MONITORING,
        UserPreferencesRepository.KEY_DOSSIER_ALERTS_ENABLED,
        UserPreferencesRepository.KEY_PROFILE_SETUP_COMPLETED
    )

    private val LONG_KEYS = listOf(
        UserPreferencesRepository.KEY_LAST_SYNC_TIMESTAMP,
        UserPreferencesRepository.KEY_DOSSIER_LAST_SYNC
    )

    /**
     * Copie chaque clé encore présente dans [legacy] vers [secure] (sans
     * écraser une valeur déjà migrée), puis la supprime de l'ancien stockage.
     * Défensif clé par clé : une valeur corrompue ne doit pas empêcher la
     * migration des autres ni faire planter le démarrage.
     */
    fun migrate(legacy: Settings, secure: Settings) {
        STRING_KEYS.forEach { key ->
            runCatching {
                legacy.getStringOrNull(key)?.let { value ->
                    if (!secure.hasKey(key)) secure.putString(key, value)
                    legacy.remove(key)
                }
            }
        }
        BOOLEAN_KEYS.forEach { key ->
            runCatching {
                if (legacy.hasKey(key)) {
                    if (!secure.hasKey(key)) secure.putBoolean(key, legacy.getBoolean(key, false))
                    legacy.remove(key)
                }
            }
        }
        LONG_KEYS.forEach { key ->
            runCatching {
                if (legacy.hasKey(key)) {
                    if (!secure.hasKey(key)) secure.putLong(key, legacy.getLong(key, 0L))
                    legacy.remove(key)
                }
            }
        }
    }
}
