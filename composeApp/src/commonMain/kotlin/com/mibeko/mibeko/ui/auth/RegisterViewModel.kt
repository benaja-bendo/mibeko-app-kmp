package com.mibeko.mibeko.ui.auth

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mibeko.mibeko.data.preferences.UserPreferencesRepository
import com.mibeko.mibeko.data.remote.AuthApiService
import com.mibeko.mibeko.data.remote.RegisterRequest
import com.mibeko.mibeko.data.repository.PushTokenRegistrar
import com.mibeko.mibeko.util.AnalyticsEvents
import com.mibeko.mibeko.util.MibekoAnalytics
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class RegisterState {
    object Idle : RegisterState()
    object Loading : RegisterState()
    data class Success(val requiresProfileSetup: Boolean) : RegisterState()
    data class Error(val message: String) : RegisterState()
}

class RegisterViewModel(
    private val authApiService: AuthApiService,
    private val userPreferences: UserPreferencesRepository,
    private val pushTokenRegistrar: PushTokenRegistrar,
    private val analytics: MibekoAnalytics
) : ViewModel() {

    private val _registerState = MutableStateFlow<RegisterState>(RegisterState.Idle)
    val registerState: StateFlow<RegisterState> = _registerState.asStateFlow()

    var name by mutableStateOf("")
        private set

    var email by mutableStateOf("")
        private set

    var password by mutableStateOf("")
        private set

    var passwordConfirmation by mutableStateOf("")
        private set

    /**
     * Updates the name field.
     */
    fun onNameChange(newName: String) {
        name = newName
    }

    /**
     * Updates the email field.
     */
    fun onEmailChange(newEmail: String) {
        email = newEmail
    }

    /**
     * Updates the password field.
     */
    fun onPasswordChange(newPassword: String) {
        password = newPassword
    }

    /**
     * Updates the password confirmation field.
     */
    fun onPasswordConfirmationChange(newPasswordConfirmation: String) {
        passwordConfirmation = newPasswordConfirmation
    }

    /**
     * Executes the registration process.
     */
    fun register() {
        if (name.isBlank() || email.isBlank() || password.isBlank() || passwordConfirmation.isBlank()) {
            _registerState.value = RegisterState.Error("Veuillez remplir tous les champs.")
            return
        }

        if (password != passwordConfirmation) {
            _registerState.value = RegisterState.Error("Les mots de passe ne correspondent pas.")
            return
        }

        viewModelScope.launch {
            _registerState.value = RegisterState.Loading
            try {
                val request = RegisterRequest(
                    name = name,
                    email = email,
                    password = password,
                    password_confirmation = passwordConfirmation,
                    device_name = "Mobile Device"
                )
                val response = authApiService.register(request)
                
                if (response.success && response.data != null) {
                    userPreferences.setAuthToken(response.data.token)
                    authApiService.invalidateTokenCache()
                    // Le choix Citoyen/Professionnel (ProfileSetupScreen) pose ce
                    // seul champ : ne plus exiger téléphone/entreprise, sinon
                    // l'écran se redéclencherait à chaque connexion.
                    val profileComplete = !response.data.user?.mobile_profile?.profession.isNullOrBlank()
                    userPreferences.setProfileSetupCompleted(profileComplete)
                    if (response.data.user != null) {
                        userPreferences.setUserInfo(response.data.user.name, response.data.user.email)
                    } else {
                        userPreferences.setUserInfo(name, email) // Fallback
                    }
                    // Un token push a pu être renouvelé hors session : l'envoyer maintenant.
                    pushTokenRegistrar.flushPendingToken()
                    analytics.logEvent(AnalyticsEvents.LOGIN_COMPLETED, mapOf("method" to "register"))
                    _registerState.value = RegisterState.Success(requiresProfileSetup = !profileComplete)
                } else {
                    // Extract the first error message if available
                    val errorMessage = response.errors?.values?.firstOrNull()?.firstOrNull()
                        ?: response.message 
                        ?: "Une erreur est survenue lors de l'inscription."
                    _registerState.value = RegisterState.Error(errorMessage)
                }
            } catch (e: Exception) {
                _registerState.value = RegisterState.Error(e.message ?: "Une erreur est survenue lors de l'inscription.")
            }
        }
    }
}
