package com.fancyfinery.mobile.features.catalog.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.fancyfinery.mobile.features.catalog.data.remote.dto.CategoryDto

/**
 * Search box for the catalogue.
 *
 * Deliberately has NO search button. The view model debounces and re-queries as
 * the customer types, so a button would be a second way to do what already
 * happens — and the one thing people reliably want instead is a fast way to
 * clear, which is what the trailing icon is.
 */
@Composable
fun SearchField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        singleLine = true,
        shape = RoundedCornerShape(2.dp),
        placeholder = {
            Text(
                text = "Search the house",
                style = MaterialTheme.typography.bodyMedium,
            )
        },
        leadingIcon = {
            Icon(imageVector = Icons.Filled.Search, contentDescription = null)
        },
        trailingIcon = {
            if (value.isNotEmpty()) {
                IconButton(onClick = { onValueChange("") }) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Clear search",
                    )
                }
            }
        },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
    )
}

/**
 * Horizontal category filter.
 *
 * "All" is a real chip rather than an implicit cleared state, because a filter
 * you cannot see how to leave is the most common complaint about chip rows.
 */
@Composable
fun CategoryFilterRow(
    categories: List<CategoryDto>,
    selected: String?,
    onSelect: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            CategoryChip(
                label = "All",
                selected = selected == null,
                onClick = { onSelect(null) },
            )
        }
        items(items = categories, key = { it.id }) { category ->
            CategoryChip(
                label = category.name,
                selected = selected == category.slug,
                onClick = { onSelect(category.slug) },
            )
        }
    }
}

@Composable
private fun CategoryChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        shape = RoundedCornerShape(2.dp),
        label = { Text(text = label, style = MaterialTheme.typography.labelLarge) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primary,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
        ),
    )
}
