package com.fancyfinery.mobile

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory
import coil3.network.ktor3.KtorNetworkFetcherFactory
import com.fancyfinery.mobile.core.di.appDeclaration
import com.fancyfinery.mobile.core.navigation.AppDestination
import com.fancyfinery.mobile.core.navigation.AppNavigation
import com.fancyfinery.mobile.core.session.SessionStore
import com.fancyfinery.mobile.core.theme.AppTheme
import com.fancyfinery.mobile.features.onboarding.data.OnboardingRepository
import io.ktor.client.HttpClient
import org.koin.compose.KoinApplication
import org.koin.compose.koinInject

@Composable
fun App(context: Any? = null) {
    KoinApplication(
        application = appDeclaration(context = context),
        content = {
            // Coil loads product imagery over the SAME Ktor client the API
            // uses, so images share its connection pool, timeouts and engine
            // rather than opening a second HTTP stack beside it.
            val httpClient = koinInject<HttpClient>()
            setSingletonImageLoaderFactory { platformContext ->
                ImageLoader.Builder(platformContext)
                    .components { add(KtorNetworkFetcherFactory(httpClient)) }
                    .build()
            }

            AppTheme { Content() }
        },
    )
}

@Composable
private fun Content() {
    val session = koinInject<SessionStore>()
    val onboarding = koinInject<OnboardingRepository>()
    var startDestination by remember { mutableStateOf<AppDestination?>(null) }

    LaunchedEffect(Unit) {
        // Hydrate the session BEFORE the first screen, so the HTTP client has
        // the bearer token in memory by the time anything requests. Doing this
        // lazily would send the opening catalogue call out unauthenticated.
        session.load()

        startDestination = if (onboarding.hasCompletedOnboarding()) {
            // The arrival sequence, then straight to the shop — signed in or
            // not.
            //
            // The website lets anyone browse and only asks for an account at
            // checkout, and the app matches that. Putting a sign-in wall in
            // front of a catalogue is the most reliable way to lose a
            // first-time customer who only wanted to see the clothes.
            AppDestination.Splash
        } else {
            AppDestination.Onboarding
        }
    }

    startDestination?.let { AppNavigation(startDestination = it) }
}
