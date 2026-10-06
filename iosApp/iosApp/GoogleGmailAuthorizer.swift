import GoogleSignIn
import Shared
import UIKit

/// Gmail permission via GoogleSignIn (Swift Package). Implements the Kotlin `GmailAuthorizer`
/// interface and is passed to Koin in `iOSApp.init()`. The access token stays on the device.
final class GoogleGmailAuthorizer: GmailAuthorizer {

    private let scope = GmailScopes.shared.GMAIL_READONLY

    func authorize(
        interactive: Bool,
        onSuccess: @escaping (String) -> Void,
        onFailure: @escaping (GmailAuthError) -> Void
    ) {
        // GoogleSignIn raises an Objective-C exception (a crash) without a client ID.
        guard Self.isConfigured else {
            print("GmailAuthorizer: GOOGLE_IOS_CLIENT_ID missing, see iosApp/Configuration/Secrets.xcconfig.example")
            onFailure(.failed)
            return
        }

        // Already signed in this session: no need to restore from the keychain again.
        if let user = GIDSignIn.sharedInstance.currentUser, hasGmailScope(user) {
            deliverToken(of: user, onSuccess: onSuccess, onFailure: onFailure)
            return
        }

        GIDSignIn.sharedInstance.restorePreviousSignIn { user, _ in
            if let user, self.hasGmailScope(user) {
                self.deliverToken(of: user, onSuccess: onSuccess, onFailure: onFailure)
            } else if !interactive {
                onFailure(.notGranted)
            } else {
                self.showConsent(signedInUser: user, onSuccess: onSuccess, onFailure: onFailure)
            }
        }
    }

    /// Signed in before but without Gmail: only ask for the missing scope. Otherwise full sign-in.
    private func showConsent(
        signedInUser: GIDGoogleUser?,
        onSuccess: @escaping (String) -> Void,
        onFailure: @escaping (GmailAuthError) -> Void
    ) {
        guard let presenter = Self.topViewController() else {
            onFailure(.failed)
            return
        }
        let completion: (GIDSignInResult?, Error?) -> Void = { result, error in
            if let error = error as NSError? {
                let cancelled = error.domain == kGIDSignInErrorDomain && error.code == GIDSignInError.canceled.rawValue
                if !cancelled { print("GmailAuthorizer: sign-in failed: \(error)") }
                onFailure(cancelled ? .cancelled : .failed)
            } else if let user = result?.user, self.hasGmailScope(user) {
                self.deliverToken(of: user, onSuccess: onSuccess, onFailure: onFailure)
            } else {
                onFailure(.notGranted) // consent confirmed with the Gmail box unticked
            }
        }
        if let signedInUser {
            signedInUser.addScopes([scope], presenting: presenter, completion: completion)
        } else {
            GIDSignIn.sharedInstance.signIn(
                withPresenting: presenter,
                hint: nil,
                additionalScopes: [scope],
                completion: completion
            )
        }
    }

    private func deliverToken(
        of user: GIDGoogleUser,
        onSuccess: @escaping (String) -> Void,
        onFailure: @escaping (GmailAuthError) -> Void
    ) {
        user.refreshTokensIfNeeded { user, error in
            if let token = user?.accessToken.tokenString, error == nil {
                onSuccess(token)
            } else {
                print("GmailAuthorizer: token refresh failed: \(String(describing: error))")
                onFailure(.failed)
            }
        }
    }

    private func hasGmailScope(_ user: GIDGoogleUser) -> Bool {
        user.grantedScopes?.contains(scope) == true
    }

    private static var isConfigured: Bool {
        let clientID = Bundle.main.object(forInfoDictionaryKey: "GIDClientID") as? String ?? ""
        return !clientID.isEmpty
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
