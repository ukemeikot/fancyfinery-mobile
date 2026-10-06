package com.fancyfinery.mobile.features.account.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import com.fancyfinery.mobile.core.ui.tabScaffoldInsets
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fancyfinery.mobile.core.theme.BrandWordmark
import com.fancyfinery.mobile.features.account.data.OrderSummaryDto
import org.koin.compose.viewmodel.koinViewModel

/**
 * The account hub.
 *
 * Signed out it is a sign-in prompt, not an empty profile — the two are
 * different states and conflating them is how an app ends up showing blank
 * fields to someone who simply has not signed in.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountScreen(
    onSignIn: () -> Unit,
    onOrderClick: (orderId: String) -> Unit,
    onOpenPolicy: (title: String) -> Unit,
    viewModel: AccountViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        contentWindowInsets = tabScaffoldInsets(),
        topBar = {
            TopAppBar(
                title = { Text("Account", style = MaterialTheme.typography.titleMedium) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = padding.calculateTopPadding()),
        ) {
            when {
                state.isLoading -> CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                )

                state.needsAuth -> SignedOut(
                    onSignIn = onSignIn,
                    currency = state.currency,
                    onCurrencyChange = viewModel::onCurrencyChange,
                    onOpenPolicy = onOpenPolicy,
                )

                else -> SignedIn(
                    state = state,
                    viewModel = viewModel,
                    onOrderClick = onOrderClick,
                    onSignedOut = onSignIn,
                    onOpenPolicy = onOpenPolicy,
                )
            }
        }
    }
}

@Composable
private fun SignedOut(
    onSignIn: () -> Unit,
    currency: String,
    onCurrencyChange: (String) -> Unit,
    onOpenPolicy: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(24.dp))
        BrandWordmark(tagline = "ELEGANCE REDEFINED")
        Spacer(Modifier.height(28.dp))
        Text(
            text = "Sign in to see your orders, saved pieces and saved address.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = onSignIn,
            shape = RoundedCornerShape(2.dp),
            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
        ) { Text("Sign in or create an account") }

        Spacer(Modifier.height(28.dp))
        CurrencySection(selected = currency, onSelect = onCurrencyChange)
        Spacer(Modifier.height(20.dp))
        PolicyLinks(onOpenPolicy)
    }
}

@Composable
private fun SignedIn(
    state: AccountState,
    viewModel: AccountViewModel,
    onOrderClick: (String) -> Unit,
    onSignedOut: () -> Unit,
    onOpenPolicy: (String) -> Unit,
) {
    val profile = state.profile

    var fullName by remember(profile?.id) { mutableStateOf(profile?.fullName.orEmpty()) }
    var phone by remember(profile?.id) { mutableStateOf(profile?.address?.phone.orEmpty()) }
    var address by remember(profile?.id) { mutableStateOf(profile?.address?.address.orEmpty()) }
    var city by remember(profile?.id) { mutableStateOf(profile?.address?.city.orEmpty()) }
    var stateField by remember(profile?.id) { mutableStateOf(profile?.address?.state.orEmpty()) }
    var country by remember(profile?.id) { mutableStateOf(profile?.address?.country.orEmpty()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
    ) {
        Spacer(Modifier.height(12.dp))
        Text(
            text = profile?.fullName?.takeIf { it.isNotBlank() } ?: "Welcome",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        profile?.email?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(Modifier.height(24.dp))
        SectionHeader("Orders")

        if (state.orders.isEmpty()) {
            Text(
                text = "No orders yet.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 8.dp),
            )
        } else {
            state.orders.forEach { order ->
                OrderRow(order = order, onClick = { onOrderClick(order.id) })
                HorizontalDivider()
            }
        }

        Spacer(Modifier.height(24.dp))
        SectionHeader("Delivery details")
        Text(
            text = "Used to prefill your next checkout.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(10.dp))

        Field("Full name", fullName) { fullName = it }
        Field("Phone", phone) { phone = it }
        Field("Address", address) { address = it }
        Field("City", city) { city = it }
        Field("State / Province", stateField) { stateField = it }
        Field("Country", country) { country = it }

        state.notice?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
            )
        }
        state.error?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }

        Spacer(Modifier.height(10.dp))
        Button(
            onClick = {
                viewModel.onSaveProfile(
                    fullName = fullName,
                    phone = phone,
                    address = address,
                    city = city,
                    state = stateField,
                    country = country,
                )
            },
            enabled = !state.isSaving,
            shape = RoundedCornerShape(2.dp),
            modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp),
        ) {
            if (state.isSaving) {
                CircularProgressIndicator(
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(18.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            } else {
                Text("Save details")
            }
        }

        Spacer(Modifier.height(28.dp))
        CurrencySection(selected = state.currency, onSelect = viewModel::onCurrencyChange)

        Spacer(Modifier.height(24.dp))
        PolicyLinks(onOpenPolicy)

        Spacer(Modifier.height(12.dp))
        TextButton(onClick = { viewModel.onSignOut(onSignedOut) }) {
            Text("Sign out", color = MaterialTheme.colorScheme.error)
        }
        Spacer(Modifier.height(32.dp))
    }
}

/**
 * Currency.
 *
 * Lives on the account screen because it is a standing preference, not a
 * per-screen control. Changing it re-prices everything: the figure shown is the
 * figure charged, there is no conversion at checkout.
 */
@Composable
private fun CurrencySection(selected: String, onSelect: (String) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        SectionHeader("Currency")
        Text(
            text = "Prices are shown and charged in this currency.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        // FlowRow, not Row: four chips do not fit across a phone, and a fixed
        // Row squeezed the last one until its label broke one letter per line.
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            CURRENCIES.forEach { (code, label) ->
                FilterChip(
                    selected = code == selected,
                    onClick = { onSelect(code) },
                    shape = RoundedCornerShape(2.dp),
                    label = { Text(label, style = MaterialTheme.typography.labelLarge) },
                )
            }
        }
    }
}

private val CURRENCIES = listOf(
    "NGN" to "₦ NGN",
    "USD" to "$ USD",
    "EUR" to "€ EUR",
    "GBP" to "£ GBP",
)

@Composable
private fun PolicyLinks(onOpen: (String) -> Unit) {
    Column {
        SectionHeader("Fancy Finery")
        listOf("About", "Contact", "Shipping & returns", "Privacy", "Terms").forEach { title ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpen(title) }
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            HorizontalDivider()
        }
    }
}

@Composable
private fun OrderRow(order: OrderSummaryDto, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                // Order ids are uuids; the last six characters are enough to
                // identify one in conversation and far easier to read.
                text = "Order ${order.id.takeLast(6).uppercase()}",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Medium,
                ),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = "${statusLabel(order.status)} · ${paymentLabel(order.paymentStatus)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = order.total.formatted,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.SemiBold,
            ),
            color = MaterialTheme.colorScheme.onSurface,
        )
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = 6.dp),
    )
}

@Composable
private fun Field(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = true,
        shape = RoundedCornerShape(2.dp),
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
    )
}

/** Human labels for the stored lifecycle values. */
fun statusLabel(status: String): String = when (status) {
    "processing" -> "Processing"
    "packed" -> "Packed"
    "shipped" -> "Shipped"
    "out_for_delivery" -> "Out for delivery"
    "delivered" -> "Delivered"
    "cancelled" -> "Cancelled"
    else -> status.replaceFirstChar { it.uppercase() }
}

fun paymentLabel(status: String): String = when (status) {
    "paid" -> "Paid"
    "unpaid" -> "Awaiting payment"
    "failed" -> "Payment failed"
    "refunded" -> "Refunded"
    else -> status
}
