package com.bw.vrtnumm.shared

import com.bw.vrtnumm.shared.transport.*
import com.bw.vrtnumm.shared.utils.DebugLog
import io.ktor.client.features.logging.*
import io.ktor.client.request.*
import kotlinx.datetime.*
import org.jsonx.JSONObject
import kotlin.time.ExperimentalTime

class Api(user: String, pass: String) {
    companion object {
        private const val EPISODE_QUERY_URL = "https://vrtnu-api.vrt.be/search?i=video&size=150&facets%5BprogramUrl%5D="
        private const val A_Z_QUERY_URL = "https://vrtnu-api.vrt.be/suggest?facets%5BtranscodingStatus%5D=AVAILABLE"
        private const val CATEGORY_QUERY_URL = "https://vrtnu-api.vrt.be/suggest?facets%5Bcategories%5D="
        private const val QUERY_URL = "https://search.vrt.be/suggest?q="
        private const val MEDIA_API_URL = "https://media-services-public.vrt.be/vualto-video-aggregator-web/rest/external/v1"
        private const val EPG_URL = "https://www.vrt.be/bin/epg/schedule."
    }

    private val client = createHttpClient()

    private val tokenResolver: TokenResolver = TokenResolver(client, user, pass)

    private var playerTokenOnDemand: String? = null
    private var playerTokenLive: String? = null


    @ExperimentalTime
    suspend fun login(): Boolean {
        return try {
            tokenResolver.login().isNotEmpty()
        } catch (e: Exception) {
            DebugLog.e("failed logging in", e)
            false
        }
    }

    internal suspend fun fetchEpisodes(programUrl: String): List<Episode>? {
        var result: List<Episode>? = null
            try {
                val searchResult: SearchResult? = client.get(EPISODE_QUERY_URL + programUrl)
                searchResult?.results?.run {
                    sort()
                    result = toList()
                }
            } catch (e: Exception) {
                DebugLog.e("failed fetching episodes for $programUrl", e)
            }
        return result
    }

    internal suspend fun fetchPrograms(category: String = "a-z"): Category? {
        var result: Category? = null
            val programs = listPrograms(if ("a-z" == category) A_Z_QUERY_URL else "$CATEGORY_QUERY_URL$category")
            if (programs != null) {
                result = Category(category, programs)
            }
        return result
    }

    internal suspend fun searchPrograms(query: String): Category? {
        var result: Category? = null
            val programs = listPrograms("$QUERY_URL${URLEncoder.encode(query)}")
            if (programs != null) {
                result = Category(query, programs)
            }
        return result
    }

    private suspend fun listPrograms(url: String): List<Program>? {
        var result: List<Program>? = null
            try {
                result = client.get(url)
            } catch (e: Exception) {
                DebugLog.e("failed getting programs for $url", e)
            }
        return result
    }

    @ExperimentalTime
    suspend fun fetchInfo(publicationId: String, videoId: String, refreshToken: Boolean = false, useHls: Boolean = true): PlayerInfo? {
        if ((playerTokenOnDemand == null) || refreshToken) {
            playerTokenOnDemand = tokenResolver.getPlayerToken("$MEDIA_API_URL/tokens", "ondemand")
            DebugLog.d("playerTokenOnDemand: $playerTokenOnDemand")
        }

        var result: PlayerInfo? = null
            try {
                val payload: String? = client.get("$MEDIA_API_URL/videos/${publicationId}\$${videoId}?vrtPlayerToken=$playerTokenOnDemand&client=vrtvideo@PROD")
                result = fetchInfo(payload, useHls)
            } catch (e: Exception) {
                DebugLog.e("failed fetching info for $publicationId/$videoId", e)
            }
        return result
    }

