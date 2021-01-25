package com.bw.vrtnumm.shared.transport

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

data class EpgEntry(
    val start: String,
    val startTime: Instant,
    val end: String,
    val endTime: Instant,
    val title: String,
    val subtitle: String = "",
    val image: String,
    val description: String = "",
    val url: String = "",
    val programPath: String = ""
) {

    val isLive
        get() = run {
            val now = Clock.System.now()
            (startTime <= now) && (now <= endTime)
        }

    val isHistoricOrStarted
        get() = run {
            val now = Clock.System.now()
            startTime <= now
        }

    val isHistoric
        get() = run {
            val now = Clock.System.now()
            endTime < now
        }

    val isNotStartedYet
        get() = run {
            val now = Clock.System.now()
            now < startTime
        }

    val hasVideo
        get() = url.isNotBlank()

    fun millisBefore(millis: Int): Long {
        return startTime.toEpochMilliseconds() - millis
    }

    fun millisAfter(millis: Int): Long {
        return endTime.toEpochMilliseconds() + millis
    }
}