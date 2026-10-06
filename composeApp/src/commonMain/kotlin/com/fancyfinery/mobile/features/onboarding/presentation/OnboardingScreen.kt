package com.fancyfinery.mobile.features.onboarding.presentation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.fancyfinery.mobile.core.theme.BrandWordmark
import com.fancyfinery.mobile.core.theme.Gold
import com.fancyfinery.mobile.features.onboarding.OnOnboardingComplete
import com.fancyfinery.mobile.features.onboarding.defaultOnboardingPages
import com.fancyfinery.mobile.features.onboarding.presentation.components.OnboardingPageContent
import org.koin.compose.viewmodel.koinViewModel

/**
 * The arrival carousel: three garments, full bleed.
 *
 * Laid out as overlays on the photograph rather than as a column beside it. The
 * clothes are the pitch, so they get the whole screen; the wordmark, the page
 * dots and the button sit on top, inside the system insets, over the scrim each
 * card draws for exactly this purpose.
 */
@Composable
fun OnboardingScreen(onComplete: OnOnboardingComplete) {
    val viewModel = koinViewModel<OnboardingViewModel>()
    val state by viewModel.state.collectAsState()

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {

        AnimatedContent(
            targetState = state.currentPage,
            transitionSpec = {
                // Crossfade WITH the slide: a hard cut between two photographs
                // reads as a glitch, where a dissolve reads as a lookbook.
                val forward = targetState > initialState
                val slideIn = slideInHorizontally(tween(420)) { if (forward) it else -it }
                val slideOut = slideOutHorizontally(tween(420)) { if (forward) -it else it }
                (slideIn + fadeIn(tween(420))) togetherWith (slideOut + fadeOut(tween(420)))
            },
            modifier = Modifier.fillMaxSize(),
            label = "onboarding_page",
        ) { page ->
            OnboardingPageContent(page = defaultOnboardingPages[page])
        }

        // --- Overlays ------------------------------------------------------

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BrandWordmark(fontSize = MaterialTheme.typography.titleLarge.fontSize)
            Spacer(Modifier.weight(1f))
            if (!state.isLastPage) {
                TextButton(onClick = { viewModel.onSkip(onComplete = onComplete) }) {
                    Text("Skip", color = Color.White)
                }
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 32.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(state.totalPages) { index ->
                    Box(
                        modifier = Modifier
                            .size(
                                width = if (index == state.currentPage) 24.dp else 8.dp,
                                height = 8.dp,
                            )
                            .background(
                                color = if (index == state.currentPage) {
                                    Gold
                                } else {
                                    Color.White.copy(alpha = 0.35f)
                                },
                                shape = CircleShape,
                            ),
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            Button(
                onClick = {
                    if (state.isLastPage) {
                        viewModel.onGetStarted(onComplete = onComplete)
                    } else {
                        viewModel.onNextPage()
                    }
                },
                shape = RoundedCornerShape(2.dp),
                colors = ButtonDefaults.buttonColors(
                    // Fixed white on black: this button sits on a photograph,
                    // so it must not follow the theme into a tone the scrim
                    // cannot separate from the image behind it.
                    containerColor = Color.White,
                    contentColor = Color.Black,
                ),
                modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp),
            ) {
                Text(if (state.isLastPage) "Enter the house" else "Next")
            }
        }
    }
}
