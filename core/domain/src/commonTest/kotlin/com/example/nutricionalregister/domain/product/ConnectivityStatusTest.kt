package com.example.nutricionalregister.domain.product

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ConnectivityStatusTest {
    @Test
    fun connectivityStatusCoversOnlineOfflineAndUnknown() {
        // WHEN every declared connectivity status is read
        // THEN the three known states are available in declaration order
        assertEquals(3, ConnectivityStatus.entries.size)
        assertEquals(ConnectivityStatus.ONLINE, ConnectivityStatus.entries[0])
        assertEquals(ConnectivityStatus.OFFLINE, ConnectivityStatus.entries[1])
        assertEquals(ConnectivityStatus.UNKNOWN, ConnectivityStatus.entries[2])
    }

    @Test
    fun connectivityStatusProviderReportsTheCurrentStatus() =
        runTest {
            // GIVEN a provider fixed to a status
            val provider = ConnectivityStatusProvider { ConnectivityStatus.OFFLINE }

            // WHEN it is queried
            val status = provider.currentStatus()

            // THEN it reports that status
            assertEquals(ConnectivityStatus.OFFLINE, status)
        }
}
