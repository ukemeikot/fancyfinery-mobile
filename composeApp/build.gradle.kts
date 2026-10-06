import org.jetbrains.compose.reload.gradle.ComposeHotRun

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kmp.library)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
    alias(libs.plugins.compose.hot.reload)
}

/**
 * Ships to Android and iOS. Desktop is a development preview host.
 *
 * The starter had four targets. wasmJs was removed outright: Fancy Finery
 * already has a web storefront, so that target would have been a second, worse
 * copy of it, and it dragged in the sqlite-wasm worker and a
 * cross-origin-isolation webpack config to pay for the privilege.
 *
 * Desktop stays, but NOT as a product. Compose Hot Reload runs only on the JVM,
 * so without a JVM target every UI change costs a Gradle install and an emulator
 * restart to look at. It earns its place twice over:
 *
 *   1. `runHot` gives a live window that redraws on save.
 *   2. `commonTest` has somewhere to run. Those tests are shared code and need
 *      no device, and `desktopTest` runs them in seconds on every push.
 *
 * Nothing desktop-specific belongs in `commonMain`. The preview is a faster way
 * to look at the same shared UI, not a third platform to support — touch
 * targets, system bars, safe areas and real network behaviour still need a
 * device before anything ships.
 */
kotlin {
    jvmToolchain(17)

    android {
        namespace = "com.fancyfinery.mobile"
        compileSdk = 36
        minSdk = 26
    }

    listOf(
        iosArm64(),
        iosSimulatorArm64(),
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }

    jvm("desktop")

    sourceSets {
        commonMain.dependencies {
            // Compose — explicit (implicit compose.* accessors deprecated)
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.compose.ui.tooling.preview)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.material.icons)

            // Lifecycle
            implementation(libs.lifecycle.runtime)
            implementation(libs.lifecycle.viewmodel)

            // Navigation 3
            implementation(libs.navigation)

            // Ktor
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.ktor.client.logging)

            // RetroStash — annotation-driven HTTP caching over Ktor
            implementation(libs.retrostash.core)
            implementation(libs.retrostash.ktor)

            // Coil — product imagery
            implementation(libs.coil.compose)
            implementation(libs.coil.network.ktor)

            // Room
            implementation(libs.room.runtime)

            // Koin — DI
            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)

            // Kotlinx
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.datetime)
            implementation(libs.coroutines.core)
        }

        androidMain.dependencies {
            implementation(libs.datastore.preferences)
            implementation(libs.ktor.client.okhttp)
            implementation(libs.koin.android)
            implementation(libs.coroutines.android)
            implementation(libs.core)
            implementation(libs.room.sqlite)
            // Opens the provider's hosted payment page in a Custom Tab.
            implementation(libs.androidx.browser)
        }

        iosMain.dependencies {
            implementation(libs.datastore.preferences)
            implementation(libs.ktor.client.darwin)
            implementation(libs.room.sqlite)
        }

        // Development preview host only — never a shipping target.
        val desktopMain by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
                implementation(libs.datastore.preferences)
                implementation(libs.room.sqlite)
                implementation(libs.ktor.client.cio)
                implementation(libs.coroutines.swing)
            }
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.coroutines.test)
            implementation(libs.ktor.client.mock)
        }
    }
}

/**
 * `./gradlew :composeApp:runHot --auto`
 *
 * Opens the shared mobile UI in a phone-sized window and redraws it on save.
 * `--auto` is what makes it recompile on file change rather than on request.
 *
 * Best results need a JetBrains Runtime, which supports enhanced class
 * redefinition — Android Studio ships one at `jbr/`, so it is already on this
 * machine. On a stock JDK it still works, but falls back to a slower reload.
 */
tasks.register<ComposeHotRun>("runHot") {
    group = "compose hot reload"
    description = "Preview the mobile UI in a hot-reloading desktop window."
    mainClass.set("com.fancyfinery.mobile.MainKt")
}

room3 {
    schemaDirectory("$projectDir/schemas")
}

// KSP annotation processors for Room, one per compiled target.
dependencies {
    add("kspAndroid", libs.room.compiler)
    add("kspIosSimulatorArm64", libs.room.compiler)
    add("kspIosArm64", libs.room.compiler)
}
