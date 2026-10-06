import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "Shared"
            isStatic = true
            // Swift implements GmailAuthorizer, so it must see the type under a clean name.
            export(project(":gmail-auth"))
        }
        // SQLDelight's native driver uses the system SQLite. Needed by the test binary; the static
        // framework leaves linking to the iOS app (OTHER_LDFLAGS in iosApp/Configuration/Config.xcconfig).
        iosTarget.binaries.all { linkerOpts("-lsqlite3") }
    }
    
    android {
       namespace = "com.majidbahmani.cesto.app"
       compileSdk = libs.versions.android.compileSdk.get().toInt()
       minSdk = libs.versions.android.minSdk.get().toInt()
    
       compilerOptions {
           jvmTarget = JvmTarget.JVM_11
       }
       androidResources {
           enable = true
       }
       withHostTest {
           isIncludeAndroidResources = true
       }
       withDeviceTestBuilder {
           sourceSetTreeName = "test"
       }.configure {
           instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
       }
    }
    
    sourceSets {
        androidMain.dependencies {
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.compose.uiTooling)
        }
        commonMain.dependencies {
            implementation(project(":core"))
            implementation(project(":systemdesign"))
            implementation(project(":feature:onboarding"))
            implementation(project(":feature:receipts"))
            implementation(project(":feature:chat"))
            implementation(project(":feature:settings"))
            implementation(project(":llm"))
            implementation(libs.compose.components.resources)
            implementation(project(":database"))
            // api: androidApp and iosApp implement GmailAuthorizer and pass it to initKoin().
            api(project(":gmail-auth"))
            implementation(libs.navigation.compose)
            // api: initKoin() exposes Koin types (KoinAppDeclaration) to androidApp.
            api(libs.koin.core)
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
        }
        getByName("androidHostTest").dependencies {
            implementation(libs.sqldelight.sqliteDriver)
        }
        iosTest.dependencies {
            implementation(libs.sqldelight.nativeDriver)
        }
    }
}

dependencies {
    androidRuntimeClasspath(libs.compose.uiTooling)
}

compose.resources {
    packageOfResClass = "com.majidbahmani.cesto.resources"
}
