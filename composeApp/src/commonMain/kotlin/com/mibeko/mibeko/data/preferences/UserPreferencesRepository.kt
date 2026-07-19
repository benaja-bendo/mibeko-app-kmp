package com.mibeko.mibeko.data.preferences

import com.russhwolf.settings.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Repository for managing user preferences using multiplatform-settings.
 * Handles onboarding completion state and other user preferences.
 *
 * Le [settings] injecté doit être le stockage sécurisé de la plateforme
 * (voir [createSecureSettings]) : il porte notamment le jeton Sanctum.
 */
class UserPreferencesRepository(private val settings: Settings) {

    private val _theme = MutableStateFlow(getAppTheme())
    val theme: StateFlow<AppTheme> = _theme.asStateFlow()

    private val _isLoggedIn = MutableStateFlow(isLoggedIn())
    val isLoggedInFlow: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _textSize = MutableStateFlow(getTextSize())
    val textSizeFlow: StateFlow<TextSize> = _textSize.asStateFlow()

    private val _isDyslexiaFontEnabled = MutableStateFlow(isDyslexiaFontEnabled())
    val isDyslexiaFontEnabledFlow: StateFlow<Boolean> = _isDyslexiaFontEnabled.asStateFlow()

    private val _readerTheme = MutableStateFlow(getReaderTheme())
    val readerThemeFlow: StateFlow<ReaderTheme> = _readerTheme.asStateFlow()

    companion object {
        // Clés internes (et non privées) : LegacySettingsMigration les référence
        // pour migrer l'ancien stockage en clair vers le stockage sécurisé.
        internal const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"
        internal const val KEY_OFFLINE_MODE = "offline_mode_enabled"
        internal const val KEY_APP_THEME = "app_theme"
        internal const val KEY_LAST_SYNC_TIMESTAMP = "last_sync_timestamp"
        internal const val KEY_NOTIFICATIONS_ENABLED = "notifications_enabled"
        internal const val KEY_DISCLAIMER_ACCEPTED = "disclaimer_accepted"
        internal const val KEY_TEXT_SIZE = "text_size"
        internal const val KEY_READER_THEME = "reader_theme"
        internal const val KEY_DYSLEXIA_FONT = "dyslexia_font_enabled"
        internal const val KEY_WIFI_ONLY_DOWNLOAD = "wifi_only_download"
        internal const val KEY_LEGAL_MONITORING = "legal_monitoring_enabled"
        internal const val KEY_DOSSIER_ALERTS_ENABLED = "dossier_alerts_enabled"
        internal const val KEY_AUTH_TOKEN = "auth_token"
        internal const val KEY_USER_EMAIL = "user_email"
        internal const val KEY_USER_NAME = "user_name"
        internal const val KEY_PROFILE_SETUP_COMPLETED = "profile_setup_completed"
        internal const val KEY_DOSSIER_SYNC_ACCOUNT = "dossier_sync_account"
        internal const val KEY_DOSSIER_LAST_SYNC = "dossier_last_sync"
        internal const val KEY_PENDING_PUSH_TOKEN = "pending_push_token"
    }

    /**
     * Represents the application theme options.
     */
    enum class AppTheme {
        SYSTEM, LIGHT, DARK
    }

    /**
     * Represents text size options for reading.
     */
    enum class TextSize {
        SMALL, MEDIUM, LARGE
    }

    /**
     * Thème de la liseuse (indépendant du thème de l'application).
     */
    enum class ReaderTheme {
        PAPER, LIGHT, DARK
    }

    /**
     * Gets the current app theme preference.
     */
    fun getAppTheme(): AppTheme {
        val themeName = settings.getString(KEY_APP_THEME, AppTheme.SYSTEM.name)
        return try {
            AppTheme.valueOf(themeName)
        } catch (e: Exception) {
            AppTheme.SYSTEM
        }
    }

    /**
     * Sets the app theme preference.
     */
    fun setAppTheme(theme: AppTheme) {
        settings.putString(KEY_APP_THEME, theme.name)
        _theme.value = theme
    }

    /**
     * Gets the text size preference.
     */
    fun getTextSize(): TextSize {
        val sizeName = settings.getString(KEY_TEXT_SIZE, TextSize.MEDIUM.name)
        return try {
            TextSize.valueOf(sizeName)
        } catch (e: Exception) {
            TextSize.MEDIUM
        }
    }

