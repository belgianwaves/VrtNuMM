import SwiftUI
import shared

struct DiscoverView: View {
    @StateObject var viewModel: ViewModel
    var onEpisodeSelected: (Episode) -> Void
    
    @State private var selectedCategory: String = "series"
    
    var body: some View {
        VStack {
            ChipsView(values: viewModel.categories, selectedValue: selectedCategory, onValueSelected: { category in
                viewModel.stopObservingCategory(category: selectedCategory)
                selectedCategory = category
                viewModel.startObservingCategory(category: selectedCategory)
            })
        
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
        }
        .onAppear {
            viewModel.startObservingCategoryNames()
            viewModel.startObservingCategory(category: selectedCategory)
        }.onDisappear {
            viewModel.stopObservingCategory(category: selectedCategory)
            viewModel.stopObservingCategoryNames()
        }
    }
}

struct ProgramRow: View {
    var program: Program
    
    var body: some View {
        HStack {
            ImageView(withURL: program.thumbnail.sanitizedUrl(), width: 128, height: 80)
            VStack(alignment: .leading) {
                Text(program.title).font(.headline).lineLimit(1)
                Text(program.desc.escHtml()).font(.body).foregroundColor(.secondary).lineLimit(3)
            }
        }
    }
}

//struct DiscoverView_Previews: PreviewProvider {
//    static var previews: some View {
//        DiscoverView()
//    }
//}
