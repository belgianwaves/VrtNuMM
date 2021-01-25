package com.bw.vrtnumm.androidApp.ui

import android.os.Handler
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bw.vrtnumm.androidApp.FirebaseSync
import com.bw.vrtnumm.androidApp.Graph
import com.bw.vrtnumm.androidApp.PlayerController
import com.bw.vrtnumm.shared.Api
import com.bw.vrtnumm.shared.db.Episode
import com.bw.vrtnumm.shared.db.Program
import com.bw.vrtnumm.shared.repository.Repository
import com.bw.vrtnumm.shared.transport.Channel
import com.bw.vrtnumm.shared.transport.EpgEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.ExperimentalTime

class MainViewModel(
    private val controller: PlayerController,
    private val repo: Repository = Graph.repo,
    private val api: Api = Graph.api,
    private val firebase: FirebaseSync? = Graph.firebase,
    initialOrientation: Int
): ViewModel() {

    private val selectedProgram = MutableStateFlow<Program?>(null)
    private val selectedEpisode = MutableStateFlow<Episode?>(null)
    private val selectedChannel = MutableStateFlow<Channel?>(null)

    private val orientation = MutableStateFlow<Int>(initialOrientation)

    private val handler = Handler()

    private val saveCurrentPositionAction = kotlinx.coroutines.Runnable {
        saveCurrentPosition()
    }

    private val _state = MutableStateFlow(MainViewState(orientation = initialOrientation))

    val state: StateFlow<MainViewState>
        get() = _state

    init {
        viewModelScope.launch {
            combine(
                selectedProgram,
                selectedEpisode,
                selectedChannel,
                orientation
            ) { selectedProgram, selectedEpisode, selectedChannel, orientation ->
                MainViewState(
                    program = selectedProgram,
                    episode = selectedEpisode,
                    channel = selectedChannel,
                    orientation = orientation
                )
            }.catch { throwable ->
                throw throwable
            }.collect {
                _state.value = it
            }
        }
    }

    val hasProgram: Boolean
        get() = (selectedProgram.value != null)
    val hasChannel: Boolean
        get() = (selectedChannel.value != null)

    fun moveBack() {
        if (selectedProgram.value != null) {
            selectedProgram.value = null
        }
        if (selectedChannel.value != null) {
            selectedChannel.value = null
        }
    }

    fun onProgramSelected(program: Program?) {
        selectedProgram.value = program
    }

    @ExperimentalTime
    fun onEpisodeSelected(episode: Episode) {
        savePositionToFirebase()

        selectedChannel.value = null
        selectedEpisode.value = episode

        viewModelScope.launch {
            handler.removeCallbacks(saveCurrentPositionAction)

            val playerInfo = withContext(Dispatchers.IO) {
                var res = api.fetchInfo(episode.publicationId, episode.videoId, false, false)
                    if (res == null) {
                        res = api.fetchInfo(episode.publicationId, episode.videoId, true, false)
                    }
                res
            }?.apply { position = repo.getPosition(episode.programUrl, episode.publicationId, episode.videoId)?.position ?: 0}

            playerInfo?.apply {
                controller.prepare(this)
            }

            saveCurrentPosition()
        }
    }

    @ExperimentalTime
    fun onChannelSelected(channel: Channel) {
        savePositionToFirebase()

        selectedEpisode.value = null
        selectedChannel.value = channel

        startLive(channel)
    }

    @ExperimentalTime
    fun onEpgEntrySelected(epg: EpgEntry) {
        if (epg.isLive) {
            startLive(selectedChannel.value!!)
            return
        }

        viewModelScope.launch {
            repo.getEpisode(epg)?.apply {
                onEpisodeSelected(this)
            }
        }
    }

    @ExperimentalTime
    private fun startLive(channel: Channel) {
        viewModelScope.launch {
            handler.removeCallbacks(saveCurrentPositionAction)

            val playerInfo = withContext(Dispatchers.IO) {
                var res = api.fetchInfo(channel.streamId, false, false)
                    if (res == null) {
                        res = api.fetchInfo(channel.streamId, true, false)
                    }
                res
            }

            playerInfo?.apply {
                controller.prepare(this)
            }
        }
    }

    fun savePositionToFirebase() {
        val selected = selectedEpisode.value
        selected?.apply {
            repo.getPosition(programUrl, publicationId, videoId)?.apply {
                firebase?.storePosition(this)
            }
        }
    }

    fun onOrientationChanged(orientation: Int) {
        this.orientation.value = orientation
    }

    private fun saveCurrentPosition() {
        controller.player.run {
            val pos = currentPosition
            val dur = duration
            if (pos > 30*1000) {
                val episode = selectedEpisode.value
                episode?.apply {
                    if (repo.getPosition(programUrl, publicationId, videoId) == null) {
                        repo.insertPosition(programUrl, title, publicationId, videoId)
                    }
                    repo.updatePosition(programUrl, publicationId, videoId, pos, dur)
                }
            }
        }

        handler.removeCallbacks(saveCurrentPositionAction)
        handler.postDelayed(saveCurrentPositionAction, 1000)
    }

    fun getProgram(url: String): Program? {
        var result = repo.getProgram(url)
            if ((result == null) && url.startsWith("https:")) {
                result = repo.getProgram(url.substring(6))
            }
        return result
    }

    fun getChannel(url: String): Channel? {
        return repo.getChannel(url)
    }

    fun dispose() {
        handler.removeCallbacks(saveCurrentPositionAction)
    }
}

data class MainViewState(
    val program: Program? = null,
    val episode: Episode? = null,
    val channel: Channel? = null,
    val orientation: Int
)