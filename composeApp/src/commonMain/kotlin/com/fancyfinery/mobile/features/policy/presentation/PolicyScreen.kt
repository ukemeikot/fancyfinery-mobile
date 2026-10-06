package com.fancyfinery.mobile.features.policy.presentation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.fancyfinery.mobile.core.network.NetworkConfig
import com.fancyfinery.mobile.core.platform.UrlOpener
import org.koin.compose.koinInject

/**
 * The house's standing pages: about, contact, shipping, privacy, terms.
 *
 * The copy lives here rather than being fetched, because it changes rarely and
 * an app that cannot explain its own returns policy offline is worse than one
 * carrying a few hundred words. The legal pages are the exception — those are
 * published on the website, are longer than anyone wants to scroll on a phone,
 * and must be the version actually in force, so they open in a browser tab
 * instead of being duplicated here where a copy could silently go stale.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PolicyScreen(title: String, onBack: () -> Unit) {
    val urlOpener = koinInject<UrlOpener>()
    val page = pageFor(title)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title, style = MaterialTheme.typography.titleMedium) },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    top = padding.calculateTopPadding(),
                    // Pushed above the bottom bar, so nothing else reserves the
                    // system navigation bar — this screen must clear it itself.
                    bottom = padding.calculateBottomPadding(),
                )
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
        ) {
            Spacer(Modifier.height(8.dp))
            page.body.forEach { paragraph ->
                Text(
                    text = paragraph,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 14.dp),
                )
            }

            page.webPath?.let { path ->
                TextButton(onClick = { urlOpener.open(NetworkConfig.BASE_URL + path) }) {
                    Text("Read the full ${title.lowercase()} on our site")
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

private data class PolicyPage(val body: List<String>, val webPath: String? = null)

private fun pageFor(title: String): PolicyPage = when (title) {
    "About" -> PolicyPage(
        listOf(
            "Fancy Finery is a curated luxury fashion house — refined ready-to-wear " +
                "and statement pieces, made to be worn rather than kept for best.",
            "Every piece is chosen for cut and cloth first. We keep the collection " +
                "small on purpose: it is easier to stand behind a short list.",
            "We ship worldwide, with flat-rate local delivery across Nigeria.",
        ),
    )

    "Contact" -> PolicyPage(
        listOf(
            "We read everything that reaches us, and a person replies.",
            "Email: fancyxquisite@gmail.com",
            "For an order, include the order reference from your confirmation — it " +
                "is the fastest way for us to find it.",
            "To ask for a piece in another colour, use \"Request another colour\" on " +
                "the product itself; it reaches us with the piece and size attached.",
        ),
    )

    "Shipping & returns" -> PolicyPage(
        listOf(
            "Delivery within Nigeria is a flat fee by area, shown at checkout once " +
                "you choose your state and area.",
            "International delivery is priced by parcel weight and destination. The " +
                "exact figure is calculated at checkout before you pay — there are " +
                "no charges added afterwards.",
            "Estimated delivery windows are shown with each option at checkout, and " +
                "again on your order once it ships.",
            "If something is not right, contact us within 14 days of delivery.",
        ),
        webPath = "/shipping",
    )

    "Privacy" -> PolicyPage(
        listOf(
            "We collect what we need to take and deliver an order: your name, " +
                "contact details and delivery address.",
            "Card details never reach us. Payments are handled on the provider's " +
                "own hosted page — which is why paying opens a browser tab rather " +
                "than a form inside the app.",
        ),
        webPath = "/privacy",
    )

    "Terms" -> PolicyPage(
        listOf(
            "Prices are shown and charged in the currency you select. The figure on " +
                "the price tag is the figure charged — there is no conversion added " +
                "at checkout.",
            "An order is confirmed when payment clears, or on placement where the " +
                "order is payable on delivery.",
        ),
        webPath = "/terms",
    )

    else -> PolicyPage(listOf("This page is not available yet."))
}
