package com.fancyfinery.mobile.features.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fancyfinery.mobile.core.theme.Gold
import com.fancyfinery.mobile.core.theme.GoldChampagne
import com.fancyfinery.mobile.core.theme.GoldDeep
import com.fancyfinery.mobile.core.theme.GoldLight
import fancyfinerymobile.composeapp.generated.resources.Res
import fancyfinerymobile.composeapp.generated.resources.logo
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource

/**
 * The arrival sequence, reproduced from the website's `IntroSplash`.
 *
 * Same beats, same timings, so the app and the site feel like one house:
 *
 *   0.0s  black
 *   0.0s  logo fades up from 88% scale               (700ms, ease-out)
 *   0.4s  a gold light sweeps diagonally across it  (1600ms, ease-in-out)
 *   1.0s  "FANCY FINERY" fades in and rises 12dp     (900ms, ease-out)
 *   2.6s  the whole overlay fades away               (600ms)
 *
 * The sweep is the one piece that does not port directly. The web does it with
 * a `mix-blend-mode: screen` gradient and an animated `background-position`;
 * Compose has no blend-mode on a background, so it is drawn as a moving linear
 * gradient in a `drawWithContent` overlay using [BlendMode.Screen], which is
 * the same operation applied the same way — light added over the artwork.
 *
 * Unlike the web this plays on COLD START only, not once per session: a phone
 * app is resumed far more often than a website is revisited, and replaying a
 * 2.6s brand animation every time someone switches back from their messages
 * would be a tax on the people who use the app most.
 */
@Composable
fun SplashScreen(onFinished: () -> Unit) {
    val logoAlpha = remember { Animatable(0f) }
    val logoScale = remember { Animatable(0.88f) }
    val sweep = remember { Animatable(0f) }
    val sweepAlpha = remember { Animatable(0f) }
    val wordAlpha = remember { Animatable(0f) }
    val wordRise = remember { Animatable(12f) }
    val overlayAlpha = remember { Animatable(1f) }

    LaunchedEffect(Unit) {
        // Each beat runs in its own coroutine so they overlap, exactly as the
        // CSS delays do. Awaiting them in sequence would make it four times
        // longer and lose the overlap that makes it read as one movement.
        launch {
            logoAlpha.animateTo(1f, tween(700, easing = LinearOutSlowInEasing))
        }
        launch {
            logoScale.animateTo(1f, tween(700, easing = LinearOutSlowInEasing))
        }
        launch {
            delay(400)
            launch { sweepAlpha.animateTo(1f, tween(320)) }
            sweep.animateTo(1f, tween(1600, easing = FastOutSlowInEasing))
            sweepAlpha.animateTo(0f, tween(240))
        }
        launch {
            delay(1000)
            launch { wordAlpha.animateTo(1f, tween(900, easing = LinearOutSlowInEasing)) }
            wordRise.animateTo(0f, tween(900, easing = LinearOutSlowInEasing))
        }

        delay(2600)
        overlayAlpha.animateTo(0f, tween(600))
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            // Pure black, matching the website's `bg-black` — not the app's
            // Obsidian. The logo artwork has a true-black ground of its own, so
            // anything lighter would show its edges as a visible square.
            .background(Color.Black)
            .alpha(overlayAlpha.value),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {

            Box {
                Image(
                    painter = painterResource(Res.drawable.logo),
                    contentDescription = null, // decorative; the wordmark below names the house
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .size(132.dp)
                        .alpha(logoAlpha.value)
                        .scale(logoScale.value),
                )

                // The light sweep: a narrow gold band travelling diagonally,
                // added over the artwork rather than painted on top of it.
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .alpha(sweepAlpha.value)
                        .drawWithContent {
                            val travel = size.width * 2.6f
                            val x = -size.width + (sweep.value * travel)
                            drawRect(
                                brush = Brush.linearGradient(
                                    colorStops = arrayOf(
                                        0.0f to Color.Transparent,
                                        0.5f to Gold.copy(alpha = 0.9f),
                                        1.0f to Color.Transparent,
                                    ),
                                    start = Offset(x, 0f),
                                    end = Offset(x + size.width * 0.9f, size.height),
                                ),
                                blendMode = BlendMode.Screen,
                            )
                        },
                )
            }

            Box(modifier = Modifier.height(24.dp))

            Text(
                text = "FANCY FINERY",
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .alpha(wordAlpha.value)
                    .padding(top = wordRise.value.dp),
                style = TextStyle(
                    brush = Brush.verticalGradient(
                        colorStops = arrayOf(
                            0.00f to GoldChampagne,
                            0.42f to GoldLight,
                            0.60f to Gold,
                            1.00f to GoldDeep,
                        ),
                    ),
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    // The website sets tracking-[0.45em] here; this is the
                    // equivalent at this size and is what makes it read as a
                    // house mark rather than a heading.
                    letterSpacing = 9.sp,
                ),
            )
        }
    }
}
