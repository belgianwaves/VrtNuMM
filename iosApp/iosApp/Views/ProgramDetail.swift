import SwiftUI
import shared

struct ProgramDetail: View {
    @StateObject var viewModel: ViewModel
    var program: Program
    var onEpisodeSelected: (Episode) -> Void
    
    @State private var selectedEpisode: Episode?
    @State private var selectedSeason = ""
    @State private var startedPlaying = false
    
    @ViewBuilder
    var body: some View {
        VStack {
            ChipsView(values: episodeSeasonNames(episodes: viewModel.episodes), selectedValue: getSelectedSeason(episodes: viewModel.episodes), onValueSelected: { seasonName in
                selectedSeason = seasonName
            })
            List {
                ForEach(episodesOfSelectedSeason(episodes: viewModel.episodes), id: \.publicationId) { episode in
                    EpisodeRow(episode: episode, selected: episode == getSelectedEpisode(episodes: viewModel.episodes)).onTapGesture {
                        selectEpisode(episode: episode)
                    }
                }
            }
        }
        .navigationBarTitle(program.title, displayMode: .inline)
        .onAppear {
            startedPlaying = false
            selectedEpisode = viewModel.getLastSeenEpisode(programUrl: program.programUrl)
            viewModel.startObservingProgram(programUrl: program.programUrl)
        }
        .onDisappear {
            viewModel.stopObservingProgram(programUrl: program.programUrl)
        }
    }
    
    func getSelectedSeason(episodes: [Episode]) -> String {
        if (selectedSeason != "") {
            return selectedSeason
        } else if let se = getSelectedEpisode(episodes: episodes) {
            return checkSeason(seasonNumber: se.seasonName)
        } else {
            return ""
        }
    }
    
    func episodesOfSelectedSeason(episodes: [Episode]) -> [Episode] {
        let currentSelectedSeason = getSelectedSeason(episodes: viewModel.episodes)
        return episodes.filter { episode in checkSeason(seasonNumber: episode.seasonName) == currentSelectedSeason }
    }
    
    func episodeSeasonNames(episodes: [Episode]) -> [String] {
        return episodes.map { episode in checkSeason(seasonNumber: episode.seasonName) }.unique
    }
    
    func selectEpisode(episode: Episode) {
        selectedEpisode = episode
        onEpisodeSelected(episode)
    }
    
    func getSelectedEpisode(episodes: [Episode]) -> Episode? {
        var result = selectedEpisode
            if (result == nil) {
                result = episodes.first
            }
        if ((result != nil) && !startedPlaying) {
            DispatchQueue.main.async {
                if (result?.programUrl == program.programUrl) {
                    startedPlaying = true
                    onEpisodeSelected(result!)
                }
            }
        }
        return result
    }
}


func checkSeason(seasonNumber: String) -> String {
    let intValue = Int(seasonNumber)
    if (intValue != nil) {
        return "season \(String(describing: intValue!))"
    } else {
        return seasonNumber
    }
}


struct EpisodeRow: View {
    var episode: Episode
    var selected: Bool
    
    var body: some View {
        HStack {
            ImageView(withURL: episode.videoThumbnailUrl.sanitizedUrl(), width: 128, height: 80)
            VStack(alignment: .leading) {
                Text(episode.title).font(.headline).lineLimit(1).modifier(if: selected) { $0.foregroundColor(.pink) }
                Text(episode.desc.escHtml()).font(.body).foregroundColor(.secondary).lineLimit(3)
            }
        }
    }
}

//struct ProgramDetail_Previews: PreviewProvider {
//    static var previews: some View {
//        ProgramDetail()
//    }
//}
