package com.fancyfinery.mobile.features.onboarding

import fancyfinerymobile.composeapp.generated.resources.Res
import fancyfinerymobile.composeapp.generated.resources.onboarding_1
import fancyfinerymobile.composeapp.generated.resources.onboarding_2
import fancyfinerymobile.composeapp.generated.resources.onboarding_3
import org.jetbrains.compose.resources.DrawableResource

typealias OnOnboardingComplete = () -> Unit

data class OnboardingPage(
    val title: String,
    val description: String,
    val image: DrawableResource,
)

/**
 * The three cards a first-time customer sees.
 *
 * Full-bleed photography from the house's own catalogue rather than icons. For
 * a fashion app the clothes ARE the pitch — an emoji in a rounded square tells
 * someone nothing about whether they want to shop here, and three garments tell
 * them immediately.
 *
 * The copy replaces the starter's developer-facing text ("Vertical slices keep
 * features isolated"), which described the template rather than the shop and
 * would have been the very first thing a customer read. Each card now answers a
 * question someone opening a fashion app actually has: what this is, whether it
 * will fit, and how it reaches them.
 *
 * The images are bundled rather than fetched, because this screen is the first
 * thing shown on a cold start — often before any network call has returned, and
 * sometimes with no connection at all.
 */
val defaultOnboardingPages = listOf(
    OnboardingPage(
        image = Res.drawable.onboarding_1,
        title = "Elegance redefined",
        description = "A curated house of refined ready-to-wear and statement " +
            "pieces, chosen for cut and cloth first.",
    ),
    OnboardingPage(
        image = Res.drawable.onboarding_2,
        title = "Know it fits",
        description = "Every piece carries its cut, its fit notes and the " +
            "measurements of the model wearing it.",
    ),
    OnboardingPage(
        image = Res.drawable.onboarding_3,
        title = "Delivered to you",
        description = "Flat-rate delivery across Nigeria and worldwide " +
            "shipping, priced in full before you pay.",
    ),
)
