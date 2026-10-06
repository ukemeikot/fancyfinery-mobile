# KMP Starter

Kotlin Multiplatform + Compose Multiplatform starter with vertical slice architecture.

**Platforms:** Android · iOS · Desktop (JVM) · Web (wasmJs)

## Quick Start

```bash
# 1. Clone
git clone https://github.com/ukemeikot/kmp-starter.git
cd kmp-starter

# 2. Rename package (optional)
./rename.sh com.yourcompany.appname

# 3. Replace simulated auth with your real API
# composeApp/src/commonMain/.../features/auth/data/remote/AuthApi.kt

# 4. Build & run
./gradlew :androidApp:assembleDebug          # Android APK
./gradlew :composeApp:wasmJsBrowserRun       # Web (dev server)
```

## Architecture

Vertical Slice — each feature is fully self-contained:

```
features/
  featureName/
    FeatureNameContract.kt   ← public API (callbacks, shared types)
    di/FeatureNameModule.kt  ← Koin DI wiring
    data/                    ← repository, API client, DAO, preferences
    domain/                  ← use cases, domain models
    presentation/            ← screens, viewmodels, UI components
```

### Adding a Feature

1. Create `composeApp/src/commonMain/.../features/<name>/` following the structure above
2. Register the Koin module in `core/di/AppModule.kt`
3. Add a destination to `core/navigation/Destinations.kt`
4. Wire the screen in `core/navigation/AppNavigation.kt`

## Structure

```
kotlin-starter/
├── androidApp/          Android application entry point
├── composeApp/          Shared KMP code (UI + logic + data)
│   └── src/
│       ├── commonMain/  Shared across all platforms
│       ├── androidMain/ Android-specific actuals
│       ├── iosMain/     iOS-specific actuals
│       ├── desktopMain/ JVM desktop actuals
│       └── wasmJsMain/  Web actuals
├── iosApp/              Xcode project (SwiftUI wrapper)
└── rename.sh            Package rename helper
```

## Defaults Included

| Feature    | Details                                                                                                     |
|------------|-------------------------------------------------------------------------------------------------------------|
| Onboarding | 3-page horizontal swipe, skip/complete stored in DataStore                                                  |
| Auth       | Login + Register screens, demo creds `demo@example.com` / `Password1`, swap `AuthApi.kt` for real API |
| Home       | Scaffold + top bar with logout                                                                              |
| Theme      | Material 3, light + dark mode, custom color palette                                                         |
| Navigation | Nav3 with explicit back stack                                                                               |

## Stack

| Layer         | Library                                                                     |
|---------------|-----------------------------------------------------------------------------|
| UI            | Compose Multiplatform 1.10.3                                                |
| DI            | Koin 4.x                                                                    |
| HTTP          | Ktor 3.4.2                                                                  |
| HTTP caching  | RetroStash 0.0.7                                                            |
| Database      | Room 3.x (all platforms)                                                    |
| Preferences   | `PreferenceStore` — DataStore on Android/iOS/Desktop, `localStorage` on Web |
| Navigation    | Navigation 3                                                                |
| Serialization | kotlinx.serialization                                                       |

## iOS Setup

Open `iosApp/iosApp.xcodeproj` in Xcode and run on a simulator or device.

If you renamed the package with `rename.sh`, also update the iOS bundle identifier:

1. Open `iosApp/iosApp.xcodeproj` → select the `iosApp` target
2. **General → Identity → Bundle Identifier** — set to your new package name
3. Optionally update the display name under **General → Display Name**

## Agent skills

Agent guidance lives in [`AGENTS.md`](AGENTS.md) and `.claude/skills/`. Claude Code
picks the skill up automatically; other agents should start from `AGENTS.md`.

| Skill                                | Covers                                                                                    |
| ------------------------------------ | ------------------------------------------------------------------------------------------ |
| [`kmp`](.claude/skills/kmp/SKILL.md) | Source sets, expect/actual, Compose Multiplatform, Koin, Ktor, Room, Gradle, the build gate |

The official skills are **installed rather than vendored**, since they track the
Android Gradle Plugin and Kotlin releases and a frozen copy would go stale:

```sh
android skills add --all
```

- [android/skills](https://github.com/android/skills) (Apache-2.0, Google) —
  `jetpack-compose`, `build-system`, `performance`, `testing`, `security`,
  `navigation`
- [JetBrains/skills](https://github.com/JetBrains/skills) — KMP-relevant entries
  are `gradle-kotlin-dsl-doctor`, `kotlin-tooling-agp9-migration` and
  `kotlin-tooling-cocoapods-spm-migration`

**Thanks to the Android and JetBrains teams for publishing these openly.**

## Security

### Supply-chain scanning

A self-contained scanner lives at
[`tools/supply_chain_scan.sh`](tools/supply_chain_scan.sh) and runs in CI on every
push and pull request:

```sh
bash tools/supply_chain_scan.sh .
```

A Gradle build executes arbitrary Kotlin at **configuration time**, before any
task runs, which makes `build.gradle.kts` and `settings.gradle.kts` this stack's
equivalent of an npm `postinstall` script. `gradle-wrapper.jar` runs even earlier
— before Gradle itself has been downloaded. Xcode run-script phases and the
webpack config injected into the wasmJs build round out the surface. All of them
execute on a normal build and none are read closely during review.

The scan verifies the Gradle wrapper (stock jar contents, HTTPS official
distribution URL, `distributionSha256Sum` pinned), that Maven repositories stay
on trusted hosts with no plaintext HTTP, that build files are not oversized or
whitespace-padded, that they contain no process execution or network access, that
`project.pbxproj` has no injected run-script phases, that the wasm-target
JavaScript carries no obfuscation or blockchain-C2 patterns, and that no CI step
downloads and runs remote code.

It is deliberately vendored rather than downloaded at scan time — fetching a
scanner over the network would reintroduce exactly the class of dependency it
exists to catch.

### Other checks

| Check                     | Runs on                   |
| ------------------------- | ------------------------- |
| CodeQL (`java-kotlin`)    | push, PR, weekly schedule |
| Gitleaks                  | push, PR, weekly schedule |
| Gradle Wrapper Validation | push, PR, weekly schedule |
| Dependabot (gradle, actions, npm) | weekly            |

`gradle-wrapper.properties` pins the Gradle distribution by SHA-256. Update that
hash from <https://gradle.org/release-checksums/> whenever you bump Gradle.

## Known constraints

- `org.jetbrains.compose.material3:material3` publishes **no stable release** —
  only alphas exist. The alpha pin in `gradle/libs.versions.toml` is deliberate.
  `room` and `sqlite` are alpha for the same reason.
- The Gradle daemon requires **JDK 21** (`gradle/gradle-daemon-jvm.properties`).
  Gradle auto-provisions it when the local JDK is older.
- `./gradlew build` attempts the iOS targets, which only link on macOS. On
  Windows and Linux use `:composeApp:desktopTest` and `:androidApp:assembleDebug`.

## License

MIT
