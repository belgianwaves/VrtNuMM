import SwiftUI
import AVKit
import shared

struct PlayerView: UIViewRepresentable {
    var playerInfo: PlayerInfo
    var player: FairPlayer
    
    func updateUIView (_ uiView: UIView, context: UIViewRepresentableContext<PlayerView>) {
    }
    func makeUIView (context: Context)->UIView {
        return PlayerUIView (playerInfo: playerInfo, player: player, frame: .zero)
    }
}

class PlayerUIView: UIView {
    private let playerLayer = AVPlayerLayer ()
    
    init (playerInfo: PlayerInfo, player: FairPlayer, frame: CGRect) {
        super.init (frame: frame)
        
        let newUrl = URL(string: playerInfo.url)!
        if (urlOfCurrentlyPlayingInPlayer(player: player) != newUrl) {
            player.play(asset: AVURLAsset(url: newUrl), drmToken: playerInfo.drmToken, contentId: playerInfo.contentId)
        }
        
        
        if (playerInfo.position > 0) {
            player.seek(to: CMTimeMakeWithSeconds(Float64(playerInfo.position), preferredTimescale: 600))
        }
        
        playerLayer.player = player
        playerLayer.videoGravity = .resizeAspect
        layer.addSublayer (playerLayer)
    }
    required init? (coder: NSCoder) {
        fatalError ("init (coder :) has not been implemented")
    }
    override func layoutSubviews () {
        super.layoutSubviews ()
        playerLayer.frame = bounds
    }
}