    /**
     * Sets the text size preference.
     */
    fun setTextSize(size: TextSize) {
        settings.putString(KEY_TEXT_SIZE, size.name)
        _textSize.value = size
    }

    /**
     * Gets the persisted reader theme (defaults to PAPER).
     */
    fun getReaderTheme(): ReaderTheme {
        val name = settings.getString(KEY_READER_THEME, ReaderTheme.PAPER.name)
        return try {
            ReaderTheme.valueOf(name)
        } catch (e: Exception) {
            ReaderTheme.PAPER
        }
    }

    /**
     * Persists the reader theme.
     */
    fun setReaderTheme(theme: ReaderTheme) {
        settings.putString(KEY_READER_THEME, theme.name)
        _readerTheme.value = theme
    }

    /**
     * Checks if dyslexia font is enabled.
     */
    fun isDyslexiaFontEnabled(): Boolean {
        return settings.getBoolean(KEY_DYSLEXIA_FONT, false)
    }

    /**
     * Sets dyslexia font preference.
     */
    fun setDyslexiaFontEnabled(enabled: Boolean) {
        settings.putBoolean(KEY_DYSLEXIA_FONT, enabled)
        _isDyslexiaFontEnabled.value = enabled
    }

    /**
     * Checks if Wi-Fi only download is enabled.
     */
    fun isWifiOnlyDownloadEnabled(): Boolean {
        return settings.getBoolean(KEY_WIFI_ONLY_DOWNLOAD, true)
    }

    /**
     * Sets Wi-Fi only download preference.
     */
    fun setWifiOnlyDownloadEnabled(enabled: Boolean) {
        settings.putBoolean(KEY_WIFI_ONLY_DOWNLOAD, enabled)
    }

    /**
     * Checks if legal monitoring is enabled.
     */
    fun isLegalMonitoringEnabled(): Boolean {
        return settings.getBoolean(KEY_LEGAL_MONITORING, true)
    }

    /**
     * Sets legal monitoring preference.
     */
    fun setLegalMonitoringEnabled(enabled: Boolean) {
        settings.putBoolean(KEY_LEGAL_MONITORING, enabled)
    }

    /**
     * Checks if dossier alerts are enabled.
     */
    fun isDossierAlertsEnabled(): Boolean {
        return settings.getBoolean(KEY_DOSSIER_ALERTS_ENABLED, true)
    }

    /**
     * Sets dossier alerts preference.
     */
    fun setDossierAlertsEnabled(enabled: Boolean) {
        settings.putBoolean(KEY_DOSSIER_ALERTS_ENABLED, enabled)
    }

    /**
     * Gets the timestamp of the last successful data synchronization.
     */
    fun getLastSyncTimestamp(): Long {
        return settings.getLong(KEY_LAST_SYNC_TIMESTAMP, 0L)
    }

    /**
     * Sets the timestamp of the last successful data synchronization.
     */
    fun setLastSyncTimestamp(timestamp: Long) {
        settings.putLong(KEY_LAST_SYNC_TIMESTAMP, timestamp)
    }

    /**
     * Checks if notifications are enabled by the user.
     */
    fun isNotificationsEnabled(): Boolean {
        return settings.getBoolean(KEY_NOTIFICATIONS_ENABLED, false)
    }

    /**
     * Sets the notifications preference.
     */
    fun setNotificationsEnabled(enabled: Boolean) {
        settings.putBoolean(KEY_NOTIFICATIONS_ENABLED, enabled)
    }

    /**
     * Checks if offline-only mode is enabled.
     * When enabled, the app will not use network for searches even if available.
     * @return true if offline mode is forced, false otherwise.
     */
    fun isOfflineModeEnabled(): Boolean {
        return settings.getBoolean(KEY_OFFLINE_MODE, false)
    }

    /**
     * Sets the offline-only mode preference.
     * @param enabled true to force offline mode, false to allow network usage.
     */
    fun setOfflineMode(enabled: Boolean) {
        settings.putBoolean(KEY_OFFLINE_MODE, enabled)
    }

    /**
     * Checks if the user has completed the onboarding flow.
     * @return true if onboarding was completed, false otherwise.
     */
    fun hasCompletedOnboarding(): Boolean {
        return settings.getBoolean(KEY_ONBOARDING_COMPLETED, false)
    }

