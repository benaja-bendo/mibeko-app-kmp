package com.mibeko.mibeko.ui.auth

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mibeko.mibeko.data.preferences.UserPreferencesRepository
import com.mibeko.mibeko.data.remote.AuthApiService
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

    fun loginWithGoogle() {
        viewModelScope.launch {
            _loginState.value = LoginState.Loading
            try {
                // In a real scenario, you'd trigger the native Google sign-in flow here
                // For KMP with gitlive firebase, this typically involves using the platform-specific Google Sign-In SDK
                // and then signing in with the credential.
                // For now, I'll use a placeholder for the native flow and exchange the token.
                
                // Example of what it would look like if we had the native credential:
                // val result = Firebase.auth.signInWithCredential(GoogleAuthProvider.credential(idToken, accessToken))
                // val idToken = result.user?.getIdToken(true)
                
                // Placeholder ID token for demonstration (requires native integration)
                val placeholderToken = "PLACEHOLDER_TOKEN"
                
                val response = authApiService.loginWithFirebase(placeholderToken, "Android Device")
                
                userPreferences.setAuthToken(response.token)
                userPreferences.setUserInfo(response.user.name, response.user.email)
                
                _loginState.value = LoginState.Success
            } catch (e: Exception) {
                _loginState.value = LoginState.Error(e.message ?: "Une erreur est survenue lors de la connexion.")
            }
        }
    }

    fun loginWithApple() {
        viewModelScope.launch {
            _loginState.value = LoginState.Loading
            try {
                // Similar to Google, but with OAuthProvider("apple.com")
                val placeholderToken = "PLACEHOLDER_TOKEN"
                val response = authApiService.loginWithFirebase(placeholderToken, "iOS Device")
                
                userPreferences.setAuthToken(response.token)
                userPreferences.setUserInfo(response.user.name, response.user.email)
                
                _loginState.value = LoginState.Success
            } catch (e: Exception) {
                _loginState.value = LoginState.Error(e.message ?: "Une erreur est survenue lors de la connexion.")
            }
        }
    }
}
