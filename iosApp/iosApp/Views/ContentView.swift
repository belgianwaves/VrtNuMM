import SwiftUI
import AVFoundation
import shared

struct ContentView: View {
    @StateObject var viewModel = ViewModel()
    private (set) var player = FairPlayer()
    
    @State private var selectedEpisode: Episode?
    @State private var selectedChannel: Channel?
    
    @State private var orientation = UIDevice.current.orientation
    @State private var selectedHomeCategory = HomeCategory.Discover
    
    let orientationChanged = NotificationCenter.default.publisher(for: UIDevice.orientationDidChangeNotification)
        .makeConnectable()
        .autoconnect()
    let timer = Timer.publish(every: 1, on: .main, in: .common).autoconnect()
    
    var body: some View {
        VStack {
            if (orientation.isLandscape) {
                if (playerInfo != nil) {
                    PlayerViewWithOverlay(playerInfo: playerInfo!, player: player, onDemand: selectedEpisode != nil)
                }
            } else {
                GeometryReader { geo in
                    VStack(alignment: .leading) {
                        if (playerInfo != nil) {
                            PlayerViewWithOverlay(playerInfo: playerInfo!, player: player, onDemand: selectedEpisode != nil)
                                .frame(width: geo.size.width, height: geo.size.width*9/16)
                        } else {
                            ImageView(withURL: getImageUrl(), width: geo.size.width, height: geo.size.width*9/16)
                        }
                        NavigationView {
                            VStack {
                                HomeCategoryRow(categories: HomeCategory.allCases, selectedCategory: self.selectedHomeCategory, onCategorySelected: {(category) -> Void in
                                    self.selectedHomeCategory = category
                                }).frame(width: geo.size.width, height: 40, alignment: .center)
                                
                                switch(self.selectedHomeCategory) {
                                case .Library: LibraryView(viewModel: viewModel, onEpisodeSelected: onEpisodeSelected(episode:))
                                    .transition(AnyTransition.opacity.animation(.easeInOut(duration: 0.7)))
                                case .Discover: DiscoverView(viewModel: viewModel, onEpisodeSelected: onEpisodeSelected(episode:))
                                    .transition(AnyTransition.opacity.animation(.easeInOut(duration: 0.7)))
                                case .Live: LiveView(viewModel: viewModel, onChannelSelected: onChannelSelected(channel:), onEpisodeSelected: onEpisodeSelected(episode:))
                                    .transition(AnyTransition.opacity.animation(.easeInOut(duration: 0.7)))
                                default: SearchView(viewModel: viewModel, onEpisodeSelected: onEpisodeSelected(episode:))
                                    .transition(AnyTransition.opacity.animation(.easeInOut(duration: 0.7)))
                                }
                            }.navigationBarHidden(true)
                        }
                    }
                }
            }
        }
        .onReceive(timer, perform: { _ in
            storeCurrentPosition()
        })
        .modifier(if: orientation.isLandscape) { $0.navigationBarTitle("", displayMode: .inline).navigationBarHidden(true).edgesIgnoringSafeArea(.all).prefersHomeIndicatorAutoHidden(true) }
            else: { $0.navigationBarTitle("VrtNu", displayMode: .inline).prefersHomeIndicatorAutoHidden(false) }
        .onReceive(orientationChanged) { _ in
            self.orientation = UIDevice.current.orientation
        }
        .onAppear {
            if (viewModel.needsInitializing()) {
                showAlert()
            } else {
                viewModel.start()
            }
        }
    }
    
    var playerInfo: PlayerInfo? {
        let result = viewModel.playerInfo
            if (result != nil) {
                result!.position = Int64(getStoredPosition())
            }
        return result
    }
    
    func storeCurrentPosition() {
        if let se = self.selectedEpisode {
            if let currentItem = player.currentItem {
                let d = currentItem.duration.seconds
                if (d.isAcceptable) {
                    let position = Int64(currentItem.currentTime().seconds)
                    let duration = Int64(d)                    
                    viewModel.storePosition(episode: se, position: position, duration: duration)
                }
            }
        }
    }
    
    func getStoredPosition() -> Int {
        var position: Position? =  nil
            if let episode = self.selectedEpisode {
                position = viewModel.getPosition(episode: episode)
            }
        if (position != nil) {
            return Int(truncating: NSNumber(value: position!.position))
        } else {
            return 0
        }
    }
    
    func onEpisodeSelected(episode: Episode) {
        if (selectedEpisode != episode) {
            storeCurrentPosition()
            
            selectedChannel = nil
            selectedEpisode = episode
            
            viewModel.fetchPlayerInfo(publicationId: episode.publicationId, videoId: episode.videoId)
        }
    }
    
    func onChannelSelected(channel: Channel) {
        if (selectedChannel != channel) {
            storeCurrentPosition()
            
            selectedEpisode = nil
            selectedChannel = channel
            
            viewModel.fetchPlayerInfo(streamId: channel.streamId)
        }
    }
    
    func getImageUrl() -> String {
        if (selectedEpisode != nil) {
            return selectedEpisode!.videoThumbnailUrl.sanitizedUrl(width: 640)
        } else if (selectedChannel != nil) {
            return selectedChannel!.thumbnail.sanitizedUrl(width: 640)
        } else {
            return ""
        }
    }
    
    func showAlert() {
        let alertHC = UIHostingController(rootView: MyAlert(viewModel: viewModel))
            alertHC.preferredContentSize = CGSize(width: 300, height: 400)
            alertHC.modalPresentationStyle = UIModalPresentationStyle.formSheet
        UIApplication.shared.windows[0].rootViewController?.present(alertHC, animated: true)
    }
}

struct HomeCategoryRow: View {
    var categories: [HomeCategory]
    var selectedCategory: HomeCategory
    var onCategorySelected: (HomeCategory) -> Void
    
    var body: some View {
        HStack {
            ForEach(categories, id: \.self) { category in
                VStack {
                    Text(category.description)
                        .modifier(if: category == selectedCategory) { $0.overlay(Rectangle().frame(height: 1).offset(y: 4).foregroundColor(.pink), alignment: .bottom)}
                }.frame(minWidth: 0, maxWidth: .infinity).onTapGesture { onCategorySelected(category) }
            }
        }
    }
}

enum HomeCategory: CaseIterable {
    case Library, Discover, Live, Search
    
    var description : String {
        switch self {
            case .Library: return "Library"
            case .Discover: return "Discover"
            case .Live: return "Live"
            default: return "Search"
        }
      }
}

struct MyAlert: View {
    @StateObject var viewModel: ViewModel
    
    @State private var email: String = ""
    @State private var password: String = ""

    var body: some View {
        VStack {
            Text("VRT NU Login").font(.title).padding()
            
            Text("Please provide your VRT NU credentials to use this application")

            TextField("email", text: $email)
                .autocapitalization(.none)
                .padding()
            SecureField("password", text: $password)
                .autocapitalization(.none)
                .padding()
            
            HStack {
                Spacer()
                Button(action: {
                    viewModel.tryLogin(email: email, password: password, success: { success in
                        if (success) {
                            viewModel.initAndStart(email: email, password: password)
                            UIApplication.shared.windows[0].rootViewController?.dismiss(animated: true, completion: {})
                        } else {
                            AudioServicesPlayAlertSound(SystemSoundID(1322))
                            password = ""
                        }
                    })
                }) {

                    Text("Accept")
                }
            }
            Spacer()
        }.padding().frame(width: 300, height:400)
    }
}


//struct ContentView_Previews: PreviewProvider {
//    static var previews: some View {
//        ContentView()
//    }
//}
