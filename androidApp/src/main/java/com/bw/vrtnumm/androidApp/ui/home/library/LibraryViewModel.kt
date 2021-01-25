package com.bw.vrtnumm.androidApp.ui.home.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bw.vrtnumm.androidApp.Graph
import com.bw.vrtnumm.shared.Api
import com.bw.vrtnumm.shared.db.Program
import com.bw.vrtnumm.shared.repository.Repository
import com.bw.vrtnumm.shared.transport.PlayerInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.ExperimentalTime

class LibraryViewModel(
    private val api: Api = Graph.api,
    private val repo: Repository = Graph.repo
) : ViewModel() {
    private val _state = MutableStateFlow(LibraryViewState())

    val state: StateFlow<LibraryViewState>
        get() = _state

    init {
        viewModelScope.launch {
            combine(
                repo.getWatchedProgramsAsFlow(),
                repo.getFavouriteProgramsAsFlow(),
                repo.getRecentProgramsAsFlow()
            ) {
                watched, favourites, recent -> LibraryViewState(watched, favourites, recent)
            }.collect { _state.value = it }
        }
    }

    @ExperimentalTime
    fun getPlayerInfo(program: Program, callback: (PlayerInfo?) -> Unit) {
        viewModelScope.launch {
            if (repo.getEpisodesForProgram(program.programUrl).isEmpty()) {
                withContext(Dispatchers.IO) {
                    repo.fetchEpisodes(program.programUrl)
                }
            }
            val episode = repo.getEpisodesForProgram(program.programUrl).firstOrNull()
            if (episode != null) callback(withContext(Dispatchers.IO) { api.fetchInfo(episode.publicationId, episode.videoId) })
        }
    }

    fun toggleFavourite(program: Program) {
        repo.setProgramFavourite(program.programUrl, !program.favourite)
        Graph.firebase?.storeFavourite(program, !program.favourite)
    }
}

data class LibraryViewState(
    val watched: List<Program> = emptyList(),
    val favourites: List<Program> = emptyList(),
    val recent: List<Program> = emptyList()
)