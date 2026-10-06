import javax.inject.Inject
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "com.fancyfinery.mobile.android"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.fancyfinery.mobile"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    jvmToolchain(17)
}

/**
 * Stage `commonMain` Compose resources into this app's assets.
 *
 * This should not be necessary, and is here because of a gap in the current
 * Compose + AGP KMP-library combination: the plugin registers
 * `copyAndroidMainComposeResourcesToAndroidAssets`, which — as its name says —
 * only looks at `androidMain`. Resources declared in `commonMain` ARE prepared
 * (the `Res` accessor compiles happily) but never reach the APK, so the build
 * succeeds and the app dies at runtime with `MissingResourceException`.
 *
 * Two details make this work rather than merely move files:
 *
 *  - `Res.readBytes` resolves against
 *    `composeResources/<module qualifier>/…`, while the prepared directory has
 *    no qualifier segment. The `into` below reinstates it; getting it wrong
 *    produces exactly the same runtime error as not copying at all.
 *
 *  - Android's asset merge runs early, so this must be ordered before
 *    `preBuild`, not merely declared as a source directory.
 *
 * Delete this once the plugin wires commonMain resources itself; the symptom
 * that it is still needed is a missing-resource crash on first launch.
 */
val composeResourceQualifier = "fancyfinerymobile.composeapp.generated.resources"

/**
 * Copies the prepared resources under the qualifier segment the runtime looks
 * for.
 *
 * A typed task rather than a `Copy`, because AGP's variant API needs an
 * `@OutputDirectory DirectoryProperty` it can wire as a generated source — and
 * AGP 9 rejects plain providers on the SourceSet API outright, precisely so the
 * task dependency is carried rather than silently lost.
 */
abstract class StageComposeResources : DefaultTask() {

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val source: DirectoryProperty

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    /** The module qualifier `Res` resolves paths against. */
    @get:Input
    abstract val qualifier: Property<String>

    @get:Inject
    abstract val fs: FileSystemOperations

    @TaskAction
    fun stage() {
        fs.sync {
            from(source)
            into(outputDir.dir("composeResources/" + qualifier.get()))
        }
    }
}

val stageCommonComposeResources =
    tasks.register<StageComposeResources>("stageCommonComposeResources") {
        dependsOn(":composeApp:prepareComposeResourcesTaskForCommonMain")
        source.set(
            project(":composeApp").layout.buildDirectory
                .dir("generated/compose/resourceGenerator/preparedResources/commonMain/composeResources"),
        )
        qualifier.set(composeResourceQualifier)
    }

androidComponents {
    onVariants { variant ->
        variant.sources.assets?.addGeneratedSourceDirectory(
            taskProvider = stageCommonComposeResources,
            wiredWith = StageComposeResources::outputDir,
        )
    }
}

dependencies {
    implementation(projects.composeApp)
    implementation(libs.activity.compose)
    implementation(libs.koin.android)

    testImplementation(libs.junit)
    androidTestImplementation(libs.runner)
    androidTestImplementation(libs.espresso.core)
}
