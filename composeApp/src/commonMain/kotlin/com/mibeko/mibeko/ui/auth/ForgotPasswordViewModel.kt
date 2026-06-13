package com.mibeko.mibeko.ui.auth

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mibeko.mibeko.data.remote.AuthApiService
import com.mibeko.mibeko.data.remote.ResetPasswordRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Réinitialisation de mot de passe en deux étapes :
 * 1. saisie de l'email → l'API envoie un code OTP à 6 chiffres ;
 * 2. saisie du code + nouveau mot de passe → réinitialisation.
 */
sealed class ForgotPasswordState {
    object EnterEmail : ForgotPasswordState()
    object SendingCode : ForgotPasswordState()
    data class EnterCode(val error: String? = null) : ForgotPasswordState()
    object Resetting : ForgotPasswordState()
    object Done : ForgotPasswordState()
    data class Error(val message: String) : ForgotPasswordState()
}

class ForgotPasswordViewModel(
    private val authApiService: AuthApiService
) : ViewModel() {

    private val _state = MutableStateFlow<ForgotPasswordState>(ForgotPasswordState.EnterEmail)
    val state: StateFlow<ForgotPasswordState> = _state.asStateFlow()

    var email by mutableStateOf("")
        private set
    var code by mutableStateOf("")
        private set
    var password by mutableStateOf("")
        private set
    var passwordConfirmation by mutableStateOf("")
        private set

    fun onEmailChange(value: String) {
        email = value
    }

    fun onCodeChange(value: String) {
        if (value.length <= 6 && value.all { it.isDigit() }) {
            code = value
        }
    }

    fun onPasswordChange(value: String) {
        password = value
    }

    fun onPasswordConfirmationChange(value: String) {
        passwordConfirmation = value
    }

    fun sendCode() {
        if (email.isBlank()) {
            _state.value = ForgotPasswordState.Error("Veuillez saisir votre adresse email.")
            return
        }

        viewModelScope.launch {
            _state.value = ForgotPasswordState.SendingCode
            try {
                authApiService.forgotPassword(email.trim())
                _state.value = ForgotPasswordState.EnterCode()
            } catch (e: Exception) {
                _state.value = ForgotPasswordState.Error(
                    e.message ?: "Impossible d'envoyer le code. Vérifiez votre connexion."
                )
            }
        }
    }

    fun resetPassword() {
        if (code.length != 6) {
            _state.value = ForgotPasswordState.EnterCode("Le code comporte 6 chiffres.")
            return
        }
        if (password.length < 8) {
            _state.value = ForgotPasswordState.EnterCode("Le mot de passe doit contenir au moins 8 caractères.")
            return
        }
        if (password != passwordConfirmation) {
            _state.value = ForgotPasswordState.EnterCode("Les mots de passe ne correspondent pas.")
            return
        }

        viewModelScope.launch {
            _state.value = ForgotPasswordState.Resetting
            try {
                val response = authApiService.resetPassword(
                    ResetPasswordRequest(
                        email = email.trim(),
                        code = code,
                        password = password,
                        password_confirmation = passwordConfirmation
                    )
                )
                if (response.success) {
                    _state.value = ForgotPasswordState.Done
                } else {
                    val message = response.errors?.values?.firstOrNull()?.firstOrNull()
                        ?: response.message
                        ?: "Le code est invalide ou a expiré."
                    _state.value = ForgotPasswordState.EnterCode(message)
                }
            } catch (e: Exception) {
                _state.value = ForgotPasswordState.EnterCode(
                    e.message ?: "Une erreur est survenue. Réessayez."
                )
            }
        }
    }

    /** Retour de l'étape code vers la saisie d'email (renvoyer un code). */
    fun backToEmail() {
        code = ""
        password = ""
        passwordConfirmation = ""
        _state.value = ForgotPasswordState.EnterEmail
    }
}
