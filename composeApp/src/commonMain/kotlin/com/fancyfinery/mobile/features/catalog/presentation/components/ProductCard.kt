package com.fancyfinery.mobile.features.catalog.presentation.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImagePainter
import coil3.compose.rememberAsyncImagePainter
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fancyfinery.mobile.core.theme.Gold
import com.fancyfinery.mobile.features.catalog.data.remote.dto.ProductSummaryDto

/**
 * One garment in the grid.
 *
 * The image is 3:4 — portrait, the proportion fashion photography is actually
 * shot in. A square crop would cut hems and shoulders, which is the detail a
 * customer is trying to judge.
 *
 * Three image outcomes are handled separately, because collapsing them is what
 * makes a catalogue look broken:
 *
 *   loading     — a shimmer, so the tile has weight while the bytes arrive
 *   loaded      — the photograph
 *   none/failed — the house monogram, dimmed
 *
 * That last case is not hypothetical: several pieces in the catalogue have no
 * photograph yet, and an empty grey rectangle reads as a bug rather than as
 * "no picture". The monogram reads as deliberate.
 */
@Composable
fun ProductCard(
    product: ProductSummaryDto,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(bottom = 8.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(3f / 4f)
                .clip(RoundedCornerShape(2.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            val url = product.image?.url
            if (url == null) {
                MissingImage()
            } else {
                val painter = rememberAsyncImagePainter(model = url)
                val painterState by painter.state.collectAsStateWithLifecycle()

                when (painterState) {
                    is AsyncImagePainter.State.Loading -> ShimmerBlock()
                    is AsyncImagePainter.State.Error -> MissingImage()
                    else -> Unit
                }

                Image(
                    painter = painter,
                    contentDescription = product.image?.alt ?: product.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }

        Text(
            text = product.name,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 10.dp),
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(top = 4.dp),
        ) {
            Text(
                text = product.price.formatted,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                ),
                color = MaterialTheme.colorScheme.onSurface,
            )

            // Shown only once something has actually been rated: "0.0" under
            // every new piece reads as bad, not as unrated.
            product.rating?.let { rating ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Star,
                        contentDescription = null,
                        tint = Gold,
                        modifier = Modifier.size(13.dp),
                    )
                    Text(
                        text = " $rating",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

/**
 * Stands in for a piece that has no photograph yet.
 *
 * Draws the house initials rather than the logo artwork. The logo PNG has a
 * BLACK ground baked into it, so on the app's dark surface it renders as a
 * black square with barely-visible gold — which is exactly the "broken tile"
 * look this is here to avoid. Text in the brand gold reads as deliberate at any
 * size, costs nothing to draw, and needs no asset.
 */
@Composable
private fun BoxScope.MissingImage() {
    // Aligned directly in the TILE's own BoxScope, with no wrapper of its own.
    // A nested Box here kept collapsing to its content height — the monogram
    // ended up a quarter of the way down the tile rather than in the middle —
    // because a child of an `aspectRatio` Box is measured against the incoming
    // constraints, not the resolved ones. Aligning against the parent that
    // already has a definite size sidesteps the question entirely.
    Text(
        text = "FF",
        modifier = Modifier.align(Alignment.Center),
        style = MaterialTheme.typography.headlineMedium.copy(
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp,
        ),
        color = Gold.copy(alpha = 0.30f),
    )
}

/**
 * The shimmer used while an image loads and by the skeleton grid.
 *
 * A moving highlight rather than a spinner: it occupies the same shape the
 * content will, so the layout does not jump when the real thing arrives.
 */
@Composable
fun ShimmerBlock(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmer-progress",
    )

    val base = MaterialTheme.colorScheme.surfaceVariant
    val highlight = MaterialTheme.colorScheme.outlineVariant

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = listOf(base, highlight, base),
                    start = androidx.compose.ui.geometry.Offset(
                        x = -300f + progress * 900f,
                        y = 0f,
                    ),
                    end = androidx.compose.ui.geometry.Offset(
                        x = progress * 900f,
                        y = 300f,
                    ),
                ),
            ),
    )
}

/** A single placeholder tile, shaped exactly like a real [ProductCard]. */
@Composable
fun ProductCardSkeleton(modifier: Modifier = Modifier) {
    Column(modifier = modifier.padding(bottom = 8.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(3f / 4f)
                .clip(RoundedCornerShape(2.dp)),
        ) { ShimmerBlock() }

        Box(
            modifier = Modifier
                .padding(top = 10.dp)
                .fillMaxWidth(0.85f)
                .height(13.dp)
                .clip(RoundedCornerShape(2.dp)),
        ) { ShimmerBlock() }

        Box(
            modifier = Modifier
                .padding(top = 6.dp)
                .fillMaxWidth(0.4f)
                .height(13.dp)
                .clip(RoundedCornerShape(2.dp)),
        ) { ShimmerBlock() }
    }
}
