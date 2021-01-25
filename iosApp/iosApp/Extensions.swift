import Foundation
import SwiftUI
import AVKit
import shared
import Cyborg

extension String {
    func sanitizedUrl(width: Int = 320) -> String {
        var url = self
            if (!url.hasPrefix("https:"))  {
                url = "https:\(url)"
            }
            if (url.contains("orig")) {
                url = url.replacingOccurrences(of: "orig", with: "w\(width)hx")
            }
        return url
    }
    
    func escHtml() -> String {
        var esc = self.replacingOccurrences(of: "&nbsp;", with: " ")
            esc  = esc.replacingOccurrences(of: "<p>", with: "")
            esc = esc.replacingOccurrences(of: "</p>", with: "")
            esc = esc.replacingOccurrences(of: "<br>", with: "").trimmingCharacters(in: .whitespacesAndNewlines)
        return esc
    }
}

extension View {
    @ViewBuilder func modifier<T: View>(
        if condition: @autoclosure () -> Bool,
        then content: (Self) -> T
    ) -> some View {
        if condition() {
            content(self)
        } else {
            self
        }
    }
    
    @ViewBuilder func modifier<TrueContent: View, FalseContent: View>(
        if condition: @autoclosure () -> Bool,
        then trueContent: (Self) -> TrueContent,
        else falseContent: (Self) -> FalseContent
    ) -> some View {
        if condition() {
            trueContent(self)
        } else {
            falseContent(self)
        }
    }
}

extension Array where Element : Equatable {
    var unique: [Element] {
        var uniqueValues: [Element] = []
        forEach { item in
            if !uniqueValues.contains(item) {
                uniqueValues += [item]
            }
        }
        return uniqueValues
    }
}

func urlOfCurrentlyPlayingInPlayer(player : AVPlayer) -> URL? {
    return ((player.currentItem?.asset) as? AVURLAsset)?.url
}

extension URLSession {
    func synchronousDataTask(with url: URL) -> (Data?, URLResponse?, Error?) {
        var data: Data?
        var response: URLResponse?
        var error: Error?

        let semaphore = DispatchSemaphore(value: 0)

        let task = self.dataTask(with: url) {
            data = $0
            response = $1
            error = $2

            semaphore.signal()
        }
        task.resume()

        _ = semaphore.wait(timeout: .distantFuture)

        return (data, response, error)
    }
}

extension Double {
    var isAcceptable: Bool  {
        return (!self.isInfinite && !self.isNaN)
    }
}

extension AVPlayer {
    func togglePlay() -> Bool {
        if (self.rate == 0) {
            self.play()
        } else {
            self.pause()
        }
        return self.rate != 0
    }
    
    func fastForward(moveForward: Float64 = 10) -> Bool {
        if let duration  = self.currentItem?.duration {
            let playerCurrentTime = CMTimeGetSeconds(self.currentTime())
            let newTime = playerCurrentTime + moveForward
            if newTime < CMTimeGetSeconds(duration) {
                let selectedTime: CMTime = CMTimeMake(value: Int64(newTime * 1000 as Float64), timescale: 1000)
                self.pause()
                self.seek(to: selectedTime)
                self.play()
            }
        }
        
        return self.rate != 0
    }

    func rewind(moveBackward: Float64 = 10) -> Bool {
        let playerCurrenTime = CMTimeGetSeconds(self.currentTime())
        var newTime = playerCurrenTime - moveBackward
            if newTime < 0 {
                newTime = 0
            }
        self.pause()
        let selectedTime: CMTime = CMTimeMake(value: Int64(newTime * 1000 as Float64), timescale: 1000)
        self.seek(to: selectedTime)
        self.play()
        
        return self.rate != 0
    }
    
    func seekRelative(progress: Double) {
        if let duration  = self.currentItem?.duration {
            let position = progress*duration.seconds/100.0
            self.pause()
            self.seek(to: CMTimeMakeWithSeconds(Float64(position), preferredTimescale: 600))
            self.play()
        }
    }
}

