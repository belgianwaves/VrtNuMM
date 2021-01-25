package com.bw.vrtnumm.androidApp.ui.home.discover

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bw.vrtnumm.androidApp.Graph
import com.bw.vrtnumm.shared.repository.Repository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class DiscoverViewModel(private val repo: Repository = Graph.repo) :ViewModel() {
    private val _selectedCategory = MutableStateFlow<String?>(null)

    private val _state = MutableStateFlow(DiscoverViewState())

    val state: StateFlow<DiscoverViewState>
        get() = _state

    init {
        viewModelScope.launch {
            combine(
                repo.getCategoryNamesAsFlow()
                    .onEach { categories ->
                        if (categories.isNotEmpty() && _selectedCategory.value == null) {
                            _selectedCategory.value = categories.first()
                        }
                    },
                _selectedCategory
            ) { categories, selectedCategory ->
                DiscoverViewState(
                    categories = categories,
                    selectedCategory = selectedCategory
                )
            }.collect { _state.value = it }
        }
    }

    fun onCategorySelected(category: String) {
        _selectedCategory.value = category
    }
}

data class DiscoverViewState(
    val categories: List<String> = emptyList(),
    val selectedCategory: String? = null
)