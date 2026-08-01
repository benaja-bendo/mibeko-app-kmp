package com.mibeko.mibeko.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mibeko.mibeko.data.preferences.UserPreferencesRepository
import com.mibeko.mibeko.data.remote.AuthApiService
import com.mibeko.mibeko.data.remote.ProfileUpdateRequest
import com.mibeko.mibeko.util.AnalyticsEvents
import com.mibeko.mibeko.util.MibekoAnalytics
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class ProfileSetupState {
    object Idle : ProfileSetupState()
    object Loading : ProfileSetupState()
    object Success : ProfileSetupState()
    data class Error(val message: String) : ProfileSetupState()
}

/**
 * Remplace l'ancien formulaire B2B (téléphone + profession + entreprise, tous
 * requis, sans bouton Passer) : un choix simple qui sert avant tout à mesurer
 * qui utilise réellement Mibeko. Réutilise le champ `profession` existant
 * (aucune migration backend) — modifiable ensuite dans Réglages si l'utilisateur
 * veut préciser (« Avocat », etc.).
 */
enum class ProfileType(val label: String) {
    CITIZEN("Citoyen"),
    PROFESSIONAL("Professionnel du droit")
}

class ProfileSetupViewModel(
    private val authApiService: AuthApiService,
    private val userPreferences: UserPreferencesRepository,
    private val analytics: MibekoAnalytics
) : ViewModel() {

    private val _setupState = MutableStateFlow<ProfileSetupState>(ProfileSetupState.Idle)
    val setupState = _setupState.asStateFlow()

    fun selectProfileType(type: ProfileType) {
        viewModelScope.launch {
            _setupState.value = ProfileSetupState.Loading
            try {
                val request = ProfileUpdateRequest(
                    name = null,
                    phone = "",
                    profession = type.label,
                    company = ""
                )
                val response = authApiService.updateProfile(request)

                if (response.success) {
                    userPreferences.setProfileSetupCompleted(true)
                    analytics.logEvent(
                        AnalyticsEvents.PROFILE_TYPE_SELECTED,
                        mapOf("type" to type.name.lowercase())
                    )
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
