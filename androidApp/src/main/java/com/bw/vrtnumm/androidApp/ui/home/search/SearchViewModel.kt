package com.bw.vrtnumm.androidApp.ui.home.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bw.vrtnumm.androidApp.Graph
import com.bw.vrtnumm.shared.Api
import com.bw.vrtnumm.shared.db.Program
import com.bw.vrtnumm.shared.repository.Repository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class SearchViewModel(
    private val api: Api = Graph.api,
    private val repo: Repository = Graph.repo
) : ViewModel() {
    private val _query = MutableStateFlow<String>("")

    private val _state = MutableStateFlow(SearchViewState())

    val state: StateFlow<SearchViewState>
        get() = _state

    init {
        viewModelScope.launch {
            combine(_query) {
                query -> SearchViewState(repo.getMatchingPrograms(query[0]))
            }.collect { _state.value = it }
        }
    }

    fun onQueryChanged(query: String) {
        _query.value = query
    }
}

data class SearchViewState(
    val programs: List<Program> = emptyList()
)