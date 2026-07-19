package com.mibeko.mibeko.data.preferences

import com.russhwolf.settings.ExperimentalSettingsImplementation
import com.russhwolf.settings.KeychainSettings
import com.russhwolf.settings.NSUserDefaultsSettings
import com.russhwolf.settings.Settings
import platform.Foundation.NSUserDefaults

/**
 * Keychain iOS derrière l'interface Settings, avec migration depuis les
 * NSUserDefaults utilisées historiquement par `Settings()` (multiplatform-settings
 * no-arg), qui stockaient le jeton en clair.
 */
@OptIn(ExperimentalSettingsImplementation::class)
actual fun createSecureSettings(): Settings {
    val secure = KeychainSettings(service = "cg.mibeko.app")

    // Ancien stockage en clair : la suite standard des NSUserDefaults.
    val legacy = NSUserDefaultsSettings(NSUserDefaults.standardUserDefaults)
    LegacySettingsMigration.migrate(legacy, secure)

    return secure
}
