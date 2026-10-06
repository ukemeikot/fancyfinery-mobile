package com.fancyfinery.mobile.features.product.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.fancyfinery.mobile.core.theme.Gold
import com.fancyfinery.mobile.features.catalog.data.remote.dto.ProductDetailDto
import com.fancyfinery.mobile.features.product.presentation.components.ReviewsSection
import com.fancyfinery.mobile.features.product.presentation.components.SelectableChipRow
import com.fancyfinery.mobile.features.product.presentation.components.SizeAndFitSection
import com.fancyfinery.mobile.features.product.presentation.components.StarRow

/**
 * One garment, in full.
 *
 * Carries what the website's `ProductDetail` carries: a swipeable gallery,
 * size and colour pickers, quantity, size-and-fit guidance, reviews, saving,
 * and a request for another colour.
 *
 * The add-to-bag bar is pinned rather than placed at the end of the page. On a
 * phone the page is long — gallery, description, fit notes, reviews — and a
 * buy button that has to be scrolled back to is the most common reason a
 * mobile storefront loses a sale it had already won.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductScreen(
    onBack: () -> Unit,
    onNeedsAuth: () -> Unit,
    onRequestColor: (productId: String, productName: String) -> Unit,
    viewModel: ProductViewModel,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(state.toast) {
        state.toast?.let {
            snackbar.showSnackbar(it)
            viewModel.dismissToast()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.onToggleSaved(onNeedsAuth) }) {
                        Icon(
                            imageVector = if (state.isSaved) {
                                Icons.Filled.Favorite
                            } else {
                                Icons.Filled.FavoriteBorder
                            },
                            contentDescription = if (state.isSaved) "Saved" else "Save",
                            tint = if (state.isSaved) Gold else MaterialTheme.colorScheme.onSurface,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        bottomBar = {
            state.product?.let { product ->
                AddToBagBar(
                    product = product,
                    enabled = state.canAddToBag && !state.isAdding,
                    busy = state.isAdding,
                    onAdd = viewModel::onAddToBag,
                )
            }
        },
    ) { padding ->
        when {
            state.isLoading -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }

            state.error != null -> Box(
                modifier = Modifier.fillMaxSize().padding(32.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = state.error!!,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                    TextButton(onClick = viewModel::load) { Text("Try again") }
                }
            }

            else -> state.product?.let { product ->
                ProductBody(
                    product = product,
                    state = state,
                    viewModel = viewModel,
                    onRequestColor = { onRequestColor(product.id, product.name) },
                    modifier = Modifier.padding(
                        top = padding.calculateTopPadding(),
                        bottom = padding.calculateBottomPadding(),
                    ),
                )
            }
        }
    }
}

@Composable
private fun ProductBody(
    product: ProductDetailDto,
    state: ProductState,
    viewModel: ProductViewModel,
    onRequestColor: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        Gallery(product = product)

        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            Spacer(Modifier.height(20.dp))

            Text(
                text = product.name,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = product.price.formatted,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                product.rating?.let { rating ->
                    Spacer(Modifier.size(12.dp))
                    StarRow(rating = rating, count = product.ratingCount)
                }
            }

            if (!product.inStock) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Currently unavailable",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            if (state.colors.isNotEmpty()) {
                Spacer(Modifier.height(20.dp))
                SelectableChipRow(
                    label = "Colour",
                    options = state.colors,
                    selected = state.selectedVariant?.color,
                    onSelect = viewModel::onSelectColor,
                )
            }

            if (state.sizes.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                SelectableChipRow(
                    label = "Size",
                    options = state.sizes,
                    selected = state.selectedVariant?.size,
                    onSelect = viewModel::onSelectSize,
                    // A size the house does not currently hold is shown but not
                    // selectable, which answers "do you have it at all?" — the
                    // question hiding it leaves unanswered.
                    unavailable = product.variants
                        .filter { !it.inStock }
                        .mapNotNull { it.size }
                        .toSet(),
                )
            }

            Spacer(Modifier.height(20.dp))
            QuantityRow(
                qty = state.qty,
                onChange = viewModel::onQtyChange,
            )

            product.description?.takeIf { it.isNotBlank() }?.let { description ->
                Spacer(Modifier.height(24.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(Modifier.height(24.dp))
            HorizontalDivider()
            SizeAndFitSection(product = product)

            HorizontalDivider()
            Spacer(Modifier.height(16.dp))
            TextButton(onClick = onRequestColor) {
                Text("Request another colour")
            }

            Spacer(Modifier.height(8.dp))
            HorizontalDivider()
            ReviewsSection(
                reviews = state.reviews,
                rating = product.rating,
                ratingCount = product.ratingCount,
            )

            Spacer(Modifier.height(32.dp))
        }
    }
}

/**
 * Swipeable gallery.
 *
 * Videos are skipped rather than rendered: the catalogue stores both stills and
 * clips under the same relation, and a video frame in a pager that cannot play
 * it is a black rectangle the customer will try to tap.
 */
@Composable
private fun Gallery(product: ProductDetailDto) {
    val images = remember(product.id) { product.images.filter { !it.isVideo } }
    if (images.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(3f / 4f)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "FF",
                style = MaterialTheme.typography.displaySmall,
                color = Gold.copy(alpha = 0.3f),
            )
        }
        return
    }

    val pagerState = rememberPagerState(pageCount = { images.size })

    Box {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(3f / 4f),
        ) { page ->
            AsyncImage(
                model = images[page].url,
                contentDescription = images[page].alt ?: product.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }

        if (images.size > 1) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                repeat(images.size) { index ->
                    Box(
                        modifier = Modifier
                            .size(if (index == pagerState.currentPage) 7.dp else 5.dp)
                            .background(
                                color = if (index == pagerState.currentPage) {
                                    Gold
                                } else {
                                    MaterialTheme.colorScheme.outline
                                },
                                shape = CircleShape,
                            ),
                    )
                }
            }
        }
    }
}

@Composable
private fun QuantityRow(qty: Int, onChange: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "Quantity",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.size(16.dp))
        IconButton(onClick = { onChange(-1) }, enabled = qty > 1) {
            Icon(Icons.Filled.Remove, contentDescription = "One fewer")
        }
        Text(
            text = "$qty",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        IconButton(onClick = { onChange(1) }) {
            Icon(Icons.Filled.Add, contentDescription = "One more")
        }
    }
}

@Composable
private fun AddToBagBar(
    product: ProductDetailDto,
    enabled: Boolean,
    busy: Boolean,
    onAdd: () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp,
        // Clears the system navigation bar. This screen is pushed above the
        // app's own bottom bar, so nothing else reserves that space — and a
        // plain Surface, unlike Material's NavigationBar, applies no insets of
        // its own. Without this the action bar renders under the system
        // buttons, which is exactly where it was.
        modifier = Modifier.windowInsetsPadding(
            WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom),
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = product.price.formatted,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            Button(
                onClick = onAdd,
                enabled = enabled,
                shape = RoundedCornerShape(2.dp),
                modifier = Modifier.heightIn(min = 50.dp),
            ) {
                if (busy) {
                    CircularProgressIndicator(
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(18.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                } else {
                    Text(
                        text = when {
                            !product.inStock -> "Sold out"
                            product.variants.isNotEmpty() && !enabled -> "Choose a size"
                            else -> "Add to bag"
                        },
                    )
                }
            }
        }
    }
}
