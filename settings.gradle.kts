rootProject.name = "FancyFineryMobile"

pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

/**
 * Repositories are limited to Google and Maven Central, and PREFER_SETTINGS
 * stops a subproject quietly introducing a third.
 *
 * The starter additionally declared three Ivy repositories (nodejs.org, the
 * binaryen releases page and the yarn releases page). Those existed solely to
 * fetch the Node/Yarn/Binaryen toolchain the Kotlin/Wasm target needs; with the
 * wasmJs target gone nothing resolves from them, and leaving them declared would
 * keep three extra download hosts in the build's trust boundary for no benefit.
 */
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    repositories {
        google()
        mavenCentral()
    }
}

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

include(":androidApp")
include(":composeApp")
