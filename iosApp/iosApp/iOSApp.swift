import GoogleSignIn
import SwiftUI
import Shared

@main
struct iOSApp: App {
    init() {
        KoinIosKt.doInitKoinIos(gmailAuthorizer: GoogleGmailAuthorizer())
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
                // Google's sign-in returns to the app through the reversed client ID URL scheme.
                .onOpenURL { url in GIDSignIn.sharedInstance.handle(url) }
        }
    }
}
