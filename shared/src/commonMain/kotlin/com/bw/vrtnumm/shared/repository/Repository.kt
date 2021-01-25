package com.bw.vrtnumm.shared.repository

import com.bw.vrtnumm.shared.Api
import com.bw.vrtnumm.shared.Platform
import com.bw.vrtnumm.shared.createDb
import com.bw.vrtnumm.shared.db.Episode
import com.bw.vrtnumm.shared.db.Position
import com.bw.vrtnumm.shared.db.Program
import com.bw.vrtnumm.shared.db.User
import com.bw.vrtnumm.shared.transport.*
import com.bw.vrtnumm.shared.utils.DebugLog
import com.squareup.sqldelight.runtime.coroutines.asFlow
import com.squareup.sqldelight.runtime.coroutines.mapToList
import com.squareup.sqldelight.runtime.coroutines.mapToOne
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.datetime.Clock

class Repository() {
    private lateinit var api: Api
    private val db = createDb()
    private val queries = db.vrtNuQueries

    private val coroutineScope: CoroutineScope = MainScope()

    private var categoryNamesJob: Job? = null
    private val categoryJobs: MutableMap<String, Job> = mutableMapOf()
    private val programJobs: MutableMap<String, Job> = mutableMapOf()
    private var channelJob: Job? = null
    private var watchedProgramsJob: Job? = null
    private var favouriteProgramsJob: Job? = null
    private var recentProgramsJob: Job? = null

    fun init(api: Api) {
        this.api = api
    }

    private fun isAndroid(): Boolean {
        return (Platform().platform.toLowerCase().indexOf("android") >= 0)
    }

    private suspend fun fetchConcurrent(names: Iterable<String>): Map<String, Category> = coroutineScope {
        names.map { name -> async(if (isAndroid()) Dispatchers.Default else Dispatchers.Main) { name to api.fetchPrograms(name)!! } }
            .map { it.await() }
            .toMap()
    }

    suspend fun fetchPrograms() = coroutineScope {
        val fetched = fetchConcurrent(
            listOf(
                "series",
                "docu",
                "films",
                "nieuws-en-actua",
                "cultuur",
                "entertainment",
                "human-interest",
                "humor",
                "levensbeschouwing",
                "lifestyle",
                "talkshows",
                "nostalgie",
                "wetenschap-en-natuur",
                "muziek",
                "voor-kinderen",
                "a-z"
            )
        )

        val now = if (getPrograms().isEmpty()) 0 else Clock.System.now().toEpochMilliseconds()

        val az = fetched.get("a-z")
        az?.run {
            queries.transaction {
                programs.map { p ->
                    var sp = getProgram(p.programUrl)
                    if (sp == null) {
                        insertProgram(programUrl = p.programUrl, title = p.title, description = p.description, thumbnail = p.thumbnail, created = now)
                    } else {
                        updateProgram(programUrl = p.programUrl, title = p.title, description = p.description, thumbnail = p.thumbnail)
                    }
                }

                fetched.map { e ->
                    val name = e.key
                    queries.clearCategory(name)
                    e.value.programs.forEach { p ->
                        queries.insertCategoryEntry(name, p.programUrl)
                    }
                }
            }
        }
    }

    suspend fun fetchEpisodes(programUrl: String) {
        var sp = getProgram(programUrl)
        sp?.run {
            val fetched = api.fetchEpisodes(programUrl)
            fetched?.map { e ->
                queries.transaction {
                    val se = getEpisode(e.programUrl, e.publicationId, e.videoId)
                    if (se == null) {
                        insertEpisode(e.programUrl, e.publicationId, e.videoId, e.title, e.shortDescription, e.description, e.seasonName, e.episodeNumber, e.videoThumbnailUrl)
                    } else {
                        updateEpisode(e.programUrl, e.publicationId, e.videoId, e.title, e.shortDescription, e.description, e.seasonName, e.episodeNumber, e.videoThumbnailUrl)
                    }
                }
            }
        }
    }

    fun allChannels(): List<Channel> {
        return listOf(
            Channel("O8", "een", "Eén", "vualto_een_geo", "vrtnu-api.vrt.be/screenshots/een_geo.jpg"),
            Channel("1H", "canvas", "Canvas", "vualto_canvas_geo", "vrtnu-api.vrt.be/screenshots/canvas_geo.jpg"),
            Channel("O9", "ketnet", "Ketnet", "vualto_ketnet_geo", "vrtnu-api.vrt.be/screenshots/ketnet_geo.jpg"),
        )
    }

    private suspend fun fetchChannels(): List<Channel> {
        val epg = api.fetchEpg()
        val result = allChannels()
            result.forEach { c ->
                c.epg = if (epg != null) epg[c.id] else null
            }
        return result
    }

