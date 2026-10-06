package com.fancyfinery.mobile.core.storage

/**
 * Every key the app persists, in one place.
 *
 * [PreferenceStore] is a stringly-typed `get(key)`/`put(key, value)`, which is
 * what lets one interface sit over DataStore on Android, iOS and the desktop
 * preview host. The cost of that is a typo in a key string failing silently —
 * a misspelled read returns null, which looks exactly like "not set yet", so
 * the app signs the customer out instead of raising anything.
 *
 * Naming them here converts that into a compile error.
 *
 * These strings are a storage format, not labels: renaming one orphans whatever
 * is already on every installed device. A customer would be signed out and
 * shown onboarding again, so treat a rename as a migration, not a tidy-up.
 */
object PreferencesKeys {

    /** Better Auth session token, presented as `Authorization: Bearer`. */
    const val AUTH_TOKEN = "auth_token"

    /** Id of the signed-in customer, for scoping cached rows. */
    const val USER_ID = "user_id"

    /** Chosen display currency — the one the shopper is charged in. */
    const val CURRENCY = "currency"

    /** Set once the intro carousel has been seen, so it never shows twice. */
    const val ONBOARDING_COMPLETE = "onboarding_complete"
}
