import SwiftUI
import shared

struct SearchView: View {
    @StateObject var viewModel: ViewModel
    var onEpisodeSelected: (Episode) -> Void
    
    @State var query: String = ""
    
    var body: some View {
        VStack(alignment: .leading) {
            TextField("Enter query...", text: $query).onChange(of: query) {
                viewModel.getMatchingPrograms(query: $0)
            }
            List {
                ForEach(viewModel.programs, id: \.programUrl) { program in
                    ZStack {
                        ProgramRow(program: program)
                        NavigationLink(destination: ProgramDetail(viewModel: viewModel, program: program, onEpisodeSelected: onEpisodeSelected)) {
                            
                        }.frame(width: 0).opacity(0.0)
                    }
                }
            }
            .listStyle(PlainListStyle())
        }.onAppear {
            viewModel.getMatchingPrograms(query: query)
        }
    }
}

//struct SearchView_Previews: PreviewProvider {
//    static var previews: some View {
//        SearchView()
//    }
//}
