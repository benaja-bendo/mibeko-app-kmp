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
    }

    /**
     * Represents the application theme options.
     */
    enum class AppTheme {
        SYSTEM, LIGHT, DARK
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
     * Clears all stored preferences.
     */
    fun clearAll() {
        settings.clear()
    }
}
