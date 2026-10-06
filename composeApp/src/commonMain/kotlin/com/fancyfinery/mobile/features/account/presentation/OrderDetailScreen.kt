package com.fancyfinery.mobile.features.account.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fancyfinery.mobile.core.theme.Gold
import com.fancyfinery.mobile.features.account.data.OrderDetailDto

/**
 * One order.
 *
 * Shows the fulfilment stage as a progress trail, the full receipt, and the two
 * actions a customer may still have: paying for an unpaid order, and cancelling
 * one that has not progressed. Both are decided server-side — the flags here
 * only decide whether a button is drawn.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderDetailScreen(
    onBack: () -> Unit,
    onPay: (orderId: String, url: String) -> Unit,
    viewModel: OrderDetailViewModel,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = state.order?.let { "Order ${it.id.takeLast(6).uppercase()}" }
                            ?: "Order",
                        style = MaterialTheme.typography.titleMedium,
                    )
                },
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
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    top = padding.calculateTopPadding(),
                    // Pushed above the bottom bar, so nothing else reserves the
                    // system navigation bar — this screen must clear it itself.
                    bottom = padding.calculateBottomPadding(),
                ),
        ) {
            when {
                state.isLoading -> CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                )

                state.error != null -> Text(
                    text = state.error!!,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.Center).padding(32.dp),
                )

                else -> state.order?.let { order ->
                    Body(
                        order = order,
                        busy = state.isBusy,
                        notice = state.notice,
                        onPay = { viewModel.onPay { url -> onPay(order.id, url) } },
                        onCancel = viewModel::onCancel,
                    )
                }
            }
        }
    }
}

@Composable
private fun Body(
    order: OrderDetailDto,
    busy: Boolean,
    notice: String?,
    onPay: () -> Unit,
    onCancel: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
    ) {
        Spacer(Modifier.height(8.dp))

        // Cancelled orders have left the trail entirely; drawing one would
        // imply the parcel is still coming.
        if (order.status != "cancelled") {
            StatusTrail(order.status)
            Spacer(Modifier.height(16.dp))
        } else {
            Text(
                text = "This order was cancelled.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
            Spacer(Modifier.height(12.dp))
        }

        Text(
            text = paymentLabel(order.paymentStatus),
            style = MaterialTheme.typography.labelLarge,
            color = if (order.paymentStatus == "paid") Gold else MaterialTheme.colorScheme.onSurfaceVariant,
        )

        order.trackingNumber?.let {
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Tracking: $it",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }

        order.courierName?.let { courier ->
            val days = listOfNotNull(order.estimatedMinDays, order.estimatedMaxDays)
            Spacer(Modifier.height(4.dp))
            Text(
                text = courier + if (days.size == 2) " · ${days[0]}–${days[1]} days" else "",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        notice?.let {
            Spacer(Modifier.height(12.dp))
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }

        if (order.payable || order.cancellable) {
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (order.payable) {
                    Button(
                        onClick = onPay,
                        enabled = !busy,
                        shape = RoundedCornerShape(2.dp),
                        modifier = Modifier.weight(1f).heightIn(min = 50.dp),
                    ) {
                        if (busy) {
                            CircularProgressIndicator(
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(16.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                            )
                        } else {
                            Text("Pay now")
                        }
                    }
                }
                if (order.cancellable) {
                    OutlinedButton(
                        onClick = onCancel,
                        enabled = !busy,
                        shape = RoundedCornerShape(2.dp),
                        modifier = Modifier.weight(1f).heightIn(min = 50.dp),
                    ) { Text("Cancel order") }
                }
            }
        }

        Spacer(Modifier.height(24.dp))
        HorizontalDivider()
        Spacer(Modifier.height(16.dp))

        Text(
            text = "ITEMS",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))

        order.items.forEach { item ->
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "${item.qty} × ${item.unitPrice.formatted}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = item.lineTotal.formatted,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }

        Spacer(Modifier.height(12.dp))
        HorizontalDivider()
        Spacer(Modifier.height(12.dp))

        Line("Subtotal", order.subtotal.formatted)
        Line("Delivery", order.shipping.formatted)
        if (order.tax.amount > 0) Line(order.taxLabel ?: "Tax", order.tax.formatted)
        if (order.discount.amount > 0) {
            Line(
                "Discount" + (order.discountCode?.let { " ($it)" } ?: ""),
                "−${order.discount.formatted}",
            )
        }

        Spacer(Modifier.height(6.dp))
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
                text = order.total.formatted,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                ),
                color = MaterialTheme.colorScheme.onSurface,
            )
        }

        Spacer(Modifier.height(24.dp))
        HorizontalDivider()
        Spacer(Modifier.height(16.dp))

        Text(
            text = "DELIVERING TO",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = listOfNotNull(
                order.address.name,
                order.address.address,
                order.address.apartment,
                order.address.city,
                order.address.state,
                order.address.postal,
                order.address.country,
            ).joinToString("\n"),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(32.dp))
    }
}

/**
 * The fulfilment trail.
 *
 * Shows every stage, with the ones reached filled in. A single current-status
 * label answers "where is it" but not "what happens next", which is the
 * question someone opens this screen to answer.
 */
@Composable
private fun StatusTrail(status: String) {
    val stages = listOf("processing", "packed", "shipped", "out_for_delivery", "delivered")
    val reached = stages.indexOf(status).let { if (it < 0) 0 else it }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            stages.forEachIndexed { index, _ ->
                Box(
                    modifier = Modifier
                        .size(if (index <= reached) 11.dp else 8.dp)
                        .background(
                            color = if (index <= reached) {
                                Gold
                            } else {
                                MaterialTheme.colorScheme.outlineVariant
                            },
                            shape = CircleShape,
                        ),
                )
                if (index < stages.lastIndex) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(2.dp)
                            .background(
                                if (index < reached) {
                                    Gold
                                } else {
                                    MaterialTheme.colorScheme.outlineVariant
                                },
                            ),
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = statusLabel(status),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun Line(label: String, value: String) {
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
