package com.mibeko.mibeko.data.preferences

import com.russhwolf.settings.MapSettings
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Unit tests for UserPreferencesRepository.
 * Uses MapSettings for in-memory testing without platform-specific dependencies.
 */
class UserPreferencesRepositoryTest {

    private lateinit var settings: MapSettings
    private lateinit var repository: UserPreferencesRepository

    @BeforeTest
    fun setup() {
        settings = MapSettings()
        repository = UserPreferencesRepository(settings)
    }

    @Test
    fun hasCompletedOnboarding_defaultsToFalse() {
        // When: checking onboarding status without any prior action
        val result = repository.hasCompletedOnboarding()

        // Then: should return false
        assertFalse(result, "Onboarding should not be completed by default")
    }

    @Test
    fun setOnboardingCompleted_setsValueToTrue() {
        // Given: onboarding not completed
        assertFalse(repository.hasCompletedOnboarding())

        // When: marking onboarding as completed
        repository.setOnboardingCompleted()

        // Then: should return true
        assertTrue(repository.hasCompletedOnboarding(), "Onboarding should be marked as completed")
    }

    @Test
    fun resetOnboarding_clearsOnboardingStatus() {
        // Given: onboarding completed
        repository.setOnboardingCompleted()
        assertTrue(repository.hasCompletedOnboarding())

        // When: resetting onboarding
        repository.resetOnboarding()

        // Then: should return false again
        assertFalse(repository.hasCompletedOnboarding(), "Onboarding status should be reset")
    }

    @Test
    fun clearAll_clearsAllPreferences() {
        // Given: onboarding completed
        repository.setOnboardingCompleted()
        assertTrue(repository.hasCompletedOnboarding())

        // When: clearing all preferences
        repository.clearAll()

        // Then: should return false
        assertFalse(repository.hasCompletedOnboarding(), "All preferences should be cleared")
    }
}
