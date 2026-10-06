package com.fancyfinery.mobile.features.onboarding

typealias OnOnboardingComplete = () -> Unit

data class OnboardingPage(
    val title: String,
    val description: String,
    val emoji: String,
)

/**
 * The three cards a first-time customer sees.
 *
 * These replace the starter's developer-facing copy ("Vertical slices keep
 * features isolated"), which described the template rather than the shop and
 * would have been the very first thing a customer read.
 *
 * Each card answers a question someone opening a fashion app actually has —
 * what is this, can I trust the sizing, and how do I get it — rather than
 * selling features back to them. Three because it is short enough to swipe
 * through, and Skip is always available.
 */
val defaultOnboardingPages = listOf(
    OnboardingPage(
        emoji = "🕊",
        title = "Elegance redefined",
        description = "A curated house of refined ready-to-wear and statement " +
            "pieces, chosen for cut and cloth first.",
    ),
    OnboardingPage(
        emoji = "📏",
        title = "Know it fits",
        description = "Every piece carries its cut, its fit notes and the " +
            "measurements of the model wearing it.",
    ),
    OnboardingPage(
        emoji = "🌍",
        title = "Delivered to you",
        description = "Flat-rate delivery across Nigeria and worldwide " +
            "shipping, priced in full before you pay.",
    ),
)
