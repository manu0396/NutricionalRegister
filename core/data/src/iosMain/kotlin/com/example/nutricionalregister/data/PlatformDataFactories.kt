package com.example.nutricionalregister.data

import app.cash.sqldelight.driver.native.NativeSqliteDriver
import com.example.nutricionalregister.domain.product.EpochClock
import com.example.nutricionalregister.domain.purchase.PurchaseClient
import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin
import io.ktor.client.plugins.HttpTimeout
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

internal fun createProductDatabase(): ProductDatabase = ProductDatabase(createProductDatabaseDriver())

internal fun createProductHttpClient(): HttpClient =
    HttpClient(Darwin) {
        install(HttpTimeout) {
            requestTimeoutMillis = REQUEST_TIMEOUT_MILLIS
        }
    }

/**
 * Builds the shared runtime on iOS and wires the saved-products and purchase
 * repositories over the same database. The platform [purchaseClient] is supplied
 * by the iOS app (the Swift `StoreKitPurchaseClient` conforming to the exported
 * `SharedPurchaseClient` protocol), so no StoreKit call lives in this source set
 * (ADR-0013).
 */
public fun createProductSearchRuntime(
    clock: EpochClock,
    purchaseClient: PurchaseClient,
    databaseDispatcher: CoroutineDispatcher = Dispatchers.Default,
): ProductSearchRuntime {
    val httpClient = createProductHttpClient()
    val databaseDriver = createProductDatabaseDriver()
    val database = ProductDatabase(databaseDriver)
    val dayBoundaryProvider = SystemDayBoundaryProvider()
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
        savedProductRepository =
            SavedProductRepositoryImpl(
                database = database,
                databaseDispatcher = databaseDispatcher,
            ),
        purchaseRepository =
            PurchaseRepositoryImpl(
                database = database,
                client = purchaseClient,
                databaseDispatcher = databaseDispatcher,
            ),
        diaryRepository =
            DiaryRepositoryImpl(
                database = database,
                dayBoundaryProvider = dayBoundaryProvider,
                databaseDispatcher = databaseDispatcher,
            ),
        nutritionGoalRepository =
            NutritionGoalRepositoryImpl(
                database = database,
                databaseDispatcher = databaseDispatcher,
            ),
        dayBoundaryProvider = dayBoundaryProvider,
        closeResources = {
            httpClient.close()
            databaseDriver.close()
        },
    )
}

private fun createProductDatabaseDriver(): NativeSqliteDriver =
    NativeSqliteDriver(
        schema = ProductDatabase.Schema,
        name = DATABASE_NAME,
    )

private const val DATABASE_NAME = "product-search.db"
private const val REQUEST_TIMEOUT_MILLIS = 10_000L
private const val SEARCH_A_LICIOUS_ORIGIN = "https://search.openfoodfacts.org"
