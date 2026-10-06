package com.fancyfinery.mobile.core.di

import dev.logickoder.retrostash.core.RetrostashStore
import com.fancyfinery.mobile.core.database.AppDatabase
import com.fancyfinery.mobile.core.database.createDatabase
import com.fancyfinery.mobile.core.network.PreferenceRetrostashStore
import com.fancyfinery.mobile.core.network.createHttpClient
import com.fancyfinery.mobile.core.platform.createUrlOpener
import com.fancyfinery.mobile.core.session.SessionStore
import io.ktor.client.engine.HttpClientEngine
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Cross-cutting singletons: database, HTTP, session.
 *
 * [SessionStore] is declared here rather than in the auth feature on purpose.
 * The HTTP client needs it to attach the bearer token to every request, so it
 * is infrastructure that auth happens to write to — not the other way round.
 * Putting it in the auth module would make `core` depend on a feature.
 */
fun coreModule(context: Any?): Module = module {
    single { createDatabase(context = context) }
    single { get<AppDatabase>().userDao() }
    single<RetrostashStore> { PreferenceRetrostashStore(get()) }
    single { SessionStore(get()) }
    single { createHttpClient(get(), get(), get()) }
    single<HttpClientEngine> { platformHttpEngine() }
    // Needs the Android Context, so it is built from the same `context` the
    // database and preferences are.
    single { createUrlOpener(context) }
}

expect fun platformHttpEngine(): HttpClientEngine
