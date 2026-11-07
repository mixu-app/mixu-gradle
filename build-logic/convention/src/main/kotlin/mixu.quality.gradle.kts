// Convention: Kotlin formatting and static analysis with ktlint.
//
// Consumers apply `id("mixu.quality")` to enforce style consistency and fail the build
// on formatting violations.
import org.jlleitschuh.gradle.ktlint.KtlintExtension

plugins {
    id("org.jlleitschuh.gradle.ktlint")
}

extensions.configure<KtlintExtension> {
    outputToConsole.set(true)
    ignoreFailures.set(false)
}

plugins.withId("com.android.base") {
    extensions.configure<KtlintExtension> {
        android.set(true)
    }
}
