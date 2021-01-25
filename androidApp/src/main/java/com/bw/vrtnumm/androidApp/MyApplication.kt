package com.bw.vrtnumm.androidApp

import android.app.Application
import com.bw.vrtnumm.shared.appContext

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        appContext = this
    }

    override fun onTerminate() {
        super.onTerminate()
    }
}