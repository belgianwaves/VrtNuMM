import SwiftUI
import shared

struct LibraryView: View {
    @StateObject var viewModel: ViewModel
    var onEpisodeSelected: (Episode) -> Void
    
    var body: some View {
        GeometryReader { geo in
            List {
                if (viewModel.watchedPrograms.count > 0) {
                    VStack(alignment: .leading) {
                        Text("Continue Watching").font(.title3)
                        ScrollView(.horizontal, showsIndicators: false) {
                            HStack {
                                ForEach(viewModel.watchedPrograms, id: \.programUrl) { program in
                                    NavigationLink(destination: ProgramDetail(viewModel: viewModel, program: program, onEpisodeSelected: onEpisodeSelected)) {
                                        MiniProgramCard(program: program)
                                    }
                                }
                            }
                        }
                    }
                }
                
                if (viewModel.favouritePrograms.count > 0) {
                    VStack(alignment: .leading) {
                        Text("Favourites").font(.title3)
                        ScrollView(.horizontal, showsIndicators: false) {
                            HStack {
                                ForEach(viewModel.favouritePrograms, id: \.programUrl) { program in
                                    NavigationLink(destination: ProgramDetail(viewModel: viewModel, program: program, onEpisodeSelected: onEpisodeSelected)) {
                                        MiniProgramCard(program: program)
                                    }
                                }
                            }
                        }
                    }
                }
                
                if (viewModel.recentPrograms.count > 0) {
                    Text("Recently Added").font(.title3)
                    ForEach(viewModel.recentPrograms, id: \.programUrl) { program in
                        NavigationLink(destination: ProgramDetail(viewModel: viewModel, program: program, onEpisodeSelected: onEpisodeSelected)) {
                            ProgramCard(program: program, width: geo.size.width, onFavourite: {
                                viewModel.setProgramFavourite(program: program, favourite: !program.favourite)
                            })
                        }
                    }
                }
            }.listStyle(PlainListStyle())
        }
        .onAppear {
            viewModel.startObservingWatchedPrograms()
            viewModel.startObservingFavouritePrograms()
            viewModel.startObservingRecentPrograms()
        }
        .onDisappear {
            viewModel.stopObservingRecentPrograms()
            viewModel.stopObservingFavouritePrograms()
            viewModel.stopObservingWatchedPrograms()
        }
    }
    
}

struct MiniProgramCard: View {
    var program: Program
    
    var body: some View {
        VStack {
            ImageView(withURL: program.thumbnail.sanitizedUrl(), width: 128, height: 80)
            Text(program.title).foregroundColor(.primary).frame(width: 128).lineLimit(1)
        }
    }
}

struct ProgramCard: View {
    var program: Program
    var width: CGFloat
    var onFavourite: () -> Void
    
    var body: some View {
        VStack(alignment: .leading) {
            ImageView(withURL: program.thumbnail.sanitizedUrl(width: 640), width: width, height: width*9/16, radius: 10)
            HStack {
                VStack(alignment: .leading) {
                    Text(program.title).font(.headline).lineLimit(1)
                    Text(program.desc.escHtml()).font(.body).foregroundColor(.secondary).lineLimit(3)
                }
                .frame(width: width*8/10, alignment: .leading)
                Image(systemName: imageName())
                .imageScale(.medium)
                .foregroundColor(.pink)
                .frame(width: width*2/10, alignment: .leading)
                .onTapGesture {
                    onFavourite()
                }
            }
        }
    }
    
    func imageName() -> String {
        if (program.favourite) {
            return "heart.fill"
        } else {
            return "heart"
        }
    }
}

//struct LibraryView_Previews: PreviewProvider {
//    static var previews: some View {
//        LibraryView()
//    }
//}
