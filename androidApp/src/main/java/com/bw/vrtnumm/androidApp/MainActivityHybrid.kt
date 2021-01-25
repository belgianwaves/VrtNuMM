package com.bw.vrtnumm.androidApp

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.RelativeLayout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.lifecycleScope
import com.bw.vrtnumm.androidApp.ui.MainViewModel
import com.bw.vrtnumm.androidApp.ui.home.Home
import com.bw.vrtnumm.androidApp.ui.home.HomeViewModel
import com.bw.vrtnumm.androidApp.ui.home.live.Channel
import com.bw.vrtnumm.androidApp.ui.home.program.Program
import com.bw.vrtnumm.androidApp.ui.theme.VrtNuComposeTheme
import com.bw.vrtnumm.androidApp.utils.AnimatorListenerAdapter
import com.bw.vrtnumm.shared.db.Program
import com.bw.vrtnumm.shared.transport.Channel
import com.google.android.exoplayer2.ui.PlayerView
import kotlinx.coroutines.launch
import kotlin.time.ExperimentalTime

class MainActivityHybrid: MainActivity() {
    private lateinit var playerView: PlayerView
    private lateinit var home: ComposeView
    private lateinit var details: ComposeView
    private lateinit var fullscreen: ImageView

    @ExperimentalTime
    override fun onBackPressed() {
        if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            return
        }

        if (model.hasProgram || model.hasChannel) {
            showHome(false)
            return
        }

        super.onBackPressed()
    }

    override fun onConfigurationChanged(config: Configuration) {
        super.onConfigurationChanged(config)
        if (::playerView.isInitialized) {
            val params = playerView.layoutParams
                if (config.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    params.height = ViewGroup.LayoutParams.MATCH_PARENT
                    home.visibility = View.INVISIBLE
                    details.visibility = View.INVISIBLE
                    fullscreen.setImageDrawable(resources.getDrawable(R.drawable.ic_baseline_fullscreen_exit_24))
                } else {
                    adaptLayoutViews()
                    home.visibility = View.VISIBLE
                    details.visibility = if (model.hasProgram) View.VISIBLE else View.INVISIBLE
                    fullscreen.setImageDrawable(resources.getDrawable(R.drawable.ic_baseline_fullscreen_24))
                }
            playerView.layoutParams = params
        }
    }

    @ExperimentalTime
    override fun initUi() {
        model = MainViewModel(controller, initialOrientation = resources.configuration.orientation)

        setContentView(R.layout.activity_main)

        playerView = findViewById(R.id.player_view)
        playerView.apply {
            controller.setPlayerView(this)
        }
        home = findViewById(R.id.home)
        details = findViewById(R.id.details)
        fullscreen = playerView.findViewById(R.id.bt_fullscreen)

        fullscreen.setOnClickListener {
            toggleFullScreen(fullscreen)
        }

        adaptLayoutViews()

        showHome(true)

        val uri = intent.data
        if (uri != null) {
            val url = uri.toString()
            if (url.startsWith("channel://")) {
                val channel = model.getChannel(url)
                channel?.apply {
                    onChannelSelected(this)
                }
            } else {
                val program = model.getProgram(url)
                program?.apply {
                    onProgramSelected(this)
                }
            }
        }
    }

    private fun adaptLayoutViews() {
        val metrics = resources.displayMetrics
        val width = metrics.widthPixels
        val height = 9*width/16

        var params = playerView.layoutParams
            params.height = height
        playerView.layoutParams = params

        params = home.layoutParams as RelativeLayout.LayoutParams
            params.topMargin = Math.min(height, metrics.heightPixels/2)
        home.layoutParams = params

        params = details.layoutParams as RelativeLayout.LayoutParams
            params.topMargin = Math.min(height, metrics.heightPixels/2)
        details.layoutParams = params
    }

    @ExperimentalTime
    private fun showHome(firstTime: Boolean = true) {
        if (firstTime) {
            home.setContent {
                VrtNuComposeTheme(darkTheme = true) {
                    Surface(color = MaterialTheme.colors.background) {
                        Home(
                            viewModel = HomeViewModel(getPrefs()),
                            onProgramSelected = ::onProgramSelected,
                            onChannelSelected = ::onChannelSelected
                        )
                    }
                }
            }
        } else {
            val anim = AnimationUtils.loadAnimation(this@MainActivityHybrid, R.anim.slide_out_right)
                anim.duration = 700
                anim.setAnimationListener(AnimatorListenerAdapter(onEnd = {
                    details.visibility = View.INVISIBLE
                }))
            details.startAnimation(anim)
        }

        model.moveBack()
    }

    @ExperimentalTime
    private fun onProgramSelected(program: Program) = lifecycleScope.launch {
        model.onProgramSelected(program)

        details.visibility = View.VISIBLE
        details.setContent {
            VrtNuComposeTheme(darkTheme = true) {
                Surface(color = MaterialTheme.colors.background) {
                    Program(
                        modifier = Modifier.fillMaxSize(),
                        programUrl = program.programUrl,
                        onEpisodeSelected = model::onEpisodeSelected
                    ) { showHome(false) }
                }
            }
        }

        val anim = AnimationUtils.loadAnimation(this@MainActivityHybrid, R.anim.slide_in_right)
            anim.duration = 700
        details.startAnimation(anim)
    }

    @ExperimentalTime
    private fun onChannelSelected(channel: Channel) {
        model.onChannelSelected(channel)

        if (channel.epg != null) {
            details.visibility = View.VISIBLE
            details.setContent {
                VrtNuComposeTheme(darkTheme = true) {
                    Surface(color = MaterialTheme.colors.background) {
                        Channel(
                            modifier = Modifier.fillMaxSize(),
                            streamId = channel.streamId,
                            onEpgEntry = model::onEpgEntrySelected,
                            onBackPressed = { showHome(false) }
                        )
                    }
                }
            }

            val anim = AnimationUtils.loadAnimation(this@MainActivityHybrid, R.anim.slide_in_right)
                anim.duration = 700
            details.startAnimation(anim)
        }
    }
}
