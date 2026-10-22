package com.example.nutricionalregister.feature.assistant.testing

import com.example.nutricionalregister.domain.product.ConnectivityStatus
import com.example.nutricionalregister.domain.product.ConnectivityStatusProvider

/**
 * A [ConnectivityStatusProvider] that always reports the same status.
 */
internal class FakeConnectivityStatusProvider(
    private val status: ConnectivityStatus,
) : ConnectivityStatusProvider {
    /** How many times the connectivity was asked for. */
    var readCount: Int = 0
        private set

    override suspend fun currentStatus(): ConnectivityStatus {
        readCount += 1
        return status
    }
}
