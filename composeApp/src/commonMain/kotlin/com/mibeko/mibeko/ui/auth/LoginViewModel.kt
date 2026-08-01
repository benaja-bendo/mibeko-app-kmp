package com.mibeko.mibeko.ui.auth

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mibeko.mibeko.data.preferences.UserPreferencesRepository
import com.mibeko.mibeko.data.remote.AuthApiService
import com.mibeko.mibeko.data.remote.LoginRequest
import com.mibeko.mibeko.data.remote.TwoFactorRequiredException
import com.mibeko.mibeko.data.repository.PushTokenRegistrar
import com.mibeko.mibeko.util.AnalyticsEvents
import com.mibeko.mibeko.util.MibekoAnalytics
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class LoginState {
    object Idle : LoginState()
    object Loading : LoginState()

    /** Le compte est protégé : afficher l'étape de saisie du code TOTP. */
    data class TwoFactorRequired(val error: String? = null) : LoginState()
    data class Success(val requiresProfileSetup: Boolean) : LoginState()
    data class Error(val message: String) : LoginState()
}

class LoginViewModel(
    private val authApiService: AuthApiService,
    private val userPreferences: UserPreferencesRepository,
    private val pushTokenRegistrar: PushTokenRegistrar,
    private val analytics: MibekoAnalytics
) : ViewModel() {

    private val _loginState = MutableStateFlow<LoginState>(LoginState.Idle)
    val loginState: StateFlow<LoginState> = _loginState.asStateFlow()

    var email by mutableStateOf("")
        private set

    var password by mutableStateOf("")
        private set

    var twoFactorCode by mutableStateOf("")
        private set

    fun onEmailChange(newEmail: String) {
        email = newEmail
    }

    fun onPasswordChange(newPassword: String) {
        password = newPassword
    }

    fun onTwoFactorCodeChange(newCode: String) {
        if (newCode.length <= 6 && newCode.all { it.isDigit() }) {
            twoFactorCode = newCode
        }
    }

    /** Retour de l'étape 2FA vers le formulaire email / mot de passe. */
    fun cancelTwoFactor() {
        twoFactorCode = ""
        _loginState.value = LoginState.Idle
    }

    fun loginWithEmail() {
        if (email.isBlank() || password.isBlank()) {
            _loginState.value = LoginState.Error("Veuillez remplir tous les champs.")
            return
        }

        viewModelScope.launch {
            _loginState.value = LoginState.Loading
            try {
                val request = LoginRequest(
                    email = email,
                    password = password,
                    device_name = "Mobile Device",
                    code = twoFactorCode.ifBlank { null }
                )
                val response = authApiService.loginWithEmail(request)

                if (response.success && response.data != null) {
                    userPreferences.setAuthToken(response.data.token)
                    authApiService.invalidateTokenCache()
                    // Le choix Citoyen/Professionnel (ProfileSetupScreen) pose ce
                    // seul champ : ne plus exiger téléphone/entreprise, sinon
                    // l'écran se redéclenche à chaque connexion pour qui n'a que
                    // renseigné son type.
                    val profileComplete = !response.data.user?.mobile_profile?.profession.isNullOrBlank()
                    userPreferences.setProfileSetupCompleted(profileComplete)
                    if (response.data.user != null) {
                        userPreferences.setUserInfo(response.data.user.name, response.data.user.email)
                    } else {
                        userPreferences.setUserInfo(email, email) // Fallback
                    }
                    // Un token push a pu être renouvelé hors session : l'envoyer maintenant.
                    pushTokenRegistrar.flushPendingToken()
                    analytics.logEvent(AnalyticsEvents.LOGIN_COMPLETED, mapOf("method" to "email"))
                    _loginState.value = LoginState.Success(requiresProfileSetup = !profileComplete)
                } else {
                    val errorMessage = response.errors?.values?.firstOrNull()?.firstOrNull()
                        ?: response.message
                        ?: "Une erreur est survenue lors de la connexion."
                    // Un code TOTP invalide ramène à l'étape 2FA, pas au formulaire.
                    _loginState.value = if (response.errors?.containsKey("code") == true) {
                        LoginState.TwoFactorRequired(errorMessage)
                    } else {
                        LoginState.Error(errorMessage)
                    }
                }
            } catch (e: TwoFactorRequiredException) {
                twoFactorCode = ""
                _loginState.value = LoginState.TwoFactorRequired()
            } catch (e: Exception) {
                _loginState.value = LoginState.Error(e.message ?: "Une erreur est survenue lors de la connexion.")
            }
        }
    }
}
