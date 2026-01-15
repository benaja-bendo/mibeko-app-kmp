package com.mibeko.mibeko.ui.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mibeko.mibeko.data.local.entities.ArticleEntity
import com.mibeko.mibeko.data.local.entities.NodeEntity
import com.mibeko.mibeko.data.repository.LocalLegalRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

import com.mibeko.mibeko.data.LawCodeSpec
import kotlinx.coroutines.flow.asStateFlow

class DocumentDetailViewModel(private val repository: LocalLegalRepository) : ViewModel() {

    private val _structure = MutableStateFlow<Map<NodeEntity, List<ArticleEntity>>>(emptyMap())
    val structure: StateFlow<Map<NodeEntity, List<ArticleEntity>>> = _structure

    private val _document = MutableStateFlow<LawCodeSpec?>(null)
    val document: StateFlow<LawCodeSpec?> = _document.asStateFlow()

    fun loadStructure(documentId: String) {
        viewModelScope.launch {
            repository.getStructure(documentId).collect {
                _structure.value = it
            }
        }
        
        viewModelScope.launch {
            repository.getLawCodes().collect { codes ->
                _document.value = codes.find { it.id == documentId }
            }
        }
    }
}
