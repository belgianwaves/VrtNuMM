import SwiftUI
import SDWebImageSwiftUI

struct ImageView: View {
    var url: String
    var width: CGFloat
    var height: CGFloat
    var radius: CGFloat

    init(withURL url:String, width: CGFloat, height: CGFloat, radius: CGFloat = 5) {
        self.url = url
        self.width = width
        self.height = height
        self.radius = radius
    }

    var body: some View {
        VStack {
            WebImage(url: URL(string: url))
                .resizable()
                .scaledToFill()
                .frame(width: width, height: height)
                .clipped()
                .cornerRadius(radius)
        }
    }
}