    fun getChannel(channelUrl: String): Channel? {
        return allChannels().firstOrNull { c -> channelUrl == "channel://${c.streamId}" }
    }

    fun getCurrentUser(): User? {
        return queries.selectUsers().executeAsOneOrNull()
    }

    fun createCurrentUser(email: String, password: String) {
        queries.insertUser(email, password)
    }

    fun getPosition(programUrl: String, publicationId: String, videoId: String): Position? {
        return queries.selectPosition(programUrl, publicationId, videoId).executeAsOneOrNull()
    }

    fun insertPosition(programUrl: String, title: String, publicationId: String, videoId: String) {
        queries.insertPosition(programUrl, title, publicationId, videoId, Clock.System.now().toEpochMilliseconds())
    }

    fun updatePosition(programUrl: String, publicationId: String, videoId: String, position: Long, duration: Long, lastSeen: Long = -1) {
        val timestamp = if (lastSeen < 0) Clock.System.now().toEpochMilliseconds() else lastSeen
        queries.updatePosition(position, duration, timestamp, programUrl, publicationId, videoId)
    }

    private fun getPrograms(): List<Program> {
        return queries.selectPrograms().executeAsList()
    }

    fun getProgram(programUrl: String): Program? {
        return queries.selectProgram(programUrl).executeAsOneOrNull()
    }

    fun getProgramAsFlow(programUrl: String): Flow<Program> {
        return queries.selectProgram(programUrl).asFlow().mapToOne()
    }

    fun insertProgram(programUrl: String, title: String, description: String, thumbnail: String, created: Long = Clock.System.now().toEpochMilliseconds()) {
        queries.insertProgram(programUrl, title, description, thumbnail, created)
    }

    private fun updateProgram(programUrl: String, title: String, description: String, thumbnail: String) {
        queries.updateProgram(title, description, thumbnail, programUrl)
    }

    fun setProgramFavourite(programUrl: String, favourite: Boolean) {
        queries.setProgramFavourite(favourite, programUrl)
    }

    suspend fun getMatchingPrograms(query: String): List<Program> {
        return api.searchPrograms(query)?.programs?.map { p -> getProgram(p.programUrl)!! } ?: emptyList<Program>()
    }

    fun getEpisodesForProgram(programUrl: String): List<Episode> {
        return queries.selectEpisodesForProgram(programUrl).executeAsList()
    }

    fun getEpisodesForProgramAsFlow(programUrl: String): Flow<List<Episode>> {
        return queries.selectEpisodesForProgram(programUrl).asFlow().mapToList()
    }

    fun getWatchedEpisodesForProgram(programUrl: String): List<Episode> {
        return queries.selectWatchedEpisodesForProgram(programUrl).executeAsList()
    }

    private fun getEpisode(programUrl: String, publicationId: String, videoId: String): Episode? {
        return queries.selectEpisode(programUrl, publicationId, videoId).executeAsOneOrNull()
    }

    private fun insertEpisode(programUrl: String, publicationId: String, videoId: String, title: String, shortDescription: String, description: String, seasonName: String, episodeNumber: Int, videoThumbnailUrl: String) {
        queries.insertEpisode(programUrl, publicationId, videoId, title, shortDescription, description, seasonName, episodeNumber.toShort(), videoThumbnailUrl)
    }

    private fun updateEpisode(programUrl: String, publicationId: String, videoId: String, title: String, shortDescription: String, description: String, seasonName: String, episodeNumber: Int, videoThumbnailUrl: String) {
        queries.updateEpisode(title, shortDescription, description, seasonName, episodeNumber.toShort(), videoThumbnailUrl, programUrl, publicationId, videoId)
    }

    suspend fun getEpisode(epgEntry: EpgEntry): Episode? {
        var result: Episode? = null
            if (epgEntry.hasVideo) {
                val episodes = api.fetchEpisodes("//www.vrt.be${epgEntry.programPath}")
                if (episodes != null) {
                    val episode = episodes.firstOrNull { e -> e.url == "//www.vrt.be${epgEntry.url}" }
                    if (episode != null) {
                        result = Episode(
                            programUrl = episode.programUrl,
                            publicationId = episode.publicationId,
                            videoId = episode.videoId,
                            title = episode.title,
                            shortDescription = episode.shortDescription,
                            desc = episode.description,
                            seasonName = episode.seasonName,
                            episodeNumber = episode.episodeNumber.toShort(),
                            videoThumbnailUrl = episode.videoThumbnailUrl
                        )
                    }
                }
            }
        return result
    }

