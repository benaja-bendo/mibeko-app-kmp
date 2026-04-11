package com.mibeko.mibeko.ui.auth

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mibeko.mibeko.data.preferences.UserPreferencesRepository
import com.mibeko.mibeko.data.remote.AuthApiService
import com.mibeko.mibeko.data.remote.LoginRequest
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.GoogleAuthProvider
import dev.gitlive.firebase.auth.OAuthProvider
import dev.gitlive.firebase.auth.auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class LoginState {
    object Idle : LoginState()
    object Loading : LoginState()
    object Success : LoginState()
    data class Error(val message: String) : LoginState()
}

class LoginViewModel(
    private val authApiService: AuthApiService,
    private val userPreferences: UserPreferencesRepository
) : ViewModel() {

    private val _loginState = MutableStateFlow<LoginState>(LoginState.Idle)
    val loginState: StateFlow<LoginState> = _loginState.asStateFlow()

    var email by mutableStateOf("")
        private set

    var password by mutableStateOf("")
        private set

    fun onEmailChange(newEmail: String) {
        email = newEmail
    }

    fun onPasswordChange(newPassword: String) {
        password = newPassword
    }

    fun loginWithEmail() {
        if (email.isBlank() || password.isBlank()) {
            _loginState.value = LoginState.Error("Veuillez remplir tous les champs.")
            return
        }

        viewModelScope.launch {
            _loginState.value = LoginState.Loading
            try {
                val request = LoginRequest(email, password, "Mobile Device")
                val response = authApiService.loginWithEmail(request)
                
                if (response.success && response.data != null) {
                    userPreferences.setAuthToken(response.data.token)
                    if (response.data.user != null) {
                        userPreferences.setUserInfo(response.data.user.name, response.data.user.email)
                    } else {
                        userPreferences.setUserInfo(email, email) // Fallback
                    }
                    _loginState.value = LoginState.Success
                } else {
                    // Extract the first error message if available
                    val errorMessage = response.errors?.values?.firstOrNull()?.firstOrNull()
                        ?: response.message 
                        ?: "Une erreur est survenue lors de la connexion."
                    _loginState.value = LoginState.Error(errorMessage)
                }
            } catch (e: Exception) {
                _loginState.value = LoginState.Error(e.message ?: "Une erreur est survenue lors de la connexion.")
            }
        }
    }

    fun loginWithGoogle() {
        viewModelScope.launch {
            _loginState.value = LoginState.Error("La connexion avec Google n'est pas encore disponible dans cette version.")
            /*
            _loginState.value = LoginState.Loading
            try {
                // In a real scenario, you'd trigger the native Google sign-in flow here
                // For KMP with gitlive firebase, this typically involves using the platform-specific Google Sign-In SDK
                // and then signing in with the credential.
                
                val placeholderToken = "PLACEHOLDER_TOKEN"
                val response = authApiService.loginWithFirebase(placeholderToken, "Android Device")
                
                userPreferences.setAuthToken(response.data.token)
                if (response.data.user != null) {
                    userPreferences.setUserInfo(response.data.user.name, response.data.user.email)
                }
                
                _loginState.value = LoginState.Success
            } catch (e: Exception) {
                _loginState.value = LoginState.Error(e.message ?: "Une erreur est survenue lors de la connexion.")
            }
            */
        }
    }

    fun loginWithApple() {
        viewModelScope.launch {
            _loginState.value = LoginState.Error("La connexion avec Apple n'est pas encore disponible dans cette version.")
            /*
            _loginState.value = LoginState.Loading
            try {
                // Similar to Google, but with OAuthProvider("apple.com")
                val placeholderToken = "PLACEHOLDER_TOKEN"
                val response = authApiService.loginWithFirebase(placeholderToken, "iOS Device")
                
                userPreferences.setAuthToken(response.data.token)
                if (response.data.user != null) {
                    userPreferences.setUserInfo(response.data.user.name, response.data.user.email)
                }
                
                _loginState.value = LoginState.Success
            } catch (e: Exception) {
                _loginState.value = LoginState.Error(e.message ?: "Une erreur est survenue lors de la connexion.")
            }
            */
        }
    }
}
