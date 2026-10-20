package com.example.nutricionalregister.data

import com.example.nutricionalregister.data.remote.MalformedCatalogueResponseException
import com.example.nutricionalregister.data.remote.SearchAliciousException
import com.example.nutricionalregister.domain.product.EpochClock
import com.example.nutricionalregister.domain.product.NutritionBasis
import com.example.nutricionalregister.domain.product.NutritionValue
import com.example.nutricionalregister.domain.product.Product
import com.example.nutricionalregister.domain.product.ProductCatalogue
import com.example.nutricionalregister.domain.product.ProductSearchFailure
import com.example.nutricionalregister.domain.product.ProductSearchOutcome
import com.example.nutricionalregister.domain.product.ProductSearchRepository
import com.example.nutricionalregister.domain.product.ProductSearchSnapshot
import com.example.nutricionalregister.domain.product.SearchTerm
import io.ktor.client.plugins.HttpRequestTimeoutException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.io.IOException
import kotlinx.serialization.SerializationException

public class ProductSearchRepositoryImpl(
    private val database: ProductDatabase,
    private val catalogue: ProductCatalogue,
    private val clock: EpochClock,
    private val databaseDispatcher: CoroutineDispatcher = Dispatchers.Default,
) : ProductSearchRepository {
    private val requestMutex = Mutex()
    private val latestRequestByTerm = mutableMapOf<String, Long>()
    private var nextRequestId: Long = 0

    override fun search(term: SearchTerm): Flow<ProductSearchOutcome> =
        flow {
            val requestId =
                requestMutex.withLock {
                    nextRequestId += 1
                    latestRequestByTerm[term.normalizedTerm] = nextRequestId
                    nextRequestId
                }
            val cachedSnapshot = withContext(databaseDispatcher) { loadSnapshot(term) }
            emit(ProductSearchOutcome.Cached(cachedSnapshot))

            try {
                val products = catalogue.search(term)
                val snapshot =
                    ProductSearchSnapshot(
                        term = term,
                        products = products.distinctBy(Product::code),
                        fetchedAtEpochMillis = clock.nowEpochMillis(),
                    )
                val shouldCommit =
                    requestMutex.withLock {
                        latestRequestByTerm[term.normalizedTerm] == requestId
                    }
                if (!shouldCommit) return@flow

                withContext(databaseDispatcher) { persistSnapshot(snapshot) }
                emit(ProductSearchOutcome.Success(snapshot))
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: SearchAliciousException) {
                if (isLatest(term, requestId)) {
                    emit(
                        ProductSearchOutcome.Failure(
                            reason = ProductSearchFailure.SERVICE,
                            cachedSnapshot = cachedSnapshot,
                        ),
                    )
                }
            } catch (exception: MalformedCatalogueResponseException) {
                if (isLatest(term, requestId)) {
                    emit(
                        ProductSearchOutcome.Failure(
                            reason = ProductSearchFailure.INVALID_RESPONSE,
                            cachedSnapshot = cachedSnapshot,
                        ),
                    )
                }
            } catch (exception: SerializationException) {
                if (isLatest(term, requestId)) {
                    emit(
                        ProductSearchOutcome.Failure(
                            reason = ProductSearchFailure.INVALID_RESPONSE,
                            cachedSnapshot = cachedSnapshot,
                        ),
                    )
                }
            } catch (exception: HttpRequestTimeoutException) {
                if (isLatest(term, requestId)) {
                    emit(
                        ProductSearchOutcome.Failure(
                            reason = ProductSearchFailure.SERVICE,
                            cachedSnapshot = cachedSnapshot,
                        ),
                    )
                }
            } catch (exception: IOException) {
                if (isLatest(term, requestId)) {
                    emit(
                        ProductSearchOutcome.Failure(
                            reason = ProductSearchFailure.OFFLINE,
                            cachedSnapshot = cachedSnapshot,
                        ),
                    )
                }
            }
        }

    override suspend fun getProduct(code: String): Product? =
        withContext(databaseDispatcher) {
            val row = database.productSearchQueries.selectProduct(code).executeAsOneOrNull() ?: return@withContext null
            row.toDomainProduct(database)
        }

    private suspend fun isLatest(
        term: SearchTerm,
        requestId: Long,
    ): Boolean =
        requestMutex.withLock {
            latestRequestByTerm[term.normalizedTerm] == requestId
        }

    private fun loadSnapshot(term: SearchTerm): ProductSearchSnapshot? {
        val row =
            database.productSearchQueries
                .selectSnapshot(term.normalizedTerm)
                .executeAsOneOrNull() ?: return null
        val products =
            database.productSearchQueries
                .selectProductsForTerm(term.normalizedTerm)
                .executeAsList()
                .map { it.toDomainProduct(database) }
        return ProductSearchSnapshot(
            term = SearchTerm(row.normalized_term, row.display_term),
            products = products,
            fetchedAtEpochMillis = row.fetched_at_epoch_millis,
        )
    }

    private fun persistSnapshot(snapshot: ProductSearchSnapshot) {
        val queries = database.productSearchQueries
        database.transaction {
            snapshot.products.forEach { product ->
                val existing = queries.selectProduct(product.code).executeAsOneOrNull()
                if (existing == null) {
                    queries.insertProduct(
                        code = product.code,
                        name = product.name,
                        brands = product.brands,
                        quantity = product.quantity,
                        nutrition_data_per = product.nutritionDataPer,
                        nutri_score_grade = product.nutriScoreGrade,
                        cached_at_epoch_millis = snapshot.fetchedAtEpochMillis,
                    )
                } else {
                    queries.updateProduct(
                        name = product.name,
                        brands = product.brands,
                        quantity = product.quantity,
                        nutrition_data_per = product.nutritionDataPer,
                        nutri_score_grade = product.nutriScoreGrade,
                        cached_at_epoch_millis = snapshot.fetchedAtEpochMillis,
                        code = product.code,
                    )
                }
                queries.deleteNutritionForProduct(product.code)
                product.nutritionValues.forEach { nutrition ->
                    queries.upsertNutritionValue(
                        product_code = product.code,
                        nutrient_key = nutrition.nutrientKey,
                        amount = nutrition.amount,
                        unit = nutrition.unit,
                        basis = nutrition.basis.name,
                    )
                }
            }

            val existingSnapshot = queries.selectSnapshot(snapshot.term.normalizedTerm).executeAsOneOrNull()
            if (existingSnapshot == null) {
                queries.insertSnapshot(
                    normalized_term = snapshot.term.normalizedTerm,
                    display_term = snapshot.term.displayTerm,
                    fetched_at_epoch_millis = snapshot.fetchedAtEpochMillis,
                )
            } else {
                queries.updateSnapshot(
                    display_term = snapshot.term.displayTerm,
                    fetched_at_epoch_millis = snapshot.fetchedAtEpochMillis,
                    normalized_term = snapshot.term.normalizedTerm,
                )
            }
            queries.deleteSnapshotProducts(snapshot.term.normalizedTerm)
            snapshot.products.forEachIndexed { position, product ->
                queries.insertSnapshotProduct(
                    normalized_term = snapshot.term.normalizedTerm,
                    product_code = product.code,
                    result_position = position.toLong(),
                )
            }
        }
    }
}

private fun com.example.nutricionalregister.data.Product.toDomainProduct(database: ProductDatabase): Product {
    val nutrition =
        database.productSearchQueries
            .selectNutritionForProduct(code)
            .executeAsList()
            .mapNotNull { row ->
                val basis = NutritionBasis.entries.firstOrNull { it.name == row.basis } ?: NutritionBasis.UNKNOWN
                if (row.nutrient_key.isBlank() || !row.amount.isFinite()) {
                    null
                } else {
                    NutritionValue(
                        nutrientKey = row.nutrient_key,
                        amount = row.amount,
                        unit = row.unit,
                        basis = basis,
                    )
                }
            }
    return Product(
        code = code,
        name = name,
        brands = brands,
        quantity = quantity,
        nutritionDataPer = nutrition_data_per,
        nutriScoreGrade = nutri_score_grade,
        nutritionValues = nutrition,
    )
}
