package com.fancyfinery.mobile.features.catalog.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fancyfinery.mobile.core.theme.BrandWordmark
import com.fancyfinery.mobile.features.catalog.presentation.components.CategoryFilterRow
import com.fancyfinery.mobile.features.catalog.presentation.components.ProductCard
import com.fancyfinery.mobile.features.catalog.presentation.components.ProductCardSkeleton
import com.fancyfinery.mobile.features.catalog.presentation.components.SearchField
import org.koin.compose.viewmodel.koinViewModel

/**
 * The shop.
 *
 * A two-column grid, which is what a 3:4 fashion image wants on a phone: one
 * column wastes the screen and reduces how much a customer sees per scroll,
 * three makes the garments too small to judge.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatalogScreen(
    onProductClick: (slug: String) -> Unit,
    viewModel: CatalogViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val gridState = rememberLazyGridState()

    /**
     * Fetch the next page slightly BEFORE the end is reached, so the customer
     * scrolls into already-loaded content instead of into a spinner. Wrapped in
     * `derivedStateOf` so this recomputes on scroll without recomposing the
     * whole screen on every pixel.
     */
    val shouldLoadMore by remember {
        derivedStateOf {
            val last = gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val total = gridState.layoutInfo.totalItemsCount
            total > 0 && last >= total - 4
        }
    }

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) viewModel.loadMore()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { BrandWordmark(tagline = "ELEGANCE REDEFINED") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { padding ->
        // ONLY the top inset.
        //
        // This screen sits inside the app shell's Scaffold, which already
        // reserves space for the bottom bar and the system navigation. Applying
        // this Scaffold's full padding as well would inset the bottom twice —
        // which showed up as a band of dead space above the tab bar and a
        // second row of products clipped in half.
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = padding.calculateTopPadding()),
        ) {

            SearchField(
                value = state.query,
                onValueChange = viewModel::onQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            )

            if (state.categories.isNotEmpty()) {
                CategoryFilterRow(
                    categories = state.categories,
                    selected = state.selectedCategory,
                    onSelect = viewModel::onCategorySelected,
                )
            }

            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    // A skeleton rather than a spinner: it occupies the shape
                    // the catalogue will, so nothing jumps when it arrives.
                    state.isLoading -> SkeletonGrid()

                    state.error != null -> ErrorState(
                        message = state.error!!,
                        retryable = state.retryable,
                        onRetry = viewModel::retry,
                        modifier = Modifier.align(Alignment.Center),
                    )

                    state.isEmpty -> Text(
                        text = if (state.query.isBlank()) {
                            "Nothing here yet."
                        } else {
                            "Nothing matched \"${state.query}\"."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.Center).padding(32.dp),
                    )

                    else -> PullToRefreshBox(
                        isRefreshing = state.isRefreshing,
                        onRefresh = viewModel::refresh,
                        modifier = Modifier.fillMaxSize(),
                    ) {
                      LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        state = gridState,
                        contentPadding = PaddingValues(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp),
                        modifier = Modifier
                            .fillMaxSize()
                            // Dim rather than blank while re-filtering: the
                            // customer keeps their context.
                            .alpha(if (state.isRefiltering) 0.45f else 1f),
                    ) {
                        items(
                            items = state.products,
                            key = { product -> product.id },
                        ) { product ->
                            ProductCard(
                                product = product,
                                onClick = { onProductClick(product.slug) },
                            )
                        }

                        if (state.isLoadingMore) {
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                Box(
                                    modifier = Modifier.fillMaxWidth().height(64.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    CircularProgressIndicator(strokeWidth = 2.dp)
                                }
                            }
                        }
                      }
                    }
                }
            }
        }
    }
}

/**
 * The first-load placeholder: a full grid of shimmering tiles.
 *
 * Shaped exactly like the real grid — same columns, same padding, same 3:4
 * tiles — so the transition into real content is a fade rather than a reflow.
 */
@Composable
private fun SkeletonGrid() {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        userScrollEnabled = false,
        modifier = Modifier.fillMaxSize(),
    ) {
        items(count = 6) { ProductCardSkeleton() }
    }
}

@Composable
private fun ErrorState(
    message: String,
    retryable: Boolean,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        if (retryable) {
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onRetry) { Text("Try again") }
        }
    }
}
