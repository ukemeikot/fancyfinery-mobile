package com.fancyfinery.mobile.core.platform

import android.app.Activity
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential

/**
 * The outcome of one credential request.
 *
 * Three cases, kept distinct because collapsing them is what made this fail
 * silently: a token, a deliberate dismissal, and nothing available. The first
 * version returned null for the last two alike, so when the chooser could not
 * complete the app treated it as "the customer changed their mind" and showed
 * nothing at all — no sheet, no error, no clue.
 */
private sealed interface Attempt {
    data class Token(val value: String) : Attempt
    data object Cancelled : Attempt
    data class Unavailable(val reason: String) : Attempt
}

/**
 * Credential Manager — the Google account sheet, shown over the app.
 *
 * Two passes. The first asks only for accounts already authorised for this app
 * (the one-tap case); if there are none, the second opens it to every Google
 * account on the device. Doing it in that order matters: asking for all
 * accounts first shows a chooser to someone who has exactly one, and asking
 * only for authorised accounts and stopping leaves a first-time customer
 * staring at nothing.
 */
private class AndroidGoogleSignIn(private val activity: Activity) : GoogleSignIn {

    private val credentialManager = CredentialManager.create(activity)

    override val isAvailable: Boolean = true

    override suspend fun idToken(): String? {
        when (val first = request(filterByAuthorizedAccounts = true)) {
            is Attempt.Token -> return first.value
            // A dismissal on the first pass is a real decision — do not then
            // shove a second chooser in front of them.
            is Attempt.Cancelled -> return null
            is Attempt.Unavailable -> Unit // fall through and ask more widely
        }

        return when (val second = request(filterByAuthorizedAccounts = false)) {
            is Attempt.Token -> second.value
            is Attempt.Cancelled -> null
            // Nothing worked on either pass. This is the case that used to
            // vanish; it is reported now.
            is Attempt.Unavailable -> throw GoogleSignInException(second.reason)
        }
    }

    private suspend fun request(filterByAuthorizedAccounts: Boolean): Attempt {
        val option = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(filterByAuthorizedAccounts)
            .setServerClientId(GOOGLE_WEB_CLIENT_ID)
            // Auto-select only on the authorised pass; silently picking an
            // account a first-time customer never chose would be presumptuous.
            .setAutoSelectEnabled(filterByAuthorizedAccounts)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(option)
            .build()

        return try {
            val response = credentialManager.getCredential(context = activity, request = request)
            val credential = response.credential
            if (
                credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                Attempt.Token(GoogleIdTokenCredential.createFrom(credential.data).idToken)
            } else {
                Attempt.Unavailable("Google returned an unexpected credential type.")
            }
        } catch (e: GetCredentialCancellationException) {
            Attempt.Cancelled
        } catch (e: NoCredentialException) {
            Attempt.Unavailable(
                "No Google account is available on this device. Add one in " +
                    "Settings, or sign in with your email instead.",
            )
        } catch (e: GetCredentialException) {
            // The common cause in a fresh project is the Android OAuth client
            // not being registered for this package and signing certificate —
            // Google answers that by opening the chooser and closing it again
            // immediately, which is indistinguishable from a glitch unless the
            // real message is surfaced. So it is, and logged.
            Log.w(TAG, "Google sign-in failed (filtered=$filterByAuthorizedAccounts)", e)
            Attempt.Unavailable(explain(e))
        }
    }

    /**
     * Turn the library's exception into something a person can act on.
     *
     * The developer-configuration case is called out by name because it is the
     * one failure the customer cannot do anything about, and the one a tester
     * needs to recognise immediately.
     */
    private fun explain(e: GetCredentialException): String {
        val detail = e.errorMessage?.toString().orEmpty() + " " + (e.message ?: "")
        return when {
            detail.contains("28444") ||
                detail.contains("Developer console", ignoreCase = true) ||
                detail.contains("not set up", ignoreCase = true) ->
                "Google sign-in isn't configured for this build yet. " +
                    "Use your email to sign in for now."

            detail.contains("network", ignoreCase = true) ->
                "Couldn't reach Google. Check your connection and try again."

            else -> "Google sign-in isn't available right now. " +
                "You can sign in with your email instead."
        }
    }

    private companion object {
        const val TAG = "FancyGoogleSignIn"
    }
}

/** Unavailable when the app was not given an Activity to present from. */
private object UnavailableGoogleSignIn : GoogleSignIn {
    override val isAvailable: Boolean = false
    override suspend fun idToken(): String? =
        throw GoogleSignInException("Google sign-in isn't available.")
}

actual fun createGoogleSignIn(context: Any?): GoogleSignIn {
    // Credential Manager must present from an Activity. The app passes one; if
    // something else arrives, report unavailable rather than crash — the
    // sign-in screen then hides the button instead of offering a dead one.
    val activity = context as? Activity
    return if (activity != null) AndroidGoogleSignIn(activity) else UnavailableGoogleSignIn
}
