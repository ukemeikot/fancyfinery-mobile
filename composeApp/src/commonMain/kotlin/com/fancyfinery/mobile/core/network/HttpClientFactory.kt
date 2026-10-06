package com.fancyfinery.mobile.core.network

import com.fancyfinery.mobile.core.session.SessionStore
import dev.logickoder.retrostash.core.RetrostashStore
import dev.logickoder.retrostash.ktor.RetrostashPlugin
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.logging.SIMPLE
import io.ktor.client.request.header
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/**
 * The one configured [HttpClient].
 *
 * Two things are attached to every request here rather than at each call site,
 * because "every call site remembers" is not a property a codebase keeps:
 *
 *  - **The bearer token**, when signed in. Read from [SessionStore]'s in-memory
 *    cache, not from disk — the request pipeline cannot afford a DataStore read
 *    per call.
 *  - **The chosen currency**, on every request including GETs, because the
 *    catalogue has to price itself in it.
 */
fun createHttpClient(
    engine: HttpClientEngine,
    retrostashStore: RetrostashStore,
    session: SessionStore,
): HttpClient = HttpClient(
    engine = engine,
    block = {
        installRetrostash(store = retrostashStore)
        installContentNegotiation()
        installLogging()
        installTimeout()
        installDefaults(session = session)
    },
)

private fun HttpClientConfig<*>.installDefaults(session: SessionStore) {
    defaultRequest {
        url(NetworkConfig.BASE_URL)
        contentType(ContentType.Application.Json)

        session.token.value?.let { token ->
            header("Authorization", "Bearer $token")
        }
        header(NetworkConfig.CURRENCY_HEADER, session.currency.value)
    }
}

private fun HttpClientConfig<*>.installRetrostash(store: RetrostashStore) {
    install(plugin = RetrostashPlugin) {
        this.store = store
    }
}

private fun HttpClientConfig<*>.installContentNegotiation() {
    install(plugin = ContentNegotiation) {
        json(
            json = Json {
                // The server adds fields without asking the app first; an
                // unknown one must not fail the whole response.
                ignoreUnknownKeys = true
                isLenient = true
                explicitNulls = false
            },
        )
    }
}

private fun HttpClientConfig<*>.installLogging() {
    install(plugin = Logging) {
        logger = Logger.SIMPLE
        // Off in release: LogLevel.ALL prints the Authorization header, and on
        // older Android any installed app can read logcat.
        level = if (NetworkConfig.LOG_HTTP) LogLevel.ALL else LogLevel.NONE
    }
}

private fun HttpClientConfig<*>.installTimeout() {
    install(plugin = HttpTimeout) {
        requestTimeoutMillis = NetworkConfig.TIMEOUT_MS
        connectTimeoutMillis = NetworkConfig.TIMEOUT_MS
        socketTimeoutMillis = NetworkConfig.TIMEOUT_MS
    }
}
