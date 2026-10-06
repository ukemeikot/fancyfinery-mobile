# Agent guide

Entry point for AI coding agents working in this repository. Claude Code also
loads the skills under `.claude/skills/`; other agents should read this file plus
the skill it points to.

## Skills

| Skill                                | Use for                                                                                  |
| ------------------------------------ | ---------------------------------------------------------------------------------------- |
| [`kmp`](.claude/skills/kmp/SKILL.md) | Source sets, expect/actual, Compose Multiplatform, Koin, Ktor, Room, Gradle, the build gate |

The official skills are **installed rather than vendored**, because they track
the Android Gradle Plugin and Kotlin releases:

```sh
android skills add --all
```

- [android/skills](https://github.com/android/skills) (Apache-2.0, Google) —
  `jetpack-compose`, `build-system`, `performance`, `testing`, `security`,
  `navigation`
- [JetBrains/skills](https://github.com/JetBrains/skills) — the KMP-relevant ones
  are `gradle-kotlin-dsl-doctor`, `kotlin-tooling-agp9-migration` and
  `kotlin-tooling-cocoapods-spm-migration`; most others are Spring/backend

## Project shape

Compose Multiplatform targeting **Android, iOS, Desktop (JVM) and Web (wasmJs)**.

```
composeApp/src/
  commonMain/     shared code -- put things here by default
  commonTest/     shared tests, run on the desktop JVM in CI
  androidMain/    androidx / Android SDK
  iosMain/        Kotlin/Native
  desktopMain/    JVM desktop + entry point
  wasmJsMain/     browser + sqlite web worker glue
androidApp/       thin Android host (MainActivity only)
iosApp/           Xcode project linking the shared framework
```

**Write in `commonMain` first.** Drop into a platform source set only when the
API genuinely differs, via `expect`/`actual` — `DatabaseFactory` and
`DataStoreFactory` are the pattern. An `expect` needs an `actual` in **all four**
platform source sets or the build fails; `wasmJsMain` is the one people forget.

- Dependencies belong in `gradle/libs.versions.toml`, never inline in a build
  file. Reference them as `libs.some.library`.
- DI is Koin: a `di/` module per feature, registered in `AppModule.kt`.
  Platform bindings live in `CoreModule.<platform>.kt`.
- A feature is `{data,domain,presentation}` plus `<Feature>Contract.kt`,
  `<Feature>State.kt`, `<Feature>ViewModel.kt`.
- Room exports schemas to `composeApp/schemas/`. Commit them — they are the
  migration history.
- Package is `com.fancyfinery.mobile`. Rebrand with `./rename.sh com.your.app`.

## Before you commit

```sh
./gradlew :composeApp:desktopTest      # commonTest on the JVM
./gradlew :androidApp:assembleDebug
bash tools/supply_chain_scan.sh .
```

`./gradlew build` also attempts the iOS targets, which only link on macOS. On
Windows and Linux use the targeted tasks above. CI covers all four platforms,
with the iOS framework linked on a macOS runner.

## Security constraints

A Gradle build runs arbitrary Kotlin at **configuration time**, before any task
executes — `build.gradle.kts` and `settings.gradle.kts` are this stack's
equivalent of a `postinstall` script. So are `gradle-wrapper.jar` (which runs
before Gradle exists), Xcode run-script phases in `project.pbxproj`, and the
webpack config injected into the wasmJs build. Those are the implant targets, and
the vendored scan covers all of them.

- No process execution, socket access, or base64 decoding in Gradle files.
- Maven repositories stay limited to Central, Google and the Gradle Plugin
  Portal. Never add a plaintext HTTP repository.
- `gradle-wrapper.properties` pins `distributionSha256Sum`. Keep it pinned and
  update it from <https://gradle.org/release-checksums/> when bumping Gradle.
- Never commit a modified `gradle-wrapper.jar`. CI validates it against Gradle's
  published checksums.
- Do not add a CI step that downloads and executes a script from the network.

## Known constraints

- `org.jetbrains.compose.material3:material3` has **no stable release** — only
  alphas are published. The alpha pin in the version catalog is deliberate, not
  an oversight. The same is true of `room` and `sqlite`.
- The Gradle daemon requires **JDK 21** (`gradle/gradle-daemon-jvm.properties`).
  Gradle auto-provisions it if the local JDK is older.