extension Episode {
    func urlAndTitle() -> String {
        return "\(self.programUrl)/\(self.title)"
    }
}

struct Landscape<Content>: View where Content: View {
    let content: () -> Content
    let height = UIScreen.main.bounds.width //toggle width height
    let width = UIScreen.main.bounds.height
    var body: some View {
        content().previewLayout(PreviewLayout.fixed(width: width, height: height))
    }
}

extension View {
    /// Controls the application's preferred home indicator auto-hiding when this view is shown.
    func prefersHomeIndicatorAutoHidden(_ value: Bool) -> some View {
        preference(key: PreferenceUIHostingController.PrefersHomeIndicatorAutoHiddenPreferenceKey.self, value: value)
    }
    
    /// Controls the application's preferred screen edges deferring system gestures when this view is shown. Default is UIRectEdgeNone.
    func edgesDeferringSystemGestures(_ edge: UIRectEdge) -> some View {
        preference(key: PreferenceUIHostingController.PreferredScreenEdgesDeferringSystemGesturesPreferenceKey.self, value: edge)
    }
}

class PreferenceUIHostingController: UIHostingController<AnyView> {
    init<V: View>(wrappedView: V) {
        let box = Box()
        super.init(rootView: AnyView(wrappedView
            .onPreferenceChange(PrefersHomeIndicatorAutoHiddenPreferenceKey.self) {
                box.value?._prefersHomeIndicatorAutoHidden = $0
            }
            .onPreferenceChange(PreferredScreenEdgesDeferringSystemGesturesPreferenceKey.self) {
                box.value?._preferredScreenEdgesDeferringSystemGestures = $0
            }
        ))
        box.value = self
    }

    @objc required dynamic init?(coder aDecoder: NSCoder) {
        super.init(coder: aDecoder)
    }
    
    private class Box {
        weak var value: PreferenceUIHostingController?
        init() {}
    }
    
    // MARK: Prefers Home Indicator Auto Hidden
    
    fileprivate struct PrefersHomeIndicatorAutoHiddenPreferenceKey: PreferenceKey {
        typealias Value = Bool
        
        static var defaultValue: Value = false
        
        static func reduce(value: inout Value, nextValue: () -> Value) {
            value = nextValue() || value
        }
    }
    
    private var _prefersHomeIndicatorAutoHidden = false {
        didSet { setNeedsUpdateOfHomeIndicatorAutoHidden() }
    }
    override var prefersHomeIndicatorAutoHidden: Bool {
        _prefersHomeIndicatorAutoHidden
    }
    
    // MARK: Preferred Screen Edges Deferring SystemGestures
    
    fileprivate struct PreferredScreenEdgesDeferringSystemGesturesPreferenceKey: PreferenceKey {
        typealias Value = UIRectEdge
        
        static var defaultValue: Value = []
        
        static func reduce(value: inout Value, nextValue: () -> Value) {
            value.formUnion(nextValue())
        }
    }
    
    private var _preferredScreenEdgesDeferringSystemGestures: UIRectEdge = [] {
        didSet { setNeedsUpdateOfScreenEdgesDeferringSystemGestures() }
    }
    override var preferredScreenEdgesDeferringSystemGestures: UIRectEdge {
        _preferredScreenEdgesDeferringSystemGestures
    }
}

protocol HasApply { }

extension HasApply {
    func apply(closure:(Self) -> ()) -> Self {
        closure(self)
        return self
    }
}

extension Optional where Wrapped: Collection {
    var isNilOrEmpty: Bool {
        return self?.isEmpty ?? true
    }
}

extension VectorDrawableView {
    public init(res: String) {
        self.init(VectorDrawable.named(res)!)
    }
}

extension VectorDrawable {
    public static func named(_ name: String) -> VectorDrawable? {
        return Bundle.main.url(forResource: name, withExtension: "xml").flatMap { url in
            switch VectorDrawable.create(from: url) {
                case .ok(let drawable):
                    return drawable
                case .error(let error):
                   NSLog("Could not create a VectorDrawable named \(name), the error was \(error)")
                   return nil
            }
        }
    }
}
