import SwiftUI
import shared

struct ChannelDetail: View {
    @StateObject var viewModel: ViewModel
    var streamId: String
    var onChannelSelected: (Channel) -> Void
    var onEpisodeSelected: (Episode) -> Void
    
    @State private var selectedEpgEntry: EpgEntry?
    
    var channel: Channel {
        viewModel.channels.first { c in c.streamId == streamId }!
    }
    
    @ViewBuilder
    var body: some View {
        List {
            ForEach(channel.epg!, id: \.startTime) { epgEntry in
                EpgRow(epgEntry: epgEntry, selected: epgEntry.startTime == getSelectedEntry()?.startTime).onTapGesture {
                    if (epgEntry.hasVideo) {
                        viewModel.getEpisode(epgEntry: epgEntry, success: {(episode) -> Void in
                            if (episode != nil) {
                                selectedEpgEntry = epgEntry
                                onEpisodeSelected(episode!)
                            }
                        })
                    }
                }
            }
        }
        .navigationBarTitle(channel.label, displayMode: .inline)
        .onAppear {
            onChannelSelected(channel)
        }
        .onDisappear {
        }
    }
    
    func getSelectedEntry() -> EpgEntry? {
        if (selectedEpgEntry != nil) {
            return selectedEpgEntry
        } else {
            return channel.nowPlaying
        }
    }
}

struct EpgRow: View {
    var epgEntry: EpgEntry
    var selected: Bool
    
    var body: some View {
        HStack {
            ImageView(withURL: epgEntry.image.sanitizedUrl(), width: 128, height: 80)
            VStack(alignment: .leading) {
                Text(epgEntry.start).font(.caption).foregroundColor(.secondary)
                Text(epgEntry.title).font(.headline).lineLimit(1).foregroundColor(getColor())
                Text(epgEntry.subtitle.escHtml()).font(.body).foregroundColor(.secondary).lineLimit(3)
            }
        }
    }
    
    func getColor() -> Color {
        if (selected) {
            return .pink
        } else if (!epgEntry.hasVideo) {
            return .secondary
        } else {
            return .primary
        }
    }
}

//struct ChannelDetail_Previews: PreviewProvider {
//    static var previews: some View {
//        ChannelDetail()
//    }
//}
