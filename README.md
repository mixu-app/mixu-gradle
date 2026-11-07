# mixu-gradle

Shared Gradle convention plugins and the single version catalog for every Mixu
Kotlin repository.

This repository is **not** published to a package registry. Consumers mount it as a
submodule and include the `build-logic` build, so one checkout gives a consumer both
the plugins and the versions — no credentials, no publishing pipeline, works
offline.

## What it provides

| Path | Purpose |
|---|---|
| `gradle/libs.versions.toml` | Every version used by any Kotlin repository (the only place a version is written) |
| `build-logic/convention/` | Precompiled script plugins: `mixu.kotlin.jvm`, `mixu.kotlin.multiplatform`, `mixu.reproducible` |
| `sample/` | A module that applies the conventions, so CI proves they compile |

## Using it from a repository

```kotlin
// settings.gradle.kts
pluginManagement {
    includeBuild("build-logic-src/build-logic")
    repositories { gradlePluginPortal(); mavenCentral(); google() }
}
dependencyResolutionManagement {
    repositories { mavenCentral(); google() }
    versionCatalogs {
        create("libs") { from(files("build-logic-src/gradle/libs.versions.toml")) }
    }
}
```

```kotlin
// build.gradle.kts
plugins {
    id("mixu.kotlin.jvm")          // or id("mixu.kotlin.multiplatform")
    id("mixu.reproducible")
}
```

`build-logic-src/` is this repository mounted as a submodule at a pinned commit.

## Rules

1. **No version numbers outside the catalog.** `2.4.21` appears once, in
   `libs.versions.toml`; a module that needs a dependency adds a `[libraries]` entry.
2. **Conventions decide policy, modules decide content.** Toolchain, compiler flags,
   test framework and archive reproducibility live here; a module sets only its
   coordinates and dependencies.
3. **Adding a convention is additive.** Renaming or removing one breaks every
   consumer at once, so it needs a coordinated pin bump.
4. **Toolchain is JDK 21 for compilation, JDK 25 to run Gradle.** The toolchain block
   here is what makes the build reproducible across hosts.
5. **The sample must keep compiling.** If the conventions break, `./gradlew build`
   in this repository fails before any consumer notices.

## Network

`./gradlew` downloads its distribution on first use, and the dependency
repositories are `mavenCentral()` and `google()`. On a network where
`services.gradle.org` or `dl.google.com` is unreachable, seed the distribution once
from a mirror and keep the repository URLs unchanged:

```sh
D=$(find ~/.gradle/wrapper/dists/gradle-9.8.2-bin -maxdepth 1 -mindepth 1 -type d)
curl -L -o "$D/gradle-9.8.2-bin.zip" https://mirrors.cloud.tencent.com/gradle/gradle-9.8.2-bin.zip
./gradlew --version
```

CI runners have direct access, so the committed `distributionUrl` stays official.

## Gates

```sh
just check     # markdownlint + ./gradlew build (compiles the sample)
```

## License

Apache-2.0. Every consumer keeps its own license; this repository contains build
configuration only.
