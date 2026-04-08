package com.mibeko.mibeko.data.preferences

import com.russhwolf.settings.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Repository for managing user preferences using multiplatform-settings.
 * Handles onboarding completion state and other user preferences.
 */
class UserPreferencesRepository(private val settings: Settings = Settings()) {

    private val _theme = MutableStateFlow(getAppTheme())
    val theme: StateFlow<AppTheme> = _theme.asStateFlow()

    companion object {
        private const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"
        private const val KEY_OFFLINE_MODE = "offline_mode_enabled"
        private const val KEY_APP_THEME = "app_theme"
        private const val KEY_LAST_SYNC_TIMESTAMP = "last_sync_timestamp"
        private const val KEY_NOTIFICATIONS_ENABLED = "notifications_enabled"
        private const val KEY_DISCLAIMER_ACCEPTED = "disclaimer_accepted"
        private const val KEY_TEXT_SIZE = "text_size"
        private const val KEY_DYSLEXIA_FONT = "dyslexia_font_enabled"
        private const val KEY_WIFI_ONLY_DOWNLOAD = "wifi_only_download"
        private const val KEY_LEGAL_MONITORING = "legal_monitoring_enabled"
        private const val KEY_DOSSIER_ALERTS_ENABLED = "dossier_alerts_enabled"
        private const val KEY_AUTH_TOKEN = "auth_token"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_USER_NAME = "user_name"
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
        } else {
            settings.putString(KEY_AUTH_TOKEN, token)
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
    }

    /**
     * Clears all stored preferences.
     */
    fun clearAll() {
        settings.clear()
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