    @ExperimentalTime
    suspend fun fetchInfo(streamId: String, refreshToken: Boolean = false, useHls: Boolean = true): PlayerInfo? {
        if ((playerTokenLive == null) || refreshToken) {
            playerTokenLive = tokenResolver.getPlayerToken("$MEDIA_API_URL/tokens", "live")
            DebugLog.d("playerTokenLive: $playerTokenLive")
        }

        var result: PlayerInfo? = null
            try {
                val payload: String? = client.get("$MEDIA_API_URL/videos/${streamId}?vrtPlayerToken=$playerTokenLive&client=vrtvideo@PROD")
                result = fetchInfo(payload, useHls)
            } catch (e: Exception) {
                DebugLog.e("failed fetching info for $streamId", e)
            }
        return result
    }

    private suspend fun fetchInfo(payload: String?, useHls: Boolean = true): PlayerInfo? {
        var url: String? = null
        var drmToken = ""
        var kid = ""
        var contentId = ""

        if (!payload.isNullOrEmpty()) {
            val json = JSONObject(payload)

            drmToken = json.optString("drm", "")
            drmToken = if ("null" == drmToken) "" else drmToken
            DebugLog.d("drmToken: $drmToken")

            val requiredType = if (useHls) "hls" else "mpeg_dash"

            val targetUrls = json.getJSONArray("targetUrls")
            for (i in 0 until targetUrls.length()) {
                val targetUrl = targetUrls.getJSONObject(i)
                if (requiredType == targetUrl.getString("type")) {
                    url = targetUrl.getString("url")
                    DebugLog.d("url: $url")
                    if (drmToken.isNotEmpty()) {
                        val s: String? = client.get(url)
                        if (!s.isNullOrEmpty()) {
                            if (useHls) {
                                for (line in s.lines()) {
                                    if (line.contains("EXT-X-SESSION-KEY")) {
                                        val keyValue = line.split(",").firstOrNull { s -> s.startsWith("URI=") }
                                        keyValue?.apply {
                                            var value = split("=")[1].replace("\"", "")
                                            contentId = value.split("/").last()
                                        }
                                    }
                                }
                            } else {
                                val k = extractKid(s)
                                if (k != null) {
                                    kid = k
                                }
                            }
                        }
                    }

                    if (useHls) {
                        DebugLog.d("contentId: $contentId")
                    } else {
                        DebugLog.d("kid: $kid")
                    }

                    break
                }
            }
        }

        return if (url != null) PlayerInfo(url, drmToken, kid, contentId) else null
    }

    suspend fun fetchEpg(): Map<String, List<EpgEntry>>? {
        var result: Map<String, List<EpgEntry>>? = null
            try {
                val now = Clock.System.now()
                val s = now.toString()
                val date = s.substring(0, s.indexOf("T"))
                val payload: String? = client.get("$EPG_URL$date.json")

                if (!payload.isNullOrEmpty()) {
                    result = HashMap()
                    val top = JSONObject(payload)
                    top.keys().forEach { id ->
                        val entries = top.getJSONArray(id as String)
                        val epgEntries = ArrayList<EpgEntry>()
                        for (i in 0 until entries.length()) {
                            val entry = entries.getJSONObject(i)
                            val start = parse(entry.getString("startTime"))
                            val end = parse(entry.getString("endTime"))
                            epgEntries.add(
                                EpgEntry(
                                    start = entry.getString("start"),
                                    startTime = start,
                                    end = entry.getString("end"),
                                    endTime = end,
                                    title = entry.getString("title"),
                                    subtitle = entry.optString("subtitle", ""),
                                    image = entry.optString("image", ""),
                                    description = entry.optString("description", ""),
                                    url = entry.optString("url", ""),
                                    programPath = entry.optString("programPath", "")
                                )
                            )
                        }
                        result[id] = epgEntries
                    }
                }
            } catch (e: Exception) {
                DebugLog.e("failed fetching epg", e)
            }
        return result
    }

    private fun parse(date: String): Instant {
        val systemTZ = TimeZone.currentSystemDefault()
        var converted = date
            val index = date.indexOf("+")
            if (index > 0) {
                converted = date.substring(0, index) + "Z"
            }
        return Instant.parse(converted).minus(1, DateTimeUnit.HOUR, systemTZ)
    }
}

class CustomHttpLogger(): Logger {
    override fun log(message: String) {
        DebugLog.i(message)
    }
}