This is a Kotlin Multiplatform project targeting Android, iOS.

* [/iosApp](./iosApp/iosApp) contains an iOS application. Even if you’re sharing your UI with Compose Multiplatform,
  you need this entry point for your iOS app. This is also where you should add SwiftUI code for your project.

* [/androidApp](./androidApp) is the Android entry point (`MainActivity`).
* [/app](./app/src) is the composition root shared by Android and iOS: `App()`, navigation and DI startup.
  It also builds the iOS framework (`Shared`).
  - [commonMain](./app/src/commonMain/kotlin) is for code that’s common for all targets.
  - [androidMain](./app/src/androidMain/kotlin) and [iosMain](./app/src/iosMain/kotlin) are for platform-specific code.
* [/systemdesign](./systemdesign/src) is the design system: `CestoTheme` (light + dark colour schemes) and components used by 2+ features.
* [/core](./core/src) is non-UI code shared by features: the Ktor `HttpClient` (OkHttp / Darwin engine) and its Koin module.

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