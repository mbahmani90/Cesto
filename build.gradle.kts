plugins {
    // this is necessary to avoid the plugins to be loaded multiple times
    // in each subproject's classloader
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidMultiplatformLibrary) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.kotlinSerialization) apply false
    alias(libs.plugins.sqldelight) apply false
    alias(libs.plugins.spotless)
}

// Code style: `./gradlew spotlessApply` formats, CI runs `spotlessCheck`. Rules here, not in .editorconfig
// (Spotless doesn't apply .editorconfig ktlint properties reliably).
val ktlintRules = mapOf(
    "ktlint_code_style" to "android_studio",
    "max_line_length" to "140",
    "ktlint_function_naming_ignore_when_annotated_with" to "Composable",
    // Generated SQLDelight names and test names with underscores stay readable.
    "ktlint_standard_function-naming" to "disabled",
    "ktlint_standard_property-naming" to "disabled"
)

spotless {
    kotlin {
        target("**/src/**/*.kt")
        targetExclude("**/build/**")
        ktlint(libs.versions.ktlint.get())
            .editorConfigOverride(ktlintRules)
            .customRuleSets(listOf("io.nlopez.compose.rules:ktlint:${libs.versions.composeRules.get()}"))
    }
    kotlinGradle {
        target("*.gradle.kts", "**/*.gradle.kts")
        targetExclude("**/build/**")
        ktlint(libs.versions.ktlint.get()).editorConfigOverride(ktlintRules)
    }
}
