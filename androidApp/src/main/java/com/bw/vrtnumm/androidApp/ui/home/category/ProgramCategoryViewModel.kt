package com.bw.vrtnumm.androidApp.ui.home.category

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bw.vrtnumm.androidApp.Graph
import com.bw.vrtnumm.shared.db.Program
import com.bw.vrtnumm.shared.repository.Repository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map

class ProgramCategoryViewModel(
    private val category: String,
    private val repo: Repository = Graph.repo
) : ViewModel() {
    private val _state = MutableStateFlow(ProgramCategoryViewState())

    val state: StateFlow<ProgramCategoryViewState>
        get() = _state

    init {
        viewModelScope.launch {
            repo.getProgramsForCategoryAsFlow(category).map { programs ->
                ProgramCategoryViewState(
                    programs
                )
            }.collect { _state.value = it }
        }
    }
}

data class ProgramCategoryViewState(
    val programs: List<Program> = emptyList()
)