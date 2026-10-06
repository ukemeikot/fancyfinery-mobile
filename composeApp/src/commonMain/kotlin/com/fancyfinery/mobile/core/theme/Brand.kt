package com.fancyfinery.mobile.core.theme

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp

/**
 * The house wordmark.
 *
 * The website paints "Fancy Finery" with a four-stop vertical gradient —
 * champagne at the top falling to bronze at the bottom — which is what makes it
 * read as metal rather than as yellow text. Compose can do the same thing
 * honestly: a [Brush] on the [TextStyle], not an image, so it stays sharp at
 * any size and recolours with the theme.
 *
 * The stops come from `globals.css` unchanged. Their ORDER matters as much as
 * their values: lightest first. Reversing them lights the piece from below,
 * which reads as plastic.
 */
@Composable
fun BrandWordmark(
    modifier: Modifier = Modifier,
    fontSize: androidx.compose.ui.unit.TextUnit = 28.sp,
    tagline: String? = null,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Fancy Finery",
            textAlign = TextAlign.Center,
            style = TextStyle(
                brush = Brush.verticalGradient(
                    // Matches .brand-wordmark: 0%, 42%, 60%, 100%.
                    colorStops = arrayOf(
                        0.00f to GoldChampagne,
                        0.42f to GoldLight,
                        0.60f to Gold,
                        1.00f to GoldDeep,
                    ),
                ),
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                fontSize = fontSize,
                letterSpacing = 0.5.sp,
            ),
        )
        tagline?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.labelSmall.copy(
                    // Wide tracking is what makes a short line read as a house
                    // mark rather than as a sentence.
                    letterSpacing = 2.sp,
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}
