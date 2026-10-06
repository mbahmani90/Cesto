This is a Kotlin Multiplatform project targeting Android, iOS.

* [/iosApp](./iosApp/iosApp) contains an iOS application. Even if you’re sharing your UI with Compose Multiplatform,
  you need this entry point for your iOS app. This is also where you should add SwiftUI code for your project.

* [/androidApp](./androidApp) is the Android entry point (`MainActivity`).
* [/app](./app/src) is the composition root shared by Android and iOS: `App()`, navigation and DI startup.
  It also builds the iOS framework (`Shared`).
  - [commonMain](./app/src/commonMain/kotlin) is for code that’s common for all targets.
  - [androidMain](./app/src/androidMain/kotlin) and [iosMain](./app/src/iosMain/kotlin) are for platform-specific code.
* [/systemdesign](./systemdesign/src) is the design system: `CestoTheme` (light + dark colour schemes) and components used by 2+ features.
* [/feature/onboarding](./feature/onboarding/src) is the first screen: what Cesto reads, "Connect Gmail" and "Try demo".
* [/feature/receipts](./feature/receipts/src) will list the receipts (placeholder for now).
* [/gmail-auth](./gmail-auth/src) is the Gmail permission interface (`GmailAuthorizer`), implemented by the platform apps:
  Android `AuthorizationClient` in `androidApp`, iOS `GoogleSignIn` (Swift Package) in `iosApp`.
* [/core](./core/src) is non-UI code shared by features: the Ktor `HttpClient` (OkHttp / Darwin engine) and its Koin module.

### Setup: Gmail permission

The app asks for read-only Gmail access (`gmail.readonly`) with Google's own dialog; the token stays on the phone.
It needs OAuth clients in a Google Cloud project (consent screen in *Testing* mode, your account as a test user,
Gmail API enabled):

- **Android:** an Android OAuth client for package `com.majidbahmani.cesto` and your debug keystore's SHA-1
  (`./gradlew :androidApp:signingReport`). Nothing to copy into the project.
- **iOS:** an iOS OAuth client for bundle ID `com.majidbahmani.cesto.Cesto`. Copy
  `iosApp/Configuration/Secrets.xcconfig.example` to `Secrets.xcconfig` (gitignored) and fill in the client ID.
  Without it the app builds and runs, but "Connect Gmail" shows an error.

### Running the apps

Use the run configurations provided by the run widget in your IDE's toolbar. You can also use these commands and options:

- Android app: `./gradlew :androidApp:assembleDebug`
- iOS app: open the [/iosApp](./iosApp) directory in Xcode and run it from there.

### Running tests

Use the run button in your IDE's editor gutter, or run tests using Gradle tasks:

- Android tests: `./gradlew :app:testAndroidHostTest`
- iOS tests: `./gradlew :app:iosSimulatorArm64Test`
- `:core` tests: `./gradlew :core:testAndroidHostTest :core:iosSimulatorArm64Test`

---

Learn more about [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html)…