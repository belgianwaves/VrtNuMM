import SwiftUI
import shared
import Cyborg

struct PlayerViewWithOverlay: View {
    var playerInfo: PlayerInfo
    var player: FairPlayer
    var onDemand: Bool = true
    
    @State private var overlayVisible: Bool = false
    @State private var isPlaying: Bool = true
    @State private var position: Double = 0.0
    @State private var duration: Double = 0.0
    @State private var currentProgress: Double = 0.0
    
    let timer = Timer.publish(every: 1, on: .main, in: .common).autoconnect()
    
    var body: some View {
        ZStack {
            PlayerView(playerInfo: playerInfo, player: player).onTapGesture {
                overlayVisible = !overlayVisible
            }
            if (overlayVisible) {
                if (onDemand) {
                    HStack {
                        Button(action: { isPlaying = player.rewind() } ) {
                            VectorDrawableView(res: "ic_replay").frame(width: 48, height: 48)
                        }.padding(10)
                        Button(action: { isPlaying = player.togglePlay() } ) {
                            VectorDrawableView(res: playIcon() ).frame(width: 48, height: 48)
                        }.padding(10)
                        Button(action: { isPlaying = player.fastForward() } ) {
                            VectorDrawableView(res: "ic_ffwd").frame(width: 48, height: 48)
                        }.padding(10)
                    }
                    VStack {
                        GeometryReader { geo in
                            VStack(alignment: .leading) {
                                Spacer()
                                HStack {
                                    Text(getFormattedString(value: position))
                                    Text("/")
                                    Text(getFormattedString(value: duration))
                                }
                                Slider(value: $currentProgress, in: 0.0...100.0)
                                    .gesture(DragGesture(minimumDistance: 0)
                                    .onChanged({ value in
                                        let coefficient = abs(100.0 / Double(geo.size.width))
                                        player.seekRelative(progress: Double(value.location.x) * coefficient)
                                    }))
                            }
                        }
                    }.padding(10)
                } else {
                    Button(action: { isPlaying = player.togglePlay() } ) {
                        VectorDrawableView(res: playIcon() ).frame(width: 48, height: 48)
                    }.padding(10)
                }
            }
        }
        .onReceive(timer, perform: { _ in
            if (onDemand) {
                adjustInfo()
            }
        })
    }
    
    func adjustInfo() {
        if let currentItem = player.currentItem {
            let d = currentItem.duration.seconds
            if (d.isAcceptable) {
                position = currentItem.currentTime().seconds
                duration = d
                
                currentProgress = position * 100.0 / duration
            }
        }
    }
    
    func getFormattedString(value: Double) -> String {
        let secondsString = String(format: "%02d", Int((value.truncatingRemainder(dividingBy: 60))))
        let minutesString = String(format: "%02d", Int(value) / 60)
        
        return "\(minutesString):\(secondsString)"
    }
    
    func playIcon() -> String {
        if (isPlaying) {
            return "ic_pause"
        } else {
            return "ic_play"
        }
    }
}

//struct VideoView_Previews: PreviewProvider {
//    static var previews: some View {
//        VideoView()
//    }
//}
