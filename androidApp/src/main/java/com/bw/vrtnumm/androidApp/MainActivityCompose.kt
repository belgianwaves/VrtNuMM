package com.bw.vrtnumm.androidApp

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.ui.platform.setContent
import com.bw.vrtnumm.androidApp.ui.Main
import com.bw.vrtnumm.androidApp.ui.MainViewModel
import com.bw.vrtnumm.androidApp.ui.theme.VrtNuComposeTheme
import kotlin.time.ExperimentalTime

class MainActivityCompose: MainActivity() {
    override fun onBackPressed() {
        if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            return
        }
        super.onBackPressed()
    }

    @ExperimentalTime
    override fun initUi() {
        model = MainViewModel(controller, initialOrientation = resources.configuration.orientation)
        setContent {
            VrtNuComposeTheme(darkTheme = true) {
                Surface(color = MaterialTheme.colors.background) {
                    Main(sharedPreferences = getPrefs(), controller = controller, viewModel = model, onToggleFullScreen = { fullscreen ->
                        toggleFullScreen(fullscreen)
                    })
                }
            }
        }

        val uri = intent.data
        if (uri != null) {
            val url = uri.toString()
            if (url.startsWith("channel://")) {
                val channel = model.getChannel(url)
                channel?.apply {
                    model.onChannelSelected(this)
                }
            } else {
                val program = model.getProgram(url)
                program?.apply {
                    model.onProgramSelected(this)
                }
            }
        }
    }
}
