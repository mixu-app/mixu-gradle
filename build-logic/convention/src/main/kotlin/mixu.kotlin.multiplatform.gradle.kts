// Convention: a Kotlin Multiplatform library.
//
// Targets are deliberately minimal: JVM now, Android/native when a consumer needs
// them. Adding a target is a change here so every consumer stays in step.
import app.mixu.gradle.mixuCatalog
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("org.jetbrains.kotlin.multiplatform")
}

kotlin {
    jvm {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
            allWarningsAsErrors.set(true)
        }
    }

    sourceSets {
        commonTest.dependencies {
            implementation(mixuCatalog.findLibrary("kotlin-test").get())
        }
    }
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
