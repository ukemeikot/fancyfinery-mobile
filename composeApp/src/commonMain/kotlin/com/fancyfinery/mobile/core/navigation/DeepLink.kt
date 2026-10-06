package com.fancyfinery.mobile.core.navigation

/**
 * Links from outside the app that should open a screen inside it.
 *
 * The app registers the website's own https URLs as App Links rather than
 * inventing a custom `fancyfinery://` scheme. That choice matters for the
 * password-reset email specifically: ONE link has to work for everybody. A
 * custom scheme in an email is a dead string for anyone without the app
 * installed — and the people most likely to be resetting a password on a
 * borrowed laptop are exactly the people who do not have it.
 *
 * With an https App Link, Android hands the URL to the app when it is installed
 * and verified, and to the browser when it is not. The email does not have to
 * know which, and nothing has to be decided when the message is sent.
 *
 * Parsing lives here, in shared code, so the same rules apply on both platforms
 * and can be tested without a device.
 */
sealed interface DeepLink {

    /** `/reset-password?token=…` — finish setting a new password. */
    data class ResetPassword(val token: String) : DeepLink

    /** `/products/<slug>` — a shared or emailed piece. */
    data class Product(val slug: String) : DeepLink

    /** `/account/orders/<id>` — a link from an order email. */
    data class Order(val orderId: String) : DeepLink

    companion object {

        /**
         * Resolve a URL to a destination, or null if it names nothing the app
         * handles.
         *
         * Deliberately tolerant about the host: the same build is pointed at
         * staging or production by one constant, and a link from the other
         * environment should still open the right screen rather than silently
         * doing nothing. What it is NOT tolerant about is a reset link with no
         * token — that is unusable, and opening an empty form would waste the
         * customer's time before telling them so.
         */
        fun parse(url: String?): DeepLink? {
            if (url.isNullOrBlank()) return null

            val withoutScheme = url.substringAfter("://", missingDelimiterValue = url)
            val pathAndQuery = withoutScheme.substringAfter('/', missingDelimiterValue = "")
            val path = "/" + pathAndQuery.substringBefore('?').trim('/')
            val query = pathAndQuery.substringAfter('?', missingDelimiterValue = "")

            return when {
                path.startsWith("/reset-password") ->
                    param(query, "token")?.let(::ResetPassword)

                path.startsWith("/products/") ->
                    path.removePrefix("/products/").takeIf { it.isNotBlank() }?.let(::Product)

                path.startsWith("/account/orders/") ->
                    path.removePrefix("/account/orders/").takeIf { it.isNotBlank() }?.let(::Order)

                else -> null
            }
        }

        private fun param(query: String, name: String): String? =
            query.split('&')
                .firstOrNull { it.startsWith("$name=") }
                ?.substringAfter('=')
                ?.takeIf { it.isNotBlank() }
                ?.let(::decode)

        /**
         * Minimal percent-decoding.
         *
         * A reset token is URL-safe base64 and needs no decoding in practice,
         * but a `+` in a query string means a space by convention and a token
         * that arrives with one would otherwise be rejected as invalid — a
         * failure that would look like an expired link.
         */
        private fun decode(value: String): String {
            val replaced = value.replace("+", " ")
            if (!replaced.contains('%')) return replaced
            return buildString {
                var i = 0
                while (i < replaced.length) {
                    val c = replaced[i]
                    if (c == '%' && i + 2 < replaced.length) {
                        val hex = replaced.substring(i + 1, i + 3)
                        val code = hex.toIntOrNull(16)
                        if (code != null) {
                            append(code.toChar())
                            i += 3
                            continue
                        }
                    }
                    append(c)
                    i++
                }
            }
        }
    }
}
