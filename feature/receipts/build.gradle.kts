import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// Receipts from Gmail (and demo mode): Gmail sync into the local database, PDFs on the phone, receipt list.
plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    listOf(iosArm64(), iosSimulatorArm64()).forEach { iosTarget ->
        // Tests use SQLDelight's native driver, which needs the system SQLite.
        iosTarget.binaries.all { linkerOpts("-lsqlite3") }
    }

    android {
        namespace = "com.majidbahmani.cesto.feature.receipts"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        compilerOptions {
            jvmTarget = JvmTarget.JVM_11
        }
        androidResources {
            enable = true
        }
        withHostTest {
            // logWarning uses android.util.Log, which does nothing in JVM unit tests instead of throwing.
            isReturnDefaultValues = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":systemdesign"))
            implementation(project(":core"))
            implementation(project(":gmail-auth"))
            implementation(project(":database"))
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.datetime)
            implementation(libs.sqldelight.coroutines)
            implementation(libs.koin.core)
            implementation(libs.koin.compose.viewmodel)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(libs.navigation.compose)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.uiToolingPreview)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.ktor.client.mock)
        }
        getByName("androidHostTest").dependencies {
            implementation(libs.sqldelight.sqliteDriver)
        }
        iosTest.dependencies {
            implementation(libs.sqldelight.nativeDriver)
        }
    }
}

compose.resources {
    packageOfResClass = "com.majidbahmani.cesto.feature.receipts.resources"
}
