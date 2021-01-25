package com.bw.vrtnumm.shared.transport

import kotlinx.serialization.Serializable

@Serializable
internal data class SearchResult(
    val results: Array<Episode>
)