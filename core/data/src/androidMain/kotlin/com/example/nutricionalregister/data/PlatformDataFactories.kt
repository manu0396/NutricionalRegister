package com.example.nutricionalregister.data

import android.content.Context
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.example.nutricionalregister.domain.product.EpochClock
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

internal fun createProductDatabase(context: Context): ProductDatabase = ProductDatabase(createProductDatabaseDriver(context))

internal fun createProductHttpClient(): HttpClient =
    HttpClient(OkHttp) {
        install(HttpTimeout) {
            requestTimeoutMillis = REQUEST_TIMEOUT_MILLIS
        }
    }

public fun createProductSearchRuntime(
    context: Context,
    clock: EpochClock,
    databaseDispatcher: CoroutineDispatcher = Dispatchers.IO,
): ProductSearchRuntime {
    val httpClient = createProductHttpClient()
    val databaseDriver = createProductDatabaseDriver(context)
    val database = ProductDatabase(databaseDriver)
    return ProductSearchRuntime(
        productSearchRepository =
            ProductSearchRepositoryImpl(
                database = database,
                catalogue =
                    com.example.nutricionalregister.data.remote.SearchAliciousClient(
                        httpClient = httpClient,
                        baseUrl = SEARCH_A_LICIOUS_ORIGIN,
                    ),
                clock = clock,
                databaseDispatcher = databaseDispatcher,
            ),
        recentSearchRepository =
            RecentSearchRepositoryImpl(
                database = database,
                databaseDispatcher = databaseDispatcher,
            ),
        closeResources = {
            httpClient.close()
            databaseDriver.close()
        },
    )
}

private fun createProductDatabaseDriver(context: Context): AndroidSqliteDriver =
    AndroidSqliteDriver(
        schema = ProductDatabase.Schema,
        context = context.applicationContext,
        name = DATABASE_NAME,
    )

private const val DATABASE_NAME = "product-search.db"
private const val REQUEST_TIMEOUT_MILLIS = 10_000L
private const val SEARCH_A_LICIOUS_ORIGIN = "https://search.openfoodfacts.org"
