package com.bw.vrtnumm.androidApp.ui.home.program

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bw.vrtnumm.androidApp.Graph
import com.bw.vrtnumm.shared.db.Episode
import com.bw.vrtnumm.shared.db.Program
import com.bw.vrtnumm.shared.repository.Repository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ProgramViewModel(
    private val programUrl: String,
    private val repo: Repository = Graph.repo
) : ViewModel() {

    private val _selectedEpisode = MutableStateFlow<Episode?>(null)
    private val _selectedSeason = MutableStateFlow<String?>(null)

    private val _state = MutableStateFlow(ProgramViewState())

    val state: StateFlow<ProgramViewState>
        get() = _state

    init {
        val program = repo.getProgram(programUrl)
        program?.run {
            viewModelScope.launch {
                withContext(Dispatchers.IO) { repo.fetchEpisodes(programUrl) }

                val lastSeen = getLastSeenEpisode(programUrl)

                combine(
                    repo.getProgramAsFlow(programUrl),
                    repo.getEpisodesForProgramAsFlow(programUrl).onEach {
                            if (it.isNotEmpty() && (_selectedEpisode.value == null)) {
                                val episode = lastSeen ?: it.first()
                                _selectedEpisode.value = episode
                                _selectedSeason.value = checkSeason(episode.seasonName)
                            }
                        },
                    _selectedEpisode,
                    _selectedSeason) {
                        program, episodes, selectedEpisode, selectedSeason -> ProgramViewState(
                            program = program,
                            episodes = episodes.filter { episode ->  checkSeason(episode.seasonName) == selectedSeason},
                            selectedEpisode = selectedEpisode,
                            seasons = episodes.map { episode ->  checkSeason(episode.seasonName)}.distinct().toSet(),
                            selectedSeason = selectedSeason)
                    }.collect {
                        _state.value = it
                    }
            }
        }
    }

    fun onEpisodeSelected(episode: Episode) {
        _selectedEpisode.value = episode
    }

    fun onSeasonSelected(season: String) {
        _selectedSeason.value = season
    }

    fun toggleFavourite(program: Program) {
        repo.setProgramFavourite(program.programUrl, !program.favourite)
        Graph.firebase?.storeFavourite(program, !program.favourite)
    }

    private fun checkSeason(seasonNumber: String): String {
        val intValue = seasonNumber.toIntOrNull()
        return if (intValue != null) "season $intValue" else seasonNumber
    }

    private fun getLastSeenEpisode(programUrl: String): Episode? {
        return repo.getWatchedEpisodesForProgram(programUrl).firstOrNull()
    }
}

data class ProgramViewState (
    val program: Program? = null,
    val episodes: List<Episode> = emptyList(),
    val selectedEpisode: Episode? = null,
    val seasons: Set<String> = emptySet(),
    val selectedSeason: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)