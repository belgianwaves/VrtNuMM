package com.bw.vrtnumm.shared.utils

import co.touchlab.kermit.Kermit
import com.bw.vrtnumm.shared.getLogger

object DebugLog {
    private val kermit = Kermit(getLogger())

    private var tag = "bw"
    fun setTag(tag: String) {
        DebugLog.tag = tag
    }

    fun e(message: String) {
        kermit.e(tag) { message }
    }

    fun e(message: String, t: Throwable?) {
        kermit.e(tag, t) { message }
    }

    fun w(message: String) {
        kermit.w(tag) { message }
    }

    fun w(message: String, t: Throwable?) {
        kermit.w(tag, t) { message }
    }

    fun i(message: String) {
        kermit.i(tag) { message }
    }

    fun i(message: String, t: Throwable?) {
        kermit.i(tag, t) { message }
    }

    fun d(message: String) {
        kermit.d(tag) { message }
    }

    fun d(message: String, t: Throwable?) {
        kermit.d(tag, t) { message }
    }

    fun v(message: String) {
        kermit.v(tag) { message }
    }

    fun v(message: String, t: Throwable?) {
        kermit.v(tag, t) { message }
    }
}