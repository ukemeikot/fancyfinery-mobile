# Fancy Finery — Mobile

The Fancy Finery storefront as a native app, built with Kotlin Multiplatform and
Compose Multiplatform.

**Targets:** Android · iOS
(Desktop is kept as a development preview host — see below. It ships nowhere.)

## What it does

Everything the web storefront does:

| | |
|---|---|
| **Home** | collections, featured, new in, the lookbook edit, recently viewed, customer reviews, Privé Circle |
| **Shop** | catalogue with debounced search, category filters, paging, pull-to-refresh |
| **Product** | swipeable gallery, size and colour pickers, size & fit, reviews, save, request another colour |
| **Bag** | device-local, server-priced |
| **Checkout** | searchable country picker, Nigerian state/area flat-fee delivery, couriers, discount codes, hosted payment |
| **Orders** | history, receipt, fulfilment trail, cancel, pay |
| **Account** | profile, saved address, currency, policies |
| **Auth** | email + password, Google, one-time sign-in links, password reset |

## Architecture

Vertical slice — each feature owns its whole stack:

```
composeApp/src/
  commonMain/        shared code — put things here by default
    core/            network, session, database, theme, navigation, platform
    features/<name>/
      data/          repository, API client, DAO
      presentation/  screens, view models, components
      di/            Koin module
  androidMain/       Android actuals (Custom Tabs, Credential Manager, Room)
  iosMain/           iOS actuals (SFSafariViewController, Room)
  desktopMain/       development preview host only
androidApp/          thin Android host
iosApp/              Xcode project linking the shared framework
```

### Two rules worth knowing before changing anything

**Money is never computed on the device.** Catalogue prices are stored in naira
and converted server-side; every total shown comes from the quote endpoint, and
checkout sends only product ids, variant ids and quantities. Cached prices in
the bag exist to draw a row. A tampered local bag changes what the customer
*sees* and nothing about what they are *charged*.

**Stock is a hint, not a control.** The quantity stepper stops the obvious
mistake. The real arbitration is a conditional decrement inside the order
transaction server-side — the only thing that can decide between two customers
reaching for the last one.

## Running it

```bash
./gradlew :androidApp:installDebug     # build and install on a connected device
./dev.sh                               # the same, then relaunch
./gradlew :composeApp:runHot --auto    # hot-reloading preview, phone-sized window
./gradlew :composeApp:desktopTest      # shared tests
```

Compose Hot Reload is JVM-only, which is why the desktop target exists: a window
that redraws on save beats a Gradle install per spacing change. It is a faster
way to look at the *same shared UI*, not a third platform — touch targets, safe
areas and real network behaviour still need a device.

`./gradlew build` attempts the iOS targets, which only link on macOS. On Windows
and Linux use the targeted tasks above.

### Pointing at a different backend

One line, in `core/network/NetworkConfig.kt`. Staging and production are the
same application deployed twice and expose an identical API.

### Google sign-in

Uses Credential Manager — the account sheet appears over the app, and the ID
token it returns is exchanged server-side for a session. No browser.

It needs an **Android OAuth client** registered in the same Google Cloud project
as the web client, matching the app's `applicationId` and signing certificate.
Without it Google raises error `28444` and the sheet closes immediately. A
release build is signed with a different key and needs its own entry.

iOS reports the native flow unavailable and hides the button rather than
offering one that fails; email, password and the one-time link work there. The
server already accepts an ID token from any provider, so finishing it is
client-side only.

## Security

`tools/supply_chain_scan.sh` runs in CI on every push. A Gradle build executes
arbitrary Kotlin at configuration time — `build.gradle.kts` is this stack's
equivalent of an npm `postinstall`, and `gradle-wrapper.jar` runs before Gradle
itself exists. The scan covers those, Xcode run-script phases, and CI steps that
fetch remote code.

Also running: CodeQL, Gitleaks, Gradle wrapper validation, Dependabot.

No secrets live in this repository. The Google client id in the source is an
OAuth **client id**, which is public by design and already served by the
website to any anonymous caller; the client *secret* is server-side only.

## Credits

Started from [ukemeikot/kmp-starter](https://github.com/ukemeikot/kmp-starter)
(MIT).
