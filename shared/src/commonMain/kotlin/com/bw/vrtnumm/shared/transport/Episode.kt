package com.bw.vrtnumm.shared.transport

import kotlinx.serialization.Serializable

@Serializable
internal data class Episode(
    val title: String,
    val shortDescription: String,
    val description: String,
    val programUrl: String,
    val url: String,
    val episodeNumber: Int,
    val seasonName: String,
    val publicationId: String,
    val videoId: String,
    val videoThumbnailUrl: String
) :Comparable<Episode> {
    override fun compareTo(other: Episode): Int = this.seasonName.compareTo(other.seasonName)*100 + this.episodeNumber.compareTo(other.episodeNumber)


    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as Episode

        if (programUrl != other.programUrl) return false
        if (publicationId != other.publicationId) return false
        if (videoId != other.videoId) return false

        return true
    }

    override fun hashCode(): Int {
        var result = programUrl.hashCode()
        result = 31 * result + publicationId.hashCode()
        result = 31 * result + videoId.hashCode()
        return result
    }
}