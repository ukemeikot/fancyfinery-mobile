plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.kmp.library) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.compose.multiplatform) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.room) apply false
}

// The starter pinned `ws` through the Kotlin/Wasm Yarn root here, because the
// wasmJs target pulled an npm tree for its webpack dev server. That target is
// gone, so the npm tree, its lockfile and the pin went with it — this build
// resolves nothing from npm at all now.
