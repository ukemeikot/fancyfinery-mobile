package com.fancyfinery.mobile.core.network

/**
 * Where the app talks to, and how patiently.
 *
 * [BASE_URL] is the Fancy Finery deployment this build points at. It carries no
 * trailing slash — every path in the app starts with one, and two slashes in a
 * row would 404 on some proxies and silently work on others.
 *
 * Staging and production are separate deployments of the same application
 * (`APP_ENV` tells them apart, not `NODE_ENV`), so pointing a build at staging
 * is a one-line change here rather than anything structural.
 */
object NetworkConfig {

    /**
     * The storefront origin — currently PRODUCTION.
     *
     * Staging is `https://app.staging.fancyfinerybup.com`. Switching is this one line,
     * because staging and production are the same application deployed twice
     * (`APP_ENV` tells them apart) and expose an identical API.
     *
     * Must be HTTPS in any build that ships: the session token travels in an
     * `Authorization` header, and cleartext would hand it to anyone sharing the
     * network — which, for this audience, is every café and campus wifi.
     *
     * For local development against `npm run dev`, an emulator cannot reach
     * "localhost" — that resolves to the emulator itself. Use `10.0.2.2` on the
     * Android emulator, or the host machine's LAN address on a real device.
     */
    const val BASE_URL: String = "https://fancyfinerybup.com"

    /** Prefix for the app's own endpoints. Versioned, so a breaking change ships as /v2. */
    const val API_PREFIX: String = "/api/mobile/v1"

    /** Better Auth's own REST surface, which the app uses directly for sign-in. */
    const val AUTH_PREFIX: String = "/api/auth"

    /**
     * 30s. Generous on purpose: the audience is substantially on Nigerian mobile
     * networks, where a cold request over a congested cell can legitimately take
     * well over ten seconds. Timing out early turns a slow checkout into a
     * failed one.
     */
    const val TIMEOUT_MS: Long = 30_000L

    /**
     * Request/response logging. Must stay false in release builds — the logs
     * would otherwise contain the `Authorization` header on every call, and on
     * Android any installed app can read logcat on older releases.
     */
    const val LOG_HTTP: Boolean = false

    /** Header the chosen currency travels in. Mirrors CURRENCY_HEADER server-side. */
    const val CURRENCY_HEADER: String = "x-ff-currency"
}
