package com.bw.vrtnumm.androidApp

import android.content.SharedPreferences
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bw.vrtnumm.androidApp.ui.MainViewModel
import com.bw.vrtnumm.shared.utils.DebugLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async

abstract class MainActivity: AppCompatActivity() {
    protected val controller = PlayerController(this)
    protected lateinit var model: MainViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        initRepo()
        initUi()
    }

    override fun onStop() {
        model.savePositionToFirebase()
        controller.player.playWhenReady = false
        super.onStop()
    }

    override fun onStart() {
        super.onStart()
        controller.player.playWhenReady = true
    }

    override fun onDestroy() {
        controller.dispose()
        model.dispose()
        super.onDestroy()
    }

    override fun onConfigurationChanged(config: Configuration) {
        super.onConfigurationChanged(config)
        adjustFullScreen(config)
        model.onOrientationChanged(config.orientation)
    }

    private fun initRepo() {
        lifecycleScope.async(Dispatchers.IO) {
            val start = System.currentTimeMillis()
            Graph.repo.fetchPrograms()
            Graph.firebase?.sync(Graph.repo)
            val end = System.currentTimeMillis()
            DebugLog.d("*** sync took ${(end - start)/1000} seconds ***")
        }
    }

    abstract fun initUi()

    protected fun getPrefs(): SharedPreferences = getSharedPreferences("saved_state", MODE_PRIVATE)

    private fun adjustFullScreen(config: Configuration) {
        val decorView = window.decorView
        if (config.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
        } else {
            decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        }
    }

    protected fun toggleFullScreen(fullscreen: ImageView) {
        val flag = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        requestedOrientation = if (flag) {
            fullscreen.setImageDrawable(resources.getDrawable(R.drawable.ic_baseline_fullscreen_24))
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        } else {
            fullscreen.setImageDrawable(resources.getDrawable(R.drawable.ic_baseline_fullscreen_exit_24))
            ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        }
    }
}