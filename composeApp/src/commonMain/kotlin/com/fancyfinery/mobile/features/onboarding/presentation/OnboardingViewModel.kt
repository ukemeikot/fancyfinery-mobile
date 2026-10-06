package com.fancyfinery.mobile.features.onboarding.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fancyfinery.mobile.features.onboarding.OnOnboardingComplete
import com.fancyfinery.mobile.features.onboarding.data.OnboardingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class OnboardingViewModel(
    private val repository: OnboardingRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(OnboardingState())
    val state: StateFlow<OnboardingState> = _state.asStateFlow()

    fun onNextPage() {
        _state.update { if (!it.isLastPage) it.copy(currentPage = it.currentPage + 1) else it }
    }

    fun onPreviousPage() {
        _state.update { if (it.currentPage > 0) it.copy(currentPage = it.currentPage - 1) else it }
    }

    fun onSkip(onComplete: OnOnboardingComplete) {
        finishOnboarding(onComplete = onComplete)
    }

    fun onGetStarted(onComplete: OnOnboardingComplete) {
        finishOnboarding(onComplete = onComplete)
    }

    private fun finishOnboarding(onComplete: OnOnboardingComplete) {
        viewModelScope.launch {
            repository.markOnboardingComplete()
            onComplete()
        }
    }
}
