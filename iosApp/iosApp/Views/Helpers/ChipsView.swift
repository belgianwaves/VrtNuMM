import SwiftUI

struct ChipsView: View {
    var values: [String]
    var selectedValue: String
    var onValueSelected: (String) -> Void
    
    var body: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack {
                ForEach(values, id: \.self) { value in
                    Button(action: {onValueSelected(value)}) {
                        Text(value)
                        .padding(.leading, 10)
                        .padding(.trailing, 10)
                        .padding(.top, 5)
                        .padding(.bottom, 5)
                        .font(.headline)
                        .foregroundColor(getChipColor(selected: value == selectedValue)).overlay(RoundedRectangle(cornerRadius: 5).stroke(getChipColor(selected: value == selectedValue), lineWidth: 0.5))
                        .padding(5)
                    }
                }
            }
        }
    }
    
    func getChipColor(selected: Bool) -> Color {
        if (selected) {
            return .pink
        } else {
            return .primary
        }
    }
}

//struct ChipsView_Previews: PreviewProvider {
//    static var previews: some View {
//        ChipsView()
//    }
//}
