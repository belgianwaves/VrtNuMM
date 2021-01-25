package com.bw.vrtnumm.androidApp.ui.home.live

import android.widget.Toast
import androidx.compose.animation.animate
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AlarmAdd
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.AmbientContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.viewModel
import com.bw.vrtnumm.androidApp.utils.VideoThumbnail
import com.bw.vrtnumm.shared.transport.EpgEntry
import kotlinx.coroutines.launch

@Composable
fun Channel(
    modifier: Modifier,
    streamId: String,
    onEpgEntry: (EpgEntry) -> Unit,
    onBackPressed: () -> Unit) {

    val viewModel: LiveViewModel = viewModel()
    val viewState by viewModel.state.collectAsState()

    val channel = viewState.channels.first { channel ->  channel.streamId == streamId}
    if (channel.epg == null) return

    val epg = channel.epg!!

    var userSelectedEpg by remember(streamId) { mutableStateOf<EpgEntry?>(null) }

    val selectedEpg = if (userSelectedEpg != null) (epg.firstOrNull { e -> e.url == userSelectedEpg?.url } ?: channel.nowPlaying) else channel.nowPlaying

    val index = if (selectedEpg != null) 0.coerceAtLeast(epg.indexOf(selectedEpg) - 1) else 0
    val lazyListState = rememberLazyListState(initialFirstVisibleItemIndex = index)

    val context = AmbientContext.current

    Column {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 16.dp, top = 16.dp)) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                modifier = Modifier.clickable(onClick = onBackPressed).padding(end = 8.dp)
            )

            Text(
                text = channel.label,
                style = MaterialTheme.typography.h5
            )
        }

        Text(
            text = selectedEpg?.title ?: "Live",
            modifier = Modifier.padding(start = 16.dp),
            style = MaterialTheme.typography.subtitle1
        )

        Spacer(Modifier.preferredHeight(8.dp))

        LazyColumn(state = lazyListState) {
            items(epg) { e ->
                EpgEntryItem(e, e == selectedEpg, onEpgEntry = { epg ->
                    if (epg.hasVideo || epg.isLive) {
                        userSelectedEpg = epg
                        onEpgEntry(epg)
                    }
                }, onAlarm = if (e.isNotStartedYet) { epg ->
                    viewModel.scheduleAlarm(context, channel, epg)
                    Toast.makeText(context, "Reminder scheduled!", Toast.LENGTH_SHORT).show()
                } else null)
            }
        }
    }

    rememberCoroutineScope().launch {
        lazyListState.snapToItemIndex(index, 0)
    }
}

@Composable
fun EpgEntryItem(
    epg: EpgEntry,
    selected: Boolean,
    onEpgEntry: (EpgEntry) -> Unit,
    onAlarm: ((EpgEntry) -> Unit)?
) {
    ListItem(
            icon = {
                VideoThumbnail(url = epg.image)
            },
            overlineText = {
                Text(epg.start, maxLines = 1)
            },
            text = {
                val color = animate(
                    if (selected) {
                        MaterialTheme.colors.primary
                    } else {
                        MaterialTheme.colors.onSurface.copy(alpha = AmbientContentAlpha.current)
                    }
                )

                Text(epg.title,
                    maxLines = 1,
                    color = color,
                    style = if (epg.hasVideo || epg.isLive) TextStyle(fontWeight = FontWeight.W700) else TextStyle.Default)
            },
            secondaryText = {
                Text(epg.subtitle, maxLines = 3, overflow = TextOverflow.Ellipsis)
            },
            trailing = {
                if (onAlarm != null) Icon(imageVector = Icons.Default.AlarmAdd, modifier = Modifier.clickable(onClick = { onAlarm(epg) } )) else Box(modifier = Modifier.preferredSize(0.dp))
            },
            modifier = Modifier.clickable(onClick = { onEpgEntry(epg) })
    )
}