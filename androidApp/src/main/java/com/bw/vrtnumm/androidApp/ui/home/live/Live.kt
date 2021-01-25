package com.bw.vrtnumm.androidApp.ui.home.live

import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.ListItem
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.viewinterop.viewModel
import com.bw.vrtnumm.androidApp.utils.VideoThumbnail
import com.bw.vrtnumm.shared.transport.Channel

@Composable
fun Live(
    modifier: Modifier,
    onChannelSelected: (Channel) -> Unit
) {
    val viewModel: LiveViewModel = viewModel()
    val viewState by viewModel.state.collectAsState()

    LazyColumn() {
        items(viewState.channels) { channel ->
            ChannelItem(channel, onChannelSelected)
        }
    }
}

@Composable
fun ChannelItem(
    channel: Channel,
    onChannelSelected: (Channel) -> Unit
) {
    var subtitle = "Live"
        val epg = channel.nowPlaying
        if (epg != null) {
            subtitle = "${epg.title}\n${epg.subtitle}"
        }
    ListItem(
        icon = {
            VideoThumbnail(url = "${channel.thumbnail}?timestamp=${System.currentTimeMillis()}")
        },
        text = {
            Text(channel.label, maxLines = 1)
        },
        secondaryText = {
            Text(
                subtitle, maxLines = 3, overflow = TextOverflow.Ellipsis
            )
        },
        modifier = Modifier.clickable(onClick = { onChannelSelected(channel) })
    )
}