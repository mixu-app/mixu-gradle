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
    // Android Gradle Plugin is added by the task that introduces an Android
    // convention plugin: declaring it here before anything uses it forces every
    // build to resolve from dl.google.com, which some networks cannot reach.
}
