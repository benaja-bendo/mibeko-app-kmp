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
 * qui utilise réellement Mibeko. Réutilise le champ `profession` existant.
 *
 * Liste fermée depuis mibeko-dashboard#98 (une contrainte CHECK en base
 * refuse désormais toute autre valeur) : ces quatre libellés sont l'unique
 * source de vérité côté mobile, réutilisée telle quelle par le sélecteur
 * des Réglages (`ui/settings/SettingsScreen.kt`) — jamais redéclarée en dur
 * une seconde fois, c'est exactement cette duplication qui avait laissé
 * passer des orthographes libres (« Avocat », « poète »…) jusqu'au serveur.
 * Seuls CITIZEN et PROFESSIONAL sont proposés ici, à l'onboarding : c'est
 * volontairement un choix grossier, affiné ensuite dans Réglages si besoin.
 */
enum class ProfileType(val label: String) {
    CITIZEN("Citoyen"),
    STUDENT("Étudiant"),
    PROFESSIONAL("Professionnel du droit"),
    OTHER("Autre");

    companion object {
        fun fromLabel(label: String): ProfileType? = entries.find { it.label == label }
    }
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
