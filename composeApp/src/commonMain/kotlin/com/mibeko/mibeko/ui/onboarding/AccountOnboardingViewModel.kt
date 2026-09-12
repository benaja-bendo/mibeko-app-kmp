package com.mibeko.mibeko.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mibeko.mibeko.data.remote.LibraryApiService
import com.mibeko.mibeko.data.remote.LibraryTheme
import com.mibeko.mibeko.data.remote.OnboardingStep
import com.mibeko.mibeko.data.repository.OnboardingLoadResult
import com.mibeko.mibeko.data.repository.OnboardingRepository
import com.mibeko.mibeko.util.recordException
import com.mibeko.mibeko.util.NetworkConnectivityChecker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonElement

data class AccountOnboardingUiState(
    val loading: Boolean = true,
    val unavailable: Boolean = false,
    val offline: Boolean = false,
    val pendingMutations: Int = 0,
    val steps: List<OnboardingStep> = emptyList(),
    val currentIndex: Int = 0,
    val themes: List<LibraryTheme> = emptyList(),
    val replay: Boolean = false,
    val finished: Boolean = false,
    val message: String? = null
) {
    val currentStep: OnboardingStep? get() = steps.getOrNull(currentIndex)
}

class AccountOnboardingViewModel(
    private val repository: OnboardingRepository,
    private val libraryApi: LibraryApiService,
    private val connectivity: NetworkConnectivityChecker
) : ViewModel() {
    private val _uiState = MutableStateFlow(AccountOnboardingUiState())
    val uiState: StateFlow<AccountOnboardingUiState> = _uiState.asStateFlow()
    private var initialized = false

    init {
        viewModelScope.launch {
            connectivity.isOnline.collect { online ->
                if (initialized && online && _uiState.value.offline) {
                    apply(repository.load(), _uiState.value.replay)
                    loadThemesIfNeeded()
                }
            }
        }
    }

    fun initialize(replay: Boolean) {
        if (initialized) return
        initialized = true
        viewModelScope.launch {
            if (replay) repository.replay()
            apply(repository.load(), replay, startAtBeginning = replay)
            loadThemesIfNeeded()
        }
    }

    fun markViewed() {
        val step = _uiState.value.currentStep ?: return
        if (step.progress.viewed_at != null || _uiState.value.replay) return
        viewModelScope.launch { apply(repository.updateStep(step.key, "view"), replay = false) }
    }

    fun answer(value: JsonElement? = null, after: (() -> Unit)? = null) {
        val state = _uiState.value
        val step = state.currentStep ?: return
        viewModelScope.launch {
            // Le rejeu conserve les premières réussites. Comme la progression
            // serveur est terminale par étape, seule la dernière action guidée
            // referme la session de guide (même règle que le client web).
            if (state.replay && state.currentIndex < state.steps.lastIndex) {
                moveNextLocally()
            } else {
                apply(repository.updateStep(step.key, "answer", value), state.replay)
            }
            after?.invoke()
        }
    }

    fun skipStep() {
        val state = _uiState.value
        val step = state.currentStep ?: return
        viewModelScope.launch {
            if (state.replay && state.currentIndex < state.steps.lastIndex) moveNextLocally()
            else apply(repository.updateStep(step.key, "skip"), state.replay)
        }
    }

    fun postpone() {
        viewModelScope.launch {
            repository.postpone()
            _uiState.update { it.copy(finished = true) }
        }
    }

    fun retry() {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, unavailable = false, message = null) }
            apply(repository.load(), _uiState.value.replay)
            loadThemesIfNeeded()
        }
    }

    private suspend fun apply(result: OnboardingLoadResult, replay: Boolean, startAtBeginning: Boolean = false) {
        when (result) {
            OnboardingLoadResult.Unavailable -> _uiState.update {
                it.copy(loading = false, unavailable = true, replay = replay)
            }
            is OnboardingLoadResult.Ready -> {
                val data = result.data
                if (!data.available || data.journey == null || data.enrollment == null) {
                    _uiState.update { it.copy(loading = false, unavailable = true, replay = replay) }
                    return
                }

                // Un composant inconnu ne bloque jamais l'app : on le passe
                // explicitement afin que le serveur puisse terminer le parcours.
                val unsupported = data.journey.steps.filter { !it.supported && !it.progress.resolved }
                if (unsupported.isNotEmpty()) {
                    unsupported.forEach { repository.updateStep(it.key, "skip") }
                    apply(repository.load(), replay, startAtBeginning)
                    return
                }

                val steps = data.journey.steps.filter { it.supported }
                val completed = data.enrollment.status == "completed"
                val index = when {
                    startAtBeginning -> 0
                    else -> steps.indexOfFirst { !it.progress.resolved }.let { if (it < 0) steps.lastIndex.coerceAtLeast(0) else it }
                }
                _uiState.update {
                    it.copy(
                        loading = false,
                        unavailable = false,
                        offline = result.offline,
                        pendingMutations = result.pendingMutations,
                        steps = steps,
                        currentIndex = index,
                        replay = replay,
                        finished = completed && !replay
                    )
                }
            }
        }
    }

    private fun moveNextLocally() {
        _uiState.update { state ->
            if (state.currentIndex >= state.steps.lastIndex) state.copy(finished = true)
            else state.copy(currentIndex = state.currentIndex + 1)
        }
    }

    private suspend fun loadThemesIfNeeded() {
        if (_uiState.value.steps.none { it.config["source"]?.toString()?.contains("themes-de-vie") == true }) return
        try {
            _uiState.update { it.copy(themes = libraryApi.fetchThemes()) }
        } catch (e: Exception) {
            recordException(e, "AccountOnboardingViewModel.loadThemes")
            // Les intérêts sont facultatifs : leur catalogue indisponible ne
            // bloque ni le parcours ni l'accès au produit.
        }
    }
}
