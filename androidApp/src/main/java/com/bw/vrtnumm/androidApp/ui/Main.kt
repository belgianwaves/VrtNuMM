package com.bw.vrtnumm.androidApp.ui

import android.content.SharedPreferences
import android.content.res.Configuration
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.preferredSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.AmbientAnimationClock
import androidx.compose.ui.platform.AmbientContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.bw.vrtnumm.androidApp.PlayerController
import com.bw.vrtnumm.androidApp.R
import com.bw.vrtnumm.androidApp.ui.home.Home
import com.bw.vrtnumm.androidApp.ui.home.HomeViewModel
import com.bw.vrtnumm.androidApp.ui.home.live.Channel
import com.bw.vrtnumm.androidApp.ui.home.program.Program
import com.bw.vrtnumm.androidApp.utils.Metrics
import com.bw.vrtnumm.androidApp.utils.Pager
import com.bw.vrtnumm.androidApp.utils.PagerState
import com.google.android.exoplayer2.ui.PlayerView
import kotlin.time.ExperimentalTime

@ExperimentalTime
@Composable
fun Main(
    sharedPreferences: SharedPreferences,
    controller: PlayerController,
    pagerState: PagerState = run {
        val clock = AmbientAnimationClock.current
        remember(clock) { PagerState(clock) }
    },
    viewModel: MainViewModel,
    onToggleFullScreen: (ImageView) -> Unit
) {
    val viewState by viewModel.state.collectAsState()

    Column(verticalArrangement = Arrangement.Top, modifier = Modifier.fillMaxSize()) {
        val modifier = if (viewState.orientation == Configuration.ORIENTATION_LANDSCAPE) Modifier.fillMaxSize()
            else {
                val width = Metrics.widthInDip(AmbientContext.current)
                val height = 9*width/16
                Modifier.preferredSize(width = width.dp, height = height.dp)
            }
        Video(
            controller = controller,
            modifier = modifier,
            onToggleFullScreen = onToggleFullScreen
        )

        pagerState.maxPage = if ((viewState.program != null) || (viewState.channel != null)) 2 else 1

        Pager(pagerState) {
            if (page == 0) {
                Home(
                    viewModel = HomeViewModel(sharedPreferences = sharedPreferences),
                    onProgramSelected = { program -> viewModel.onProgramSelected(program); pagerState.moveNext()},
                    onChannelSelected = { channel -> viewModel.onChannelSelected(channel); pagerState.moveNext()}
                )
            } else if (page == 1) {
                if (viewState.program != null) {
                    Program(
                        modifier = Modifier.fillMaxSize(),
                        programUrl = viewState.program?.programUrl ?: "",
                        onEpisodeSelected = viewModel::onEpisodeSelected
                    ) { pagerState.movePrev(); viewModel.moveBack() }
                } else if (viewState.channel != null) {
                    Channel(
                        modifier = Modifier.fillMaxSize(),
                        streamId = viewState.channel?.streamId ?: "",
                        onEpgEntry = viewModel::onEpgEntrySelected,
                        onBackPressed = { pagerState.movePrev(); viewModel.moveBack() }
                    )
                }
            }
        }
    }
}

@Composable
private fun Video(
    controller: PlayerController,
    modifier: Modifier = Modifier,
    onToggleFullScreen: (ImageView) -> Unit) {

    AndroidView(viewBlock = { context ->
        (LayoutInflater.from(context).inflate(R.layout.player, null) as PlayerView).apply {
            controller.setPlayerView(this)
            val fullscreen = findViewById<ImageView>(R.id.bt_fullscreen)
            fullscreen?.setOnClickListener {
                onToggleFullScreen(fullscreen)
            }
        layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
    } }, modifier = modifier)
}