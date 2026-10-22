package com.example.nutricionalregister.feature.search

import com.example.nutricionalregister.domain.product.Product
import com.example.nutricionalregister.domain.product.ProductSearchRepository
import com.example.nutricionalregister.domain.purchase.SavedProduct
import com.example.nutricionalregister.domain.purchase.SavedProductRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * The state of the product detail route.
 */
public sealed interface ProductDetailState {
    /** The product is being read. */
    public data object Loading : ProductDetailState

    /** The product was found. */
    public data class Ready(
        public val product: Product,
    ) : ProductDetailState

    /** The product is not on the device and is not in the catalogue cache. */
    public data object NotFound : ProductDetailState
}

/**
 * Presentation logic for the product detail route.
 *
 * The detail is a **route**, not a piece of shared screen state: the view model
 * is created for one product code and is discarded with the route. Nothing about
 * "which product is open" is stored globally, so leaving an area can never leave
 * a stale detail behind, which is what caused the reported navigation defect.
 *
 * The product is read from the catalogue cache first, so it works offline once
 * the food has been seen. A product that is only in the user's library falls
 * back to the saved record, so a saved food always opens something.
 *
 * @param scope the lifecycle scope that owns the load; the caller clears it on
 *   dispose.
 */
public class ProductDetailViewModel(
    private val productSearchRepository: ProductSearchRepository,
    private val savedProductRepository: SavedProductRepository,
    private val scope: CoroutineScope,
) {
    private val mutableState = MutableStateFlow<ProductDetailState>(ProductDetailState.Loading)
    public val state: StateFlow<ProductDetailState> = mutableState.asStateFlow()

    private var loadJob: Job? = null

    /** Loads the product with [code]; a new call replaces the one in flight. */
    public fun load(code: String) {
        if (code.isBlank()) {
            mutableState.value = ProductDetailState.NotFound
            return
        }
        loadJob?.cancel()
        mutableState.value = ProductDetailState.Loading
        loadJob =
            scope.launch {
                val product = productSearchRepository.getProduct(code) ?: savedProduct(code)
                mutableState.value =
                    if (product != null) {
                        ProductDetailState.Ready(product)
                    } else {
                        ProductDetailState.NotFound
                    }
            }
    }

    /** Stops the load in flight; called when the route is left. */
    public fun clear() {
        loadJob?.cancel()
        loadJob = null
    }

    private suspend fun savedProduct(code: String): Product? =
        savedProductRepository
            .observeSaved()
            .first()
            .firstOrNull { saved -> saved.code == code }
            ?.toProduct()

    private fun SavedProduct.toProduct(): Product =
        Product(
            code = code,
            name = name,
            brands = brands,
            quantity = quantity,
            nutriScoreGrade = nutriScoreGrade,
            imageThumbnailUrl = imageThumbnailUrl,
        )
}
