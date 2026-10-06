package com.fancyfinery.mobile.features.onboarding.presentation.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.fancyfinery.mobile.features.onboarding.OnboardingPage
import org.jetbrains.compose.resources.painterResource

/**
 * One onboarding card: a garment, full bleed, with the copy over it.
 *
 * The scrim is the load-bearing part. White text straight onto a photograph is
 * legible on one image and unreadable on the next — these are real catalogue
 * shots with pale backgrounds and bright garments, not art directed for text.
 * A vertical gradient from transparent to near-opaque black guarantees contrast
 * at the bottom whatever the picture does, and reads as a deliberate editorial
 * treatment rather than a patch.
 */
@Composable
fun OnboardingPageContent(
    page: OnboardingPage,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        Image(
            painter = painterResource(page.image),
            contentDescription = null, // decorative; the heading carries the meaning
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            // Slight darkening at the very top so the wordmark
                            // above the photograph never fights a bright sky.
                            0.00f to Color.Black.copy(alpha = 0.35f),
                            0.30f to Color.Transparent,
                            0.62f to Color.Black.copy(alpha = 0.55f),
                            1.00f to Color.Black.copy(alpha = 0.92f),
                        ),
                    ),
                ),
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .navigationBarsPadding()
                // Clears the page dots and the button, which the SCREEN draws
                // bottom-aligned over this same card. Both were anchored to the
                // bottom, so without this reservation the button sat squarely
                // on top of the heading.
                .padding(bottom = CONTROLS_HEIGHT)
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = page.title,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontFamily = FontFamily.Serif,
                ),
                // Fixed white rather than a theme colour: this sits on a
                // photograph, so it must not follow the surface palette into
                // dark-on-dark in light mode.
                color = Color.White,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = page.description,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.85f),
                textAlign = TextAlign.Center,
            )
        }
    }
}

/**
 * Vertical space the screen's controls occupy: page dots, the gap, the button
 * and its padding. Kept here beside the layout that has to avoid them, so the
 * two cannot drift apart silently.
 */
private val CONTROLS_HEIGHT = 124.dp
