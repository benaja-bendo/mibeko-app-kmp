package com.mibeko.mibeko.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mibeko.mibeko.data.LawCodeSpec
import com.mibeko.mibeko.data.remote.ApiResponse
import com.mibeko.mibeko.data.repository.LocalLegalRepository
import com.mibeko.mibeko.data.preferences.UserPreferencesRepository
import com.mibeko.mibeko.util.NetworkConnectivityChecker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
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
    val isLoggedIn: Boolean = false,
    val recentItems: List<RecentItem> = emptyList(),
    val popularCodes: List<com.mibeko.mibeko.data.remote.RemoteDocument> = emptyList(),
    val recentlyAdded: List<com.mibeko.mibeko.data.remote.RemoteDocument> = emptyList(),
    val officialJournals: List<com.mibeko.mibeko.data.remote.RemoteOfficialJournal> = emptyList(),
    val aiSuggestions: List<String> = emptyList(),
    val isSyncing: Boolean = false,
    val error: String? = null
)

class HomeViewModel(
    private val repository: LocalLegalRepository,
    private val networkChecker: NetworkConnectivityChecker,
    private val userPreferences: UserPreferencesRepository
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
        initialSyncIfNeeded()
        loadHomeData()
    }
    
    private fun loadHomeData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            var validPopular: List<com.mibeko.mibeko.data.remote.RemoteDocument> = emptyList()
            var validRecentlyAdded: List<com.mibeko.mibeko.data.remote.RemoteDocument> = emptyList()
            var suggestions: List<String> = emptyList()
            var journals: List<com.mibeko.mibeko.data.remote.RemoteOfficialJournal> = emptyList()
            var isOffline = false

            if (networkChecker.isNetworkAvailable()) {
                // Fetch Home Data
                try {
                    val homeResponse = repository.getHomeData()
                    if (homeResponse.success && homeResponse.data != null) {
                        validPopular = homeResponse.data.popular_codes.filter { 
                            it.id.isNotBlank() && it.title.isNotBlank() 
                        }
                        validRecentlyAdded = homeResponse.data.recently_added.filter { 
                            it.id.isNotBlank() && it.title.isNotBlank() 
                        }
                        suggestions = homeResponse.data.ai_suggestions
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                // Fetch Official Journals
                try {
                    val journalsResponse = repository.getOfficialJournals()
                    if (journalsResponse.success) {
                        journals = journalsResponse.data
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            } else {
                isOffline = true
            }

            // Fallback for popular codes if we didn't get any from API
            if (validPopular.isEmpty()) {
                try {
                    val codes = repository.getLawCodes().first()
                    if (codes.isNotEmpty()) {
                        validPopular = codes.take(5).map { code ->
                            com.mibeko.mibeko.data.remote.RemoteDocument(
                                id = code.id,
                                title = code.title,
                                status = "published",
                                updated_at = ""
                            )
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            _uiState.value = _uiState.value.copy(
                popularCodes = validPopular,
                recentlyAdded = validRecentlyAdded,
                officialJournals = journals,
                aiSuggestions = suggestions,
                isOfflineMode = isOffline,
                isLoading = false
            )
        }
    }


    private fun loadInitialState() {
        // Check initial network state
        val isOnline = networkChecker.isNetworkAvailable()
        _uiState.value = _uiState.value.copy(
            isNetworkAvailable = isOnline,
            isLoggedIn = userPreferences.isLoggedIn(),
            isLoading = true
        )
        
        loadRecentItems()
    }

    /**
     * Sync catalog if no data exists locally.
     */
    private fun initialSyncIfNeeded() {
        viewModelScope.launch {
            val codes = repository.getLawCodes().first()
            if (codes.isEmpty() && networkChecker.isNetworkAvailable()) {
                syncData()
            } else {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }
    
    /**
     * Refresh network status.
     */
    fun refreshNetworkStatus() {
        val isOnline = networkChecker.isNetworkAvailable()
        _uiState.value = _uiState.value.copy(
            isNetworkAvailable = isOnline,
            isLoggedIn = userPreferences.isLoggedIn()
        )
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
