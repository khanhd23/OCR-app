package com.example.ocr.presentation.feature.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ocr.domain.model.OCRDocument
import com.example.ocr.domain.usecase.DeleteDocumentUseCase
import com.example.ocr.domain.usecase.GetAllDocumentsUseCase
import com.example.ocr.domain.usecase.SearchDocumentsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val getAllDocumentsUseCase: GetAllDocumentsUseCase,
    private val deleteDocumentUseCase: DeleteDocumentUseCase,
    private val searchDocumentsUseCase: SearchDocumentsUseCase
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _allDocuments = MutableStateFlow<List<OCRDocument>>(emptyList())
    private val _searchResults = MutableStateFlow<List<OCRDocument>>(emptyList())
    private val _isSearching = MutableStateFlow(false)

    val documents: StateFlow<List<OCRDocument>> = combine(_searchQuery, _allDocuments, _searchResults) { query, all, search ->
        if (query.isBlank()) all else search
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    init {
        viewModelScope.launch {
            getAllDocumentsUseCase().collect { _allDocuments.value = it }
        }
        observeSearch()
    }

    @OptIn(FlowPreview::class)
    private fun observeSearch() {
        viewModelScope.launch {
            _searchQuery
                .debounce(300)
                .collect { query ->
                    if (query.isNotBlank()) {
                        _isSearching.value = true
                        _searchResults.value = searchDocumentsUseCase(query)
                        _isSearching.value = false
                    }
                }
        }
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun deleteDocument(id: Long) {
        viewModelScope.launch { deleteDocumentUseCase(id) }
    }
}
