package com.fancyfinery.mobile.core.platform

/**
 * Native Google sign-in, in the app.
 *
 * Returns a Google **ID token**, which the server exchanges for a session via
 * Better Auth's `/sign-in/social` with `idToken: { token }`. The token is
 * opaque to this app: it is not parsed, trusted or inspected here — Google
 * signs it, and the server verifies that signature. All this layer does is
 * obtain one and hand it over.
 *
 * This replaces the browser-tab OAuth flow. That flow worked, but it took the
 * customer out of the app to a web page and the session it created landed in
 * the browser's cookie jar rather than in the app — which would have needed a
 * one-time-token handoff to retrieve. A native sheet has neither problem: the
 * customer never leaves, and the app receives the credential directly.
 */
interface GoogleSignIn {

    /** Whether this build can sign in natively at all. */
    val isAvailable: Boolean

    /**
     * Show the account chooser and return a Google ID token.
     *
     * Returns null when the customer dismisses the sheet — a cancellation is
     * not a failure and must not surface as an error.
     *
     * Throws [GoogleSignInException] when something genuinely went wrong, with
     * a message fit to show.
     */
    suspend fun idToken(): String?
}

class GoogleSignInException(message: String, cause: Throwable? = null) :
    Exception(message, cause)

/** Platform implementation. */
expect fun createGoogleSignIn(context: Any?): GoogleSignIn

/**
 * The OAuth **Web** client id — not an Android one.
 *
 * Credential Manager wants the web client as `serverClientId`: it is the
 * audience the returned ID token is minted for, and the server verifies it
 * against the same value. The Android OAuth client still has to exist in the
 * Google Cloud project (registered against this app's package name and signing
 * certificate) for Google to trust the request at all — but its id is never
 * named in code.
 *
 * This is the client already configured for the website, which is what makes an
 * account created in a browser and one created in the app the same account.
 */
const val GOOGLE_WEB_CLIENT_ID =
    "93694195180-sl5j1kekhn2dbh3kkerpaj99lgpa7t1k.apps.googleusercontent.com"
