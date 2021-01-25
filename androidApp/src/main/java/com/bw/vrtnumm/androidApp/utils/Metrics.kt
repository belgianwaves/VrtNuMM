package com.bw.vrtnumm.androidApp.utils

import android.content.Context

object Metrics {
    fun screenSizeInInches(context: Context): Double {
        val dm = context.resources.displayMetrics
        val x = Math.pow((dm.widthPixels/dm.xdpi).toDouble(), 2.0)
        val y = Math.pow((dm.heightPixels/dm.ydpi).toDouble(), 2.0)
        return Math.sqrt(x + y)
    }

    fun isTablet(context: Context): Boolean = (screenSizeInInches(context) >= 7.0)

    fun widthInDip(context: Context): Float {
        val dm = context.resources.displayMetrics
        return dm.widthPixels / dm.density
    }
}