    /**
     * Marks the onboarding as completed.
     * This prevents the onboarding from showing on subsequent app launches.
     */
    fun setOnboardingCompleted() {
        settings.putBoolean(KEY_ONBOARDING_COMPLETED, true)
    }

    fun isProfileSetupCompleted(): Boolean {
        return settings.getBoolean(KEY_PROFILE_SETUP_COMPLETED, false)
    }

    fun setProfileSetupCompleted(completed: Boolean) {
        settings.putBoolean(KEY_PROFILE_SETUP_COMPLETED, completed)
    }

    /**
     * Resets the onboarding state (useful for testing or settings reset).
     */
    fun resetOnboarding() {
        settings.remove(KEY_ONBOARDING_COMPLETED)
    }

    /**
     * Gets the stored authentication token.
     */
    fun getAuthToken(): String? {
        return settings.getStringOrNull(KEY_AUTH_TOKEN)
    }

    /**
     * Sets the authentication token.
     */
    fun setAuthToken(token: String?) {
        if (token == null) {
            settings.remove(KEY_AUTH_TOKEN)
            _isLoggedIn.value = false
        } else {
            settings.putString(KEY_AUTH_TOKEN, token)
            _isLoggedIn.value = true
        }
    }

    /**
     * Token push (FCM/APNs) reçu alors que l'envoi au backend n'était pas
     * possible (utilisateur déconnecté, réseau absent). Il sera transmis au
     * prochain login ou démarrage connecté par PushTokenRegistrar.
     */
    fun getPendingPushToken(): String? {
        return settings.getStringOrNull(KEY_PENDING_PUSH_TOKEN)
    }

    fun setPendingPushToken(token: String?) {
        if (token == null) {
            settings.remove(KEY_PENDING_PUSH_TOKEN)
        } else {
            settings.putString(KEY_PENDING_PUSH_TOKEN, token)
        }
    }

    /**
     * Sets the user info.
     */
    fun setUserInfo(name: String, email: String) {
        settings.putString(KEY_USER_NAME, name)
        settings.putString(KEY_USER_EMAIL, email)
    }

    /**
     * Gets the user name.
     */
    fun getUserName(): String? {
        return settings.getStringOrNull(KEY_USER_NAME)
    }

    /**
     * Gets the user email.
     */
    fun getUserEmail(): String? {
        return settings.getStringOrNull(KEY_USER_EMAIL)
    }

    /**
     * Checks if the user is logged in.
     */
    fun isLoggedIn(): Boolean {
        return getAuthToken() != null
    }

    /**
     * Logs out the user by clearing the auth token.
     */
    fun logout() {
        settings.remove(KEY_AUTH_TOKEN)
        settings.remove(KEY_USER_NAME)
        settings.remove(KEY_USER_EMAIL)
        settings.remove(KEY_PROFILE_SETUP_COMPLETED)
        _isLoggedIn.value = false
    }

    /**
     * Clears all stored preferences.
     */
    fun clearAll() {
        settings.clear()
    }

    /**
     * Compte (email) auquel appartiennent les dossiers synchronisés localement.
     * Permet de détecter un changement d'utilisateur et d'éviter de mélanger
     * les dossiers de deux comptes.
     */
    fun getDossierSyncAccount(): String? {
        return settings.getStringOrNull(KEY_DOSSIER_SYNC_ACCOUNT)
    }

    fun setDossierSyncAccount(email: String?) {
        if (email == null) {
            settings.remove(KEY_DOSSIER_SYNC_ACCOUNT)
        } else {
            settings.putString(KEY_DOSSIER_SYNC_ACCOUNT, email)
        }
    }

    /** Horodatage (epoch ms) de la dernière synchronisation réussie des dossiers. */
    fun getDossierLastSyncAt(): Long {
        return settings.getLong(KEY_DOSSIER_LAST_SYNC, 0L)
    }

    fun setDossierLastSyncAt(timestamp: Long) {
        settings.putLong(KEY_DOSSIER_LAST_SYNC, timestamp)
    }

    /**
     * Checks if the user has accepted the disclaimer.
     */
    fun hasAcceptedDisclaimer(): Boolean {
        return settings.getBoolean(KEY_DISCLAIMER_ACCEPTED, false)
    }

    /**
     * Marks the disclaimer as accepted.
     */
    fun setDisclaimerAccepted() {
        settings.putBoolean(KEY_DISCLAIMER_ACCEPTED, true)
    }
}
