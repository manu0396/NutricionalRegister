package com.example.nutricionalregister.domain.product

public enum class ConnectivityStatus {
    ONLINE,
    OFFLINE,
    UNKNOWN,
}

public fun interface ConnectivityStatusProvider {
    public suspend fun currentStatus(): ConnectivityStatus
}
