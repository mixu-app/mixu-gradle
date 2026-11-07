pluginManagement {
    // The convention plugins and the shared version catalog live in this repository,
    // so a consumer only needs one checkout (or submodule) of it.
    includeBuild("build-logic")
    repositories {
        gradlePluginPortal()
        mavenCentral()
        google()
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        google()
    }
}

rootProject.name = "mixu-gradle"

// The sample modules exist to prove the conventions compile. They are part of the
// build so `./gradlew build` in CI is a real signal, not a lint of YAML.
include(":sample")
include(":sample-android")
include(":sample-compose")
