package com.fancyfinery.mobile.features.home.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import com.fancyfinery.mobile.core.ui.tabScaffoldInsets
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.fancyfinery.mobile.core.theme.BrandWordmark
import com.fancyfinery.mobile.core.theme.Gold
import com.fancyfinery.mobile.features.catalog.data.remote.dto.ProductSummaryDto
import com.fancyfinery.mobile.features.home.data.LookbookEntryDto
import com.fancyfinery.mobile.features.product.presentation.components.StarRow
import org.koin.compose.viewmodel.koinViewModel

/**
 * The front door.
 *
 * Mirrors the website's home page — featured pieces, collections, the lookbook
 * edit, what customers have said, and the Privé Circle — as horizontal shelves
 * rather than full-width sections. On a phone a shelf shows what is there and
 * invites a sideways flick; stacking the same content vertically would make the
 * page several screens long before reaching the second section.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onProductClick: (slug: String) -> Unit,
    onCategoryClick: (slug: String) -> Unit,
    onSeeAll: () -> Unit,
    viewModel: HomeViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        contentWindowInsets = tabScaffoldInsets(),
        topBar = {
            TopAppBar(
                title = { BrandWordmark(tagline = "ELEGANCE REDEFINED") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(top = padding.calculateTopPadding())) {
            when {
                state.isLoading -> CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                )

                state.error != null && state.isEmpty -> Column(
                    modifier = Modifier.align(Alignment.Center).padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = state.error!!,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    TextButton(onClick = viewModel::load) { Text("Try again") }
                }

                else -> PullToRefreshBox(
                    isRefreshing = state.isRefreshing,
                    onRefresh = viewModel::refresh,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {

                        if (state.categories.isNotEmpty()) {
                            item {
                                SectionTitle("Collections")
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    items(state.categories, key = { it.id }) { category ->
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(2.dp))
                                                .background(
                                                    MaterialTheme.colorScheme.surfaceVariant,
                                                )
                                                .clickable { onCategoryClick(category.slug) }
                                                .padding(horizontal = 18.dp, vertical = 12.dp),
                                        ) {
                                            Text(
                                                text = category.name,
                                                style = MaterialTheme.typography.labelLarge,
                                                color = MaterialTheme.colorScheme.onSurface,
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        if (state.featured.isNotEmpty()) {
                            item {
                                SectionTitle("Featured", onSeeAll = onSeeAll)
                                ProductShelf(state.featured, onProductClick)
                            }
                        }

                        if (state.newArrivals.isNotEmpty()) {
                            item {
                                SectionTitle("New in", onSeeAll = onSeeAll)
                                ProductShelf(state.newArrivals, onProductClick)
                            }
                        }

                        if (state.lookbook.isNotEmpty()) {
                            item {
                                SectionTitle("The Lookbook")
                                LookbookShelf(state.lookbook, onProductClick)
                            }
                        }

                        if (state.recentlyViewed.isNotEmpty()) {
                            item {
                                SectionTitle("Recently viewed")
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    items(state.recentlyViewed, key = { it.slug }) { entry ->
                                        Column(
                                            modifier = Modifier
                                                .width(120.dp)
                                                .clickable { onProductClick(entry.slug) },
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .aspectRatio(3f / 4f)
                                                    .clip(RoundedCornerShape(2.dp))
                                                    .background(
                                                        MaterialTheme.colorScheme.surfaceVariant,
                                                    ),
                                            ) {
                                                entry.imageUrl?.let {
                                                    AsyncImage(
                                                        model = it,
                                                        contentDescription = entry.name,
                                                        contentScale = ContentScale.Crop,
                                                        modifier = Modifier.fillMaxSize(),
                                                    )
                                                }
                                            }
                                            Text(
                                                text = entry.name,
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.padding(top = 6.dp),
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        if (state.reviews.isNotEmpty()) {
                            item {
                                SectionTitle("What customers say")
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    items(state.reviews, key = { it.id }) { review ->
                                        Column(
                                            modifier = Modifier
                                                .width(260.dp)
                                                .clip(RoundedCornerShape(2.dp))
                                                .background(
                                                    MaterialTheme.colorScheme.surfaceVariant,
                                                )
                                                .clickable { onProductClick(review.productSlug) }
                                                .padding(14.dp),
                                        ) {
                                            StarRow(
                                                rating = review.rating.toDouble(),
                                                count = 0,
                                            )
                                            Spacer(Modifier.height(6.dp))
                                            Text(
                                                text = review.body,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 4,
                                                overflow = TextOverflow.Ellipsis,
                                            )
                                            Spacer(Modifier.height(8.dp))
                                            Text(
                                                text = "${review.authorName} · ${review.productName}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Gold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            PriveCircle(
                                busy = state.isJoining,
                                message = state.newsletterMessage,
                                onJoin = viewModel::onJoinPriveCircle,
                            )
                        }

                        item { Spacer(Modifier.height(32.dp)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProductShelf(
    products: List<ProductSummaryDto>,
    onClick: (String) -> Unit,
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(products, key = { it.id }) { product ->
            Column(
                modifier = Modifier
                    .width(160.dp)
                    .clickable { onClick(product.slug) },
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(3f / 4f)
                        .clip(RoundedCornerShape(2.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center,
                ) {
                    if (product.image != null) {
                        AsyncImage(
                            model = product.image.url,
                            contentDescription = product.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else {
                        Text(
                            text = "FF",
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontFamily = FontFamily.Serif,
                            ),
                            color = Gold.copy(alpha = 0.3f),
                        )
                    }
                }
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 8.dp),
                )
                Text(
                    text = product.price.formatted,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.SemiBold,
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

@Composable
private fun LookbookShelf(
    entries: List<LookbookEntryDto>,
    onClick: (String) -> Unit,
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(entries, key = { it.slug }) { entry ->
            Box(
                modifier = Modifier
                    .width(240.dp)
                    .aspectRatio(3f / 4f)
                    .clip(RoundedCornerShape(2.dp))
                    .clickable { onClick(entry.slug) },
            ) {
                AsyncImage(
                    model = entry.imageUrl,
                    contentDescription = entry.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
                Text(
                    text = entry.name,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(12.dp),
                )
            }
        }
    }
}

/** Privé Circle — the website's VIP newsletter, as a shelf-height block. */
@Composable
private fun PriveCircle(
    busy: Boolean,
    message: String?,
    onJoin: (String) -> Unit,
) {
    var email by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
        Text(
            text = "The Privé Circle",
            style = MaterialTheme.typography.titleMedium.copy(fontFamily = FontFamily.Serif),
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Private previews and first access to new pieces.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(10.dp))

        message?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = Gold,
            )
            Spacer(Modifier.height(8.dp))
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                placeholder = { Text("Your email") },
                singleLine = true,
                shape = RoundedCornerShape(2.dp),
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.size(8.dp))
            Button(
                onClick = { onJoin(email) },
                enabled = !busy && email.contains('@'),
                shape = RoundedCornerShape(2.dp),
                modifier = Modifier.heightIn(min = 54.dp),
            ) {
                if (busy) {
                    CircularProgressIndicator(
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(16.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                } else {
                    Text("Join")
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String, onSeeAll: (() -> Unit)? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 8.dp, top = 20.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium.copy(fontFamily = FontFamily.Serif),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        onSeeAll?.let {
            TextButton(onClick = it) {
                Text("See all", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}
