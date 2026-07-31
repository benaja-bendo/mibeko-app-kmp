package com.mibeko.mibeko.ui.contact

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mibeko.mibeko.data.preferences.UserPreferencesRepository
import com.mibeko.mibeko.data.remote.LegalApiService
import com.mibeko.mibeko.util.AnalyticsEvents
import com.mibeko.mibeko.util.MibekoAnalytics
import com.mibeko.mibeko.util.recordException
import io.ktor.client.plugins.ResponseException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ContactUiState(
    val name: String = "",
    val email: String = "",
    val message: String = "",
    val isSending: Boolean = false,
    val isSent: Boolean = false,
    val snackbar: String? = null
) {
    /** Miroir de la validation serveur (name+email requis, message ≥ 10). */
    val canSubmit: Boolean
        get() = name.isNotBlank() && email.contains("@") && message.trim().length >= 10
}

class ContactViewModel(
    private val legalApiService: LegalApiService,
    userPreferencesRepository: UserPreferencesRepository,
    private val analytics: MibekoAnalytics
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        ContactUiState(
            name = userPreferencesRepository.getUserName() ?: "",
            email = userPreferencesRepository.getUserEmail() ?: ""
        )
    )
    val uiState: StateFlow<ContactUiState> = _uiState.asStateFlow()

    fun onNameChange(value: String) {
        _uiState.value = _uiState.value.copy(name = value)
    }

    fun onEmailChange(value: String) {
        _uiState.value = _uiState.value.copy(email = value)
    }

    fun onMessageChange(value: String) {
        _uiState.value = _uiState.value.copy(message = value)
    }

    fun clearSnackbar() {
        _uiState.value = _uiState.value.copy(snackbar = null)
    }

    fun submit() {
        val state = _uiState.value
        // `isSent` reste dans le garde même si l'écran de confirmation remplace
        // le formulaire après succès : sans lui, tout chemin résiduel vers
        // submit() créerait un doublon de ticket support.
        if (!state.canSubmit || state.isSending || state.isSent) return
        _uiState.value = state.copy(isSending = true)
        viewModelScope.launch {
            try {
                val ok = legalApiService.sendContactMessage(
                    name = state.name.trim(),
                    email = state.email.trim(),
                    message = state.message.trim()
                )
                if (ok) {
                    analytics.logEvent(AnalyticsEvents.CONTACT_SUBMITTED)
                    _uiState.value = _uiState.value.copy(isSending = false, isSent = true)
                } else {
                    _uiState.value = _uiState.value.copy(
                        isSending = false,
                        snackbar = "L'envoi a échoué. Vérifiez vos informations et réessayez."
                    )
                }
            } catch (e: ResponseException) {
                val label = if (e.response.status.value == 429) {
                    "Trop de messages envoyés. Réessayez dans quelques minutes."
                } else {
                    "L'envoi a échoué. Vérifiez vos informations et réessayez."
                }
                _uiState.value = _uiState.value.copy(isSending = false, snackbar = label)
            } catch (e: Exception) {
                recordException(e, "ContactViewModel.submit")
                _uiState.value = _uiState.value.copy(
                    isSending = false,
                    snackbar = "Connexion impossible. Vérifiez votre accès internet."
                )
            }
        }
    }
}
