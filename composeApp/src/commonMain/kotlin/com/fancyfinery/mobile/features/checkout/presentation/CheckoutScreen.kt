package com.fancyfinery.mobile.features.checkout.presentation

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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fancyfinery.mobile.core.ui.PickerField
import com.fancyfinery.mobile.features.account.data.QuoteBreakdownDto
import org.koin.compose.viewmodel.koinViewModel

/**
 * Checkout.
 *
 * One scrolling page rather than the website's multi-step `CheckoutProgress`.
 * On a phone, steps add navigation to a form that is already short, and the
 * main cost of a single page — not seeing the total until the end — is removed
 * by pinning the summary to the bottom.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckoutScreen(
    onBack: () -> Unit,
    onSignIn: () -> Unit,
    onOutcome: (CheckoutOutcome) -> Unit,
    viewModel: CheckoutViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Checkout", style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        bottomBar = {
            if (!state.needsAuth && !state.isLoading) {
                SummaryBar(
                    breakdown = state.quote?.breakdown,
                    quoting = state.isQuoting,
                    placing = state.isPlacing,
                    enabled = state.canPlace,
                    onPlace = { viewModel.onPlaceOrder(onOutcome) },
                )
            }
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    top = padding.calculateTopPadding(),
                    bottom = padding.calculateBottomPadding(),
                ),
        ) {
            when {
                state.isLoading -> CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                )

                state.needsAuth -> Column(
                    modifier = Modifier.align(Alignment.Center).padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = "Sign in to check out",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "So we can send your confirmation and keep your order history.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = onSignIn, shape = RoundedCornerShape(2.dp)) {
                        Text("Sign in")
                    }
                }

                else -> Form(state = state, viewModel = viewModel)
            }
        }
    }
}

@Composable
private fun Form(state: CheckoutState, viewModel: CheckoutViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
    ) {
        Spacer(Modifier.height(8.dp))
        SectionHeader("Contact")
        Field("Full name", state.name, viewModel::onName)
        Field("Email", state.email, viewModel::onEmail, keyboard = KeyboardType.Email)
        Field("Phone", state.phone, viewModel::onPhone, keyboard = KeyboardType.Phone)

        Spacer(Modifier.height(16.dp))
        SectionHeader("Delivery address")

        PickerField(
            label = "Country",
            value = state.countryName,
            options = state.countries.map { it.code to "${it.flag.orEmpty()} ${it.name}".trim() },
            onSelect = { code ->
                state.countries.firstOrNull { it.code == code }?.let(viewModel::onCountry)
            },
        )

        Field("Street address", state.address, viewModel::onAddress)
        Field("Apartment / suite (optional)", state.apartment, viewModel::onApartment)
        Field("City", state.city, viewModel::onCity)
        Field("State / Province", state.stateProvince, viewModel::onStateProvince)
        Field("ZIP / Postal code", state.postal, viewModel::onPostal)

        /**
         * Nigerian local delivery.
         *
         * Shown only for Nigeria, and only as a pair: a state narrows the list,
         * an area sets the flat fee. The fee beside each area is for display —
         * the order re-reads it from the database by id, so what is charged
         * cannot be set from here.
         */
        if (state.isNigeria && state.ngStates.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            SectionHeader("Local delivery")
            Text(
                text = "Choose your area for a flat delivery fee.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))

            PickerField(
                label = "State",
                value = state.ngStates.firstOrNull { it.id == state.selectedNgStateId }?.name
                    ?: "Select a state",
                options = state.ngStates.map { it.id to it.name },
                onSelect = viewModel::onNgState,
            )

            if (state.ngAreas.isNotEmpty()) {
                PickerField(
                    label = "Area",
                    value = state.ngAreas.firstOrNull { it.id == state.selectedNgAreaId }?.name
                        ?: "Select an area",
                    options = state.ngAreas.map { it.id to "${it.name} — ${it.fee.formatted}" },
                    onSelect = viewModel::onNgArea,
                )
            } else if (state.selectedNgStateId != null) {
                // Not an error: the house simply has not priced that state, and
                // the weight engine quotes it instead.
                Text(
                    text = "No local areas set for that state — we'll quote by weight.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // Courier choice, when the weight engine offers more than one.
        val options = state.quote?.options.orEmpty()
        if (options.size > 1) {
            Spacer(Modifier.height(16.dp))
            SectionHeader("Delivery method")
            options.forEach { option ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(
                        selected = option.courierId == state.quote?.selectedCourierId,
                        onClick = { viewModel.onCourier(option.courierId) },
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = option.courierName,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        val days = listOfNotNull(option.minDays, option.maxDays)
                        if (days.size == 2) {
                            Text(
                                text = "${days[0]}–${days[1]} business days",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    Text(
                        text = if (option.free) "Free" else option.price.formatted,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        SectionHeader("Discount code")
        Field("Code (optional)", state.couponCode, viewModel::onCoupon)
        state.quote?.coupon?.message?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error,
            )
        }

        state.unavailableReason?.let {
            Spacer(Modifier.height(12.dp))
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }

        state.error?.let {
            Spacer(Modifier.height(12.dp))
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun SummaryBar(
    breakdown: QuoteBreakdownDto?,
    quoting: Boolean,
    placing: Boolean,
    enabled: Boolean,
    onPlace: () -> Unit,
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
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
            if (breakdown != null) {
                SummaryLine("Subtotal", breakdown.subtotal.formatted)
                SummaryLine("Delivery", breakdown.shipping.formatted)
                if (breakdown.tax.amount > 0) {
                    SummaryLine(
                        breakdown.taxLabel.ifBlank { "Tax" },
                        breakdown.tax.formatted,
                    )
                }
                if (breakdown.discount.amount > 0) {
                    SummaryLine(
                        "Discount" + (breakdown.discountCode?.let { " ($it)" } ?: ""),
                        "−${breakdown.discount.formatted}",
                    )
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "Total",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = breakdown.total.formatted,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            } else if (quoting) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(14.dp),
                    )
                    Spacer(Modifier.size(8.dp))
                    Text(
                        text = "Calculating delivery…",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(Modifier.height(10.dp))
            Button(
                onClick = onPlace,
                enabled = enabled,
                shape = RoundedCornerShape(2.dp),
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
            ) {
                if (placing) {
                    CircularProgressIndicator(
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(18.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                } else {
                    Text("Place order")
                }
            }
        }
    }
}

@Composable
private fun SummaryLine(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = 6.dp, top = 4.dp),
    )
}

@Composable
private fun Field(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    keyboard: KeyboardType = KeyboardType.Text,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = true,
        shape = RoundedCornerShape(2.dp),
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
            keyboardType = keyboard,
        ),
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
    )
}
