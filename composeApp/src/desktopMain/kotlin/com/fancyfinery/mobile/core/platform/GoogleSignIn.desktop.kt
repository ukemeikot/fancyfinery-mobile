package com.fancyfinery.mobile.core.platform

/**
 * Desktop preview host: no native Google sign-in, and none wanted.
 *
 * This target exists to look at the shared UI quickly, not to sign anyone in.
 * Reporting unavailable hides the button, which is also a useful reminder that
 * the preview is not the product.
 */
private object DesktopGoogleSignIn : GoogleSignIn {
    override val isAvailable: Boolean = false
    override suspend fun idToken(): String? =
        throw GoogleSignInException("Google sign-in isn't available in the preview.")
}

actual fun createGoogleSignIn(context: Any?): GoogleSignIn = DesktopGoogleSignIn
