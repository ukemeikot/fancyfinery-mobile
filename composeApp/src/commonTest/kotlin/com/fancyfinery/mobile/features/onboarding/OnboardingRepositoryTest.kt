package com.fancyfinery.mobile.features.onboarding

import com.fancyfinery.mobile.core.storage.FakePreferenceStore
import com.fancyfinery.mobile.features.onboarding.data.OnboardingPreferences
import com.fancyfinery.mobile.features.onboarding.data.OnboardingRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class OnboardingRepositoryTest {

    private val store = FakePreferenceStore()
    private val repo = OnboardingRepository(prefs = OnboardingPreferences(store))

    @Test
    fun newUserHasNotCompletedOnboarding() = runTest {
        assertFalse(repo.hasCompletedOnboarding())
    }

    @Test
    fun markingCompleteReturnsTrueOnNextCheck() = runTest {
        repo.markOnboardingComplete()
        assertTrue(repo.hasCompletedOnboarding())
    }

    @Test
    fun completionStateDoesNotResetAcrossInstances() = runTest {
        repo.markOnboardingComplete()
        val repo2 = OnboardingRepository(prefs = OnboardingPreferences(store))
        assertTrue(repo2.hasCompletedOnboarding())
    }
}
