import Foundation
import shared

class ViewModel: ObservableObject {
    @Published private(set) var categories = [String]()
    @Published private(set) var programs = [Program]()
    @Published private(set) var episodes = [Episode]()
    @Published private(set) var channels = [Channel]()
    
    @Published private(set) var watchedPrograms = [Program]()
    @Published private(set) var favouritePrograms = [Program]()
    @Published private(set) var recentPrograms = [Program]()
    
    @Published private(set) var playerInfo : PlayerInfo? = nil
    
    private var api: Api? = nil
    private let repo: Repository
    
    init () {
        self.repo = Repository()
    }
    
    deinit {
        stopObservingChannels()
    }
    
    func needsInitializing() -> Bool {
        return repo.getCurrentUser() == nil
    }
    
    func tryLogin(email: String, password: String, success: @escaping (Bool) -> Void) {
        Api(user: email, pass: password).login(completionHandler: { result, error in
            if (error == nil) {
                success(result!.boolValue)
            } else {
                success(false)
            }
        })
    }
    
    func initAndStart(email: String, password: String) {
        repo.createCurrentUser(email: email, password: password)
        doStart(email: email, password: password)
    }
    
    func start() {
        if let currentUser = repo.getCurrentUser() {
            doStart(email: currentUser.email, password: currentUser.password)
        }
    }
    
    private func doStart(email: String, password: String) {
        self.api = Api(user: email, pass: password)
        self.repo.doInit(api: api!)

        self.repo.fetchPrograms(completionHandler: { _, error in
            if (error != nil) {
                DebugLog().e(message: "failed fetching programs, error: \(error!)")
            }
        })
        
        startObservingChannels()
    }
    
    func startObservingCategoryNames() {
        repo.startObservingCategoryNames(success: { data in
            self.categories = data
        })
    }
    func stopObservingCategoryNames() {
        repo.stopObservingCategoryNames()
    }
    
    func startObservingCategory(category: String) {
        self.programs = []
        repo.startObservingCategory(category: category, success: { data in
            self.programs = data
        })
    }
    func stopObservingCategory(category: String) {
        repo.stopObservingCategory(category: category)
    }
    
    func startObservingProgram(programUrl: String) {
        self.episodes = []
        repo.fetchEpisodes(programUrl: programUrl, completionHandler:  { _, error in
            if (error != nil) {
                DebugLog().e(message: "failed fetching episodes, error: \(error!)")
            }
        })
        repo.startObservingProgram(programUrl: programUrl, success: { data in
            self.episodes = data
        })
    }
    func stopObservingProgram(programUrl: String) {
        repo.stopObservingProgram(programUrl: programUrl)
    }
    
    private func startObservingChannels() {
        repo.startObservingChannels(success: { data in
            self.channels = data
        })
    }
    private func stopObservingChannels() {
        repo.stopObservingChannels()
    }
    
    func startObservingWatchedPrograms() {
        self.watchedPrograms = []
        repo.startObservingWatchedPrograms(success: { data in
            self.watchedPrograms = data
        })
    }
    func stopObservingWatchedPrograms() {
        repo.stopObservingWatchedPrograms()
    }
    
    func startObservingFavouritePrograms() {
        self.favouritePrograms = []
        repo.startObservingFavouritePrograms(success: { data in
            self.favouritePrograms = data
        })
    }
    func stopObservingFavouritePrograms() {
        repo.stopObservingFavouritePrograms()
    }
    
    func startObservingRecentPrograms() {
        self.recentPrograms = []
        repo.startObservingRecentPrograms(success: { data in
            self.recentPrograms = data
        })
    }
    func stopObservingRecentPrograms() {
        repo.stopObservingRecentPrograms()
    }
    
    func getMatchingPrograms(query: String) {
        self.programs = []
        repo.getMatchingPrograms(query: query, completionHandler: { programs, error in
            if (error == nil) {
                self.programs = programs!
            }
        })
    }
    
    func setProgramFavourite(program: Program, favourite: Bool) {
        repo.setProgramFavourite(programUrl: program.programUrl, favourite: favourite)
    }
    
    func getPosition(episode: Episode) -> Position? {
        return repo.getPosition(programUrl: episode.programUrl, publicationId: episode.publicationId, videoId: episode.videoId)
    }
    
    func storePosition(episode: Episode, position: Int64, duration: Int64)  {
        if (repo.getPosition(programUrl: episode.programUrl, publicationId: episode.publicationId, videoId: episode.videoId) == nil) {
            repo.insertPosition(programUrl: episode.programUrl, title: episode.title, publicationId: episode.publicationId, videoId: episode.videoId)
        }
        repo.updatePosition(programUrl: episode.programUrl, publicationId: episode.publicationId, videoId: episode.videoId, position: position, duration: duration, lastSeen: -1)
    }
    
    func getEpisode(epgEntry: EpgEntry, success: @escaping (Episode?) -> Void) {
        repo.getEpisode(epgEntry: epgEntry, completionHandler:  { episode, error in
            if (error == nil) {
                success(episode)
            }
        })
    }
    
    func getLastSeenEpisode(programUrl: String) -> Episode? {
        return repo.getWatchedEpisodesForProgram(programUrl: programUrl).first
    }
    
    func fetchPlayerInfo(publicationId: String, videoId: String) {
        playerInfo = nil
        api!.fetchInfo(publicationId: publicationId, videoId: videoId, refreshToken: false, useHls: true, completionHandler: { playerInfo, error in
            if (error == nil) {
                self.playerInfo = playerInfo
            }
        })
    }
    
    func fetchPlayerInfo(streamId: String) {
        playerInfo = nil
        api!.fetchInfo(streamId: streamId, refreshToken: false, useHls: true, completionHandler: { playerInfo, error in
            if (error == nil) {
                self.playerInfo = playerInfo
            }
        })
    }
}
