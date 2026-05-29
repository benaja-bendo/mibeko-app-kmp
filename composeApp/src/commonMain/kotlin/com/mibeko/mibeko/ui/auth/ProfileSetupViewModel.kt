package com.mibeko.mibeko.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mibeko.mibeko.data.preferences.UserPreferencesRepository
import com.mibeko.mibeko.data.remote.AuthApiService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

import com.mibeko.mibeko.data.remote.ProfileUpdateRequest

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
                val request = ProfileUpdateRequest(
                    name = null,
                    phone = phone,
                    profession = profession,
                    company = company
                )
                val response = authApiService.updateProfile(request)
                
                if (response.success) {
                    userPreferences.setProfileSetupCompleted(true)
                    _setupState.value = ProfileSetupState.Success
                } else {
                    _setupState.value = ProfileSetupState.Error(response.message ?: "Une erreur est survenue.")
                }
            } catch (e: Exception) {
                _setupState.value = ProfileSetupState.Error(e.message ?: "Une erreur est survenue.")
            }
        }
    }
}
