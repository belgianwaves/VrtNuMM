package com.bw.vrtnumm.shared.transport

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class Program(
    val title: String,
    val programUrl: String,
    val thumbnail: String,
    val description: String
)