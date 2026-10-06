---
name: kmp
description:
  How to work with this Kotlin Multiplatform project, and how to reach the
  official Android and JetBrains agent skills. Use when writing Kotlin in
  commonMain or a platform source set, adding expect/actual declarations, wiring
  Koin modules, building Compose Multiplatform UI, using Ktor or Room, or
  changing the Gradle build.
metadata:
  source: https://github.com/android/skills
---

# Kotlin Multiplatform

Compose Multiplatform targeting **Android, iOS, Desktop (JVM) and Web (wasmJs)**
from one shared codebase. Kotlin, Gradle with a version catalog, Koin for DI,
Ktor for HTTP, Room for persistence, DataStore for preferences, Compose
Navigation, MVI-ish `Contract` + `State` + `ViewModel` per feature.

## Official skills are not vendored here

- [android/skills](https://github.com/android/skills) (Apache-2.0, Google) —
  `jetpack-compose`, `build-system`, `performance`, `testing`, `security`,
  `navigation`, `devtools`, and more. The Compose and build-system skills apply
  directly to `composeApp` and `androidApp`.
- [JetBrains/skills](https://github.com/JetBrains/skills) — curated collection.
  The KMP-relevant ones are `gradle-kotlin-dsl-doctor`,
  `kotlin-tooling-agp9-migration` and `kotlin-tooling-cocoapods-spm-migration`;
  most of the rest are Spring/backend and do not apply here.

They are **installed**, not copied into the repo, because they track the Android
Gradle Plugin and Kotlin releases — a vendored copy would go stale.

```sh
android skills add --all          # all Android skills, all detected agents
android skills add --skill=jetpack-compose --project=.
```

If they are not installed, prefer the canonical docs over recalling API shapes
from memory: <https://kotlinlang.org/docs/multiplatform.html>,
<https://developer.android.com/develop/ui/compose>, and
<https://www.jetbrains.com/help/kotlin-multiplatform-dev/>.

## Source set layout

```
composeApp/src/
  commonMain/     shared code -- put things here by default
  commonTest/     shared tests, run on the desktop JVM in CI
  androidMain/    androidx / Android SDK only
  iosMain/        Kotlin/Native, CoreFoundation, platform.*
  desktopMain/    JVM desktop, includes the app entry point
  wasmJsMain/     browser, includes the sqlite web worker glue
androidApp/       thin Android host: MainActivity only
iosApp/           Xcode project that links the shared framework
```

**Write in `commonMain` first.** Only drop into a platform source set when the
API genuinely differs, and do it through `expect`/`actual`. The existing
`DatabaseFactory` and `DataStoreFactory` are the pattern to copy: an `expect`
declaration in `commonMain`, one `actual` per target.

When you add an `expect`, you must add an `actual` in **all four** platform
source sets or the build fails — including `wasmJsMain`, which is easy to forget.

## Package and rename

The package is `com.fancyfinery.mobile`. To rebrand, use `./rename.sh
com.your.app` — it rewrites text references, moves the Kotlin source
directories, and renames the Room schema export directory.

## Conventions

- **Dependencies** go in `gradle/libs.versions.toml`, never inline in a
  `build.gradle.kts`. Reference them as `libs.some.library`.
- **DI**: Koin. Each feature has a `di/` module; register it in `AppModule.kt`.
  Platform-specific bindings go in `CoreModule.<platform>.kt`.
- **Feature shape**: `<feature>/{data,domain,presentation}` with a
  `<Feature>Contract.kt` describing intents and effects, a `<Feature>State.kt`,
  and a `<Feature>ViewModel.kt`.
- **Room** exports schemas to `composeApp/schemas/`. Commit schema changes — they
  are the migration history.
- **Network base URL** lives in `core/network/NetworkConfig.kt`.

## Before you commit

```sh
./gradlew :composeApp:desktopTest      # commonTest on the JVM
./gradlew :androidApp:assembleDebug
bash tools/supply_chain_scan.sh .
```

`./gradlew build` also tries the iOS targets, which only link on macOS. On
Windows or Linux, use the targeted tasks above.

## Security constraints

A Gradle build executes arbitrary Kotlin at **configuration time**, before any
task runs — so `build.gradle.kts` and `settings.gradle.kts` are the equivalent of
a `postinstall` script, and are the implant target here. So are the wrapper jar
(which runs before Gradle exists), Xcode run-script phases, and the webpack
config injected into the wasmJs build.

- Never add process execution, socket access or base64 decoding to a Gradle file.
- Keep Maven repositories to Central, Google and the Gradle Plugin Portal. Never
  add a plaintext HTTP repository.
- `gradle-wrapper.properties` pins `distributionSha256Sum`. Keep it pinned, and
  update it from <https://gradle.org/release-checksums/> when bumping Gradle.
- Never commit a modified `gradle-wrapper.jar`. CI validates it against Gradle's
  published checksums.
- Do not add a CI step that downloads and executes a script from the network.
