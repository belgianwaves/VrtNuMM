package com.bw.vrtnumm.shared.transport

data class Channel(
    val id: String,
    val name: String,
    val label: String,
    val streamId: String,
    val thumbnail: String,
    var epg: List<EpgEntry>? = null
) {
    val nowPlaying
        get() = run {
            var result = epg?.firstOrNull{ e -> e.isLive }
                if (result == null) {
                    result = epg?.lastOrNull{ e -> e.isHistoricOrStarted }
                }
            result
        }
}