package com.fancyfinery.mobile.core.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.fancyfinery.mobile.features.auth.presentation.AuthScreen
import com.fancyfinery.mobile.features.catalog.presentation.CatalogScreen
import com.fancyfinery.mobile.features.onboarding.presentation.OnboardingScreen
import com.fancyfinery.mobile.features.splash.SplashScreen

/**
 * The app shell: a back stack, plus the bottom bar.
 *
 * Navigation 3 hands you the stack as a plain list rather than hiding it behind
 * a controller, which makes one distinction visible that is easy to get wrong:
 * going FORWARD is `add`, while finishing a flow is `clear` then `add`. Splash,
 * onboarding and sign-in all use the second form — a customer must not be able
 * to press Back into a splash screen or into a sign-in they just completed.
 *
 * The bottom bar is hidden for the full-screen flows (splash, onboarding, auth)
 * and on the product screen, which wants the whole display for photography.
 */
@Composable
fun AppNavigation(startDestination: AppDestination) {
    val backStack = remember { mutableStateListOf(startDestination) }

    fun replaceAll(destination: AppDestination) {
        backStack.clear()
        backStack.add(destination)
    }

    /**
     * Switching tabs REPLACES the root rather than pushing.
     *
     * Pushing would make Back walk through every tab the customer has ever
     * visited, which is the single most common complaint about hand-rolled
     * bottom bars — twelve Back presses to leave an app.
     */
    fun selectTab(destination: AppDestination) {
        if (backStack.lastOrNull() == destination) return
        replaceAll(destination)
    }

    val current = backStack.lastOrNull()
    val activeTab = current?.let { TopLevelTab.of(it) }
    val showBottomBar = activeTab != null

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                ) {
                    TopLevelTab.entries.forEach { tab ->
                        NavigationBarItem(
                            selected = activeTab == tab,
                            onClick = { selectTab(tab.destination) },
                            icon = {
                                Icon(
                                    imageVector = tab.icon,
                                    contentDescription = tab.label,
                                )
                            },
                            label = {
                                Text(
                                    text = tab.label,
                                    style = MaterialTheme.typography.labelSmall,
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.onSurface,
                                selectedTextColor = MaterialTheme.colorScheme.onSurface,
                                indicatorColor = MaterialTheme.colorScheme.surfaceVariant,
                            ),
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavDisplay(
            backStack = backStack,
            onBack = { if (backStack.size > 1) backStack.removeLastOrNull() },
            modifier = Modifier.padding(
                // Only inset for the bar when there IS one; a full-screen flow
                // would otherwise sit above a strip of empty space.
                bottom = if (showBottomBar) padding.calculateBottomPadding() else 0.dp,
            ),
            entryProvider = entryProvider {

                entry<AppDestination.Splash> {
                    SplashScreen(onFinished = { replaceAll(AppDestination.Catalog) })
                }

                entry<AppDestination.Onboarding> {
                    OnboardingScreen(onComplete = { replaceAll(AppDestination.Catalog) })
                }

                entry<AppDestination.Auth> { destination ->
                    AuthScreen(
                        initialMode = destination.initialMode,
                        onAuthSuccess = { replaceAll(AppDestination.Account) },
                        onDismiss = { replaceAll(AppDestination.Catalog) },
                    )
                }

                entry<AppDestination.Catalog> {
                    CatalogScreen(
                        onProductClick = { slug ->
                            backStack.add(AppDestination.Product(slug))
                        },
                    )
                }

                entry<AppDestination.Wishlist> { ComingSoon("Saved") }
                entry<AppDestination.Bag> { ComingSoon("Bag") }
                entry<AppDestination.Account> { ComingSoon("Account") }

                entry<AppDestination.Product> { destination ->
                    ComingSoon(destination.slug)
                }
            },
        )
    }
}

/** Placeholder for a tab whose screen is not built yet. */
@Composable
private fun ComingSoon(label: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
