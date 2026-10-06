package com.fancyfinery.mobile.core.platform

/**
 * iOS: not yet native.
 *
 * The native equivalent is Google's `GoogleSignIn` SDK, which has to be added
 * to the Xcode project through Swift Package Manager and configured with the
 * iOS OAuth client — neither of which can be done from a non-macOS machine, and
 * neither of which can be verified without building the app on one.
 *
 * Rather than ship something that compiles and fails on a device, this reports
 * unavailable. The sign-in screen then hides the Google button on iOS and
 * offers email, password and the one-time sign-in link, all of which work
 * today.
 *
 * To finish it: add GoogleSignIn-iOS via SPM, put the reversed client id in the
 * app's URL types, call `GIDSignIn.sharedInstance.signIn(withPresenting:)`, and
 * return `user.idToken?.tokenString` here. The server side already accepts it —
 * `/sign-in/social` takes `idToken` for any provider.
 */
private object IosGoogleSignIn : GoogleSignIn {
    override val isAvailable: Boolean = false
    override suspend fun idToken(): String? =
        throw GoogleSignInException("Google sign-in isn't available on iOS yet.")
}

actual fun createGoogleSignIn(context: Any?): GoogleSignIn = IosGoogleSignIn
