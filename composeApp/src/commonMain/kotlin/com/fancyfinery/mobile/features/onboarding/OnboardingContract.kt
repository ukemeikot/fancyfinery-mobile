package com.fancyfinery.mobile.features.onboarding

typealias OnOnboardingComplete = () -> Unit

data class OnboardingPage(
    val title: String,
    val description: String,
    val emoji: String,
)

val defaultOnboardingPages = listOf(
    OnboardingPage(
        emoji = "🚀",
        title = "Welcome",
        description = "This starter is your launchpad. Pull it, rename it, ship it.",
    ),
    OnboardingPage(
        emoji = "🎨",
        title = "Built for Teams",
        description = "Vertical slices keep features isolated. Each team owns their slice end-to-end.",
    ),
    OnboardingPage(
        emoji = "📱",
        title = "Every Platform",
        description = "One codebase. Android, iOS, Desktop, and Web — all from Kotlin.",
    ),
)
