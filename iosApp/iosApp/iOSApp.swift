import SwiftUI

@main
struct iOSApp: App {
    var body: some Scene {
        WindowGroup {
            ContentView()
                .onOpenURL { url in
                    // Forward deep links to the shared KMP layer via NSUserDefaults.
                    // The Compose Multiplatform App composable reads this on next
                    // composition, matching the Android deep-link flow.
                    UserDefaults.standard.set(url.absoluteString, forKey: "deepLinkUrl")
                    NotificationCenter.default.post(name: .init("DeepLinkReceived"), object: nil)
                }
        }
    }
}