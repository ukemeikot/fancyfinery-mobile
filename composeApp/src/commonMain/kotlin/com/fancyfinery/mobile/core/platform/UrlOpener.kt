package com.fancyfinery.mobile.core.platform

/**
 * Open a URL in the platform's in-app browser.
 *
 * Needed for exactly two things, and both of them are things a WebView must not
 * do: the Google sign-in consent screen, and the payment provider's hosted card
 * page.
 *
 * An in-app browser tab is the right component for both because it keeps the
 * real browser's address bar and TLS indicator in front of the customer, and
 * shares the system browser's cookie jar — so someone already signed in to
 * Google is not asked for their password again. A WebView has neither property,
 * which is why Google blocks OAuth in one and why app reviewers object to card
 * forms in one.
 *
 *   Android — Chrome Custom Tabs
 *   iOS     — SFSafariViewController
 *   Desktop — the system browser (preview host only)
 */
expect class UrlOpener {
    fun open(url: String)
}

/** Obtain an opener. [context] is an Android `Context`; ignored elsewhere. */
expect fun createUrlOpener(context: Any?): UrlOpener
