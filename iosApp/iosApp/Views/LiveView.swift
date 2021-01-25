import SwiftUI
import shared

struct LiveView: View {
    @StateObject var viewModel: ViewModel
    var onChannelSelected: (Channel) -> Void
    var onEpisodeSelected: (Episode) -> Void
    
    var body: some View {
        List {
            ForEach(viewModel.channels, id: \.streamId) { channel in
                ZStack {
                    ChannelRow(channel: channel)
                    NavigationLink(destination: ChannelDetail(viewModel: viewModel, streamId: channel.streamId, onChannelSelected: onChannelSelected, onEpisodeSelected: onEpisodeSelected)) {
                        
                    }.frame(width: 0).opacity(0.0)
                }
            }
        }
        .listStyle(PlainListStyle())
        .onAppear {
        }
        .onDisappear {
        }
    }
}

struct ChannelRow: View {
    var channel: Channel
    
    var body: some View {
        HStack {
            ImageView(withURL: timeStampedThumbnailUrl(), width: 128, height: 80)
            VStack(alignment: .leading) {
                Text(channel.label).font(.headline).lineLimit(1)
                Text(nowPlayingLabel()).font(.body).foregroundColor(.secondary).lineLimit(3)
            }
        }
    }
    
    func timeStampedThumbnailUrl() -> String {
        return channel.thumbnail.sanitizedUrl(width: 640) + "?timestamp=" + String(Int(NSDate().timeIntervalSince1970 * 1000))
    }
    
    func nowPlayingLabel() -> String {
        var result = "Live"
            let nowPlaying = channel.nowPlaying
            if (nowPlaying != nil) {
                result = nowPlaying!.title
            }
        return result
    }
}

//struct LiveView_Previews: PreviewProvider {
//    static var previews: some View {
//        LiveView()
//    }
//}
