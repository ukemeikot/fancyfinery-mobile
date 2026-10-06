package com.fancyfinery.mobile.core.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * A read-only field that opens a searchable sheet.
 *
 * Replaces the website's `SelectMenu`, and exists for the same reason that one
 * does: the country list runs to around 250 entries, and a plain dropdown of
 * that length is unusable on a phone. The filter box is the whole point — it
 * was added to the web component for exactly this, and leaving it out here
 * would make the app's address form the worst place to choose a country.
 *
 * A bottom sheet rather than a dialog because it is reachable one-handed, which
 * on a long form matters more than it sounds.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PickerField(
    label: String,
    value: String,
    options: List<Pair<String, String>>,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var open by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Box(modifier = modifier.fillMaxWidth().padding(bottom = 8.dp)) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            enabled = false,
            label = { Text(label) },
            trailingIcon = {
                Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
            },
            shape = RoundedCornerShape(2.dp),
            modifier = Modifier.fillMaxWidth(),
        )
        // The field is disabled so it cannot be typed into; the click target is
        // this overlay, which keeps the whole row tappable rather than only the
        // trailing icon.
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable {
                    query = ""
                    open = true
                },
        )
    }

    if (open) {
        ModalBottomSheet(
            onDismissRequest = { open = false },
            sheetState = sheetState,
        ) {
            val filtered = remember(query, options) {
                if (query.isBlank()) {
                    options
                } else {
                    options.filter { it.second.contains(query, ignoreCase = true) }
                }
            }

            Column(modifier = Modifier.fillMaxHeight(0.9f)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("Search $label") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(2.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                )

                if (filtered.isEmpty()) {
                    Text(
                        text = "Nothing matched \"$query\".",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(24.dp),
                    )
                } else {
                    LazyColumn(modifier = Modifier.fillMaxWidth()) {
                        items(items = filtered, key = { it.first }) { (key, text) ->
                            Text(
                                text = text,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onSelect(key)
                                        open = false
                                    }
                                    .padding(horizontal = 20.dp, vertical = 14.dp),
                            )
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
    }
}
