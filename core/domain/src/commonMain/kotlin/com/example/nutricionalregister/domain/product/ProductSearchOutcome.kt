package com.example.nutricionalregister.domain.product

public data class ProductSearchSnapshot(
    val term: SearchTerm,
    val products: List<Product>,
    val fetchedAtEpochMillis: Long,
)

public enum class ProductSearchFailure {
    OFFLINE,
    SERVICE,
    INVALID_RESPONSE,
}

public sealed interface ProductSearchOutcome {
    public data class Cached(
        val snapshot: ProductSearchSnapshot?,
    ) : ProductSearchOutcome

    public data class Success(
        val snapshot: ProductSearchSnapshot,
    ) : ProductSearchOutcome

    public data class Failure(
        val reason: ProductSearchFailure,
        val cachedSnapshot: ProductSearchSnapshot?,
    ) : ProductSearchOutcome
}
