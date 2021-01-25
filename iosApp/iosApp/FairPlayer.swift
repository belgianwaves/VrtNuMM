import AVKit
import Foundation

extension URLSessionConfiguration: HasApply {}

class FairPlayer: AVPlayer {
    private var drmToken: String?
    private var contentId: String?

    private let queue = DispatchQueue(label: "com.bw.fairplay.queue")

    func play(asset: AVURLAsset, drmToken: String? = nil, contentId: String? = nil) {
        self.drmToken = drmToken
        self.contentId = contentId
        
        if (!drmToken.isNilOrEmpty) {
            asset.resourceLoader.setDelegate(self, queue: queue)
        }

        let item = AVPlayerItem(asset: asset)
        replaceCurrentItem(with: item)

        play()
    }
}

extension FairPlayer: AVAssetResourceLoaderDelegate {
    func resourceLoader(_: AVAssetResourceLoader, shouldWaitForLoadingOfRequestedResource loadingRequest: AVAssetResourceLoadingRequest) -> Bool {
        guard loadingRequest.request.url != nil else {
            loadingRequest.finishLoading(with: NSError(domain: "com.bw.error", code: -1, userInfo: nil))
            return false
        }

        let drmCertUrl = URL(string: "https://fairplay-license.vudrm.tech/certificate")!
        let configuration = URLSessionConfiguration.default.apply {
            $0.httpAdditionalHeaders = [
                "x-vudrm-token": "\(drmToken!)",
            ]
        }

        var session = URLSession(configuration: configuration)
        let (certificateData, _, _) = session.synchronousDataTask(with: drmCertUrl)
        if (certificateData == nil) {
            print("🔑", #function, "Unable to load certificate")
            loadingRequest.finishLoading(with: NSError(domain: "com.bw.error", code: -2, userInfo: nil))
            return false
        }

        guard
            let contentIdData = contentId!.data(using: String.Encoding.utf8),
            let spcData = try? loadingRequest.streamingContentKeyRequestData(forApp: certificateData!, contentIdentifier: contentIdData, options: nil),
            let dataRequest = loadingRequest.dataRequest
        else {
            print("🔑", #function, "Unable to read the SPC data")
            loadingRequest.finishLoading(with: NSError(domain: "com.bw.error", code: -3, userInfo: nil))
            return false
        }

        struct UploadData: Codable {
            let token: String
            let contentId: String
            let payload: String
        }

        let uploadData = UploadData(token: drmToken!, contentId: contentId!, payload: spcData.base64EncodedString())
        guard let jsonData = try? JSONEncoder().encode(uploadData) else {
            return false
        }

        var request = URLRequest(url: URL(string: "https://fairplay-license.vudrm.tech/license")!)
            request.httpMethod = "POST"
            request.setValue("application/json", forHTTPHeaderField: "Content-Type")
            request.httpBody = jsonData
        session = URLSession(configuration: URLSessionConfiguration.default)
        let task = session.dataTask(with: request) { data, _, _ in
            if let data = data {
                dataRequest.respond(with: data)
                loadingRequest.finishLoading()
            } else {
                print("🔑", #function, "Unable to fetch the CKC")
                loadingRequest.finishLoading(with: NSError(domain: "com.bw.error", code: -4, userInfo: nil))
            }
        }
        task.resume()

        return true
    }
}
