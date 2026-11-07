// Convention: an Android library module.
//
// Consumers apply `id("mixu.android.library")` and set only coordinates: SDK levels,
// toolchains, compiler flags, lint rules, and test framework are single-sourced here.
import app.mixu.gradle.findVersionInt
import app.mixu.gradle.mixuCatalog
import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.JavaVersion
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("com.android.library")
}

val compileSdkVersion = findVersionInt("android-compileSdk")
val minSdkVersion = findVersionInt("android-minSdk")

extensions.configure<LibraryExtension> {
    compileSdk = compileSdkVersion

    defaultConfig {
        minSdk = minSdkVersion
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    // Build features are off by default; a module opts in explicitly.
    buildFeatures {
        buildConfig = false
        aidl = false
        resValues = false
        shaders = false
        viewBinding = false
    }

    // Lint is configured to fail on errors, not warnings.
    lint {
        abortOnError = true
        warningsAsErrors = false
    }

    testOptions {
        unitTests.all {
            it.useJUnitPlatform()
            it.testLogging {
                exceptionFormat = TestExceptionFormat.FULL
                events("failed", "skipped")
            }
        }
    }
}

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)
        allWarningsAsErrors.set(true)
        freeCompilerArgs.addAll("-Xjsr305=strict")
    }
}

val catalog = mixuCatalog
dependencies {
    "testImplementation"(catalog.findLibrary("kotlin-test").get())
    "testImplementation"(catalog.findLibrary("kotlin-test-junit5").get())
    "testImplementation"(catalog.findLibrary("junit-jupiter").get())
    "testRuntimeOnly"(catalog.findLibrary("junit-platform-launcher").get())
}
