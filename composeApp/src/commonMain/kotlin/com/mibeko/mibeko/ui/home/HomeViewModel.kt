package com.mibeko.mibeko.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mibeko.mibeko.data.LawCodeSpec
import com.mibeko.mibeko.data.repository.LocalLegalRepository
import com.mibeko.mibeko.util.NetworkConnectivityChecker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Represents a recently viewed item for the home screen.
 */
data class RecentItem(
    val id: String,
    val title: String
)

/**
 * UI State for the home screen.
 */
data class HomeUiState(
    val isLoading: Boolean = true,
    val isNetworkAvailable: Boolean = true,
    val isOfflineMode: Boolean = false,
    val downloadInProgress: DownloadProgress? = null,
    val fundamentalTexts: List<FundamentalText> = emptyList(),
    val lifeThemes: List<LifeTheme> = LifeThemes.all,
    val recentItems: List<RecentItem> = emptyList(),
    val isSyncing: Boolean = false,
    val error: String? = null
)

class HomeViewModel(
    private val repository: LocalLegalRepository,
    private val networkChecker: NetworkConnectivityChecker
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    val lawCodes: StateFlow<List<LawCodeSpec>> = repository.getLawCodes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Legacy accessors for backward compatibility
    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing
    
    private val _recentItems = MutableStateFlow<List<RecentItem>>(emptyList())
    val recentItems: StateFlow<List<RecentItem>> = _recentItems
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error
    
    init {
        loadInitialState()
        observeLawCodes()
        initialSyncIfNeeded()
    }
    
    private fun loadInitialState() {
        // Check initial network state
        val isOnline = networkChecker.isNetworkAvailable()
        _uiState.value = _uiState.value.copy(
            isNetworkAvailable = isOnline,
            lifeThemes = LifeThemes.all,
            isLoading = true
        )
        
        loadRecentItems()
    }

    /**
     * Sync catalog if no data exists locally.
     */
    private fun initialSyncIfNeeded() {
        viewModelScope.launch {
            repository.getLawCodes().collect { codes ->
                if (codes.isEmpty() && networkChecker.isNetworkAvailable()) {
                    syncData()
                } else {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                }
            }
        }
    }
    
    /**
     * Observe law codes and extract fundamental texts for the carousel.
     */
    private fun observeLawCodes() {
        viewModelScope.launch {
            repository.getLawCodes().collect { codes ->
                val fundamentals = extractFundamentalTexts(codes)
                _uiState.value = _uiState.value.copy(
                    fundamentalTexts = fundamentals
                )
            }
        }
    }
    
    /**
     * Extract fundamental legal texts from all available codes.
     * Prioritizes: Constitution, Code de la Famille, Code Pénal, Code du Travail
     */
    private fun extractFundamentalTexts(codes: List<LawCodeSpec>): List<FundamentalText> {
        val priorityTitles = listOf(
            "Constitution",
            "Code de la Famille",
            "Code Pénal",
            "Code du Travail",
            "Code Civil"
        )
        
        // First, find codes matching priority titles
        val priorityCodes = priorityTitles.mapNotNull { priority ->
            codes.find { code -> 
                code.title.contains(priority, ignoreCase = true) 
            }
        }
        
        // Then, add other codes up to a limit of 6
        val otherCodes = codes
            .filter { code -> priorityCodes.none { it.id == code.id } }
            .take(6 - priorityCodes.size)
        
        return (priorityCodes + otherCodes).map { code ->
            FundamentalText(
                id = code.id,
                title = code.title,
                shortTitle = getShortTitle(code.title),
                isDownloaded = code.isDownloaded,
                typeCode = if (code.title.contains("Constitution", ignoreCase = true)) 
                    "CONSTITUTION" else "CODE"
            )
        }
    }
    
    /**
     * Get a shortened title for display in cards.
     */
    private fun getShortTitle(title: String): String {
        return when {
            title.contains("Constitution", ignoreCase = true) -> "Constitution"
            title.contains("Famille", ignoreCase = true) -> "Famille"
            title.contains("Pénal", ignoreCase = true) -> "Pénal"  
            title.contains("Travail", ignoreCase = true) -> "Travail"
            title.contains("Civil", ignoreCase = true) -> "Civil"
            title.length > 15 -> title.take(15) + "..."
            else -> title
        }
    }
    
    /**
     * Refresh network status.
     */
    fun refreshNetworkStatus() {
        val isOnline = networkChecker.isNetworkAvailable()
        _uiState.value = _uiState.value.copy(isNetworkAvailable = isOnline)
    }
    
    /**
     * Update download progress (called from download manager).
     */
    fun updateDownloadProgress(progress: DownloadProgress?) {
        _uiState.value = _uiState.value.copy(downloadInProgress = progress)
    }
    
    private fun loadRecentItems() {
        // For now, this is a placeholder. In a real implementation,
        // this would query recent views from local storage.
        // The UI will show demo items if this is empty.
    }

    fun clearError() {
        _error.value = null
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun syncData() {
        viewModelScope.launch {
            _isSyncing.value = true
            _uiState.value = _uiState.value.copy(isSyncing = true, isLoading = true)
            _error.value = null
            try {
                repository.sync()
            } catch (e: Exception) {
                e.printStackTrace()
                val errorMsg = "Erreur de synchronisation: ${e.message ?: "Inconnue"}"
                _error.value = errorMsg
                _uiState.value = _uiState.value.copy(error = errorMsg)
            } finally {
                _isSyncing.value = false
                _uiState.value = _uiState.value.copy(isSyncing = false, isLoading = false)
            }
        }
    }
}
