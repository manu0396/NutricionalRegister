package com.example.nutricionalregister.feature.savedproducts

import com.example.nutricionalregister.domain.product.EpochClock
import com.example.nutricionalregister.domain.product.Product
import com.example.nutricionalregister.domain.purchase.SavedProduct
import com.example.nutricionalregister.domain.purchase.SavedProductRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Presentation logic for the Saved products destination (FR-001 to FR-004).
 *
 * Saving and removing are single actions that write straight to the repository and
 * update the exposed state immediately, so the saved flag flips with no network
 * call and no second step (SC-001). The list mirrors the repository's
 * `observeSaved` stream, so a change made on any surface (the detail view) is
 * reflected here without a manual refresh. The view model is deliberately
 * repository-driven: it holds no cache of its own, so it cannot drift from storage.
 *
 * @param scope the lifecycle scope that owns the collection; mirroring
 *   `ProductSearchViewModel`, the caller clears it on dispose.
 */
public class SavedProductsViewModel(
    private val savedProductRepository: SavedProductRepository,
    private val clock: EpochClock,
    private val scope: CoroutineScope,
) {
    private val mutableState = MutableStateFlow(SavedProductsState())
    public val state: StateFlow<SavedProductsState> = mutableState.asStateFlow()

    private var observeJob: Job? = null

    init {
        observeSaved()
    }

    /**
     * Saves [product], stamping the save time from [clock]. Saving a product that is
     * already saved is an idempotent upsert, so the list never gains a duplicate
     * (FR-004). The saved flag updates immediately in this single action (SC-001).
     */
    public fun save(product: Product) {
        scope.launch {
            savedProductRepository.save(
                SavedProduct.fromProduct(product, savedAtEpochMillis = clock.nowEpochMillis()),
            )
        }
    }

    /** Removes the saved product with [code] in a single action (FR-003). */
    public fun remove(code: String) {
        scope.launch {
            savedProductRepository.remove(code)
        }
    }

    /** Opens the detail view for the saved product with [code] (FR-007). */
    public fun openProduct(code: String) {
        val saved = mutableState.value.saved.firstOrNull { it.code == code } ?: return
        mutableState.update { it.copy(selectedProduct = saved.toProduct()) }
    }

    /** Closes the detail view, returning to the saved list (FR-007). */
    public fun closeProductDetails() {
        mutableState.update { it.copy(selectedProduct = null) }
    }

    /** Stops the collection; called when the destination's dependencies are closed. */
    public fun clear() {
        observeJob?.cancel()
        observeJob = null
    }

    private fun observeSaved() {
        observeJob?.cancel()
        observeJob =
            scope.launch {
                savedProductRepository.observeSaved().collect { saved ->
                    mutableState.update {
                        it.copy(
                            saved = saved,
                            savedCodes = saved.mapTo(mutableSetOf()) { product -> product.code },
                            isLoading = false,
                        )
                    }
                }
            }
    }
}

private fun SavedProduct.toProduct(): Product =
    Product(
        code = code,
        name = name,
        brands = brands,
        quantity = quantity,
        nutriScoreGrade = nutriScoreGrade,
        imageThumbnailUrl = imageThumbnailUrl,
    )
