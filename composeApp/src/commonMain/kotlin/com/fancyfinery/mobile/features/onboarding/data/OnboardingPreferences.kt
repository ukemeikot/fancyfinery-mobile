package com.fancyfinery.mobile.features.onboarding.data

import com.fancyfinery.mobile.core.storage.PreferenceStore

class OnboardingPreferences(private val store: PreferenceStore) {
    suspend fun isComplete(): Boolean = store.get("onboarding_complete") == "true"
    suspend fun markComplete() {
        store.put("onboarding_complete", "true")
    }
}
