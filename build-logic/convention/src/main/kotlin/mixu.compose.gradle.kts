// Convention: a Compose Multiplatform UI module.
//
// Consumers apply `id("mixu.compose")` to configure the Compose compiler and wire
// Compose runtime, foundation, material3 and UI dependencies from the catalog.
import app.mixu.gradle.mixuCatalog
import com.android.build.api.dsl.CommonExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

plugins {
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
}

val catalog = mixuCatalog
val composeRuntime = catalog.findLibrary("compose-runtime").get()
val composeFoundation = catalog.findLibrary("compose-foundation").get()
val composeMaterial3 = catalog.findLibrary("compose-material3").get()
val composeUi = catalog.findLibrary("compose-ui").get()
val composeResources = catalog.findLibrary("compose-components-resources").get()

// Wire dependencies for Kotlin Multiplatform modules:
plugins.withId("org.jetbrains.kotlin.multiplatform") {
    extensions.configure<KotlinMultiplatformExtension> {
        sourceSets.named("commonMain").configure {
            dependencies {
                implementation(composeRuntime)
                implementation(composeFoundation)
                implementation(composeMaterial3)
                implementation(composeUi)
                implementation(composeResources)
            }
        }
    }
}

// Wire dependencies for single-platform JVM modules:
plugins.withId("org.jetbrains.kotlin.jvm") {
    if (!plugins.hasPlugin("org.jetbrains.kotlin.multiplatform")) {
        dependencies {
            "implementation"(composeRuntime)
            "implementation"(composeFoundation)
            "implementation"(composeMaterial3)
            "implementation"(composeUi)
            "implementation"(composeResources)
        }
    }
}

// Wire dependencies and buildFeatures for Android library modules:
plugins.withId("com.android.library") {
    extensions.findByType(CommonExtension::class.java)?.apply {
        buildFeatures.compose = true
    }
    if (!plugins.hasPlugin("org.jetbrains.kotlin.multiplatform")) {
        dependencies {
            "implementation"(composeRuntime)
            "implementation"(composeFoundation)
            "implementation"(composeMaterial3)
            "implementation"(composeUi)
            "implementation"(composeResources)
        }
    }
}

// Wire dependencies and buildFeatures for Android application modules:
plugins.withId("com.android.application") {
    extensions.findByType(CommonExtension::class.java)?.apply {
        buildFeatures.compose = true
    }
    if (!plugins.hasPlugin("org.jetbrains.kotlin.multiplatform")) {
        dependencies {
            "implementation"(composeRuntime)
            "implementation"(composeFoundation)
            "implementation"(composeMaterial3)
            "implementation"(composeUi)
            "implementation"(composeResources)
        }
    }
}
