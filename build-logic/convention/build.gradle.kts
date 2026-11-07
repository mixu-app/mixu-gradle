plugins {
    `kotlin-dsl`
}

group = "app.mixu.gradle"
version = "0.1.0"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

dependencies {
    // The plugins the precompiled scripts apply must be on the runtime classpath.
    implementation(libs.kotlin.gradle.plugin)
    implementation(libs.kotlin.compose.compiler.plugin)
    implementation(libs.android.gradle.plugin)
    implementation(libs.compose.gradle.plugin)
    implementation(libs.ktlint.gradle.plugin)
}
