package com.mibeko.mibeko.data.preferences

import com.russhwolf.settings.Settings

/**
 * Repository for managing user preferences using multiplatform-settings.
 * Handles onboarding completion state and other user preferences.
 */
class UserPreferencesRepository(private val settings: Settings = Settings()) {

    companion object {
        private const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"
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
