import Foundation
import Cyborg
import SwiftUI


class Theme: Cyborg.ThemeProviding {
    func colorFromTheme(named _: String) -> UIColor {
        return .white
    }

}

class Resources: ResourceProviding {
    func colorFromResources(named _: String) -> UIColor {
        return .white
    }

}
