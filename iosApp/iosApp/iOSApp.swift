import GoogleSignIn
import SwiftUI
import Shared

@main
struct iOSApp: App {
    init() {
        // From Secrets.xcconfig through Info.plist; empty when not set up (sign-in then fails, the app still runs).
        let apiKey = Bundle.main.object(forInfoDictionaryKey: "IdentityPlatformApiKey") as? String ?? ""
        KoinIosKt.doInitKoinIos(
            gmailAuthorizer: GoogleGmailAuthorizer(),
            googleIdTokenProvider: GoogleIdTokenSignIn(),
            identityPlatformApiKey: apiKey
        )
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
                // Google's sign-in returns to the app through the reversed client ID URL scheme.
                .onOpenURL { url in GIDSignIn.sharedInstance.handle(url) }
        }
    }
}
