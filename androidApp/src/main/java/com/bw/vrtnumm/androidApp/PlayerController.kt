package com.bw.vrtnumm.androidApp

import android.content.Context
import android.net.Uri
import com.bw.vrtnumm.shared.transport.PlayerInfo
import com.bw.vrtnumm.shared.utils.DebugLog
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.drm.DefaultDrmSessionManager
import com.google.android.exoplayer2.drm.DrmSessionManager
import com.google.android.exoplayer2.drm.FrameworkMediaDrm
import com.google.android.exoplayer2.source.dash.DashMediaSource
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory
import com.google.android.exoplayer2.util.Util

class PlayerController(
    private val context: Context
) {
    companion object {
        private const val USER_AGENT = "vrt-nu"
        private const val DRM_LICENSE_URL = "https://widevine-proxy.drm.technology/proxy"
    }

    val player by lazy {
        SimpleExoPlayer.Builder(context).build().apply {
            addListener(object : Player.EventListener {
                override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
                    DebugLog.d("$playWhenReady/$playbackState")
                }
            })
        }
    }

    fun prepare(playerInfo: PlayerInfo) {
        try {
            val useDrm = !playerInfo.drmToken.isNullOrEmpty()
            val drmSessionManager: DrmSessionManager = if (useDrm) {
                val drmCallback =
                    VuDrmCallback(DRM_LICENSE_URL, DefaultHttpDataSourceFactory(USER_AGENT)).
                    apply {
                        setDrmToken(playerInfo.drmToken)
                        setKid(playerInfo.kid)
                    }
                DefaultDrmSessionManager.Builder()
                    .setUuidAndExoMediaDrmProvider(C.WIDEVINE_UUID, FrameworkMediaDrm.DEFAULT_PROVIDER)
                    .setMultiSession(true)
                .build(drmCallback)
            } else {
                DrmSessionManager.getDummyDrmSessionManager()
            }

            player.run {
                val mediaDataSourceFactory = DefaultDataSourceFactory(context, Util.getUserAgent(context, USER_AGENT))
                val mediaSource = DashMediaSource.Factory(mediaDataSourceFactory)
                    .setDrmSessionManager(drmSessionManager)
                    .createMediaSource(Uri.parse(playerInfo.url))

                setMediaSource(mediaSource, true)
                prepare()

                val pos = playerInfo.position
                if (pos > 0) {
                    seekTo(pos)
                }

                playWhenReady = true
            }
        } catch (t: Throwable) {
            DebugLog.e("failed to initialize player", t)
        }
    }

    fun setPlayerView(playerView: PlayerView) {
        playerView.apply {
            player = this@PlayerController.player
            hideController()
        }
    }

    fun mute() {
        player.volume = 0f
    }

    fun dispose() {
        player.release()
    }
}