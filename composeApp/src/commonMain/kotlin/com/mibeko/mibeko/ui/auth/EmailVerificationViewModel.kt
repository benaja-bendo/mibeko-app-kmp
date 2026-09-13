package com.mibeko.mibeko.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mibeko.mibeko.data.preferences.UserPreferencesRepository
import com.mibeko.mibeko.data.remote.AuthApiService
import com.mibeko.mibeko.data.remote.ResendVerificationResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class EmailVerificationUiState(
    val checking: Boolean = false,
    val resending: Boolean = false,
    val verified: Boolean = false,
    val loggedOut: Boolean = false,
    val message: String? = null
)

class EmailVerificationViewModel(
    private val authApiService: AuthApiService,
    private val preferences: UserPreferencesRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(EmailVerificationUiState())
    val uiState: StateFlow<EmailVerificationUiState> = _uiState.asStateFlow()

    fun checkVerification() {
        if (_uiState.value.checking) return
        viewModelScope.launch {
            _uiState.update { it.copy(checking = true, message = null) }
            try {
                val response = authApiService.getProfile()
                val verified = response.success && response.data?.let {
                    it.email_verified == true || it.email_verified_at != null
                } == true
                val required = response.data?.email_verification_required ?: true
                val accessGranted = !required || verified
                preferences.setEmailVerificationState(required, verified)
                _uiState.update {
                    it.copy(
                        checking = false,
                        verified = accessGranted,
                        message = if (accessGranted) null else "L’adresse n’est pas encore vérifiée."
                    )
                }
            } catch (_: Exception) {
                _uiState.update {
                    it.copy(checking = false, message = "Vérification impossible. Contrôlez votre connexion puis réessayez.")
                }
            }
        }
    }

    fun resend() {
        if (_uiState.value.resending) return
        viewModelScope.launch {
            _uiState.update { it.copy(resending = true, message = null) }
            val message = when (authApiService.resendEmailVerification()) {
                ResendVerificationResult.SENT -> "E-mail renvoyé. Pensez à vérifier vos spams."
                ResendVerificationResult.THROTTLED -> "Trop de tentatives. Réessayez dans quelques minutes."
                ResendVerificationResult.ERROR -> "L’envoi a échoué. Vérifiez votre connexion."
            }
            _uiState.update { it.copy(resending = false, message = message) }
        }
    }

    fun logout() {
        viewModelScope.launch {
            runCatching { authApiService.logout() }
            preferences.logout()
            authApiService.invalidateTokenCache()
            _uiState.update { it.copy(loggedOut = true) }
        }
    }
}
