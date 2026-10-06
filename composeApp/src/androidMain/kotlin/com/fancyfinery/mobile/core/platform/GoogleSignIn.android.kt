package com.fancyfinery.mobile.core.platform

import android.app.Activity
import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential

/**
 * Credential Manager — the Google account sheet, shown over the app.
 *
 * Two passes, deliberately. The first asks only for accounts already signed in
 * on the device (`filterByAuthorizedAccounts = true`), which is the one-tap
 * case and the whole point of the native flow. If there are none, the second
 * pass opens it up to every account on the device.
 *
 * Doing it in that order matters: asking for all accounts first shows a chooser
 * to someone who has exactly one, and asking only for authorised accounts and
 * stopping there leaves a first-time customer with a sheet that says nothing is
 * available.
 */
private class AndroidGoogleSignIn(private val activity: Activity) : GoogleSignIn {

    private val credentialManager = CredentialManager.create(activity)

    override val isAvailable: Boolean = true

    override suspend fun idToken(): String? {
        val authorized = request(filterByAuthorizedAccounts = true)
        if (authorized != null) return authorized
        return request(filterByAuthorizedAccounts = false)
    }

    private suspend fun request(filterByAuthorizedAccounts: Boolean): String? {
        val option = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(filterByAuthorizedAccounts)
            .setServerClientId(GOOGLE_WEB_CLIENT_ID)
            // Skips the chooser when exactly one account is available, but only
            // on the authorized pass — auto-selecting an account a first-time
            // customer has not chosen would be presumptuous.
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
                GoogleIdTokenCredential.createFrom(credential.data).idToken
            } else {
                throw GoogleSignInException("Google returned an unexpected credential.")
            }
        } catch (e: GetCredentialCancellationException) {
            // The customer dismissed the sheet. Not an error.
            null
        } catch (e: NoCredentialException) {
            // No account matched this pass. Null lets the caller try the wider
            // one; on the wider pass it genuinely means no Google account is on
            // the device.
            null
        } catch (e: GetCredentialException) {
            throw GoogleSignInException(
                "Google sign-in isn't available on this device right now.",
                e,
            )
        }
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
    // some other context arrives, report unavailable rather than crashing — the
    // sign-in screen hides the button instead of offering a dead one.
    val activity = (context as? Activity)
        ?: (context as? Context)?.let { null }
    return if (activity != null) AndroidGoogleSignIn(activity) else UnavailableGoogleSignIn
}
