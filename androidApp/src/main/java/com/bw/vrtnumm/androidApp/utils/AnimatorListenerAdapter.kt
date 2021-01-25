package com.bw.vrtnumm.androidApp.utils

import android.view.animation.Animation

internal open class AnimatorListenerAdapter(
    val onStart: ((Animation) -> Unit)? = null,
    val onRepeat: ((Animation) -> Unit)? = null,
    val onEnd: ((Animation) -> Unit)? = null,
): Animation.AnimationListener {

    override fun onAnimationStart(animation: Animation) = onStart?.invoke(animation) ?: Unit
    override fun onAnimationRepeat(animation: Animation) = onRepeat?.invoke(animation) ?: Unit
    override fun onAnimationEnd(animation: Animation) = onEnd?.invoke(animation) ?: Unit
}
