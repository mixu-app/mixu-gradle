package app.mixu.gradle

import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.getByType

/**
 * The shared version catalog as seen by a convention plugin.
 *
 * Precompiled script plugins have no generated `libs` accessor, so they resolve the
 * catalog at runtime. Every consumer registers it under the name `libs`.
 */
internal val Project.mixuCatalog: VersionCatalog
    get() = extensions.getByType<VersionCatalogsExtension>().named("libs")
