import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_11
    }
}
dependencies {
    implementation(project(":app"))

    implementation(libs.androidx.activity.compose)
    implementation(libs.koin.android)
    implementation(libs.play.services.auth)

    implementation(libs.compose.uiToolingPreview)
    debugImplementation(libs.compose.uiTooling)
}

android {
    namespace = "com.majidbahmani.cesto"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.majidbahmani.cesto"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    // Release signing only from environment variables (CI secrets, doc 31): without them the release APK
    // is unsigned, so local builds never need the key. Never in files or gradle.properties.
    val keystore = System.getenv("CESTO_KEYSTORE_FILE")
    if (!keystore.isNullOrBlank()) {
        signingConfigs.create("release") {
            storeFile = file(keystore)
            storePassword = System.getenv("CESTO_KEYSTORE_PASSWORD")
            keyAlias = System.getenv("CESTO_KEY_ALIAS")
            keyPassword = System.getenv("CESTO_KEY_PASSWORD")
        }
    }
    buildTypes {
        release {
            signingConfigs.findByName("release")?.let { signingConfig = it }
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
    lint {
        // Also lint the KMP modules (:app, features, :core…): they have no lint task of their own.
        checkDependencies = true
        // Errors fail CI; warnings are only reported (HTML report in the CI artifacts).
        abortOnError = true
    }
}

// Version from the release tag (v1.2.3 → -Pcesto.versionName=1.2.3 -Pcesto.versionCode=10203). The variant API
// runs after android {}, so defaultConfig's "1.0" / 1 don't overwrite it; local builds keep those.
androidComponents {
    val versionName = findProperty("cesto.versionName") as String?
    val versionCode = (findProperty("cesto.versionCode") as String?)?.toInt()
    onVariants { variant ->
        variant.outputs.forEach { output ->
            versionName?.let { output.versionName.set(it) }
            versionCode?.let { output.versionCode.set(it) }
        }
    }
}
