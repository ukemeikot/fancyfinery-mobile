package com.fancyfinery.mobile.features.onboarding.presentation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fancyfinery.mobile.features.onboarding.OnOnboardingComplete
import com.fancyfinery.mobile.features.onboarding.defaultOnboardingPages
import com.fancyfinery.mobile.features.onboarding.presentation.components.OnboardingPageContent
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun OnboardingScreen(onComplete: OnOnboardingComplete) {
    val viewModel = koinViewModel<OnboardingViewModel>()
    val state by viewModel.state.collectAsState()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
        content = {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding(),
                content = {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.End,
                        content = {
                            if (!state.isLastPage) {
                                TextButton(
                                    onClick = { viewModel.onSkip(onComplete = onComplete) },
                                    content = { Text(text = "Skip") },
                                )
                            }
                        },
                    )

                    AnimatedContent(
                        targetState = state.currentPage,
                        transitionSpec = {
                            when {
                                targetState > initialState -> slideInHorizontally { it } togetherWith slideOutHorizontally { -it }
                                else -> slideInHorizontally { -it } togetherWith slideOutHorizontally { it }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        content = { page ->
                            OnboardingPageContent(page = defaultOnboardingPages[page])
                        },
                        label = "onboarding_page",
                    )

                    PageIndicator(
                        currentPage = state.currentPage,
                        totalPages = state.totalPages,
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = {
                            when {
                                state.isLastPage -> viewModel.onGetStarted(onComplete = onComplete)
                                else -> viewModel.onNextPage()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 32.dp),
                        content = {
                            Text(
                                text = when {
                                    state.isLastPage -> "Get Started"; else -> "Next"
                                }
                            )
                        },
                    )

                    Spacer(modifier = Modifier.height(32.dp))
                },
            )
        },
    )
}

@Composable
private fun PageIndicator(
    currentPage: Int,
    totalPages: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        content = {
            repeat(totalPages) { index ->
                Box(
                    modifier = Modifier.size(
                        width = when {
                            index == currentPage -> 24.dp; else -> 8.dp
                        },
                        height = 8.dp,
                    ),
                    content = {
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            shape = CircleShape,
                            color = when {
                                index == currentPage -> MaterialTheme.colorScheme.primary
                                else -> MaterialTheme.colorScheme.surfaceVariant
                            },
                            content = {},
                        )
                    },
                )
            }
        },
    )
}