    suspend fun getFavProgramsWithNewEpisodes(): List<Program> {
        val result = ArrayList<Program>()
            val toCheck = queries.selectFavouritePrograms().executeAsList()
            toCheck.forEach { p ->
                try {
                    val nbrOfEpisodes = getEpisodesForProgram(p.programUrl).size
                        fetchEpisodes(p.programUrl)
                    if (getEpisodesForProgram(p.programUrl).size > nbrOfEpisodes) {
                        result.add(p)
                    }
                } catch (e: Exception) {
                    DebugLog.e("failed checking program ${p.programUrl}", e)
                }
            }
        return result
    }

    fun getCategoryNames(): List<String> {
        return queries.selectCategoryNames().executeAsList()
    }

    fun getCategoryNamesAsFlow(): Flow<List<String>> {
        return queries.selectCategoryNames().asFlow().mapToList()
    }

    fun startObservingCategoryNames(success: (List<String>) -> Unit) {
        categoryNamesJob = coroutineScope.launch {
            getCategoryNamesAsFlow().collect {
                success(it)
            }
        }
    }

    fun stopObservingCategoryNames() {
        categoryNamesJob?.cancel()
    }

    fun getProgramsForCategory(category: String): List<Program> {
        return queries.selectProgramsForCategory(category).executeAsList()
    }

    fun getProgramsForCategoryAsFlow(category: String): Flow<List<Program>> {
        return queries.selectProgramsForCategory(category).asFlow().mapToList()
    }

    fun startObservingCategory(category: String, success: (List<Program>) -> Unit) {
        DebugLog.d("startObservingCategory: $category")
        categoryJobs.put(category, coroutineScope.launch {
            getProgramsForCategoryAsFlow(category).collect {
                success(it)
            }
        })
    }

    fun stopObservingCategory(category: String) {
        DebugLog.d("stopObservingCategory: $category")
        categoryJobs[category]?.cancel()
    }

    fun startObservingProgram(programUrl: String, success: (List<Episode>) -> Unit) {
        DebugLog.d("startObservingProgram: $programUrl")
        programJobs.put(programUrl, coroutineScope.launch {
            getEpisodesForProgramAsFlow(programUrl).collect {
                success(it)
            }
        })
    }

    fun stopObservingProgram(programUrl: String) {
        DebugLog.d("stopObservingProgram: $programUrl")
        programJobs[programUrl]?.cancel()
    }

    fun startObservingChannels(success: (List<Channel>) -> Unit) {
        DebugLog.d("startObservingChannels")
        channelJob = coroutineScope.launch {
            while (isActive) {
                success(fetchChannels())
                delay(60 * 1000)
            }
        }
    }

    fun stopObservingChannels() {
        DebugLog.d("stopObservingChannels")
        channelJob?.cancel()
    }

    fun getWatchedProgramsAsFlow(): Flow<List<Program>> {
        return queries.selectWatchedPrograms().asFlow().mapToList()
    }

    fun startObservingWatchedPrograms(success: (List<Program>) -> Unit) {
        DebugLog.d("startObservingWatchedPrograms")
        watchedProgramsJob = coroutineScope.launch {
            getWatchedProgramsAsFlow().collect {
                success(it)
            }
        }
    }

    fun stopObservingWatchedPrograms() {
        DebugLog.d("stopObservingWatchedPrograms")
        watchedProgramsJob?.cancel()
    }

    fun getFavouriteProgramsAsFlow(): Flow<List<Program>> {
        return queries.selectFavouritePrograms().asFlow().mapToList()
    }

    fun startObservingFavouritePrograms(success: (List<Program>) -> Unit) {
        DebugLog.d("startObservingFavouritePrograms")
        favouriteProgramsJob = coroutineScope.launch {
            getFavouriteProgramsAsFlow().collect {
                success(it)
            }
        }
    }

    fun stopObservingFavouritePrograms() {
        DebugLog.d("stopObservingFavouritePrograms")
        favouriteProgramsJob?.cancel()
    }

    fun getRecentProgramsAsFlow(): Flow<List<Program>> {
        return queries.selectRecentPrograms(Clock.System.now().toEpochMilliseconds() - 3*24*60*60*1000).asFlow().mapToList()
    }

    fun startObservingRecentPrograms(success: (List<Program>) -> Unit) {
        DebugLog.d("startObservingRecentPrograms")
        recentProgramsJob = coroutineScope.launch {
            getRecentProgramsAsFlow().collect {
                success(it)
            }
        }
    }

    fun stopObservingRecentPrograms() {
        DebugLog.d("stopObservingRecentPrograms")
        recentProgramsJob?.cancel()
    }
}