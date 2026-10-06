package com.fancyfinery.mobile.features.product.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.StarHalf
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fancyfinery.mobile.core.theme.Gold
import com.fancyfinery.mobile.features.catalog.data.remote.dto.ProductDetailDto
import com.fancyfinery.mobile.features.catalog.data.remote.dto.ReviewDto

/**
 * A star row, matching the website's `Stars`.
 *
 * Renders halves rather than rounding to whole stars: 4.5 shown as 5 overstates
 * the piece, and shown as 4 understates it. The numeral beside it is what most
 * people actually read, so it is never omitted.
 */
@Composable
fun StarRow(
    rating: Double,
    count: Int,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        repeat(5) { index ->
            val position = index + 1
            val icon = when {
                rating >= position -> Icons.Filled.Star
                rating >= position - 0.5 -> Icons.Filled.StarHalf
                else -> Icons.Filled.StarBorder
            }
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Gold,
                modifier = Modifier.size(15.dp),
            )
        }
        // Zero means "no tally to show" — a review's own stars need no count
        // beside them, and "(0)" beside them reads as nobody agreeing.
        if (count > 0) {
            Spacer(Modifier.size(6.dp))
            Text(
                text = "$rating ($count)",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * A labelled row of choices — sizes or colours.
 *
 * Options in [unavailable] are drawn but not selectable. Hiding them would be
 * tidier and worse: a customer looking for their size needs to know whether the
 * house makes it at all, which an absent chip does not tell them.
 */
@Composable
fun SelectableChipRow(
    label: String,
    options: List<String>,
    selected: String?,
    onSelect: (String) -> Unit,
    unavailable: Set<String> = emptySet(),
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { option ->
                val isOut = option in unavailable
                FilterChip(
                    selected = option == selected,
                    onClick = { if (!isOut) onSelect(option) },
                    enabled = !isOut,
                    shape = RoundedCornerShape(2.dp),
                    label = {
                        Text(
                            text = if (isOut) "$option — sold out" else option,
                            style = MaterialTheme.typography.labelLarge,
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                )
            }
        }
    }
}

/**
 * Size and fit — the app's `SizeAndFit`.
 *
 * Shows the cut, and the model's measurements when all three were recorded.
 * Those three travel together for a reason: "worn by a 178cm model in a size M"
 * is useful, while any one of those facts alone is not.
 */
@Composable
fun SizeAndFitSection(product: ProductDetailDto, modifier: Modifier = Modifier) {
    Column(modifier = modifier.padding(vertical = 20.dp)) {
        Text(
            text = "Size & fit",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(10.dp))

        Text(
            text = fitDescription(product.fitType),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        product.model?.let { model ->
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Our model is ${model.heightCm}cm and wears a size ${model.size}.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (product.weightGrams > 0) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Shipping weight: ${formatWeight(product.weightGrams)}.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Mirrors `fitNote` in the website's sizing domain. */
private fun fitDescription(fitType: String): String = when (fitType) {
    "slim" -> "Cut close to the body. Consider sizing up for a looser line."
    "relaxed" -> "An easy, relaxed cut with room through the body."
    "oversized" -> "Deliberately oversized. Size down for a closer fit."
    else -> "A regular fit — true to size."
}

/** Mirrors `formatWeight` in the website's product entity. */
private fun formatWeight(grams: Int): String =
    if (grams >= 1000) {
        val kg = grams / 1000.0
        val trimmed = if (kg % 1.0 == 0.0) kg.toInt().toString() else kg.toString()
        "$trimmed kg"
    } else {
        "$grams g"
    }

/**
 * Customer reviews.
 *
 * Collapsed to three by default. The full list can run to fifty, and a product
 * page that buries its fit notes and its buy button under them is worse for
 * everyone including the people writing reviews.
 */
@Composable
fun ReviewsSection(
    reviews: List<ReviewDto>,
    rating: Double?,
    ratingCount: Int,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val shown = if (expanded) reviews else reviews.take(3)

    Column(modifier = modifier.padding(vertical = 20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Reviews",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            rating?.let {
                Spacer(Modifier.size(12.dp))
                StarRow(rating = it, count = ratingCount)
            }
        }

        if (reviews.isEmpty()) {
            Spacer(Modifier.height(10.dp))
            Text(
                text = "No reviews yet.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            return@Column
        }

        shown.forEach { review ->
            Spacer(Modifier.height(16.dp))
            ReviewRow(review)
        }

        if (reviews.size > 3) {
            TextButton(onClick = { expanded = !expanded }) {
                Text(if (expanded) "Show fewer" else "All ${reviews.size} reviews")
            }
        }
    }
}

@Composable
private fun ReviewRow(review: ReviewDto) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = review.authorName,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                ),
                color = MaterialTheme.colorScheme.onSurface,
            )
            // The badge is most of what makes a review row credible, and the
            // server resolves it from a delivered order rather than trusting
            // the reviewer, so it is worth showing prominently.
            if (review.verified) {
                Spacer(Modifier.size(8.dp))
                Text(
                    text = "Verified purchase",
                    style = MaterialTheme.typography.labelSmall,
                    color = Gold,
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        StarRow(rating = review.rating.toDouble(), count = 0)
        review.title?.takeIf { it.isNotBlank() }?.let {
            Spacer(Modifier.height(6.dp))
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Medium,
                ),
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = review.body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
