package com.fancyfinery.mobile.features.auth.data

import com.fancyfinery.mobile.core.network.NetworkConfig
import com.fancyfinery.mobile.core.platform.GoogleSignIn
import com.fancyfinery.mobile.core.session.SessionStore
import com.fancyfinery.mobile.features.auth.data.local.UserDao
import com.fancyfinery.mobile.features.auth.data.local.UserEntity
import com.fancyfinery.mobile.features.auth.data.remote.AuthApi
import com.fancyfinery.mobile.features.auth.domain.model.User

/**
 * Everything the app can do with an account.
 *
 * Sits between Better Auth and the rest of the app, and owns three things that
 * must happen together or not at all:
 *
 *  1. **Store the bearer token** so later requests are authenticated.
 *  2. **Cache the user row** so the account screen can render before the
 *     network answers — and still render with no network at all.
 *  3. **Clear both on sign-out**, including when the server could not be
 *     reached, because a customer tapping "sign out" on a train must still end
 *     up signed out on their own device.
 */
class AuthRepository(
    private val api: AuthApi,
    private val session: SessionStore,
    private val userDao: UserDao,
    private val google: GoogleSignIn,
) {

    /**
     * Sign in with email and password.
     *
     * Returns null when the credentials were accepted but no session was
     * issued, which happens when the address has not been confirmed — the
     * server requires verification before sign-in. The caller shows
     * "check your email" rather than treating it as a failure.
     */
    suspend fun signIn(email: String, password: String): User? {
        val result = api.signIn(email = email.trim(), password = password)
        return result.token?.let { token -> persist(token, result.user) }
    }

    /**
     * Create an account.
     *
     * Usually returns null: `requireEmailVerification` is on, so a brand-new
     * account cannot sign in until the customer opens the confirmation email.
     * That is the expected path, not an error.
     */
    suspend fun signUp(name: String, email: String, password: String): User? {
        val result = api.signUp(
            name = name.trim(),
            email = email.trim(),
            password = password,
        )
        return result.token?.let { token -> persist(token, result.user) }
    }

    /** Email a one-time sign-in link. */
    suspend fun sendMagicLink(email: String) =
        api.sendMagicLink(email = email.trim(), callbackUrl = NetworkConfig.BASE_URL)

    /** Email a password-reset link. */
    suspend fun forgotPassword(email: String) =
        api.forgotPassword(
            email = email.trim(),
            // Points at the website's /reset-password, which is also the
            // app's App Link. Android hands a verified link to the app when it
            // is installed and to the browser when it is not, so ONE url serves
            // both without the email having to know which.
            redirectTo = "${NetworkConfig.BASE_URL}/reset-password",
        )

    /** Finish a reset started from the emailed link. */
    suspend fun resetPassword(token: String, newPassword: String) =
        api.resetPassword(token = token, newPassword = newPassword)

    /** Re-send the address-confirmation email. */
    suspend fun resendVerification(email: String) =
        api.resendVerification(email = email.trim(), callbackUrl = NetworkConfig.BASE_URL)

    /**
     * Sign in with Google, natively.
     *
     * The account sheet is shown by the platform, over the app; the ID token it
     * returns is exchanged server-side for a session. Returns null when the
     * customer dismisses the sheet, which is a cancellation rather than a
     * failure and must not be reported as one.
     */
    suspend fun signInWithGoogle(): User? {
        val idToken = google.idToken() ?: return null
        val result = api.signInWithGoogleIdToken(idToken)
        return result.token?.let { token -> persist(token, result.user) }
    }

    /** Whether this build can show the native sheet at all. */
    val isGoogleAvailable: Boolean get() = google.isAvailable

    /**
     * Adopt a token obtained outside the normal flow — currently the Google
     * redirect, which completes in a browser tab rather than in the app.
     */
    suspend fun adoptToken(token: String): User? {
        session.saveToken(token)
        // Confirm the token really is good before claiming the customer is in.
        val remote = api.session()
        val user = remote?.user ?: run {
            session.clear()
            return null
        }
        return persist(token, user)
    }

    /** The signed-in customer from the local cache, or null. */
    suspend fun currentUser(): User? {
        val id = session.userId() ?: return null
        return userDao.findById(id = id)?.toDomain()
    }

    /**
     * Ask the server who we are, and reconcile.
     *
     * Called at startup when a token exists. A token the server no longer
     * honours — revoked, expired, or issued by a different deployment — is
     * cleared here rather than being allowed to fail every subsequent request
     * with a 401 the UI has to interpret.
     */
    suspend fun refreshSession(): User? {
        if (!session.isSignedIn) return null
        val remote = api.session()
        if (remote?.user == null) {
            signOut()
            return null
        }
        return persist(session.token.value ?: return null, remote.user)
    }

    suspend fun signOut() {
        api.signOut()
        session.clear()
        userDao.deleteAll()
    }

    private suspend fun persist(
        token: String,
        dto: com.fancyfinery.mobile.features.auth.data.remote.dto.UserDto?,
    ): User? {
        if (dto == null) return null
        session.saveToken(token)
        session.saveUserId(dto.id)
        val entity = UserEntity(
            id = dto.id,
            email = dto.email.orEmpty(),
            name = dto.name.orEmpty(),
        )
        userDao.upsert(user = entity)
        return entity.toDomain()
    }

    private fun UserEntity.toDomain() = User(id = id, email = email, name = name)
}
