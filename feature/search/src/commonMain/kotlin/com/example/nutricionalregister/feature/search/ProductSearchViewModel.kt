package com.example.nutricionalregister.feature.search

import com.example.nutricionalregister.domain.product.ConnectivityStatus
import com.example.nutricionalregister.domain.product.ConnectivityStatusProvider
import com.example.nutricionalregister.domain.product.EpochClock
import com.example.nutricionalregister.domain.product.ProductSearchFailure
import com.example.nutricionalregister.domain.product.ProductSearchOutcome
import com.example.nutricionalregister.domain.product.ProductSearchRepository
import com.example.nutricionalregister.domain.product.RecentSearchRepository
import com.example.nutricionalregister.domain.product.SearchTerm
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

public class ProductSearchViewModel(
    private val productSearchRepository: ProductSearchRepository,
    private val recentSearchRepository: RecentSearchRepository,
    private val clock: EpochClock,
    private val scope: CoroutineScope,
    private val connectivityStatusProvider: ConnectivityStatusProvider =
        ConnectivityStatusProvider { ConnectivityStatus.UNKNOWN },
) {
    private val mutableState = MutableStateFlow(ProductSearchState())
    private var searchJob: Job? = null
    private var historyJob: Job? = null
    private var requestIdentity: Long = 0L

    public val state: StateFlow<ProductSearchState> = mutableState.asStateFlow()

    init {
        historyJob =
            scope.launch {
                recentSearchRepository.observeRecentSearches().collect { searches ->
                    mutableState.update { it.copy(recentSearches = searches) }
                }
            }
    }

    public fun onQueryChanged(query: String) {
        requestIdentity += 1
        searchJob?.cancel()
        mutableState.update {
            it.copy(
                query = query,
                phase = SearchPhase.INITIAL,
                products = emptyList(),
                selectedProduct = null,
                isRefreshing = false,
            )
        }
    }

    public fun submitSearch() {
        val term = SearchTerm.fromInput(mutableState.value.query) ?: return
        val currentRequest = ++requestIdentity
        searchJob?.cancel()
        mutableState.update {
            it.copy(
                query = term.displayTerm,
                phase = SearchPhase.LOADING,
                products = emptyList(),
                selectedProduct = null,
                isRefreshing = true,
            )
        }
        searchJob =
            scope.launch {
                var historyRecorded = false
                productSearchRepository.search(term).collect { outcome ->
                    if (currentRequest != requestIdentity) return@collect
                    when (outcome) {
                        is ProductSearchOutcome.Cached -> {
                            val snapshot = outcome.snapshot
                            mutableState.update {
                                it.copy(
                                    products = snapshot?.products.orEmpty(),
                                    phase = SearchPhase.REFRESHING,
                                    isRefreshing = true,
                                )
                            }
                        }
                        is ProductSearchOutcome.Success -> {
                            mutableState.update {
                                it.copy(
                                    products = outcome.snapshot.products,
                                    phase =
                                        if (outcome.snapshot.products.isEmpty()) {
                                            SearchPhase.NO_RESULTS
                                        } else {
                                            SearchPhase.RESULTS
                                        },
                                    isRefreshing = false,
                                )
                            }
                            if (!historyRecorded) {
                                recentSearchRepository.recordCompletedSearch(term, clock.nowEpochMillis())
                                historyRecorded = true
                            }
                        }
                        is ProductSearchOutcome.Failure -> {
                            val reason = effectiveFailure(outcome.reason)
                            val cachedProducts = outcome.cachedSnapshot?.products.orEmpty()
                            mutableState.update {
                                it.copy(
                                    products = cachedProducts,
                                    phase = phaseForFailure(reason, cachedProducts.isNotEmpty()),
                                    isRefreshing = false,
                                )
                            }
                            if (!historyRecorded) {
                                recentSearchRepository.recordCompletedSearch(term, clock.nowEpochMillis())
                                historyRecorded = true
                            }
                        }
                    }
                }
            }
    }

    public fun retrySearch() {
        submitSearch()
    }

    public fun selectRecentSearch(normalizedTerm: String) {
        val recent = mutableState.value.recentSearches.firstOrNull { it.normalizedTerm == normalizedTerm } ?: return
        onQueryChanged(recent.displayTerm)
        submitSearch()
    }

    public fun deleteRecentSearch(normalizedTerm: String) {
        scope.launch {
            recentSearchRepository.deleteRecentSearch(normalizedTerm)
        }
    }

    public fun openProduct(code: String) {
        val currentRequest = requestIdentity
        val selected = mutableState.value.products.firstOrNull { it.code == code }
        if (selected != null) {
            mutableState.update { it.copy(selectedProduct = selected) }
            return
        }
        scope.launch {
            productSearchRepository.getProduct(code)?.let { product ->
                if (currentRequest == requestIdentity) {
                    mutableState.update { it.copy(selectedProduct = product) }
                }
            }
        }
    }

    public fun closeProductDetails() {
        mutableState.update { it.copy(selectedProduct = null) }
    }

    public fun clear() {
        searchJob?.cancel()
        historyJob?.cancel()
    }

    private suspend fun effectiveFailure(reason: ProductSearchFailure): ProductSearchFailure =
        if (reason == ProductSearchFailure.SERVICE &&
            connectivityStatusProvider.currentStatus() == ConnectivityStatus.OFFLINE
        ) {
            ProductSearchFailure.OFFLINE
        } else {
            reason
        }

    private fun phaseForFailure(
        reason: ProductSearchFailure,
        hasCachedProducts: Boolean,
    ): SearchPhase =
        when (reason) {
            ProductSearchFailure.OFFLINE ->
                if (hasCachedProducts) SearchPhase.OFFLINE_WITH_CACHE else SearchPhase.OFFLINE_WITHOUT_CACHE
            ProductSearchFailure.SERVICE, ProductSearchFailure.INVALID_RESPONSE ->
                if (hasCachedProducts) SearchPhase.ERROR_WITH_CACHE else SearchPhase.ERROR_WITHOUT_CACHE
        }
}
