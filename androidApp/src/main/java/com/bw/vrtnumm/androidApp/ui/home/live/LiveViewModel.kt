package com.bw.vrtnumm.androidApp.ui.home.live

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bw.vrtnumm.androidApp.Graph
import com.bw.vrtnumm.androidApp.ProgramReminder
import com.bw.vrtnumm.shared.Api
import com.bw.vrtnumm.shared.repository.Repository
import com.bw.vrtnumm.shared.transport.Channel
import com.bw.vrtnumm.shared.transport.EpgEntry
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class LiveViewModel(
    private val repo: Repository = Graph.repo,
    private val api: Api = Graph.api): ViewModel() {

    private val _state = MutableStateFlow(LiveViewState())

    val state: StateFlow<LiveViewState>
        get() = _state

    init {
        viewModelScope.launch {
            while (isActive) {
                val epg = withContext(Dispatchers.IO) { api.fetchEpg() }
                val channels = repo.allChannels()
                channels.forEach { c ->
                    c.epg = if (epg != null) epg[c.id] else null
                }
                _state.value = LiveViewState(channels)
                delay(60*1000)
            }
        }
    }

    fun scheduleAlarm(context: Context, channel: Channel, epg: EpgEntry) {
        val intent = Intent(context, ProgramReminder::class.java).apply {
            putExtra("title", epg.title)
            putExtra("streamId", channel.streamId)
            putExtra("imageUrl", epg.image)
        }
        val pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)

        val alarmManager: AlarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.setExact(AlarmManager.RTC_WAKEUP, epg.millisBefore(60*1000), pendingIntent)
    }
}

data class LiveViewState(
    val channels: List<Channel> = emptyList(),
    val timestamp: Long = System.currentTimeMillis()
)
