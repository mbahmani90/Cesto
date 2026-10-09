import GoogleSignIn
import Shared
import UIKit

/// "Sign in with Google" via GoogleSignIn: returns the Google ID token that `:account` exchanges for a
/// Cesto (Identity Platform) account. Its audience is the iOS client ID, which is listed under
/// "Allowed client IDs" in Identity Platform's Google provider. Gmail access is asked for afterwards
/// by `GoogleGmailAuthorizer`, which adds the scope to this same Google user.
final class GoogleIdTokenSignIn: GoogleIdTokenProvider {

    func signIn(onSuccess: @escaping (String) -> Void, onFailure: @escaping (GoogleSignInError) -> Void) {
        // GoogleSignIn raises an Objective-C exception (a crash) without a client ID.
        let clientID = Bundle.main.object(forInfoDictionaryKey: "GIDClientID") as? String ?? ""
        guard !clientID.isEmpty, let presenter = Self.topViewController() else {
            print("GoogleIdTokenSignIn: GOOGLE_IOS_CLIENT_ID missing or no screen to present on")
            onFailure(.failed)
            return
        }
        GIDSignIn.sharedInstance.signIn(withPresenting: presenter) { result, error in
            if let error = error as NSError? {
                let cancelled = error.domain == kGIDSignInErrorDomain && error.code == GIDSignInError.canceled.rawValue
                if !cancelled { print("GoogleIdTokenSignIn: sign-in failed: \(error)") }
                onFailure(cancelled ? .cancelled : .failed)
            } else if let idToken = result?.user.idToken?.tokenString {
                onSuccess(idToken)
            } else {
                onFailure(.failed)
            }
        }
    }

    private static func topViewController() -> UIViewController? {
        let window = UIApplication.shared.connectedScenes
            .compactMap { $0 as? UIWindowScene }
            .flatMap(\.windows)
            .first(where: \.isKeyWindow)
        var top = window?.rootViewController
        while let presented = top?.presentedViewController { top = presented }
        return top
    }
}
