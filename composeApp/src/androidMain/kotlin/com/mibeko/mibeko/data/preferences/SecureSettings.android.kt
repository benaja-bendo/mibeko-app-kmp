package com.mibeko.mibeko.data.preferences

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.mibeko.mibeko.MibekoApp
import com.russhwolf.settings.Settings
import com.russhwolf.settings.SharedPreferencesSettings

/** Fichier de préférences chiffré dédié aux données sensibles (jeton Sanctum…). */
private const val SECURE_PREFS_FILE = "mibeko_secure_prefs"

/**
 * EncryptedSharedPreferences (AES-256, clé maître dans l'Android Keystore)
 * derrière l'interface Settings, avec migration depuis les SharedPreferences
 * par défaut utilisées historiquement par `Settings()` (multiplatform-settings
 * no-arg), qui stockaient le jeton en clair.
 */
actual fun createSecureSettings(): Settings {
    val context = MibekoApp.INSTANCE
    val secure = SharedPreferencesSettings(createEncryptedPreferences(context))

    // Ancien stockage en clair : PreferenceManager.getDefaultSharedPreferences,
    // soit le fichier "<package>_preferences" (contrat Android stable).
    val legacy = SharedPreferencesSettings(
        context.getSharedPreferences("${context.packageName}_preferences", Context.MODE_PRIVATE)
    )
    LegacySettingsMigration.migrate(legacy, secure)

    return secure
}

private fun createEncryptedPreferences(context: Context): SharedPreferences {
    return try {
        buildEncryptedPreferences(context)
    } catch (e: Exception) {
        // Clé maître ou fichier corrompu (réinstallation, restauration de
        // sauvegarde…) : on repart d'un fichier neuf plutôt que de planter
        // au démarrage. L'utilisateur devra se reconnecter.
        context.deleteSharedPreferences(SECURE_PREFS_FILE)
        try {
            buildEncryptedPreferences(context)
        } catch (e2: Exception) {
            // Dernier recours : fichier dédié non chiffré pour que l'app
            // reste utilisable sur les appareils au Keystore défaillant.
            context.getSharedPreferences("${SECURE_PREFS_FILE}_fallback", Context.MODE_PRIVATE)
        }
    }
}

private fun buildEncryptedPreferences(context: Context): SharedPreferences {
    val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()
    return EncryptedSharedPreferences.create(
        context,
        SECURE_PREFS_FILE,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
}
