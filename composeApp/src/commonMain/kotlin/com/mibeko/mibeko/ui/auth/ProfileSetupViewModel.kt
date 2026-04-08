package com.mibeko.mibeko.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mibeko.mibeko.data.preferences.UserPreferencesRepository
import com.mibeko.mibeko.data.remote.AuthApiService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class ProfileSetupState {
    object Idle : ProfileSetupState()
    object Loading : ProfileSetupState()
    object Success : ProfileSetupState()
    data class Error(val message: String) : ProfileSetupState()
}

class ProfileSetupViewModel(
    private val authApiService: AuthApiService,
    private val userPreferences: UserPreferencesRepository
) : ViewModel() {

    private val _setupState = MutableStateFlow<ProfileSetupState>(ProfileSetupState.Idle)
    val setupState = _setupState.asStateFlow()

    fun saveProfile(phone: String, profession: String, company: String) {
        viewModelScope.launch {
            _setupState.value = ProfileSetupState.Loading
            try {
                // In a real scenario, we'd have a specific endpoint to update the profile
                // For now, I'll simulate a success response since the backend stores blank 
                // profile upon create and it can be updated via generic user/profile update API later.
                
                // simulate API call
                kotlinx.coroutines.delay(1000)
                
                userPreferences.setOnboardingCompleted()
                _setupState.value = ProfileSetupState.Success
            } catch (e: Exception) {
                _setupState.value = ProfileSetupState.Error(e.message ?: "Une erreur est survenue.")
            }
        }
    }
}
