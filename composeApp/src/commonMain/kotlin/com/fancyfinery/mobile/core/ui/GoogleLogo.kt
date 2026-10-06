package com.fancyfinery.mobile.core.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate

/**
 * The Google "G", drawn rather than bundled.
 *
 * Google's branding guidelines require their own mark on a Google sign-in
 * button — a generic icon or a letter G in the app's colours is not acceptable,
 * and is a thing app review checks.
 *
 * Drawn as vector paths instead of shipping a PNG for three reasons: it stays
 * sharp at any density, it needs no per-density asset, and the four brand
 * colours are exact rather than approximated by compression. The path data is
 * Google's own, on their 18x18 viewport, scaled to whatever size is asked for.
 */
@Composable
fun GoogleLogo(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        // The artwork is authored on an 18x18 grid; scale it to the slot it is
        // given and centre it, so a caller only has to pick a size.
        val side = minOf(size.width, size.height)
        val factor = side / VIEWPORT
        translate(left = (size.width - side) / 2f, top = (size.height - side) / 2f) {
            scale(scale = factor, pivot = androidx.compose.ui.geometry.Offset.Zero) {
                drawPath(path = parse(BLUE_PATH), color = GOOGLE_BLUE)
                drawPath(path = parse(GREEN_PATH), color = GOOGLE_GREEN)
                drawPath(path = parse(YELLOW_PATH), color = GOOGLE_YELLOW)
                drawPath(path = parse(RED_PATH), color = GOOGLE_RED)
            }
        }
    }
}

private fun parse(data: String): Path = PathParser().parsePathString(data).toPath()

private const val VIEWPORT = 18f

private val GOOGLE_BLUE = Color(0xFF4285F4)
private val GOOGLE_GREEN = Color(0xFF34A853)
private val GOOGLE_YELLOW = Color(0xFFFBBC05)
private val GOOGLE_RED = Color(0xFFEA4335)

// Google's own path data for the four-colour G.
private const val BLUE_PATH =
    "M17.64 9.2045c0-.6381-.0573-1.2518-.1636-1.8409H9v3.4814h4.8436c-.2086 1.125-.8427 " +
        "2.0782-1.7959 2.7164v2.2581h2.9087c1.7018-1.5668 2.6836-3.874 2.6836-6.615z"

private const val GREEN_PATH =
    "M9 18c2.43 0 4.4673-.806 5.9564-2.1805l-2.9087-2.2581c-.8059.54-1.8368.8591-3.0477.8591-2.344 " +
        "0-4.3282-1.5831-5.036-3.7104H.9574v2.3318C2.4382 15.9832 5.4818 18 9 18z"

private const val YELLOW_PATH =
    "M3.964 10.71c-.18-.54-.2822-1.1168-.2822-1.71s.1023-1.17.2823-1.71V4.9582H.9573A8.9965 " +
        "8.9965 0 0 0 0 9c0 1.4523.3477 2.8268.9573 4.0418L3.964 10.71z"

private const val RED_PATH =
    "M9 3.5795c1.3214 0 2.5077.4541 3.4405 1.346l2.5813-2.5814C13.4632.8918 11.426 0 9 " +
        "0 5.4818 0 2.4382 2.0168.9573 4.9582L3.9641 7.29C4.6718 5.1627 6.6559 3.5795 9 3.5795z"
