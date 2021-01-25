package com.bw.vrtnumm.shared.transport

data class PlayerInfo(
    val url: String,
    val drmToken: String? = null,
    val kid: String? = null,
    val contentId: String? = null,
    var position: Long = 0
)