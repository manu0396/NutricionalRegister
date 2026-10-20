package com.example.nutricionalregister.feature.search

import com.example.nutricionalregister.domain.product.Product
import com.example.nutricionalregister.domain.product.RecentSearch

public enum class SearchPhase {
    INITIAL,
    LOADING,
    REFRESHING,
    RESULTS,
    NO_RESULTS,
    OFFLINE_WITH_CACHE,
    OFFLINE_WITHOUT_CACHE,
    ERROR_WITH_CACHE,
    ERROR_WITHOUT_CACHE,
}

public data class ProductSearchState(
    val query: String = "",
    val phase: SearchPhase = SearchPhase.INITIAL,
    val products: List<Product> = emptyList(),
    val selectedProduct: Product? = null,
    val recentSearches: List<RecentSearch> = emptyList(),
    val isRefreshing: Boolean = false,
)
